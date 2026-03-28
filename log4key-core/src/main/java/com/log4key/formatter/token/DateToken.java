/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date token.
 *
 * 日期Token。
 */
public class DateToken implements Token {

    // 默认日期格式
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    // 使用线程安全的Map存储SimpleDateFormat实例
    private static final ConcurrentHashMap<String, ThreadLocal<SimpleDateFormat>> FORMAT_MAP = new ConcurrentHashMap<>();

    private final String format;

    /**
     * 构造函数，使用默认日期格式
     */
    public DateToken() {
        this(DEFAULT_FORMAT);
    }

    /**
     * 构造函数，指定日期格式
     *
     * @param format 日期格式字符串
     */
    public DateToken(String format) {
        this.format = format != null ? format : DEFAULT_FORMAT;

        // 初始化ThreadLocal的SimpleDateFormat
        FORMAT_MAP.computeIfAbsent(this.format, k -> ThreadLocal.withInitial(() -> new SimpleDateFormat(k)));
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        Date timestamp = new Date(event.getTimestampMillis());
        out.append(FORMAT_MAP.get(format).get().format(timestamp));
    }
}
