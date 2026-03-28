/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.resolver;

import com.log4key.config.key.ConfigKey;
import com.log4key.internal.InternalLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration resolver.
 *
 * 配置解析器。
 */
public final class ConfigResolver {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(ConfigResolver.class);

    /**
     * 空配置解析器实例
     */
    public static final ConfigResolver EMPTY = new ConfigResolver(Collections.emptyMap());

    /**
     * 配置值映射
     */
    private final Map<ConfigKey<?>, Object> values;

    /**
     * Creates a new configuration resolver.
     *
     * 创建配置解析器。
     *
     * @param values the configuration values map / 配置值映射
     */
    public ConfigResolver(Map<ConfigKey<?>, Object> values) {
        this.values = values != null ? new HashMap<>(values) : new HashMap<>();
    }

    // ========================== public 方法 ==========================

    /**
     * Gets the configuration value.
     *
     * 获取配置值。
     *
     * @param <T> the value type / 配置值类型
     * @param key the configuration key / 配置键
     * @return the configuration value, or default value if not found / 配置值，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(ConfigKey<T> key) {
        Objects.requireNonNull(key, "ConfigKey cannot be null");
        Object value = values.get(key);
        if (value != null) {
            // 确保类型匹配
            if (key.type().isInstance(value)) {
                return (T) value;
            } else {
                // 类型不匹配，尝试转换
                try {
                    return (T) convertValue(value, key.type());
                } catch (Exception e) {
                    // 转换失败，返回默认值
                    logger.warn("Value type mismatch for key '" + key.name() +
                              "'. Expected " + key.type().getSimpleName() +
                              ", got " + value.getClass().getSimpleName() +
                              ". Conversion failed: " + e.getMessage());
                    return key.defaultValue();
                }
            }
        }
        logger.warn("Value not found for key '" + key.name() + "'");
        // 值不存在，返回默认值
        return key.defaultValue();
    }

    /**
     * Checks if this resolver contains the specified key.
     *
     * 检查是否包含指定配置键。
     *
     * @param key the configuration key / 配置键
     * @return true if contained / 如果包含则返回true
     */
    public boolean contains(ConfigKey<?> key) {
        return values.containsKey(key);
    }

    /**
     * Gets all configuration keys.
     *
     * 获取所有配置键。
     *
     * @return the set of configuration keys / 配置键集合
     */
    public Iterable<ConfigKey<?>> keys() {
        return values.keySet();
    }

    /**
     * Gets the number of configuration values.
     *
     * 获取配置值数量。
     *
     * @return the number of configuration values / 配置值数量
     */
    public int size() {
        return values.size();
    }

    /**
     * Checks if this resolver is empty.
     *
     * 检查是否为空。
     *
     * @return true if empty / 如果为空则返回true
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Merges this resolver with another.
     *
     * 合并另一个配置解析器。
     *
     * @param other the other configuration resolver / 另一个配置解析器
     * @return a new merged resolver / 合并后的新配置解析器
     */
    public ConfigResolver merge(ConfigResolver other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }

        Map<ConfigKey<?>, Object> mergedValues = new HashMap<>(this.values);
        mergedValues.putAll(other.values);
        return new ConfigResolver(mergedValues);
    }

    /**
     * Collects configuration items by pattern.
     *
     * 根据模板收集配置项。
     *
     * @param pattern the regex pattern to match configuration keys / 匹配日志配置项的正则表达式
     * @return the map of configuration items / 配置项映射
     */
    public Map<String, Map<String, Object>> collect(Pattern pattern) {
        Map<String, Map<String, Object>> propsMap = new HashMap<>();
        for (ConfigKey<?> key : keys()) {
            String keyName = key.name();
            Matcher matcher = pattern.matcher(keyName);
            if (matcher.matches()) {
                String loggerName = matcher.group(1);
                String propName = matcher.group(2);
                Object value = get(key);

                propsMap.computeIfAbsent(loggerName, k -> new HashMap<>())
                        .put(propName, value);
            }
        }
        return propsMap;
    }

    @Override
    public String toString() {
        return "ConfigResolver{size=" + values.size() + "}";
    }

    // ========================== private 方法 ==========================

    /**
     * Tries to convert the value type.
     *
     * 尝试转换值的类型。
     *
     * @param value the value to convert / 要转换的值
     * @param targetType the target type / 目标类型
     * @return the converted value / 转换后的值
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (targetType == String.class) {
            return String.valueOf(value);
        } else if (targetType == Integer.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
        } else if (targetType == Long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value instanceof String) {
                return Long.parseLong((String) value);
            }
        } else if (targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            } else if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        } else if (targetType == Double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " to " + targetType.getName());
    }
}
