/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Log configuration model class that encapsulates all configuration information of log components.
 *
 * 日志配置模型类，封装日志组件的所有配置信息。
 */
public class LogConfig {
    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Gets the configuration property value.
     *
     * 获取配置属性值。
     *
     * @param key property key / 属性键名
     * @return property value / 属性值
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Gets the configuration property value as String.
     *
     * 获取字符串类型的配置属性值。
     *
     * @param key property key / 属性键名
     * @return String property value or null / 字符串类型的属性值
     */
    public String getStringProperty(String key) {
        Object value = properties.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    /**
     * Gets the configuration property value as String, returns default value if not found.
     *
     * 获取字符串类型的配置属性值，如果不存在则返回默认值。
     *
     * @param key property key / 属性键名
     * @param defaultValue default value / 默认值
     * @return String property value or default value / 字符串类型的属性值或默认值
     */
    public String getStringProperty(String key, String defaultValue) {
        String value = getStringProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets the configuration property value as Integer.
     *
     * 获取整数类型的配置属性值。
     *
     * @param key property key / 属性键名
     * @return Integer property value or null / 整数类型的属性值
     */
    public Integer getIntProperty(String key) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the configuration property value as int, returns default value if not found.
     *
     * 获取整数类型的配置属性值，如果不存在则返回默认值。
     *
     * @param key property key / 属性键名
     * @param defaultValue default value / 默认值
     * @return int property value or default value / 整数类型的属性值或默认值
     */
    public int getIntProperty(String key, int defaultValue) {
        Integer value = getIntProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets the configuration property value as Boolean.
     *
     * 获取布尔类型的配置属性值。
     *
     * @param key property key / 属性键名
     * @return Boolean property value or null / 布尔类型的属性值
     */
    public Boolean getBooleanProperty(String key) {
        Object value = properties.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    /**
     * Gets the configuration property value as boolean, returns default value if not found.
     *
     * 获取布尔类型的配置属性值，如果不存在则返回默认值。
     *
     * @param key property key / 属性键名
     * @param defaultValue default value / 默认值
     * @return boolean property value or default value / 布尔类型的属性值或默认值
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        Boolean value = getBooleanProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Sets the configuration property value.
     *
     * 设置配置属性值。
     *
     * @param key property key / 属性键名
     * @param value property value / 属性值
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Gets all configuration properties.
     *
     * 获取所有配置属性。
     *
     * @return all configuration properties map / 所有配置属性的映射
     */
    public Map<String, Object> getAllProperties() {
        return properties;
    }

    /**
     * Merges other configuration into current configuration.
     *
     * 合并其他配置到当前配置。
     *
     * @param other configuration to merge / 要合并的配置
     * @deprecated This method will be removed in future versions.
     */
    @Deprecated
    public void merge(LogConfig other) {
        if (other != null) {
            properties.putAll(other.properties);
        }
    }

    /**
     * Clears all configuration.
     *
     * 清空所有配置。
     */
    public void clear() {
        properties.clear();
    }
}
