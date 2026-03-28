/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

/**
 * Executor health status enumeration.
 *
 * 执行器健康状态枚举。
 */
public enum ExecutorHealthStatus {
    /**
     * 健康状态，执行器正常运行
     */
    HEALTHY,
    
    /**
     * 警告状态，执行器可能存在问题，但仍能处理任务
     */
    WARNING,
    
    /**
     * 降级状态，执行器已降级到备用执行器
     */
    DEGRADED,
    
    /**
     * 关闭中状态，执行器正在关闭
     */
    SHUTTING_DOWN,
    
    /**
     * 已关闭状态，执行器已完全关闭
     */
    SHUTDOWN,
    
    /**
     * 异常状态，执行器无法正常处理任务，需要降级
     */
    ERROR
}
