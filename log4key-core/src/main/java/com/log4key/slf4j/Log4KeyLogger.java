/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.slf4j;

import com.log4key.LogManager;
import com.log4key.api.ILogKey;
import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.location.CallerLocation;
import com.log4key.location.DefaultLocationProvider;
import com.log4key.location.LocationProvider;
import org.slf4j.Marker;
import org.slf4j.spi.LocationAwareLogger;

import java.util.Map;

/**
 * Log4Key SLF4J Logger implementation.
 *
 * Log4Key的SLF4J Logger接口实现类。
 */
public class Log4KeyLogger implements LocationAwareLogger {

    // 当前类的完全限定类名，用于LocationAwareLogger接口
    private static final String FQCN = Log4KeyLogger.class.getName();

    private static final int TRACE_INT = LocationAwareLogger.TRACE_INT;
    private static final int DEBUG_INT = LocationAwareLogger.DEBUG_INT;
    private static final int INFO_INT = LocationAwareLogger.INFO_INT;
    private static final int WARN_INT = LocationAwareLogger.WARN_INT;
    private static final int ERROR_INT = LocationAwareLogger.ERROR_INT;

    private final String name;
    private volatile int currentLogLevel = INFO_INT;
    // 行号记录开关，默认开启
    private volatile boolean includeLocation = true;
    // 位置信息提供者
    private final LocationProvider locationProvider;

    /**
     * 构造函数
     *
     * @param name Logger名称
     */
    public Log4KeyLogger(String name) {
        this.name = name;
        // 初始化位置信息提供者
        this.locationProvider = DefaultLocationProvider.INSTANCE;

        // 从配置中加载初始日志级别和行号记录开关，添加空检查，避免循环依赖导致的NPE
        try {
            LogManager logManager = LogManager.getInstance();
            if (logManager != null) {
                // 使用与 LogManager.isLogLevelEnabled() 相似的流程获取日志级别
                String configuredLevel = logManager.getConfig().getLoggerAdmissionLevel(name);
                // 如果没有匹配的logger配置，直接使用全局defaultAdmissionLevel
                if (configuredLevel == null) {
                    configuredLevel = logManager.getConfig().getDefaultAdmissionLevel();
                }
                // 设置日志级别
                if (configuredLevel != null) {
                    setLogLevel(configuredLevel);
                }

                // 加载行号记录开关配置
                this.includeLocation = logManager.getConfig().isIncludeLocation();
            }
        } catch (Exception e) {
            // 忽略初始化时的异常，使用默认值
        }
    }

    /**
     * 设置日志级别
     *
     * @param logLevel 日志级别字符串
     */
    public void setLogLevel(String logLevel) {
        switch (logLevel.toUpperCase()) {
            case "TRACE":
                currentLogLevel = TRACE_INT;
                break;
            case "DEBUG":
                currentLogLevel = DEBUG_INT;
                break;
            case "WARN":
                currentLogLevel = WARN_INT;
                break;
            case "ERROR":
                currentLogLevel = ERROR_INT;
                break;
            default:
                currentLogLevel = INFO_INT;
        }
    }

    /**
     * 获取当前日志级别。
     *
     * 获取当前日志级别。
     *
     * @return the current log level / 当前日志级别
     */
    public int getLogLevel() {
        return currentLogLevel;
    }

    /**
     * 带ILogKey参数的debug日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     */
    public void debug(ILogKey logKey, String msg) {
        if (isDebugEnabled()) {
            log(null, FQCN, DEBUG_INT, msg, new Object[] { logKey }, null);
        }
    }

    /**
     * 带ILogKey参数的debug日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg    参数
     */
    public void debug(ILogKey logKey, String format, Object arg) {
        if (isDebugEnabled()) {
            log(null, FQCN, DEBUG_INT, format, new Object[] { logKey, arg }, null);
        }
    }

