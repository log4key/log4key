/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fallback log executor.
 *
 * 降级专用执行器。
 */
public class FallbackLogExecutor implements LogExecutor {

    /**
     * 单线程执行器
     */
    private final PausableLogExecutor executor;

    /**
     * 关闭状态标志
     */
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    /**
     * 最后一次事件接收时间
     */
    private final AtomicLong lastEventTime = new AtomicLong(System.currentTimeMillis());

    /**
     * 等待新事件的最大时间（毫秒）
     */
    private static final long MAX_WAIT_TIME = 5000;

    /**
     * 检查间隔时间（毫秒）
     */
    private static final long CHECK_INTERVAL = 100;

    /**
     * 是否已关闭
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Submits a key-based task.
     *
     * 提交基于主键的任务。
     *
     * @param key the task key / 任务主键
     * @param task the task / 任务
     * @return the Future object / Future对象
     */
    @Override
    public Future<?> submit(String key, Runnable task) {
        // 记录事件
        lastEventTime.set(System.currentTimeMillis());

        // 如果已关闭，直接在调用线程执行
        if (closed.get()) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }

        try {
            return executor.submit(task);
        } catch (RejectedExecutionException e) {
            // 执行器拒绝执行，在调用线程执行
            task.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Executes a key-based task.
     *
     * 执行基于主键的任务。
     *
     * @param key the task key / 任务主键
     * @param command the task / 任务
     */
    @Override
    public void execute(String key, Runnable command) {
        // 记录事件
        lastEventTime.set(System.currentTimeMillis());

        // 如果已关闭，直接在调用线程执行
        if (closed.get()) {
            command.run();
            return;
        }

        try {
            executor.execute(command);
        } catch (RejectedExecutionException e) {
            // 执行器拒绝执行，在调用线程执行
            command.run();
        }
    }

    @Override
    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            // 标记开始关闭流程
            isShuttingDown.set(true);

            // 等待直到可以安全关闭（5秒内没有新事件）
            awaitSafeShutdown();

            // 关闭执行器
            executor.shutdown();

            // 使用提供的gracefulShutdown逻辑
            while (!executor.isTerminated()) {
                try {
                    // 分批等待
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (closed.compareAndSet(false, true)) {
            // 立即关闭执行器，不等待
            return executor.shutdownNow();
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return closed.get();
    }

    @Override
    public boolean isTerminated() {
        return closed.get() && executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // 如果未关闭，先等待安全关闭时机
        if (!closed.get()) {
            awaitSafeShutdown(timeout, unit);
        }

        // 等待执行器终止
        return executor.awaitTermination(timeout, unit);
    }

    /**
     * 获取内部执行器实例
     *
     * @return 内部PausableLogExecutor实例
     */
    public PausableLogExecutor getExecutor() {
        return executor;
    }

    /**
     * 暂停执行器
     * 调用后，所有新任务将在beforeExecute方法中等待，直到调用resume()
     */
    public void pause() {
        executor.pause();
    }

    /**
     * 恢复执行器
     * 唤醒所有等待的任务，允许它们继续执行
     */
    public void resume() {
        executor.resume();
    }

    /**
     * 构造函数
     * 创建一个单线程的可暂停执行器，用于处理降级日志事件
     *
     * 默认配置：
     * - 核心线程数：1（单线程设计，减少资源占用）
     * - 最大线程数：1（单线程设计，确保日志事件按顺序处理）
     * - 线程存活时间：60秒（空闲线程60秒后回收）
     * - 工作队列：无界LinkedBlockingQueue（避免拒绝执行异常，确保所有日志事件都能被处理）
     * - 线程工厂：创建名为"log4key-fallback-executor"的守护线程
     * - 拒绝策略：CallerRunsPolicy（当队列满时，在调用线程中执行任务，作为最后的安全网）
     */
    public FallbackLogExecutor() {

        // 创建可暂停的单线程执行器
        this.executor = new PausableLogExecutor(
                1, // 核心线程数：1（单线程设计）
                1, // 最大线程数：1（单线程设计）
                60L, // 线程存活时间
                TimeUnit.SECONDS, // 时间单位
                new LinkedBlockingQueue<>(), // 无界队列，避免拒绝异常
                r -> {
                    Thread t = new Thread(r, "log4key-fallback-executor");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 使用CallerRunsPolicy作为最后的安全网
        );
    }

    /**
     * 等待直到可以安全关闭
     * 等待条件：5秒内没有新的事件
     *
     * @return 是否成功等待到安全关闭时机
     */
    private boolean awaitSafeShutdown() {
        long startTime = System.currentTimeMillis();
        long elapsedTime;

        // 循环等待，直到5秒内没有新事件或超过最大等待时间
        do {
            elapsedTime = System.currentTimeMillis() - startTime;
            long timeSinceLastEvent = System.currentTimeMillis() - lastEventTime.get();

            // 如果5秒内没有新事件，返回true
            if (timeSinceLastEvent >= MAX_WAIT_TIME) {
                return true;
            }

            // 否则等待一段时间后再次检查
            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (elapsedTime < MAX_WAIT_TIME);

        // 超过最大等待时间，返回true
        return true;
    }

    /**
     * 等待指定时间直到可以安全关闭
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 是否成功等待到安全关闭时机
     */
    private boolean awaitSafeShutdown(long timeout, TimeUnit unit) {
        long maxWaitMillis = unit.toMillis(timeout);
        long startTime = System.currentTimeMillis();
        long elapsedTime;

        // 循环等待，直到5秒内没有新事件或超过指定超时时间
        do {
            elapsedTime = System.currentTimeMillis() - startTime;
            long timeSinceLastEvent = System.currentTimeMillis() - lastEventTime.get();

            // 如果5秒内没有新事件，返回true
            if (timeSinceLastEvent >= MAX_WAIT_TIME) {
                return true;
            }

            // 如果已经超过指定超时时间，返回false
            if (elapsedTime >= maxWaitMillis) {
                return false;
            }

            // 否则等待一段时间后再次检查
            try {
                Thread.sleep(CHECK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (true);
    }
}