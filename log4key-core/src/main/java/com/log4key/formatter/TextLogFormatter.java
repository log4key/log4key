/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter;

import com.log4key.api.LogEvent;
import com.log4key.api.spi.LogFormatter;

import java.util.Map;

/**
 * Text log formatter implementation.
 *
 * 文本格式日志格式化器。
 */
public class TextLogFormatter implements LogFormatter {
    
    private static final String DEFAULT_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} - -: %msg";

    private PatternFormatter delegate = new PatternFormatter(DEFAULT_PATTERN);
    
    @Override
    public String getName() {
        return "text";
    }
    
    @Override
    public String getType() {
        return "text";
    }
    
    /**
     * Sets the log pattern.
     *
     * 设置日志格式模式。
     *
     * @param pattern the log pattern / 日志格式模式
     */
    public void setPattern(String pattern) {
        if (pattern != null && !pattern.isEmpty()) {
            this.delegate = new PatternFormatter(pattern);
        }
    }
    
    @Override
    public String format(LogEvent event, Map<String, Object> context) {
        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }
        return delegate.format(event, context);
    }
}