    /**
     * 带ILogKey参数的debug日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg1   参数1
     * @param arg2   参数2
     */
    public void debug(ILogKey logKey, String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            log(null, FQCN, DEBUG_INT, format, new Object[] { logKey, arg1, arg2 }, null);
        }
    }

    /**
     * 带ILogKey参数的debug日志方法
     *
     * @param logKey    日志主键
     * @param format    格式化消息
     * @param arguments 参数数组
     */
    public void debug(ILogKey logKey, String format, Object... arguments) {
        if (isDebugEnabled()) {
            // 将logKey插入到参数数组的开头
            Object[] newArgs = new Object[arguments.length + 1];
            newArgs[0] = logKey;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
            log(null, FQCN, DEBUG_INT, format, newArgs, null);
        }
    }

    /**
     * 带ILogKey参数的debug日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     * @param t      异常信息
     */
    public void debug(ILogKey logKey, String msg, Throwable t) {
        if (isDebugEnabled()) {
            log(null, FQCN, DEBUG_INT, msg, new Object[] { logKey }, t);
        }
    }

    /**
     * 带ILogKey参数的info日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     */
    public void info(ILogKey logKey, String msg) {
        if (isInfoEnabled()) {
            log(null, FQCN, INFO_INT, msg, new Object[] { logKey }, null);
        }
    }

    /**
     * 带ILogKey参数的info日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg    参数
     */
    public void info(ILogKey logKey, String format, Object arg) {
        if (isInfoEnabled()) {
            log(null, FQCN, INFO_INT, format, new Object[] { logKey, arg }, null);
        }
    }

    /**
     * 带ILogKey参数的info日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg1   参数1
     * @param arg2   参数2
     */
    public void info(ILogKey logKey, String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            log(null, FQCN, INFO_INT, format, new Object[] { logKey, arg1, arg2 }, null);
        }
    }

    /**
     * 带ILogKey参数的info日志方法
     *
     * @param logKey    日志主键
     * @param format    格式化消息
     * @param arguments 参数数组
     */
    public void info(ILogKey logKey, String format, Object... arguments) {
        if (isInfoEnabled()) {
            // 将logKey插入到参数数组的开头
            Object[] newArgs = new Object[arguments.length + 1];
            newArgs[0] = logKey;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
            log(null, FQCN, INFO_INT, format, newArgs, null);
        }
    }

    /**
     * 带ILogKey参数的info日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     * @param t      异常信息
     */
    public void info(ILogKey logKey, String msg, Throwable t) {
        if (isInfoEnabled()) {
            log(null, FQCN, INFO_INT, msg, new Object[] { logKey }, t);
        }
    }

    /**
     * 带ILogKey参数的warn日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     */
    public void warn(ILogKey logKey, String msg) {
        if (isWarnEnabled()) {
            log(null, FQCN, WARN_INT, msg, new Object[] { logKey }, null);
        }
    }

    /**
     * 带ILogKey参数的warn日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg    参数
     */
    public void warn(ILogKey logKey, String format, Object arg) {
        if (isWarnEnabled()) {
            log(null, FQCN, WARN_INT, format, new Object[] { logKey, arg }, null);
        }
    }

    /**
     * 带ILogKey参数的warn日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg1   参数1
     * @param arg2   参数2
     */
    public void warn(ILogKey logKey, String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            log(null, FQCN, WARN_INT, format, new Object[] { logKey, arg1, arg2 }, null);
        }
    }

    /**
     * 带ILogKey参数的warn日志方法
     *
     * @param logKey    日志主键
     * @param format    格式化消息
     * @param arguments 参数数组
     */
    public void warn(ILogKey logKey, String format, Object... arguments) {
        if (isWarnEnabled()) {
            // 将logKey插入到参数数组的开头
            Object[] newArgs = new Object[arguments.length + 1];
            newArgs[0] = logKey;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
            log(null, FQCN, WARN_INT, format, newArgs, null);
        }
    }

    /**
     * 带ILogKey参数的warn日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     * @param t      异常信息
     */
    public void warn(ILogKey logKey, String msg, Throwable t) {
        if (isWarnEnabled()) {
            log(null, FQCN, WARN_INT, msg, new Object[] { logKey }, t);
        }
    }

    /**
     * 带ILogKey参数的error日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     */
    public void error(ILogKey logKey, String msg) {
        if (isErrorEnabled()) {
            log(null, FQCN, ERROR_INT, msg, new Object[] { logKey }, null);
        }
    }

    /**
     * 带ILogKey参数的error日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg    参数
     */
    public void error(ILogKey logKey, String format, Object arg) {
        if (isErrorEnabled()) {
            log(null, FQCN, ERROR_INT, format, new Object[] { logKey, arg }, null);
        }
    }

    /**
     * 带ILogKey参数的error日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg1   参数1
     * @param arg2   参数2
     */
    public void error(ILogKey logKey, String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            log(null, FQCN, ERROR_INT, format, new Object[] { logKey, arg1, arg2 }, null);
        }
    }

    /**
     * 带ILogKey参数的error日志方法
     *
     * @param logKey    日志主键
     * @param format    格式化消息
     * @param arguments 参数数组
     */
    public void error(ILogKey logKey, String format, Object... arguments) {
        if (isErrorEnabled()) {
            // 将logKey插入到参数数组的开头
            Object[] newArgs = new Object[arguments.length + 1];
            newArgs[0] = logKey;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
            log(null, FQCN, ERROR_INT, format, newArgs, null);
        }
    }

    /**
     * 带ILogKey参数的error日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     * @param t      异常信息
     */
    public void error(ILogKey logKey, String msg, Throwable t) {
        if (isErrorEnabled()) {
            log(null, FQCN, ERROR_INT, msg, new Object[] { logKey }, t);
        }
    }

    /**
     * 带ILogKey参数的error日志方法
     *
     * @param logKey 日志主键
     * @param t      异常信息
     * @param format    格式化消息
     * @param arguments 参数数组
     */
    public void error(ILogKey logKey, Throwable t, String format, Object... arguments) {
        if (isErrorEnabled()) {
            // 将logKey插入到参数数组的开头
            Object[] newArgs = new Object[arguments.length + 1];
            newArgs[0] = logKey;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
            log(null, FQCN, ERROR_INT, format, newArgs, t);
        }
    }

    /**
     * 带ILogKey参数的trace日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     */
    public void trace(ILogKey logKey, String msg) {
        if (isTraceEnabled()) {
            log(null, FQCN, TRACE_INT, msg, new Object[] { logKey }, null);
        }
    }

    /**
     * 带ILogKey参数的trace日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg    参数
     */
    public void trace(ILogKey logKey, String format, Object arg) {
        if (isTraceEnabled()) {
            log(null, FQCN, TRACE_INT, format, new Object[] { logKey, arg }, null);
        }
    }

    /**
     * 带ILogKey参数的trace日志方法
     *
     * @param logKey 日志主键
     * @param format 格式化消息
     * @param arg1   参数1
     * @param arg2   参数2
     */
    public void trace(ILogKey logKey, String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            log(null, FQCN, TRACE_INT, format, new Object[] { logKey, arg1, arg2 }, null);
        }
    }

    /**
     * 带ILogKey参数的trace日志方法
     *
     * @param logKey    日志主键
     * @param format    格式化消息
     * @param arguments 参数数组
     */
    public void trace(ILogKey logKey, String format, Object... arguments) {
        if (isTraceEnabled()) {
            // 将logKey插入到参数数组的开头
            Object[] newArgs = new Object[arguments.length + 1];
            newArgs[0] = logKey;
            System.arraycopy(arguments, 0, newArgs, 1, arguments.length);
            log(null, FQCN, TRACE_INT, format, newArgs, null);
        }
    }

    /**
     * 带ILogKey参数的trace日志方法
     *
     * @param logKey 日志主键
     * @param msg    日志消息
     * @param t      异常信息
     */
    public void trace(ILogKey logKey, String msg, Throwable t) {
        if (isTraceEnabled()) {
            log(null, FQCN, TRACE_INT, msg, new Object[] { logKey }, t);
        }
    }

    /**
     * 设置日志级别
     *
     * @param level 日志级别
     */
    public void setLogLevel(int level) {
        this.currentLogLevel = level;
    }

    /**
     * Sets the include location flag.
     *
     * 设置行号记录开关。
     *
     * @param includeLocation whether to include location / 是否包含行号记录
     */
    public void setIncludeLocation(boolean includeLocation) {
        this.includeLocation = includeLocation;
    }

    /**
     * Gets the include location flag status.
     *
     * 获取行号记录开关状态。
     *
     * @return true if location is included / 是否包含行号记录
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    @Override
    public void trace(String msg) {
        log(null, FQCN, TRACE_INT, msg, null, null);
    }

    @Override
    public void trace(String format, Object arg) {
        log(null, FQCN, TRACE_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log(null, FQCN, TRACE_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void trace(String format, Object... arguments) {
        log(null, FQCN, TRACE_INT, format, arguments, null);
    }

    @Override
    public void trace(String msg, Throwable t) {
        log(null, FQCN, TRACE_INT, msg, null, t);
    }

    @Override
    public void trace(Marker marker, String msg) {
        log(marker, FQCN, TRACE_INT, msg, null, null);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log(marker, FQCN, TRACE_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, FQCN, TRACE_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        log(marker, FQCN, TRACE_INT, format, argArray, null);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        log(marker, FQCN, TRACE_INT, msg, null, t);
    }

    @Override
    public void debug(String msg) {
        log(null, FQCN, DEBUG_INT, msg, null, null);
    }

    @Override
    public void debug(String format, Object arg) {
        log(null, FQCN, DEBUG_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log(null, FQCN, DEBUG_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void debug(String format, Object... arguments) {
        log(null, FQCN, DEBUG_INT, format, arguments, null);
    }

    @Override
    public void debug(String msg, Throwable t) {
        log(null, FQCN, DEBUG_INT, msg, null, t);
    }

    @Override
    public void debug(Marker marker, String msg) {
        log(marker, FQCN, DEBUG_INT, msg, null, null);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log(marker, FQCN, DEBUG_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, FQCN, DEBUG_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        log(marker, FQCN, DEBUG_INT, format, argArray, null);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        log(marker, FQCN, DEBUG_INT, msg, null, t);
    }

    @Override
    public void info(String msg) {
        log(null, FQCN, INFO_INT, msg, null, null);
    }

    @Override
    public void info(String format, Object arg) {
        log(null, FQCN, INFO_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log(null, FQCN, INFO_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void info(String format, Object... arguments) {
        log(null, FQCN, INFO_INT, format, arguments, null);
    }

    @Override
    public void info(String msg, Throwable t) {
        log(null, FQCN, INFO_INT, msg, null, t);
    }

    @Override
    public void info(Marker marker, String msg) {
        log(marker, FQCN, INFO_INT, msg, null, null);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log(marker, FQCN, INFO_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, FQCN, INFO_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        log(marker, FQCN, INFO_INT, format, argArray, null);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        log(marker, FQCN, INFO_INT, msg, null, t);
    }

    @Override
    public void warn(String msg) {
        log(null, FQCN, WARN_INT, msg, null, null);
    }

    @Override
    public void warn(String format, Object arg) {
        log(null, FQCN, WARN_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log(null, FQCN, WARN_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void warn(String format, Object... arguments) {
        log(null, FQCN, WARN_INT, format, arguments, null);
    }

    @Override
    public void warn(String msg, Throwable t) {
        log(null, FQCN, WARN_INT, msg, null, t);
    }

    @Override
    public void warn(Marker marker, String msg) {
        log(marker, FQCN, WARN_INT, msg, null, null);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log(marker, FQCN, WARN_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, FQCN, WARN_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        log(marker, FQCN, WARN_INT, format, argArray, null);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        log(marker, FQCN, WARN_INT, msg, null, t);
    }

    @Override
    public void error(String msg) {
        log(null, FQCN, ERROR_INT, msg, null, null);
    }

    @Override
    public void error(String format, Object arg) {
        log(null, FQCN, ERROR_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log(null, FQCN, ERROR_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void error(String format, Object... arguments) {
        log(null, FQCN, ERROR_INT, format, arguments, null);
    }

    @Override
    public void error(String msg, Throwable t) {
        log(null, FQCN, ERROR_INT, msg, null, t);
    }

    @Override
    public void error(Marker marker, String msg) {
        log(marker, FQCN, ERROR_INT, msg, null, null);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log(marker, FQCN, ERROR_INT, format, new Object[] { arg }, null);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log(marker, FQCN, ERROR_INT, format, new Object[] { arg1, arg2 }, null);
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        log(marker, FQCN, ERROR_INT, format, argArray, null);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        log(marker, FQCN, ERROR_INT, msg, null, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return currentLogLevel <= DEBUG_INT;
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return currentLogLevel <= ERROR_INT;
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return currentLogLevel <= INFO_INT;
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled() {
        return currentLogLevel <= TRACE_INT;
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        // 暂不支持Marker，默认返回与isTraceEnabled()相同结果
        return isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return currentLogLevel <= WARN_INT;
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
        // 如果当前日志级别高于要记录的级别，则不记录
        if (currentLogLevel > level) {
            return;
        }

        // 检查argArray中是否包含ILogKey对象，提取ILogKey
        ILogKey logKey = extractLogKey(argArray);

        // 过滤掉argArray中的ILogKey
        Object[] filteredArgs = filterOutLogKey(argArray);

        // 格式化消息
        String logLevel = getLevelString(level);
        String formattedMessage = formatMessage(message, filteredArgs);

        // 创建LogEvent对象
        LogEventBuilder builder = LogEventBuilder.builder()
                .level(logLevel)
                .loggerName(name)
                .message(formattedMessage)
                .throwable(t);

        if (marker != null) {
            builder.markerName(marker.getName());
        }

        // 获取调用栈信息，提取调用者的类名、方法名、文件名和行号（根据配置开关决定是否开启）
        if (includeLocation) {
            CallerLocation location = locationProvider.getCallerLocation();
            builder.className(location.className)
                   .methodName(location.methodName)
                   .fileName(location.fileName)
                   .lineNumber(location.lineNumber);
        }

        // 设置日志主键
        if (logKey != null) {
            builder.logKey(logKey);
        } else {
            builder.nodeId("1"); // 默认节点ID
        }

        // 添加MDC上下文信息
        Map<String, String> mdcContext = new Log4KeyMDCAdapter().getCopyOfContextMap();
        if (!mdcContext.isEmpty()) {
            // 将String类型的MDC转换为Object类型，以便存储到LogEvent中
            Map<String, Object> mdcObjectMap = new java.util.HashMap<>(mdcContext);
            builder.mdc(mdcObjectMap);
        }

        // 构建LogEvent并发送给LogManager处理
        LogEvent event = builder.build();
        LogManager.getInstance().processLogEvent(event);
    }

    /**
     * Formats log message with placeholders.
     *
     * 格式化日志消息，处理占位符。
     *
     * @param message the original message / 原始消息
     * @param argArray the argument array / 参数数组
     * @return the formatted message / 格式化后的消息
     */
    private String formatMessage(String message, Object[] argArray) {
        if (message == null) {
            return "null";
        }

        if (argArray == null || argArray.length == 0) {
            return message;
        }

        // 简单的占位符替换实现
        StringBuilder result = new StringBuilder(message.length() + 50);
        int i = 0;
        int j = 0;

        while (i < message.length() && j < argArray.length) {
            int placeholderIndex = message.indexOf("{}", i);
            if (placeholderIndex == -1) {
                break;
            }

            result.append(message, i, placeholderIndex);
            result.append(argArray[j++]);
            i = placeholderIndex + 2;
        }

        // 添加剩余部分
        result.append(message.substring(i));

        // 如果还有未使用的参数，添加到消息末尾
        if (j < argArray.length) {
            result.append(" [");
            for (; j < argArray.length; j++) {
                if (j > 0) {
                    result.append(", ");
                }
                result.append(argArray[j]);
            }
            result.append("]");
        }

        return result.toString();
    }

    /**
     * Converts log level integer to string.
     *
     * 将日志级别整数转换为字符串。
     *
     * @param level the log level integer / 日志级别整数
     * @return the log level string / 日志级别字符串
     */
    private String getLevelString(int level) {
        switch (level) {
            case TRACE_INT:
                return "TRACE";
            case DEBUG_INT:
                return "DEBUG";
            case INFO_INT:
                return "INFO";
            case WARN_INT:
                return "WARN";
            case ERROR_INT:
                return "ERROR";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 从参数数组中提取ILogKey对象
     *
     * @param argArray 参数数组
     * @return ILogKey对象，如果没有则返回null
     */
    private ILogKey extractLogKey(Object[] argArray) {
        if (argArray == null) {
            return null;
        }

        for (Object arg : argArray) {
            if (arg instanceof ILogKey) {
                return (ILogKey) arg;
            }
        }
        return null;
    }

    /**
     * Filters out ILogKey objects from argument array.
     *
     * 从参数数组中过滤掉ILogKey对象。
     *
     * @param argArray the original argument array / 原始参数数组
     * @return the filtered argument array / 过滤后的参数数组
     */
    private Object[] filterOutLogKey(Object[] argArray) {
        if (argArray == null) {
            return null;
        }

        // 统计ILogKey的数量
        int logKeyCount = 0;
        for (Object arg : argArray) {
            if (arg instanceof ILogKey) {
                logKeyCount++;
            }
        }

        // 当没有ILogKey时，直接返回原始数组（避免不必要的数组复制）
        if (logKeyCount == 0) {
            return argArray;
        }

        // 计算过滤后的数组长度
        int filteredLength = argArray.length - logKeyCount;
        Object[] filteredArgs = new Object[filteredLength];

        // 填充过滤后的数组
        int index = 0;
        for (Object arg : argArray) {
            if (!(arg instanceof ILogKey)) {
                filteredArgs[index++] = arg;
            }
        }

        return filteredArgs;
    }
}
