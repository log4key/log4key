/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.appender;

/**
 * Appender type interface that defines the type identifier for appenders.
 *
 * Appender类型接口，定义Appender的类型标识符。
 */
public interface AppenderType {

    /**
     * Gets the type ID.
     *
     * 获取类型ID。
     *
     * @return appender type ID / Appender类型标识ID
     */
    String getId();

    /**
     * Checks if async operation is supported.
     *
     * 是否支持异步。
     *
     * @return true if async is supported / 是否支持异步
     */
    default boolean supportsAsync() {
        return true;
    }

    /**
     * Checks if key routing is supported.
     *
     * 是否支持key路由。
     *
     * @return true if key routing is supported / 是否支持key路由
     */
    default boolean supportsKeyRouting() {
        return true;
    }

}
