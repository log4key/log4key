/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import com.log4key.path.PathKey;
import com.log4key.worker.WorkerGroup;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executor controller.
 *
 * 执行器控制器。
 */
public class ExecutorController {

    /**
     * 主执行器
     */
    private final LogExecutor mainExecutor;

    /**
     * 降级执行器
     */
    private final FallbackLogExecutor fallbackExecutor;

    /**
     * 执行器健康状态
     */
    private volatile ExecutorHealthStatus healthStatus = ExecutorHealthStatus.HEALTHY;

    /**
     * 是否已关闭
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Executes key-based log task.
     *
     * 提交基于key的日志任务。
     *
     * @param key the log key for thread allocation / 日志主键，用于线程分配
     * @param task the task to execute / 要执行的任务
     */
    public void execute(String key, Runnable task) {
        // 根据执行器健康状态选择执行器
        if (healthStatus == ExecutorHealthStatus.HEALTHY) {
            try {
                // 尝试使用主执行器
                mainExecutor.execute(key, task);
            } catch (RejectedExecutionException e) {
                // 队列满 - 立即降级
                healthStatus = ExecutorHealthStatus.DEGRADED;
                fallbackExecutor.execute(key, task);
            } catch (RuntimeException e) {
                // 运行时异常 - 降级并标记为错误
                healthStatus = ExecutorHealthStatus.ERROR;
                fallbackExecutor.execute(key, task);
            } catch (Exception e) {
                // 其他异常 - 降级并记录
                healthStatus = ExecutorHealthStatus.DEGRADED;
                fallbackExecutor.execute(key, task);
            }
        } else if (healthStatus == ExecutorHealthStatus.DEGRADED) {
            // 已降级，直接使用降级执行器
            fallbackExecutor.execute(key, task);
        } else {
            // 错误状态，尝试使用降级执行器，如果失败则直接执行
            try {
                fallbackExecutor.execute(key, task);
            } catch (Exception e) {
                // 降级执行器也失败，直接在调用线程执行
                task.run();
            }
        }
    }

    /**
     * 执行写入任务（委托给 WorkerGroup）。
     *
     * 由 FileAppender 调用，将格式化后的日志消息通过 WorkerGroup 投递到对应 Worker 的 Mailbox 中异步执行。
     *
     * @param key               workerId 字符串（由 FileAppender.shard() 计算）
     * @param pathKey           路径键
     * @param formattedMessage  已格式化的日志消息
     */
    public void executeWrite(String key, PathKey pathKey, String formattedMessage) {
        if (healthStatus == ExecutorHealthStatus.HEALTHY) {
            try {
                if (mainExecutor instanceof WorkerGroup) {
                    ((WorkerGroup) mainExecutor).executeWrite(key, pathKey, formattedMessage);
                } else {
                    // 非 WorkerGroup 时，fallback：构造 Runnable 投递
                    mainExecutor.execute(key, () -> {});
                }
            } catch (Exception e) {
                healthStatus = ExecutorHealthStatus.DEGRADED;
                fallbackExecutor.execute(key, () -> {});
            }
        } else {
            fallbackExecutor.execute(key, () -> {});
        }
    }

    /**
     * Checks the health status of the main executor.
     /**
     * 检查主执行器健康状态。
     *
     * @return the health status of the main executor / 主执行器健康状态
     * @deprecated This method will be removed in future versions.
     */
    public ExecutorHealthStatus checkMainExecutorHealth() {
        // 检查主执行器是否已关闭或已终止
        if (mainExecutor.isShutdown() || mainExecutor.isTerminated()) {
            healthStatus = ExecutorHealthStatus.ERROR;
        } else {
            // 检查主执行器是否可以正常接受新任务
            try {
                // 尝试提交一个空任务来测试主执行器是否正常
                mainExecutor.submit("health-check", () -> {});
                // 主执行器正常运行，尝试恢复健康状态
                healthStatus = ExecutorHealthStatus.HEALTHY;
            } catch (RejectedExecutionException e) {
                // 主执行器拒绝任务，保持降级状态
                healthStatus = ExecutorHealthStatus.DEGRADED;
            } catch (Exception e) {
                // 其他异常，保持错误状态
                healthStatus = ExecutorHealthStatus.ERROR;
            }
        }

        return healthStatus;
    }

