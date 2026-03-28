/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.model;

/**
 * Output level policy enumeration.
 *
 * 输出级别策略枚举。
 */
public enum OutputLevelPolicy {
    /**
     * 只输出指定级别的日志
     */
    EXACT,
    
    /**
     * 输出指定级别及以上的日志
     */
    AT_LEAST
}