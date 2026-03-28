/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.spi;

import com.log4key.api.LogEvent;

import java.util.Map;

/**
 * Log formatter SPI interface that converts LogEvent to specified format string.
 *
 * 日志格式化器SPI接口，用于将LogEvent转换为指定格式的字符串。
 * 实现类需要实现format方法，将日志事件格式化为特定格式。
 */
public interface LogFormatter extends NamedExtension {

    /**
     * Formats the LogEvent into a string with context information.
     *
     * 将LogEvent格式化为字符串（带上下文信息）。
     *
     * @param event log event object / 日志事件对象
     * @param context context information, can be null / 上下文信息，可为null
     * @return formatted log string / 格式化后的日志字符串
     */
    String format(LogEvent event, Map<String, Object> context);

    /**
     * Formats the LogEvent into a string without context information.
     *
     * 将LogEvent格式化为字符串（无上下文版本）。
     *
     * @param event log event object / 日志事件对象
     * @return formatted log string / 格式化后的日志字符串
     */
    default String format(LogEvent event) {
        return format(event, null);
    }

    /**
     * Gets the priority of the formatter.
     *
     * 获取格式化器的优先级。
     * Higher priority formatters are called first in chain mode.
     *
     * @return priority value, smaller values have higher priority / 优先级值，值越小优先级越高
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Supports chained formatter invocation.
     *
     * 支持链式调用的格式化方法。
     *
     * @param previousResult output from the previous formatter in the chain / 前一个格式化器的输出结果
     * @param event original log event object / 原始日志事件对象
     * @param context context information, can be null / 上下文信息，可为null
     * @return formatted log string / 格式化后的日志字符串
     */
    default String formatChain(String previousResult, LogEvent event, Map<String, Object> context) {
        // 默认实现：直接返回前一个结果
        return previousResult;
    }

    /**
     * Gets the type identifier of the formatter.
     *
     * 获取格式化器的类型标识符。
     *
     * @return formatter type identifier / 格式化器类型标识符
     */
    String getType();
}
