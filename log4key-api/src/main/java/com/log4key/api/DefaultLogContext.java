/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Default log context implementation class.
 *
 * 日志上下文默认实现类。
 * 提供线程局部上下文和全局上下文管理功能。
 */
public class DefaultLogContext implements LogContext {

    /**
     * Thread-local context for storing current thread's context information.
     *
     * 线程局部上下文，用于存储当前线程的上下文信息。
     */
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_CONTEXT = ThreadLocal.withInitial(HashMap::new);

    /**
     * Global context for storing application-level context information.
     *
     * 全局上下文，用于存储应用级别的上下文信息。
     */
    private static final Map<String, Object> GLOBAL_CONTEXT = new HashMap<>();

    /**
     * Current context data.
     *
     * 当前上下文数据。
     */
    private final Map<String, Object> contextData;

    /**
     * Flag indicating whether the current context is read-only.
     *
     * 标记当前上下文是否为只读。
     */
    private final boolean readOnly;

    /**
     * Gets the current thread's context instance.
     *
     * 获取当前线程的上下文实例。
     *
     * @return current thread's context instance / 当前线程的上下文实例
     */
    public static LogContext current() {
        return new DefaultLogContext(THREAD_LOCAL_CONTEXT.get(), false);
    }

    /**
     * Gets the global context instance.
     *
     * 获取全局上下文实例。
     *
     * @return global context instance / 全局上下文实例
     */
    public static LogContext global() {
        return new DefaultLogContext(GLOBAL_CONTEXT, false);
    }

    /**
     * Gets a read-only context instance.
     *
     * 获取只读的上下文实例。
     *
     * @return read-only context instance / 只读上下文实例
     */
    public static LogContext readonly() {
        Map<String, Object> combined = new HashMap<>(GLOBAL_CONTEXT);
        combined.putAll(THREAD_LOCAL_CONTEXT.get());
        return new DefaultLogContext(Collections.unmodifiableMap(combined), true);
    }

    /**
     * Private constructor to create a context instance.
     *
     * 私有构造函数，创建上下文实例。
     *
     * @param contextData context data / 上下文数据
     * @param readOnly whether read-only / 是否只读
     */
    private DefaultLogContext(Map<String, Object> contextData, boolean readOnly) {
        this.contextData = contextData;
        this.readOnly = readOnly;
    }

    @Override
    public void put(String key, Object value) {
        if (readOnly) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        contextData.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return key == null ? null : (T) contextData.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return key == null ? defaultValue : (T) contextData.getOrDefault(key, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T remove(String key) {
        if (readOnly) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }
        return key == null ? null : (T) contextData.remove(key);
    }

    @Override
    public void clear() {
        if (readOnly) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }
        contextData.clear();
    }

    @Override
    public Map<String, Object> getAll() {
        return new HashMap<>(contextData);
    }

    @Override
    public void merge(LogContext other) {
        if (readOnly) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }
        if (other != null) {
            contextData.putAll(other.getAll());
        }
    }
}
