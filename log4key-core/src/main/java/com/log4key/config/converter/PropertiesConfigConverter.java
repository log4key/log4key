/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.converter;

import com.log4key.config.ConfigKeys;
import com.log4key.config.key.ConfigKey;
import com.log4key.internal.InternalLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Properties configuration converter.
 *
 * Properties配置转换器。
 */
public class PropertiesConfigConverter implements ConfigConverter {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(PropertiesConfigConverter.class);

    // ========================== public 方法 ==========================

    @Override
    public Map<ConfigKey<?>, Object> parse(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);

        Map<ConfigKey<?>, Object> result = new HashMap<>();

        // 遍历所有已知的ConfigKey
        for (ConfigKey<?> key : ConfigKeys.ALL_KEYS.values()) {
            String raw = props.getProperty(key.name());
            if (raw != null) {
                try {
                    // 使用接口的默认convert方法进行类型转换
                    Object value = convert(raw, key.type());
                    result.put(key, value);
                } catch (IllegalArgumentException e) {
                    // 转换失败，记录警告但继续处理其他配置
                    logger.warn("Failed to convert value '" + raw +
                              "' for key '" + key.name() + "' to type " +
                              key.type().getSimpleName() + ": " + e.getMessage());
                }
            }
        }

        return result;
    }

    @Override
    public <T> T convert(String raw, Class<T> type) {
        try {
            return ConfigConverter.super.convert(raw, type);
        } catch (IllegalArgumentException e) {
            // 添加更详细的上下文信息
            throw new IllegalArgumentException("Failed to convert '" + raw + "' to " + type.getSimpleName(), e);
        }
    }
}
