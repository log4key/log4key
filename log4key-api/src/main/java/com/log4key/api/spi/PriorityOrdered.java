/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.spi;

/**
 * Priority ordering interface for extension point priority management.
 *
 * 优先级排序接口，用于扩展点的优先级管理。
 */
public interface PriorityOrdered {
    /**
     * Gets the priority of the extension point.
     *
     * 获取扩展点优先级，数值越小优先级越高。
     *
     * @return priority value / 优先级值
     */
    int getPriority();
}
