/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

/**
 * Abstract log key class that provides basic implementation of ILogKey interface.
 *
 * 日志主键抽象类，提供ILogKey接口的基本实现。
 * 存储键值和节点ID，实现基本的主键功能。
 */
public abstract class AbstractLogKey implements ILogKey {

    /**
     * Serial version UID for serialization compatibility.
     *
     * 序列化版本UID，用于确保序列化的兼容性。
     */
    private static final long serialVersionUID = 1L;

    /**
     * Log key value.
     *
     * 日志键值。
     */
    protected final String key;

    /**
     * Node ID in distributed deployment.
     *
     * 分布式部署下的节点ID。
     */
    protected long nodeId;

    /**
     * Constructor that initializes key value and default node ID.
     *
     * 构造函数，初始化键值和默认节点ID。
     *
     * @param key log key value / 日志键值
     * @throws IllegalArgumentException when key is null or empty / 当键值为null或空字符串时抛出
     */
    protected AbstractLogKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Log key cannot be null or empty");
        }
        this.key = key;
        this.nodeId = 0; // 默认节点ID为0
    }

    /**
     * Gets the node ID.
     *
     * 获取节点ID。
     *
     * @return node ID / 节点ID
     */
    @Override
    public long getNodeId() {
        return nodeId;
    }

    /**
     * Sets the node ID.
     *
     * 设置节点ID。
     *
     * @param nodeId node ID / 节点ID
     */
    @Override
    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Gets the key value.
     *
     * 获取键值。
     *
     * @return key value string / 键值字符串
     */
    @Override
    public String value() {
        return key;
    }

    /**
     * Compares two log keys for equality.
     *
     * 比较两个日志键是否相等。
     *
     * @param obj object to compare / 要比较的对象
     * @return true if equal / 如果相等则返回true
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AbstractLogKey other = (AbstractLogKey) obj;
        return nodeId == other.nodeId && key.equals(other.key);
    }

    /**
     * Gets the hash code.
     *
     * 获取哈希码。
     *
     * @return hash code value / 哈希码值
     */
    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + (int) (nodeId ^ (nodeId >>> 32));
        return result;
    }

    /**
     * Gets the string representation.
     *
     * 获取字符串表示。
     *
     * @return string representation / 字符串表示
     */
    @Override
    public String toString() {
        return "LogKey{" +
                "key='" + key + '\'' +
                ", nodeId=" + nodeId +
                '}';
    }
}
