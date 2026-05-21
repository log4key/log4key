/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;

/**
 * Path segment interface.
 *
 * 路径片段接口，定义路径拼接行为。
 */
public interface Segment {

    /**
     * Appends this segment's content to the given StringBuilder.
     *
     * 将当前片段的内容追加到指定的 StringBuilder 中。
     *
     * @param sb the StringBuilder to append to / 待追加的 StringBuilder
     * @param e  the log event providing contextual data / 提供上下文数据的日志事件
     */
    void append(StringBuilder sb, LogEvent e);

    /**
     * Appends this segment's content to the given StringBuilder,
     * with an optional override for the log level.
     *
     * 将当前片段的内容追加到指定的 StringBuilder 中，支持覆盖日志级别。
     * 当 overrideLevel 为 null 时，行为与 {@link #append(StringBuilder, LogEvent)} 一致。
     *
     * @param sb the StringBuilder to append to / 待追加的 StringBuilder
     * @param e  the log event providing contextual data / 提供上下文数据的日志事件
     * @param overrideLevel 覆盖的日志级别（为 null 时使用 event 中的级别）
     */
    default void append(StringBuilder sb, LogEvent e, String overrideLevel) {
        append(sb, e);
    }
}