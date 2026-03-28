/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.key;

/**
 * Configuration key.
 *
 * 配置键。
 */
public final class ConfigKey<T> {
    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    /**
     * Creates a new configuration key.
     *
     * 创建配置键。
     *
     * @param name the key name / 配置键名称
     * @param type the value type / 配置值类型
     * @param defaultValue the default value / 默认值
     */
    public ConfigKey(String name, Class<T> type, T defaultValue) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("ConfigKey name cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("ConfigKey type cannot be null");
        }
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the configuration key name.
     *
     * 获取配置键名称。
     *
     * @return the key name / 配置键名称
     */
    public String name() {
        return name;
    }

    /**
     * Gets the configuration value type.
     *
     * 获取配置值类型。
     *
     * @return the value type / 配置值类型
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Gets the default value.
     *
     * 获取默认值。
     *
     * @return the default value / 默认值
     */
    public T defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigKey<?> configKey = (ConfigKey<?>) o;
        return name.equals(configKey.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "ConfigKey{" +
                "name='" + name + '\'' +
                ", type=" + type.getSimpleName() +
                ", defaultValue=" + defaultValue +
                '}';
    }
}