/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.log4key.api.LogEvent;
import com.log4key.api.spi.LogFormatter;
import com.log4key.internal.InternalLogger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON log formatter implementation.
 *
 * JSON格式日志格式化器。
 */
public class JsonLogFormatter implements LogFormatter {
    
    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(JsonLogFormatter.class);

    // 默认不使用美化格式，提高性能
    private static final Gson DEFAULT_GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            .create();
    
    private Gson gson = DEFAULT_GSON;
    private String timestampFormat = "yyyy-MM-dd HH:mm:ss.SSS";
    private boolean includeLevel = true;
    private boolean includeLogger = true;
    private boolean includeThread = true;
    private boolean includeMdc = true;
    private boolean includeNodeId = true;
    private boolean includeKey = true;
    
    @Override
    public String getName() {
        return "json";
    }
    
    @Override
    public String getType() {
        return "json";
    }
    
    /**
     * Sets the timestamp format.
     *
     * 设置时间戳格式。
     *
     * @param timestampFormat the timestamp format / 时间戳格式
     */
    public void setTimestampFormat(String timestampFormat) {
        if (timestampFormat != null && !timestampFormat.isEmpty()) {
            this.timestampFormat = timestampFormat;
            if ("ISO8601".equalsIgnoreCase(timestampFormat)) {
                this.gson = new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                        .create();
            } else {
                this.gson = new GsonBuilder()
                        .setDateFormat(timestampFormat)
                        .create();
            }
        }
    }

    /**
     * Sets whether to include log level.
     *
     * 设置是否包含日志级别。
     *
     * @param includeLevel whether to include level / 是否包含level字段
     */
    public void setIncludeLevel(boolean includeLevel) {
        this.includeLevel = includeLevel;
    }

    /**
     * Sets whether to include logger name.
     *
     * 设置是否包含Logger名称。
     *
     * @param includeLogger whether to include logger / 是否包含loggerName字段
     */
    public void setIncludeLogger(boolean includeLogger) {
        this.includeLogger = includeLogger;
    }

    /**
     * Sets whether to include thread information.
     *
     * 设置是否包含线程信息。
     *
     * @param includeThread whether to include thread / 是否包含thread字段
     */
    public void setIncludeThread(boolean includeThread) {
        this.includeThread = includeThread;
    }

    /**
     * Sets whether to include MDC context.
     *
     * 设置是否包含MDC上下文。
     *
     * @param includeMdc whether to include MDC / 是否包含mdc字段
     */
    public void setIncludeMdc(boolean includeMdc) {
        this.includeMdc = includeMdc;
    }

    /**
     * Sets whether to include node ID.
     *
     * 设置是否包含节点ID。
     *
     * @param includeNodeId whether to include node ID / 是否包含nodeId字段
     */
    public void setIncludeNodeId(boolean includeNodeId) {
        this.includeNodeId = includeNodeId;
    }

    /**
     * Sets whether to include log key.
     *
     * 设置是否包含键值。
     *
     * @param includeKey whether to include key / 是否包含key字段
     */
    public void setIncludeKey(boolean includeKey) {
        this.includeKey = includeKey;
    }

    /**
     * Sets the JSON formatting pattern (compatible with TextFormatter interface).
     *
     * 设置JSON格式化模式（兼容TextFormatter接口）。
     *
     * @param pattern the JSON pattern string / JSON模式字符串
     */
    public void setPattern(String pattern) {
        // 对于JSON formatter，pattern可能包含配置信息
        // 这里简单处理，如果有需要可以解析pattern中的配置
        if (pattern != null && !pattern.isEmpty()) {
            // 可以解析pattern中的配置，但当前实现简单忽略
            logger.debug("setPattern called with: " + pattern);
        }
    }

    @Override
    public String format(LogEvent event, Map<String, Object> context) {
        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }

        Map<String, Object> logMap = new HashMap<>();

        // 时间戳 - 使用配置的格式
        logMap.put("timestamp", event.getTimestampMillis());

        // 根据配置决定包含哪些字段
        if (includeLevel) {
            logMap.put("level", event.getLevel());
        }
        if (includeLogger) {
            logMap.put("loggerName", event.getLoggerName());
        }

        // 消息总是包含
        logMap.put("message", event.getMessage());

        if (includeNodeId && event.getNodeId() != null) {
            logMap.put("nodeId", event.getNodeId());
        }
        if (includeKey && event.getKey() != null) {
            logMap.put("key", event.getKey());
        }
        if (includeThread) {
            logMap.put("thread", event.getThreadName());
        }

        // 异常信息
        if (event.getThrowable() != null) {
            Map<String, Object> exceptionMap = new HashMap<>();
            exceptionMap.put("type", event.getThrowable().getClass().getName());
            exceptionMap.put("message", event.getThrowable().getMessage());

            // 转换堆栈跟踪
            String[] stackTrace = new String[event.getThrowable().getStackTrace().length];
            for (int i = 0; i < stackTrace.length; i++) {
                stackTrace[i] = event.getThrowable().getStackTrace()[i].toString();
            }
            exceptionMap.put("stackTrace", stackTrace);

            logMap.put("exception", exceptionMap);
        }

        // MDC上下文
        if (includeMdc && event.getMdc() != null && !event.getMdc().isEmpty()) {
            logMap.put("mdc", event.getMdc());
        }

        // 额外上下文信息
        if (context != null && !context.isEmpty()) {
            logMap.put("context", context);
        }

        // 返回时自动加入换行符
        return gson.toJson(logMap) + System.lineSeparator();
    }
}
