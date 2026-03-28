/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.slf4j;

import org.slf4j.spi.MDCAdapter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * MDC (Mapped Diagnostic Context) adapter implementation.
 *
 * MDC (Mapped Diagnostic Context) 实现类。
 */
public class Log4KeyMDCAdapter implements MDCAdapter {

    // 使用ThreadLocal存储每个线程的MDC映射
    private static final ThreadLocal<Map<String, String>> threadLocalMap = ThreadLocal.withInitial(HashMap::new);

    @Override
    public void clear() {
        threadLocalMap.get().clear();
    }

    @Override
    public String get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return threadLocalMap.get().get(key);
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return new HashMap<>(threadLocalMap.get());
    }

    @Override
    public void put(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        threadLocalMap.get().put(key, value);
    }

    @Override
    public void remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        threadLocalMap.get().remove(key);
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        if (contextMap == null) {
            clear();
            return;
        }
        Map<String, String> map = threadLocalMap.get();
        map.clear();
        map.putAll(contextMap);
    }

    /**
     * SLF4J 2.x新增方法，清除指定键的双端队列
     * 在Log4Key中，我们目前使用简单的Map存储，所以只需调用remove方法
     *
     * @param key 要清除的键
     * @since SLF4J 2.x
     */
    @Override
    public void clearDequeByKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        remove(key);
    }

    /**
     * SLF4J 2.x新增方法，获取指定键的双端队列副本
     * 在Log4Key中，我们使用简单的Map存储，所以返回包含单个值的双端队列
     *
     * @param key 要获取的键
     * @return 包含值的双端队列，如果键不存在则返回空双端队列
     * @since SLF4J 2.x
     */
    @Override
    public Deque<String> getCopyOfDequeByKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        String value = get(key);
        Deque<String> deque = new ArrayDeque<>();
        if (value != null) {
            deque.add(value);
        }
        return deque;
    }

    /**
     * SLF4J 2.x新增方法，从指定键的双端队列中弹出顶部元素
     * 在Log4Key中，我们使用简单的Map存储，所以移除并返回该键对应的值
     *
     * @param key 要操作的键
     * @return 弹出的值，如果键不存在则返回null
     * @since SLF4J 2.x
     */
    @Override
    public String popByKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        String value = get(key);
        if (value != null) {
            remove(key);
        }
        return value;
    }

    /**
     * SLF4J 2.x新增方法，将值添加到指定键的双端队列顶部
     * 在Log4Key中，我们使用简单的Map存储，所以调用put方法（会覆盖旧值）
     *
     * @param key 要操作的键
     * @param value 要添加的值
     * @since SLF4J 2.x
     */
    @Override
    public void pushByKey(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        put(key, value);
    }

}
