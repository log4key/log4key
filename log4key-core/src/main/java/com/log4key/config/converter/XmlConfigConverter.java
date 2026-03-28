/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.converter;

import com.log4key.config.Log4KeyConfigurationLoader;
import com.log4key.config.key.ConfigKey;
import com.log4key.internal.InternalLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * XML configuration converter.
 *
 * XML配置转换器。
 */
public class XmlConfigConverter implements ConfigConverter {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(XmlConfigConverter.class);

    // ========================== public 方法 ==========================

    @Override
    public Map<ConfigKey<?>, Object> parse(InputStream in) throws IOException {
        // 使用现有的XML解析逻辑获取扁平化Map
        Map<String, Object> xmlMap = Log4KeyConfigurationLoader.loadFromXmlStream(in);
        logger.debug("loadFromXmlStream returned " + xmlMap.size() + " entries");

        // 处理所有XML条目，确保每个条目都被转换
        Map<ConfigKey<?>, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> xmlEntry : xmlMap.entrySet()) {
            String xmlKey = xmlEntry.getKey();
            Object rawValue = xmlEntry.getValue();

            if (logger.isDebugEnabled()) {
                logger.debug("Processing XML entry: " + xmlKey + " = " + rawValue);
            }

            // 推断类型
            Class<?> targetType = inferType(xmlKey, rawValue);

            // 创建动态ConfigKey
            ConfigKey<?> configKey = new ConfigKey<>(xmlKey, targetType, null);
            logger.debug("Created dynamic ConfigKey for: " + xmlKey + " (type: " + targetType.getSimpleName() + ")");

            // 转换值并添加到结果
            try {
                Object value = convertValue(rawValue, configKey.type());
                result.put(configKey, value);
                logger.debug("Added key: " + xmlKey + " = " + value + " (type: " + configKey.type().getSimpleName() + ")");
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to convert key '" + xmlKey + "' value '" + rawValue + "' to type " +
                          configKey.type().getSimpleName() + ": " + e.getMessage());
                // 如果转换失败，尝试使用字符串类型
                try {
                    ConfigKey<String> stringKey = new ConfigKey<>(xmlKey, String.class, null);
                    result.put(stringKey, String.valueOf(rawValue));
                    logger.debug("Fallback to string type for: " + xmlKey);
                } catch (Exception e2) {
                    logger.warn("Cannot convert key '" + xmlKey + "' even to string: " + e2.getMessage());
                }
            }
        }

        logger.debug("parse() returning " + result.size() + " config entries");
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

    // ========================== private 方法 ==========================

    /**
     * 根据键名和值内容推断类型
     * @param key 键名
     * @param value 值内容
     * @return 推断的类型
     */
    private Class<?> inferType(String key, Object value) {
        // 1. 首先根据值的实际类型推断
        if (value != null) {
            Class<?> valueType = value.getClass();
            if (valueType == Integer.class) {
                return Integer.class;
            } else if (valueType == Boolean.class) {
                return Boolean.class;
            } else if (valueType == Long.class) {
                return Long.class;
            } else if (valueType == Double.class) {
                return Double.class;
            } else if (valueType == String.class) {
                // 如果是字符串，根据内容进一步推断
                String strValue = value.toString().trim();
                if (strValue.equalsIgnoreCase("true") || strValue.equalsIgnoreCase("false")) {
                    return Boolean.class;
                } else if (strValue.matches("\\d+")) {
                    return Integer.class;
                } else if (strValue.matches("\\d+\\.\\d+")) {
                    return Double.class;
                }
            }
        }

        // 2. 根据键名推断类型
        if (key.endsWith(".level") || key.endsWith(".levelPolicy") ||
            key.endsWith(".type") || key.endsWith(".formatter") ||
            key.endsWith(".charset") || key.endsWith(".directory") ||
            key.endsWith(".pattern") || key.endsWith(".timestamp")) {
            return String.class;
        } else if (key.endsWith(".consoleEnabled") || key.endsWith(".asyncEnabled") ||
                  key.endsWith(".includeLocation") || key.endsWith(".includeLevel") ||
                  key.endsWith(".includeLogger") || key.endsWith(".includeThread") ||
                  key.endsWith(".includeMdc") || key.endsWith(".shutdownHook")) {
            return Boolean.class;
        } else if (key.endsWith(".threads") || key.endsWith(".queueSize") ||
                  key.endsWith(".bufferSize") || key.endsWith(".maxFileSizeMB") ||
                  key.endsWith(".maxBackupIndex")) {
            return Integer.class;
        } else if (key.endsWith(".flushInterval")) {
            return Long.class;
        }

        // 3. 默认类型为字符串
        return String.class;
    }

    /**
     * 将原始值转换为指定类型
     * 支持字符串、整数、长整型、布尔值等基本类型
     * @param rawValue 原始值
     * @param type 目标类型
     * @param <T> 目标类型参数
     * @return 转换后的值
     */
    @SuppressWarnings("unchecked")
    private <T> T convertValue(Object rawValue, Class<T> type) {
        if (rawValue == null) {
            return null;
        }

        // 如果类型已经匹配，直接返回
        if (type.isInstance(rawValue)) {
            return (T) rawValue;
        }

        // 转换为字符串后使用接口的默认convert方法
        String strValue = String.valueOf(rawValue);
        return convert(strValue, type);
    }
}
