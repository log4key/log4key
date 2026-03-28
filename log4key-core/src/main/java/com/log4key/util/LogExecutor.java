/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Log executor interface.
 *
 * 日志执行器接口。
 */
public interface LogExecutor {
    
    /**
     * 提交基于key的日志任务
     * @param key 日志主键，用于线程分配
     * @param task 要执行的任务
     * @return Future对象，用于获取任务结果
     */
    Future<?> submit(String key, Runnable task);
    
    /**
     * 执行基于key的日志任务
     * @param key 日志主键，用于线程分配
     * @param command 要执行的命令
     */
    void execute(String key, Runnable command);
    
    /**
     * 关闭执行器，不再接受新任务
     */
    void shutdown();
    
    /**
     * 立即关闭执行器，尝试中断正在执行的任务
     * @return 未执行的任务列表
     */
    List<Runnable> shutdownNow();
    
    /**
     * 检查执行器是否已关闭
     * @return 是否已关闭
     */
    boolean isShutdown();
    
    /**
     * 检查执行器是否已终止
     * @return 是否已终止
     */
    boolean isTerminated();
    
    /**
     * 等待执行器终止
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否在超时时间内终止
     * @throws InterruptedException 等待被中断
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    

}