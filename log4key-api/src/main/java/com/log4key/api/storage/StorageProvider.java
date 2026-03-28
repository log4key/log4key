/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.storage;

import com.log4key.api.LogEvent;
import java.util.List;
import java.util.Map;

/**
 * Storage provider SPI interface that defines the extension point for log storage.
 *
 * 存储策略SPI接口，定义日志存储的扩展点。
 */
public interface StorageProvider {

    /**
     * Initializes the storage provider.
     *
     * 初始化存储提供者。
     *
     * @param config configuration parameters / 配置参数
     */
    void initialize(Map<String, Object> config);

    /**
     * Stores a single log event.
     *
     * 存储单条日志事件。
     *
     * @param event log event / 日志事件
     */
    void store(LogEvent event);

    /**
     * Stores multiple log events in batch.
     *
     * 批量存储日志事件。
     *
     * @param events list of log events / 日志事件列表
     */
    void storeBatch(List<LogEvent> events);

    /**
     * Queries log events.
     *
     * 查询日志事件。
     *
     * @param query query conditions / 查询条件
     * @return list of log events / 日志事件列表
     */
    List<LogEvent> query(LogQuery query);

    /**
     * Closes the storage provider and releases resources.
     *
     * 关闭存储提供者，释放资源。
     */
    void close();

    /**
     * Gets the provider name.
     *
     * 获取提供者名称。
     *
     * @return provider name / 名称
     */
    String getName();

    /**
     * Gets the priority.
     *
     * 获取优先级。
     *
     * @return priority value, smaller values have higher priority / 优先级值，值越小优先级越高
     */
    int getPriority();
}
