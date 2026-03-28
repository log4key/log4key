/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.spi.Lifecycle;
import com.log4key.api.spi.NamedExtension;
import com.log4key.api.spi.PriorityOrdered;
import java.util.List;

/**
 * Appender provider SPI interface that defines the extension point for log output.
 *
 * 输出目标SPI接口，定义日志输出的扩展点。
 */
public interface AppenderProvider extends Lifecycle, PriorityOrdered, NamedExtension {

    /**
     * Outputs a single log event.
     *
     * 输出单条日志事件。
     *
     * @param event log event / 日志事件
     */
    void append(LogEvent event);

    /**
     * Outputs multiple log events in batch.
     *
     * 批量输出日志事件。
     *
     * @param events list of log events / 日志事件列表
     */
    void appendBatch(List<LogEvent> events);

    /**
     * Flushes the buffer.
     *
     * 刷新缓冲区。
     */
    void flush();

    /**
     * Closes the appender and releases resources.
     *
     * 关闭输出提供者，释放资源。
     */
    void close();

    /**
     * Gets the name of the appender provider.
     *
     * 获取提供者名称。
     *
     * @return provider name / 名称
     */
    String getName();

    /**
     * Gets the type of the appender provider.
     *
     * 获取提供者类型。
     *
     * @return appender type / 类型
     */
    AppenderType getType();

    /**
     * Checks if async operation is supported.
     *
     * 是否支持异步操作。
     *
     * @return true if async is supported / 是否支持异步
     */
    boolean isAsyncSupported();

    /**
     * Gets the priority of the extension point.
     *
     * 获取扩展点优先级，默认值为100。
     *
     * @return priority value / 优先级值
     */
    @Override
    default int getPriority() {
        return 100;
    }

    /**
     * Checks if the extension point is currently running.
     *
     * 检查扩展点是否正在运行，默认返回true。
     *
     * @return true if running / 是否正在运行
     */
    @Override
    default boolean isRunning() {
        return true;
    }
}
