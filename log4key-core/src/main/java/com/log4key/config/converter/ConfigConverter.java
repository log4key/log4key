/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.converter;

import com.log4key.config.key.ConfigKey;
import java.io.InputStream;
import java.util.Map;

/**
 * Configuration converter interface.
 *
 * 配置转换器接口。
 */
public interface ConfigConverter {
    
    /**
     * Parses the configuration from an input stream.
     *
     * 解析输入流中的配置。
     *
     * @param in the input stream / 输入流
     * @return the mapping from ConfigKey to configuration value / ConfigKey到配置值的映射
     * @throws Exception if parsing fails / 解析过程中发生错误
     */
    Map<ConfigKey<?>, Object> parse(InputStream in) throws Exception;
    
    /**
     * Converts a string to the specified type.
     *
     * 将字符串转换为指定类型的值。
     *
     * @param <T> the target type / 目标类型
     * @param raw the raw string value / 原始字符串值
     * @param type the target type / 目标类型
     * @return the converted value / 转换后的值
     * @throws IllegalArgumentException if conversion fails / 如果转换失败
     */
    default <T> T convert(String raw, Class<T> type) {
        if (raw == null) {
            return null;
        }
        
        if (type == String.class) {
            return type.cast(raw);
        } else if (type == Integer.class || type == int.class) {
            try {
                return type.cast(Integer.valueOf(raw.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + raw + "' to Integer", e);
            }
        } else if (type == Long.class || type == long.class) {
            try {
                return type.cast(Long.valueOf(raw.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Cannot convert '" + raw + "' to Long", e);
            }
        } else if (type == Boolean.class || type == boolean.class) {
            String lower = raw.trim().toLowerCase();
            if ("true".equals(lower) || "yes".equals(lower) || "1".equals(lower)) {
                return type.cast(Boolean.TRUE);
            } else if ("false".equals(lower) || "no".equals(lower) || "0".equals(lower)) {
                return type.cast(Boolean.FALSE);
            } else {
                throw new IllegalArgumentException("Cannot convert '" + raw + "' to Boolean");
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }
}