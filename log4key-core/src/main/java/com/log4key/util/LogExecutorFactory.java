/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import com.log4key.config.Log4KeyConfiguration;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Log executor factory.
 *
 * 日志执行器工厂。
 */
public class LogExecutorFactory {

    /**
     * 执行器类型枚举
     */
    public enum ExecutorType {
        /**
         * 基于主键的执行器
         */
        KEY_BASED,

        /**
         * 普通线程池执行器
         */
        DEFAULT
    }

    /**
     * 创建LogExecutor实例
     * @param type 执行器类型
     * @param corePoolSize 核心线程数
     * @return LogExecutor实例
     */
    public static LogExecutor createExecutor(ExecutorType type, int corePoolSize) {
        switch (type) {
            case KEY_BASED:
                return new KeyBasedLogExecutor(corePoolSize);
            case DEFAULT:
            default:
                return new DefaultLogExecutor(corePoolSize);
        }
    }


    /**
     * 根据配置创建LogExecutor实例
     * @param configuration 配置管理器实例
     * @return LogExecutor实例
     */
    public static LogExecutor createExecutorFromConfig(Log4KeyConfiguration configuration) {
        // 默认使用基于主键的执行器
        ExecutorType type = ExecutorType.KEY_BASED;

        // 从结构化配置获取执行器类型
        String typeStr = configuration.getExecutorType();
        if (typeStr == null || typeStr.isEmpty()) {
            // 回退到旧的配置键
            typeStr = configuration.getExecutorType();
        }
        try {
            type = ExecutorType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 类型无效，使用默认值
            org.slf4j.LoggerFactory.getLogger(LogExecutorFactory.class).warn("Invalid executorType: {}", typeStr, e);
        }

        // 获取核心线程数
        int corePoolSize = configuration.getCorePoolSize();

        // 获取队列大小（目前未使用，但保留供将来扩展）
        int queueSize = configuration.getExecutorQueueSize();

        return createExecutor(type, corePoolSize);
    }

    /**
     * 默认日志执行器实现
     * 使用普通的FixedThreadPool
     */
    private static class DefaultLogExecutor implements LogExecutor {

        private final ExecutorService executor;
        private static final AtomicInteger threadCounter = new AtomicInteger(1);

        public DefaultLogExecutor(int corePoolSize) {
            this.executor = Executors.newFixedThreadPool(corePoolSize, r -> {
                Thread thread = new Thread(r, "log4key-default-executor-" + threadCounter.getAndIncrement());
                thread.setDaemon(false); // 非守护线程
                return thread;
            });
        }

        @Override
        public Future<?> submit(String key, Runnable task) {
            // 默认执行器忽略key，直接提交任务
            return executor.submit(task);
        }

        @Override
        public void execute(String key, Runnable command) {
            // 默认执行器忽略key，直接执行任务
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