/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;

import java.text.SimpleDateFormat;

/**
 * Date path segment.
 *
 * 日期路径片段，将日志事件的时间戳格式化为 yyyyMMdd。
 */
public class DateSegment implements Segment {

    /**
     * ThreadLocal 持有 SimpleDateFormat，避免并发问题。
     */
    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMdd"));

    @Override
    public void append(StringBuilder sb, LogEvent e) {
        String dateStr = DATE_FORMAT.get().format(e.getTimestampMillis());
        sb.append(dateStr);
    }

    /**
     * Appends the formatted date (yyyyMMdd) to the StringBuilder.
     * Ignores overrideLevel since date segments are not affected by log level.
     *
     * 将格式化日期（yyyyMMdd）追加到 StringBuilder 中，忽略 overrideLevel（日期片段不受日志级别影响）。
     *
     * @param sb the StringBuilder to append to
     * @param e  the log event
     * @param overrideLevel ignored / 忽略此参数
     */
    @Override
    public void append(StringBuilder sb, LogEvent e, String overrideLevel) {
        String dateStr = DATE_FORMAT.get().format(e.getTimestampMillis());
        sb.append(dateStr);
    }
}