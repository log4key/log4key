/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

import java.util.Map;

/**
 * Log context interface for storing and managing log-related context information.
 *
 * 日志上下文接口，用于存储和管理日志相关的上下文信息。
 * 提供线程局部上下文和全局上下文管理功能。
 */
public interface LogContext {
    /**
     * Adds a key-value pair to the context.
     *
     * 添加键值对到上下文。
     *
     * @param key context key / 上下文键
     * @param value context value / 上下文值
     */
    void put(String key, Object value);

    /**
     * Gets the value from the context.
     *
     * 从上下文获取值。
     *
     * @param key context key / 上下文键
     * @param <T> value type / 值的类型
     * @return context value or null if not found / 上下文值，如果不存在则返回null
     */
    <T> T get(String key);

    /**
     * Gets the value from the context, returns default value if not found.
     *
     * 从上下文获取值，如果不存在则返回默认值。
     *
     * @param key context key / 上下文键
     * @param defaultValue default value / 默认值
     * @param <T> value type / 值的类型
     * @return context value or default value / 上下文值或默认值
     */
    <T> T get(String key, T defaultValue);

    /**
     * Removes the specified key from the context.
     *
     * 从上下文移除指定键。
     *
     * @param key context key / 上下文键
     * @return removed value or null if not found / 被移除的值，如果不存在则返回null
     */
    <T> T remove(String key);

    /**
     * Clears the current context.
     *
     * 清空当前上下文。
     */
    void clear();

    /**
     * Gets all key-value pairs from the context.
     *
     * 获取上下文的所有键值对。
     *
     * @return context key-value map / 上下文键值对映射
     */
    Map<String, Object> getAll();

    /**
     * Merges another context into the current context.
     *
     * 合并另一个上下文到当前上下文。
     *
     * @param other context to merge / 要合并的上下文
     */
    void merge(LogContext other);

    /**
     * Gets the current thread's context instance.
     *
     * 获取当前线程的上下文实例。
     *
     * @return current thread's context instance / 当前线程的上下文实例
     */
    static LogContext current() {
        return DefaultLogContext.current();
    }

    /**
     * Gets the global context instance.
     *
     * 获取全局上下文实例。
     *
     * @return global context instance / 全局上下文实例
     */
    static LogContext global() {
        return DefaultLogContext.global();
    }

    /**
     * Gets a read-only context instance containing merged current thread and global context.
     *
     * 获取只读的上下文实例，包含当前线程上下文和全局上下文的合并结果。
     *
     * @return read-only context instance / 只读上下文实例
     */
    static LogContext readonly() {
        return DefaultLogContext.readonly();
    }
}
