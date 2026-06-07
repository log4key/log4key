/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.worker;

import com.log4key.channel.FileChannelManager;
import com.log4key.internal.InternalLogger;
import com.log4key.mailbox.Mailbox;
import com.log4key.path.PathKey;
import com.log4key.util.LogExecutor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * WorkerGroup 实现 LogExecutor 接口，管理多个 Worker 实例。
 *
 * 每个 Worker 持有独立的 Mailbox 和 FileChannelManager。
 * execute() 不做路由，workerId 由 FileAppender.shard(PathKey) 计算后传入。
 * 可直接替换旧执行器嵌入 ExecutorController。
 */
public class WorkerGroup implements LogExecutor {

    private static final InternalLogger logger = InternalLogger.getLogger(WorkerGroup.class);

    /** Worker 数量（由 executor.threads 配置，默认 4） */
    private final int workerCount;

    /** Mailbox 数组 */
    private final Mailbox[] mailboxes;

    /** Worker 数组 */
    private final Worker[] workers;

    /** 关闭标记 */
    private volatile boolean shutdown;

    /** 终止标记 */
    private volatile boolean terminated;

    /** Flush 参数 */
    private final long batchSize;
    private final long flushIntervalMs;
    private final long highWaterMark;
    private final int initialBufferSize;

    /** FD 上限配置 */
    private final int maxFileWriters;

    /** 空闲超时（毫秒） */
    private final long idleTimeoutMs;

    /** 最大文件大小（字节） */
    private final long maxFileSize;

    /** 字符编码 */
    private final String charset;

    /**
     * 构造 WorkerGroup 实例。
     *
     * @param workerCount       Worker 数量
     * @param queueCapacity     Mailbox RingBuffer 容量
     * @param maxFileWriters    最大文件写入器数（作为 FD 上限）
     * @param idleTimeoutMs     空闲超时（毫秒）
     * @param batchSize         Flush 字节阈值
     * @param flushIntervalMs   Flush 时间间隔（毫秒）
     * @param highWaterMark     Buffer 扩容回收阈值
     * @param initialBufferSize StringBuilder 初始容量
     * @param maxFileSize       最大文件大小（字节）
     * @param charset           字符编码
     */
    public WorkerGroup(int workerCount, int queueCapacity, int maxFileWriters,
                       long idleTimeoutMs, long batchSize, long flushIntervalMs,
                       long highWaterMark, int initialBufferSize, long maxFileSize,
                       String charset) {
        // workerCount 非 2 的幂时自动修正并告警
        if ((workerCount & (workerCount - 1)) != 0) {
            int corrected = roundUpToPowerOfTwo(workerCount);
            logger.warn("workerCount {} 不是 2 的幂，已自动修正为 {}", workerCount, corrected);
            this.workerCount = corrected;
        } else {
            this.workerCount = workerCount;
        }

        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.highWaterMark = highWaterMark;
        this.initialBufferSize = initialBufferSize;
        this.maxFileWriters = maxFileWriters;
        this.idleTimeoutMs = idleTimeoutMs;
        this.maxFileSize = maxFileSize;
        this.charset = charset;

        this.mailboxes = new Mailbox[this.workerCount];
        this.workers = new Worker[this.workerCount];

        // 计算每个 Worker 的 FD 上限
        int perWorkerLimit = FileChannelManager.calculatePerWorkerLimit(maxFileWriters, this.workerCount);

        // 创建 Mailbox 和 Worker
        for (int i = 0; i < this.workerCount; i++) {
            mailboxes[i] = new Mailbox(queueCapacity);

            FileChannelManager channelManager = new FileChannelManager(
                    perWorkerLimit, idleTimeoutMs, batchSize, flushIntervalMs,
                    highWaterMark, maxFileSize, charset, initialBufferSize);

            workers[i] = new Worker(i, mailboxes[i], channelManager,
                    batchSize, flushIntervalMs, highWaterMark, initialBufferSize);
        }
    }

    /**
     * 启动所有 Worker 线程。
     */
    public void start() {
        for (Worker worker : workers) {
            worker.start();
        }
        logger.debug("WorkerGroup started, workerCount={}", workerCount);
    }

    /**
     * 停止所有 Worker 线程。
     */
    public void stop() {
        for (Worker worker : workers) {
            worker.shutdown();
        }
        logger.debug("WorkerGroup stopped");
    }

    // ---- LogExecutor 接口实现 ----

