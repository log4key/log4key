/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import com.log4key.internal.InternalLogger;

/**
 * Configuration utilities.
 *
 * 配置工具类。
 */
public class ConfigUtils {
    
    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(ConfigUtils.class);
    
    /**
     * Converts an object to an integer.
     *
     * 将对象转换为整数。
     *
     * @param value the value to convert / 要转换的对象
     * @param defaultValue the default value / 默认值
     * @return the converted integer / 转换后的整数
     */
    public static int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value: " + value + ", using default: " + defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Converts an object to a long integer.
     *
     * 将对象转换为长整数。
     *
     * @param value the value to convert / 要转换的对象
     * @param defaultValue the default value / 默认值
     * @return the converted long integer / 转换后的长整数
     */
    public static long parseLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid long value: " + value + ", using default: " + defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
}