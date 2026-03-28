/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

/**
 * Default log key implementation class.
 *
 * 日志主键默认实现类。
 * 提供静态工厂方法创建日志主键实例，方便使用。
 */
public class DefaultLogKey extends AbstractLogKey {

    /**
     * Private constructor, use static factory method to create instances.
     *
     * 私有构造函数，通过静态工厂方法创建实例。
     *
     * @param key log key value / 日志键值
     */
    private DefaultLogKey(String key) {
        super(key);
    }

    /**
     * Creates a default log key instance.
     *
     * 创建默认日志主键实例。
     *
     * @param key log key value / 日志键值
     * @return DefaultLogKey instance / DefaultLogKey实例
     * @throws IllegalArgumentException when key is null or empty / 当键值为null或空字符串时抛出
     */
    public static DefaultLogKey of(String key) {
        return new DefaultLogKey(key);
    }

    /**
     * Creates a default log key instance with node ID.
     *
     * 创建带节点ID的日志主键实例。
     *
     * @param key log key value / 日志键值
     * @param nodeId node ID / 节点ID
     * @return DefaultLogKey instance / DefaultLogKey实例
     * @throws IllegalArgumentException when key is null or empty / 当键值为null或空字符串时抛出
     */
    public static DefaultLogKey of(String key, long nodeId) {
        DefaultLogKey logKey = new DefaultLogKey(key);
        logKey.setNodeId(nodeId);
        return logKey;
    }
}