    /**
     * 提交基于 key 的日志任务。
     *
     * workerId 由 FileAppender.shard(PathKey) 计算，WorkerGroup 不做二次路由。
     *
     * @param key  workerId 字符串（由 FileAppender.shard() 计算）
     * @param task 要执行的任务
     * @return Future 对象
     */
    @Override
    public Future<?> submit(String key, Runnable task) {
        if (shutdown) {
            throw new IllegalStateException("WorkerGroup is shutdown");
        }

        int workerId = Integer.parseInt(key);
        CompletableFuture<Void> future = new CompletableFuture<>();

        Runnable wrappedTask = () -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };

        boolean offered = mailboxes[workerId].offer(wrappedTask);
        if (!offered) {
            future.completeExceptionally(new IllegalStateException("Mailbox full for worker " + workerId));
        }

        return future;
    }

    /**
     * 执行基于 key 的日志任务。
     *
     * 不做路由，直接投递到对应 Worker 的 Mailbox。
     *
     * @param key     workerId 字符串（由 FileAppender.shard() 计算）
     * @param command 要执行的命令
     */
    @Override
    public void execute(String key, Runnable command) {
        if (shutdown) {
            logger.warn("WorkerGroup is shutdown, task rejected");
            return;
        }

        int workerId = Integer.parseInt(key);
        boolean offered = mailboxes[workerId].offer(command);

        if (!offered) {
            // L3 背压：Mailbox 满，拒绝投递
            logger.warn("Mailbox full for worker {}, task rejected", workerId);
        }
    }

    /**
     * 执行写入任务（封装了 Worker.doWrite() 调用）。
     *
     * 由 FileAppender 通过 ExecutorController 间接调用，将格式化后的日志消息
     * 投递到对应 Worker 的 Mailbox 中异步执行写入。
     *
     * @param workerId          Worker 编号字符串
     * @param pathKey           路径键
     * @param formattedMessage  已格式化的日志消息
     */
    public void executeWrite(String workerId, PathKey pathKey, String formattedMessage) {
        if (shutdown) {
            logger.warn("WorkerGroup is shutdown, write task rejected");
            return;
        }
        int id = Integer.parseInt(workerId);
        boolean offered = mailboxes[id].offer(() -> {
            try {
                workers[id].doWrite(pathKey, formattedMessage);
            } catch (Exception e) {
                logger.error("Worker {} write error: {}", workerId, e.getMessage());
            }
        });
        if (!offered) {
            logger.warn("Mailbox full for worker {}, write task rejected", workerId);
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
        for (Worker worker : workers) {
            worker.shutdown();
        }
        terminated = true;
        logger.debug("WorkerGroup shutdown completed");
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> remaining = new ArrayList<>();
        for (Worker worker : workers) {
            // 收集未执行的任务
            Mailbox mailbox = worker.getMailbox();
            Runnable task;
            while ((task = mailbox.poll()) != null) {
                remaining.add(task);
            }
            worker.shutdownNow();
        }
        terminated = true;
        logger.debug("WorkerGroup shutdownNow completed, remaining tasks={}", remaining.size());
        return remaining;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return terminated || checkAllWorkersStopped();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for (Worker worker : workers) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }
            // 等待 Worker 线程结束
            Thread workerThread = worker.getWorkerThread();
            if (workerThread != null && workerThread.isAlive()) {
                workerThread.join(Math.min(remaining, 1000));
            }
        }
        return checkAllWorkersStopped();
    }

    // ---- 辅助方法 ----

    /**
     * 检查所有 Worker 是否已停止。
     *
     * @return true 如果全部停止
     */
    private boolean checkAllWorkersStopped() {
        for (Worker worker : workers) {
            if (!worker.isStopped()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取 Worker 数量。
     *
     * @return workerCount
     */
    public int getWorkerCount() {
        return workerCount;
    }

    /**
     * 获取指定 Worker 的 Mailbox（供测试使用）。
     *
     * @param workerId Worker 编号
     * @return Mailbox 实例
     */
    public Mailbox getMailbox(int workerId) {
        return mailboxes[workerId];
    }

    // ---- 静态方法 ----

    /**
     * 将数值向上取整到最近的 2 的幂。
     */
    static int roundUpToPowerOfTwo(int n) {
        if (n <= 1) {
            return 1;
        }
        if (n > (1 << 30)) {
            return 1 << 30;
        }
        n--;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }
}