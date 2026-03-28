/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Log event builder for creating LogEvent instances.
 *
 * 日志事件构建器，用于创建LogEvent实例。
 */
public class LogEventBuilder {
    // 必须属性 (Necessary Attributes)

    /**
     * 日志级别
     */
    private String level;

    /**
     * 日志名称
     */
    private String loggerName;

    /**
     * 日志消息
     */
    private String message;

    /**
     * 线程名
     */
    private String threadName;

    /**
     * 时间戳（毫秒级）
     */
    private long timestampMillis;

    // 补充属性 (Supplementary Attributes)

    /**
     * 异常信息
     */
    private Throwable throwable;

    /**
     * Marker名称
     */
    private String markerName;

    /**
     * 调用者类名
     */
    private String className;

    /**
     * 调用者方法名
     */
    private String methodName;

    /**
     * 调用者文件名
     */
    private String fileName;

    /**
     * 调用者行号
     */
    private int lineNumber;

    /**
     * MDC上下文信息
     */
    private Map<String, Object> mdc;

    /**
     * 日志上下文
     */
    private LogContext logContext;

    /**
     * 日志主键对象
     */
    private ILogKey logKey;

    // 派生属性 (Derived Attributes)

    /**
     * 键值（派生属性）
     */
    private String key;

    /**
     * 节点ID（派生属性）
     */
    private String nodeId;

    /**
     * Static factory method to create a LogEventBuilder instance.
     *
     * 创建LogEventBuilder实例的静态工厂方法。
     *
     * @return LogEventBuilder instance / LogEventBuilder实例
     */
    public static LogEventBuilder builder() {
        return new LogEventBuilder();
    }

    // 必须属性设置方法

    /**
     * Sets the log level.
     *
     * 设置日志级别。
     *
     * @param level log level / 日志级别
     * @return this builder / 当前构建器
     */
    public LogEventBuilder level(String level) {
        this.level = level;
        return this;
    }

    /**
     * Sets the logger name.
     *
     * 设置日志名称。
     *
     * @param loggerName logger name / 日志名称
     * @return this builder / 当前构建器
     */
    public LogEventBuilder loggerName(String loggerName) {
        this.loggerName = loggerName;
        return this;
    }

    /**
     * Sets the log message.
     *
     * 设置日志消息。
     *
     * @param message log message / 日志消息
     * @return this builder / 当前构建器
     */
    public LogEventBuilder message(String message) {
        this.message = message;
        return this;
    }

    /**
     * Sets the thread name.
     *
     * 设置线程名。
     *
     * @param threadName thread name / 线程名
     * @return this builder / 当前构建器
     */
    public LogEventBuilder threadName(String threadName) {
        this.threadName = threadName;
        return this;
    }

