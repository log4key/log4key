/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.worker;

import com.log4key.channel.FileChannel;
import com.log4key.channel.FileChannelManager;
import com.log4key.internal.InternalLogger;
import com.log4key.metrics.LogMetrics;
import com.log4key.path.PathKey;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

/**
 * 单线程 Worker，消费 Mailbox 中的写入任务，管理 FileChannelManager。
 *
 * 每个 Worker 持有独立的 Mailbox 和 FileChannelManager，串行处理投递的日志写入任务。
 * 主循环中轮询 Mailbox，执行 Runnable 任务，定期执行 idle 扫描。
 */
public class Worker implements Runnable {

    private static final InternalLogger logger = InternalLogger.getLogger(Worker.class);

    /** Worker 编号 */
    private final int workerId;

    /** 当前 Worker 的 Mailbox */
    private final Mailbox mailbox;

    /** FileChannel 生命周期管理器 */
    private final FileChannelManager channelManager;

    /** Worker 线程 */
    private volatile Thread workerThread;

    /** 运行标记 */
    private volatile boolean running;

    /** 停止标记 */
    private volatile boolean stopped;

    /** Flush 字节阈值 */
    private final long batchSize;

    /** Flush 时间间隔（毫秒） */
    private final long flushIntervalMs;

    /** Buffer 扩容回收阈值 */
    private final long highWaterMark;

    /** StringBuilder 初始容量 */
    private final int initialBufferSize;

    /** Idle 扫描间隔（默认 10 秒） */
    private static final long IDLE_SCAN_INTERVAL_MS = 10000L;

    /** 上次 idle 扫描时间 */
    private long lastIdleScanTime;

    /**
     * 构造 Worker 实例。
     *
     * @param workerId          Worker 编号
     * @param mailbox           Mailbox 实例
     * @param channelManager    FileChannelManager 实例
     * @param batchSize         Flush 字节阈值
     * @param flushIntervalMs   Flush 时间间隔（毫秒）
     * @param highWaterMark     Buffer 扩容回收阈值
     * @param initialBufferSize StringBuilder 初始容量
     */
    public Worker(int workerId, Mailbox mailbox, FileChannelManager channelManager,
                  long batchSize, long flushIntervalMs, long highWaterMark, int initialBufferSize) {
        this.workerId = workerId;
        this.mailbox = mailbox;
        this.channelManager = channelManager;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.highWaterMark = highWaterMark;
        this.initialBufferSize = initialBufferSize;
        this.lastIdleScanTime = System.currentTimeMillis();
    }

    /**
     * 启动 Worker 线程。
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;
        stopped = false;
        workerThread = new Thread(this, "log4key-worker-" + workerId);
        workerThread.setDaemon(true);
        workerThread.start();
        logger.debug("Worker {} started", workerId);
    }

    /**
     * Worker 主循环。
     */
    @Override
    public void run() {
        logger.debug("Worker {} event loop started", workerId);

        while (running) {
            try {
                // 1. 轮询 Mailbox
                Runnable task = mailbox.poll();
                if (task == null) {
                    // 队列为空，短暂休眠避免空转
                    // 替换掉 Thread.sleep(1)，并处理中断
                    LockSupport.parkNanos(1_000_000L); // 1ms，但实际精度更高
                    // parkNanos 不抛 InterruptedException，需要检查中断标志
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    continue;
                }

                // 2. 记录消费统计
                LogMetrics.recordConsumed();

                // 3. 处理写入任务（包含内部 flush 逻辑）
                processTask(task);

                // 4. 定期执行 idle 扫描
                long now = System.currentTimeMillis();
                if (now - lastIdleScanTime >= IDLE_SCAN_INTERVAL_MS) {
                    channelManager.idleScan();
                    lastIdleScanTime = now;
                }
            } catch (Exception e) {
                logger.warn("Worker {} event loop error: {}", workerId, e.getMessage());
            }
        }

        logger.debug("Worker {} event loop stopped", workerId);
    }

    /**
     * 处理写入任务。
     *
     * 投递到 Mailbox 的 Runnable 在 Worker 线程中执行，内部完成：
     * FileChannel.getOrCreate → channel.append → shouldWrite/write → shouldFlush/flush。
     *
     * @param task 写入任务
     */
    private void processTask(Runnable task) {
        try {
            // WriteTask 内部已封装所有写入逻辑，这里直接执行
            task.run();
        } catch (Exception e) {
            logger.error("Worker {} failed to execute write task: {}", workerId, e.getMessage());
        }
    }

    /**
     * 处理单个日志写入操作（供 WriteTask 内部调用）。
     *
     * 三阶段：append → write → flush。
     * append 将格式化消息追加到 StringBuilder 缓冲区；
     * write（batchSize 触发）将缓冲区内容编码写入 BufferedWriter（不刷盘）；
     * flush（flushInterval 或 highWaterMark 触发）将 BufferedWriter 刷入 OS Page Cache。
     *
     * @param pathKey          路径键
     * @param formattedMessage 已格式化的日志消息
     * @throws IOException 如果写入失败
     */
    public void doWrite(PathKey pathKey, String formattedMessage) throws IOException {
        // 获取或创建 FileChannel
        FileChannel channel = channelManager.getOrCreate(pathKey);

        // 追加到缓冲区
        channel.append(formattedMessage);

        // 阶段1: batchSize 触发 → write() 编码写入 BufferedWriter（不刷盘）
        if (channel.shouldWrite(batchSize)) {
            channel.write(highWaterMark, initialBufferSize);
        }

        // 阶段2: flushInterval 或 highWaterMark 触发 → flush() 刷入 OS Page Cache
        if (channel.shouldFlush(flushIntervalMs, highWaterMark)) {
            channel.flush();
        }
    }

    /**
     * 停止 Worker（优雅关闭：处理完 Mailbox 剩余事件后关闭）。
     */
    public void shutdown() {
        running = false;
        if (workerThread != null) {
            try {
                workerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        channelManager.closeAll();
        stopped = true;
        logger.debug("Worker {} shutdown completed", workerId);
    }

    /**
     * 立即停止 Worker（不处理剩余事件）。
     */
    public void shutdownNow() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        channelManager.closeAll();
        stopped = true;
        logger.debug("Worker {} shutdownNow completed", workerId);
    }

    /**
     * 判断 Worker 是否已停止。
     *
     * @return true 如果已停止
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * 获取 Mailbox 实例。
     *
     * @return mailbox
     */
    public Mailbox getMailbox() {
        return mailbox;
    }

    /**
     * 获取 Worker 线程（供 awaitTermination 使用）。
     *
     * @return workerThread
     */
    public Thread getWorkerThread() {
        return workerThread;
    }
}