    /**
     * 获取当前执行器健康状态
     * @return 当前执行器健康状态
     */
    public ExecutorHealthStatus getHealthStatus() {
        return healthStatus;
    }

    /**
     * Closes the executor controller.
     *
     * 关闭执行器控制器。
     */
    public void shutdown() {
        if (closed.compareAndSet(false, true)) {
            prepareForShutdown();
            shutdownAndWaitForMainExecutor();
            processFallbackExecutorAfterMainShutdown();
            finalizeShutdown();
        }
    }

    /**
     * 立即关闭执行器控制器
     */
    public void shutdownNow() {
        if (closed.compareAndSet(false, true)) {
            // 更新健康状态为关闭中
            healthStatus = ExecutorHealthStatus.SHUTTING_DOWN;

            // 立即关闭主执行器和降级执行器
            mainExecutor.shutdownNow();
            fallbackExecutor.shutdownNow();

            // 更新健康状态为已关闭
            healthStatus = ExecutorHealthStatus.SHUTDOWN;
        }
    }

    /**
     * 检查执行器控制器是否已关闭
     * @return 是否已关闭
     */
    public boolean isShutdown() {
        return closed.get();
    }

    /**
     * 检查执行器控制器是否已终止
     * @return 是否已终止
     */
    public boolean isTerminated() {
        return closed.get() && mainExecutor.isTerminated() && fallbackExecutor.isTerminated();
    }

    /**
     * 等待执行器控制器终止
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否在超时时间内终止
     * @throws InterruptedException 等待被中断
     */
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long remainingTime = unit.toMillis(timeout);

        // 等待主执行器终止
        if (!mainExecutor.awaitTermination(remainingTime, TimeUnit.MILLISECONDS)) {
            return false;
        }

        // 计算剩余时间
        remainingTime -= (System.currentTimeMillis() - startTime);
        if (remainingTime <= 0) {
            return false;
        }

        // 等待降级执行器终止
        return fallbackExecutor.awaitTermination(remainingTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取降级执行器
     * @return 降级执行器
     */
    public FallbackLogExecutor getFallbackExecutor() {
        return fallbackExecutor;
    }

    /**
     * 获取主执行器（供 LogManager 注入 workerCount 到 FileAppender 使用）。
     *
     * @return 主执行器
     */
    public LogExecutor getMainExecutor() {
        return mainExecutor;
    }

    /**
     * 构造函数
     * @param mainExecutor 主执行器
     */
    public ExecutorController(LogExecutor mainExecutor) {
        this.mainExecutor = mainExecutor;
        this.fallbackExecutor = new FallbackLogExecutor();
    }

    /**
     * Prepares for shutdown.
     *
     * 准备关闭。
     */
    private void prepareForShutdown() {
        // 更新健康状态为关闭中
        healthStatus = ExecutorHealthStatus.SHUTTING_DOWN;

        // 主执行器准备关闭：通知Fallback执行器进入准备阶段
        // 暂停Fallback执行器，不再处理新任务
        fallbackExecutor.pause();
    }

    /**
     * 关闭并等待主执行器
     * 关闭主执行器，并等待其在指定时间内关闭
     */
    private void shutdownAndWaitForMainExecutor() {
        // 关闭主执行器
        mainExecutor.shutdown();
    }

    /**
     * 主执行器关闭后处理Fallback执行器
     * 恢复Fallback执行器，处理剩余任务，然后关闭Fallback执行器
     */
    private void processFallbackExecutorAfterMainShutdown() {
        // 主执行器关闭完成：通知Fallback执行器进入正式阶段
        // 恢复Fallback执行器，处理剩余任务
        fallbackExecutor.resume();

        // 关闭降级执行器
        fallbackExecutor.shutdown();
    }

    /**
     * Finalizes shutdown.
     *
     * 完成关闭。
     */
    private void finalizeShutdown() {
        // 更新健康状态为已关闭
        healthStatus = ExecutorHealthStatus.SHUTDOWN;
    }
}