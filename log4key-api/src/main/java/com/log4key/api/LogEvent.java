/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

import java.util.Collections;
import java.util.Map;

/**
 * Log event entity that encapsulates all log information.
 *
 * 日志事件实体类，封装日志的所有信息。
 */
public class LogEvent implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间戳（毫秒级）
     */
    private final long timestampMillis;

    /**
     * 日志级别
     */
    private final String level;

    /**
     * 日志名称
     */
    private final String loggerName;

    /**
     * 日志消息
     */
    private final String message;

    /**
     * 线程名
     */
    private final String threadName;

    // 补充属性 (Supplementary Attributes)

    /**
     * 异常信息
     */
    private final Throwable throwable;

    /**
     * Marker名称
     */
    private final String markerName;

    /**
     * 调用者类名
     */
    private final String className;

    /**
     * 调用者方法名
     */
    private final String methodName;

    /**
     * 调用者文件名
     */
    private final String fileName;

    /**
     * 调用者行号
     */
    private final int lineNumber;

    /**
     * MDC上下文信息（标记为transient，避免序列化）
     */
    private final transient Map<String, Object> mdc;

    /**
     * 日志上下文（标记为transient，避免序列化）
     */
    private final transient LogContext logContext;

    /**
     * 日志主键对象（支持序列化，用于分布式场景）
     */
    private final ILogKey logKey;

    // 派生属性 (Derived Attributes)
    // key 和 nodeId 属性已移除，直接从 logKey 获取

    /**
     * Package-private constructor, use LogEventBuilder to create instances.
     *
     * 包私有构造方法，使用LogEventBuilder创建实例。
     *
     * @param builder LogEvent builder / LogEvent构建器
     */
    LogEvent(LogEventBuilder builder) {
        // 必须属性
        this.timestampMillis = builder.getTimestampMillis();
        this.level = builder.getLevel();
        this.loggerName = builder.getLoggerName();
        this.message = builder.getMessage();
        this.threadName = builder.getThreadName();

        // 补充属性
        this.throwable = builder.getThrowable();
        this.markerName = builder.getMarkerName();
        this.className = builder.getClassName();
        this.methodName = builder.getMethodName();
        this.fileName = builder.getFileName();
        this.lineNumber = builder.getLineNumber();
        // 收紧边界，确保mdc是不可变的
        this.mdc = Collections.unmodifiableMap(builder.getMdc());
        // 收紧边界，确保logContext是只读的
        this.logContext = builder.getLogContext();
        this.logKey = builder.getLogKey();
    }

    // Getter methods (移除所有Setter方法，LogEvent变为不可变对象)

    /**
     * Gets the timestamp in milliseconds.
     *
     * 获取毫秒级时间戳。
     *
     * @return timestamp in milliseconds / 毫秒级时间戳
     */
    public long getTimestampMillis() {
        return timestampMillis;
    }

    /**
     * Gets the log level.
     *
     * 获取日志级别。
     *
     * @return log level / 日志级别
     */
    public String getLevel() {
        return level;
    }

    /**
     * Gets the logger name.
     *
     * 获取日志名称。
     *
     * @return logger name / 日志名称
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Gets the log message.
     *
     * 获取日志消息。
     *
     * @return log message / 日志消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the thread name.
     *
     * 获取线程名。
     *
     * @return thread name / 线程名
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Gets the throwable associated with this log event.
     *
     * 获取与此日志事件关联的异常对象。
     *
     * @return throwable or null / 异常对象，如果没有则为null
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Gets the marker name.
     *
     * 获取Marker名称。
     *
     * @return marker name / Marker名称
     */
    public String getMarkerName() {
        return markerName;
    }

    /**
     * Gets the caller class name.
     *
     * 获取调用者类名。
     *
     * @return caller class name / 调用者类名
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the caller method name.
     *
     * 获取调用者方法名。
     *
     * @return caller method name / 调用者方法名
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the caller file name.
     *
     * 获取调用者文件名。
     *
     * @return caller file name / 调用者文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets the caller line number.
     *
     * 获取调用者行号。
     *
     * @return caller line number / 调用者行号
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Gets the MDC (Mapped Diagnostic Context) map.
     *
     * 获取MDC（映射诊断上下文）映射。
     *
     * @return unmodifiable MDC map / 不可变的MDC映射
     */
    public Map<String, Object> getMdc() {
        return mdc;
    }

    /**
     * Gets the log context.
     *
     * 获取日志上下文。
     *
     * @return log context / 日志上下文
     */
    public LogContext getLogContext() {
        return logContext;
    }

    /**
     * Gets the log key.
     *
     * 获取日志主键。
     *
     * @return log key / 日志主键
     */
    public ILogKey getLogKey() {
        return logKey;
    }

    /**
     * Gets the key value from the log key.
     *
     * 从日志主键获取键值。
     *
     * @return key value or null / 键值，如果没有则返回null
     */
    public String getKey() {
        return logKey != null ? logKey.value() : null;
    }

    /**
     * Gets the node ID from the log key.
     *
     * 从日志主键获取节点ID。
     *
     * @return node ID as string or null / 节点ID字符串形式，如果没有则返回null
     */
    public String getNodeId() {
        return logKey != null ? String.valueOf(logKey.getNodeId()) : null;
    }
}
