/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import com.log4key.config.ConfigKeys;
import com.log4key.config.Log4KeyConfiguration;
import com.log4key.worker.WorkerGroup;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志执行器工厂。
 *
 * 用于创建日志执行器实例，当前默认创建 WorkerGroup 作为主执行器。
 */
public class LogExecutorFactory {

    /**
     * 根据配置创建 WorkerGroup 实例。
     *
     * @param config 配置管理器实例
     * @return WorkerGroup 实例（已启动）
     */
    public static WorkerGroup createWorkerGroup(Log4KeyConfiguration config) {
        int workerCount = config.getCorePoolSize();
        int queueCapacity = config.getExecutorQueueSize();
        int maxFileWriters = config.getMaxFileWriters();
        long writerIdleTimeout = ConfigKeys.WRITER_IDLE_TIMEOUT_KEY.defaultValue();
        long batchSize = config.getBatchSize();
        long flushIntervalMs = config.getFlushInterval();
        int highWaterMark = config.getHighWaterMark();
        int initialBufferSize = config.getInitialBufferSize();
        long maxFileSize = config.getMaxFileSizeMB() * 1024L * 1024L;
        String charset = config.getDefaultCharset();

        WorkerGroup workerGroup = new WorkerGroup(
                workerCount, queueCapacity, maxFileWriters,
                writerIdleTimeout, batchSize, flushIntervalMs,
                highWaterMark, initialBufferSize, maxFileSize, charset);

        workerGroup.start();
        return workerGroup;
    }

    /**
     * 根据配置创建 LogExecutor 实例。
     *
     * 当前默认创建 WorkerGroup 作为主执行器。
     *
     * @param configuration 配置管理器实例
     * @return LogExecutor 实例
     */
    public static LogExecutor createExecutorFromConfig(Log4KeyConfiguration configuration) {
        return createWorkerGroup(configuration);
    }

    /**
     * 默认日志执行器实现（fallback）。
     *
     * 使用普通的 FixedThreadPool，忽略 key 路由。
     */
    private static class DefaultLogExecutor implements LogExecutor {

        private final ExecutorService executor;
        private static final AtomicInteger threadCounter = new AtomicInteger(1);

        public DefaultLogExecutor(int corePoolSize) {
            this.executor = Executors.newFixedThreadPool(corePoolSize, r -> {
                Thread thread = new Thread(r, "log4key-default-executor-" + threadCounter.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            });
        }

        @Override
        public Future<?> submit(String key, Runnable task) {
            return executor.submit(task);
        }

        @Override
        public void execute(String key, Runnable command) {
            executor.execute(command);
        }

        @Override
        public void shutdown() {
            executor.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return executor.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return executor.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return executor.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(timeout, unit);
        }
    }
}