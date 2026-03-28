/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import com.log4key.internal.InternalLogger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Key-based log executor.
 *
 * 基于主键的日志执行器。
 */
public class KeyBasedLogExecutor extends AbstractExecutorService implements LogExecutor {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(KeyBasedLogExecutor.class);

    private final ExecutorService[] threadPools;
    private final int poolCount;

    /**
     * Submits a key-based task.
     *
     * 提交基于主键的任务。
     *
     * @param key the task key / 任务主键
     * @param task the task / 任务
     * @param <T> the result type
     * @return the Future object / Future对象
     */
    public <T> Future<T> submit(String key, Callable<T> task) {
        ExecutorService pool = getThreadPool(key);
        return pool.submit(task);
    }

    /**
     * 提交基于主键的任务
     * @param key 任务主键
     * @param task 任务
     * @return Future对象
     */
    public Future<?> submit(String key, Runnable task) {
        ExecutorService pool = getThreadPool(key);
        return pool.submit(task);
    }

    /**
     * Submits a key-based task with result.
     *
     * 提交基于主键的任务。
     *
     * @param key the task key / 任务主键
     * @param task the task / 任务
     * @param result the result / 结果
     * @param <T> the result type
     * @return the Future object / Future对象
     */
    public <T> Future<T> submit(String key, Runnable task, T result) {
        ExecutorService pool = getThreadPool(key);
        return pool.submit(task, result);
    }

    /**
     * 执行基于主键的任务
     * @param key 任务主键
     * @param command 任务
     */
    public void execute(String key, Runnable command) {
        ExecutorService pool = getThreadPool(key);
        pool.execute(command);
    }

    /**
     * Executes a task.
     *
     * 执行任务。
     *
     * @param command the task / 任务
     */
    @Override
    public void execute(Runnable command) {
        // 对于普通Runnable，使用随机分配策略，将任务分配到任意子线程池
        int index = ThreadLocalRandom.current().nextInt(poolCount);
        threadPools[index].execute(command);
    }

    /**
     * 关闭执行器，不再接受新任务
     * 遵循gracefulShutdown逻辑，分批等待任务完成
     */
    @Override
    public void shutdown() {
        for (ExecutorService pool : threadPools) {
            pool.shutdown();
        }

        // 使用提供的gracefulShutdown逻辑
        while (!isTerminated()) {
            try {
                // 分批等待
                awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 立即关闭执行器，尝试中断正在执行的任务
     */
    @Override
    public List<Runnable> shutdownNow() {
        long less = 0;
        for (ExecutorService pool : threadPools) {
            List<Runnable> tasks = pool.shutdownNow();
            less += tasks.size();
        }
        logger.warn("ShutdownNow task:" + less);
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        for (ExecutorService pool : threadPools) {
            if (!pool.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isTerminated() {
        for (ExecutorService pool : threadPools) {
            if (!pool.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        for (ExecutorService pool : threadPools) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                return false;
            }

            if (!pool.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 构造函数 - 使用默认配置
     * @param poolCount 子线程池数量
     */
    public KeyBasedLogExecutor(int poolCount) {
        this(poolCount,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(false); // 非守护线程
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 构造函数
     * @param poolCount 子线程池数量
     * @param threadFactory 线程工厂
     * @param handler 拒绝策略
     */
    private KeyBasedLogExecutor(int poolCount,
                               ThreadFactory threadFactory,
                               RejectedExecutionHandler handler) {
        // 每个线程池只有一个线程，确保同一 key 的任务由同一线程处理
        this.poolCount = poolCount;
        this.threadPools = new ExecutorService[poolCount];

        // 为每个 key 创建一个单线程执行器
        for (int i = 0; i < poolCount; i++) {
            threadPools[i] = new ThreadPoolExecutor(
                    1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new NamedThreadFactory(threadFactory, "key-based-" + i),
                    handler);
        }
    }

    /**
     * 根据主键获取对应的线程池
     * @param key 任务主键
     * @return 对应的线程池
     */
    private ExecutorService getThreadPool(String key) {
        if (key == null) {
            // 空主键使用第一个线程池
            return threadPools[0];
        }

        // 使用MurmurHash3算法获取哈希值，分布更均匀
        int hashCode = MurmurHash3.hash32(key);
        int index = Math.abs(hashCode) % poolCount;
        return threadPools[index];
    }

    /**
     * 命名线程工厂，用于区分不同子线程池的线程
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate;
        private final String prefix;
        private final AtomicLong threadNumber = new AtomicLong(1);

        public NamedThreadFactory(ThreadFactory delegate, String prefix) {
            this.delegate = delegate;
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = delegate.newThread(r);
            thread.setName(prefix + "-thread-" + threadNumber.getAndIncrement());
            return thread;
        }
    }
}