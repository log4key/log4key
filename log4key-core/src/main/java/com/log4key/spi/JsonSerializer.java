/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spi;

import java.util.Map;

/**
 * JSON serializer SPI interface.
 *
 * JSON序列化器SPI接口，用于将日志Map序列化为JSON字符串。
 */
public interface JsonSerializer {

    /**
     * Serializes the log map to a JSON string.
     *
     * 将日志Map序列化为JSON字符串。
     *
     * @param map the log content map / 日志内容Map
     * @return the serialized JSON string / 序列化后的JSON字符串
     */
    String serialize(Map<String, Object> map);
}
