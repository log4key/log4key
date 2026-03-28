/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

/**
 * Log key interface that defines core functionality of log keys.
 *
 * 日志主键接口，定义日志键的核心功能。
 * 用于标识和路由日志，支持分布式场景下的唯一标识。
 */
public interface ILogKey extends java.io.Serializable {

    /**
     * Gets the node ID (used in distributed scenarios).
     *
     * 获取节点ID（分布式场景使用）。
     *
     * @return node ID / 节点ID
     */
    long getNodeId();

    /**
     * Sets the node ID.
     *
     * 设置节点ID。
     *
     * @param nodeId node ID / 节点ID
     */
    void setNodeId(long nodeId);

    /**
     * Gets the key value.
     *
     * 获取键值。
     *
     * @return key value string / 键值字符串
     */
    String value();

    // Note: hashCode method is inherited from Object class, implemented by AbstractLogKey
    // No default implementation needed in this interface
}