    /**
     * Sets the timestamp in milliseconds.
     *
     * 设置毫秒级时间戳。
     *
     * @param timestampMillis timestamp in milliseconds / 毫秒级时间戳
     * @return this builder / 当前构建器
     */
    public LogEventBuilder timestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
        return this;
    }

    // 补充属性设置方法

    /**
     * Sets the throwable.
     *
     * 设置异常对象。
     *
     * @param throwable throwable / 异常对象
     * @return this builder / 当前构建器
     */
    public LogEventBuilder throwable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    /**
     * Sets the marker name.
     *
     * 设置Marker名称。
     *
     * @param markerName marker name / Marker名称
     * @return this builder / 当前构建器
     */
    public LogEventBuilder markerName(String markerName) {
        this.markerName = markerName;
        return this;
    }

    /**
     * Sets the caller class name.
     *
     * 设置调用者类名。
     *
     * @param className caller class name / 调用者类名
     * @return this builder / 当前构建器
     */
    public LogEventBuilder className(String className) {
        this.className = className;
        return this;
    }

    /**
     * Sets the caller method name.
     *
     * 设置调用者方法名。
     *
     * @param methodName caller method name / 调用者方法名
     * @return this builder / 当前构建器
     */
    public LogEventBuilder methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * Sets the caller file name.
     *
     * 设置调用者文件名。
     *
     * @param fileName caller file name / 调用者文件名
     * @return this builder / 当前构建器
     */
    public LogEventBuilder fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Sets the caller line number.
     *
     * 设置调用者行号。
     *
     * @param lineNumber caller line number / 调用者行号
     * @return this builder / 当前构建器
     */
    public LogEventBuilder lineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
        return this;
    }

    /**
     * Sets the MDC (Mapped Diagnostic Context) map.
     *
     * 设置MDC（映射诊断上下文）映射。
     *
     * @param mdc MDC map / MDC映射
     * @return this builder / 当前构建器
     */
    public LogEventBuilder mdc(Map<String, Object> mdc) {
        // 收紧边界，创建不可变映射
        this.mdc = mdc != null ? Collections.unmodifiableMap(new HashMap<>(mdc)) : Collections.emptyMap();
        return this;
    }

    /**
     * Sets the log context.
     *
     * 设置日志上下文。
     *
     * @param logContext log context / 日志上下文
     * @return this builder / 当前构建器
     */
    public LogEventBuilder logContext(LogContext logContext) {
        // 收紧边界，确保无论传入什么LogContext，都会转换为只读的
        this.logContext = logContext != null ? new ReadOnlyLogContextWrapper(logContext) : LogContext.readonly();
        return this;
    }

    /**
     * Sets the log key.
     *
     * 设置日志主键。
     *
     * @param logKey log key / 日志主键
     * @return this builder / 当前构建器
     */
    public LogEventBuilder logKey(ILogKey logKey) {
        this.logKey = logKey;
        return this;
    }

    // 派生属性设置方法

    /**
     * Sets the key value (derived attribute).
     *
     * 设置键值（派生属性）。
     *
     * @param key key value / 键值
     * @return this builder / 当前构建器
     */
    public LogEventBuilder key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Sets the node ID (derived attribute).
     *
     * 设置节点ID（派生属性）。
     *
     * @param nodeId node ID / 节点ID
     * @return this builder / 当前构建器
     */
    public LogEventBuilder nodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    /**
     * Builds the LogEvent instance.
     *
     * 构建LogEvent实例。
     *
     * @return built LogEvent instance / 构建完成的LogEvent实例
     * @throws IllegalArgumentException if required fields are missing / 如果缺少必需字段则抛出异常
     */
    public LogEvent build() {
        // 验证必须属性
        if (level == null || loggerName == null || message == null) {
            throw new IllegalArgumentException("Level, loggerName and message are required fields");
        }

        // 处理LogKey
        if (this.logKey == null && this.key != null) {
            long nId = 0;
            try {
                if (this.nodeId != null) {
                    nId = Long.parseLong(this.nodeId);
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误，使用默认值0
            }
            this.logKey = DefaultLogKey.of(this.key, nId);
        }

        // timestampMillis 和 threadName 有默认值，无需校验
        return new LogEvent(this);
    }

    /**
     * Private constructor, use static factory method to create instances.
     *
     * 私有构造方法，使用静态工厂方法创建实例。
     */
    private LogEventBuilder() {
        // 初始化默认值
        this.timestampMillis = System.currentTimeMillis();
        this.threadName = Thread.currentThread().getName();
        this.logContext = LogContext.readonly();
        this.mdc = Collections.emptyMap();
    }

    /**
     * Read-only LogContext wrapper that throws UnsupportedOperationException for all modification operations.
     *
     * 只读LogContext包装类，确保所有修改操作都会抛出UnsupportedOperationException。
     */
    private static class ReadOnlyLogContextWrapper implements LogContext {
        private final LogContext original;

        public ReadOnlyLogContextWrapper(LogContext original) {
            this.original = original;
        }

        @Override
        public void put(String key, Object value) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }

        @Override
        public <T> T get(String key) {
            return original.get(key);
        }

        @Override
        public <T> T get(String key, T defaultValue) {
            return original.get(key, defaultValue);
        }

        @Override
        public <T> T remove(String key) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }

        @Override
        public Map<String, Object> getAll() {
            return Collections.unmodifiableMap(original.getAll());
        }

        @Override
        public void merge(LogContext other) {
            throw new UnsupportedOperationException("Cannot modify read-only context");
        }
    }

    // Getter方法，供LogEvent构造函数使用

    String getLevel() {
        return level;
    }

    String getLoggerName() {
        return loggerName;
    }

    String getMessage() {
        return message;
    }

    String getThreadName() {
        return threadName;
    }

    long getTimestampMillis() {
        return timestampMillis;
    }

    Throwable getThrowable() {
        return throwable;
    }

    String getMarkerName() {
        return markerName;
    }

    String getClassName() {
        return className;
    }

    String getMethodName() {
        return methodName;
    }

    String getFileName() {
        return fileName;
    }

    int getLineNumber() {
        return lineNumber;
    }

    Map<String, Object> getMdc() {
        return mdc;
    }

    LogContext getLogContext() {
        return logContext;
    }

    ILogKey getLogKey() {
        return logKey;
    }
}
