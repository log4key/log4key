/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.internal;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.ConsoleHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Internal logger utility.
 *
 * 内部日志工具类。
 */
public class InternalLogger {
    
    /**
     * 日志实例缓存
     */
    private static final ConcurrentMap<String, InternalLogger> LOGGER_CACHE = new ConcurrentHashMap<>();
    
    /**
     * DEBUG级别开关，由系统属性控制
     */
    private static final boolean DEBUG_ENABLED = Boolean.getBoolean("log4key.internal.debug");
    
    /**
     * 日志名称
     */
    private final String name;
    
    /**
     * JUL Logger实例
     */
    private final Logger logger;
    
    /**
     * 私有构造方法，初始化日志配置
     * @param name 日志名称
     */
    private InternalLogger(String name) {
        this.name = name;
        this.logger = Logger.getLogger("com.log4key.internal." + name);
        configureLogger();
    }
    
    /**
     * 配置JUL Logger
     */
    private void configureLogger() {
        // 移除默认处理器，避免重复输出
        for (java.util.logging.Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
        }
        
        // 创建控制台处理器
        ConsoleHandler handler = new ConsoleHandler();
        
        // 配置格式化器
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public String format(java.util.logging.LogRecord record) {
                StringBuilder sb = new StringBuilder();
                sb.append(java.time.LocalDateTime.now());
                sb.append(" ");
                sb.append(record.getLevel().getName());
                sb.append(" [");
                sb.append(name);
                sb.append("] ");
                sb.append(record.getMessage());
                sb.append(System.lineSeparator());
                if (record.getThrown() != null) {
                    sb.append(record.getThrown());
                    sb.append(System.lineSeparator());
                }
                return sb.toString();
            }
        });
        
        // 设置处理器级别
        handler.setLevel(Level.ALL);
        
        // 添加处理器到Logger
        logger.addHandler(handler);
        
        // 设置Logger级别
        logger.setLevel(Level.ALL);
        
        // 禁止父Logger继承，避免重复输出
        logger.setUseParentHandlers(false);
    }
    
    /**
     * Gets a logger instance for the specified class.
     *
     * 根据类获取日志实例。
     *
     * @param clazz the class object / 类对象
     * @return the InternalLogger instance / InternalLogger实例
     */
    public static InternalLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }
    
    /**
     * Gets a logger instance for the specified name.
     *
     * 根据名称获取日志实例。
     *
     * @param name the logger name / 日志名称
     * @return the InternalLogger instance / InternalLogger实例
     */
    public static InternalLogger getLogger(String name) {
        return LOGGER_CACHE.computeIfAbsent(name, InternalLogger::new);
    }
    
    /**
     * Logs a DEBUG level message.
     *
     * 输出DEBUG级别日志。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param args the placeholder arguments / 占位符参数
     */
    public void debug(String message, Object... args) {
        if (DEBUG_ENABLED) {
            String formattedMessage = formatMessage(message, args);
            logger.fine(formattedMessage);
        }
    }
    
    /**
     * Logs a DEBUG level message with exception.
     *
     * 输出DEBUG级别日志，支持异常。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param thrown the exception object / 异常对象
     * @param args the placeholder arguments / 占位符参数
     */
    public void debug(String message, Throwable thrown, Object... args) {
        if (DEBUG_ENABLED) {
            String formattedMessage = formatMessage(message, args);
            logger.log(Level.FINE, formattedMessage, thrown);
        }
    }
    
    /**
     * Logs an INFO level message.
     *
     * 输出INFO级别日志。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param args the placeholder arguments / 占位符参数
     */
    public void info(String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        logger.info(formattedMessage);
    }
    
    /**
     * Logs an INFO level message with exception.
     *
     * 输出INFO级别日志，支持异常。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param thrown the exception object / 异常对象
     * @param args the placeholder arguments / 占位符参数
     */
    public void info(String message, Throwable thrown, Object... args) {
        String formattedMessage = formatMessage(message, args);
        logger.log(Level.INFO, formattedMessage, thrown);
    }
    
    /**
     * Logs a WARN level message.
     *
     * 输出WARN级别日志。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param args the placeholder arguments / 占位符参数
     */
    public void warn(String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        logger.warning(formattedMessage);
    }
    
    /**
     * Logs a WARN level message with exception.
     *
     * 输出WARN级别日志，支持异常。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param thrown the exception object / 异常对象
     * @param args the placeholder arguments / 占位符参数
     */
    public void warn(String message, Throwable thrown, Object... args) {
        String formattedMessage = formatMessage(message, args);
        logger.log(Level.WARNING, formattedMessage, thrown);
    }

    /**
     * Logs an ERROR level message.
     *
     * 输出ERROR级别日志。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param args the placeholder arguments / 占位符参数
     */
    public void error(String message, Object... args) {
        String formattedMessage = formatMessage(message, args);
        logger.severe(formattedMessage);
    }

    /**
     * Logs an ERROR level message with exception.
     *
     * 输出ERROR级别日志，支持异常。
     *
     * @param message the log message, supports {} placeholders / 日志消息，支持{}占位符
     * @param thrown the exception object / 异常对象
     * @param args the placeholder arguments / 占位符参数
     */
    public void error(String message, Throwable thrown, Object... args) {
        String formattedMessage = formatMessage(message, args);
        logger.log(Level.SEVERE, formattedMessage, thrown);
    }

    /**
     * Formats message by replacing {} placeholders.
     *
     * 格式化消息，替换{}占位符。
     *
     * @param message the original message / 原始消息
     * @param args the placeholder arguments / 占位符参数
     * @return the formatted message / 格式化后的消息
     */
    private String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int lastIndex = 0;
        
        while (argIndex < args.length) {
            int placeholderIndex = message.indexOf("{}", lastIndex);
            if (placeholderIndex == -1) {
                break;
            }
            
            sb.append(message, lastIndex, placeholderIndex);
            sb.append(args[argIndex]);
            
            lastIndex = placeholderIndex + 2;
            argIndex++;
        }
        
        sb.append(message.substring(lastIndex));
        return sb.toString();
    }
    
    /**
     * 检查DEBUG级别是否启用
     * @return true if DEBUG is enabled, false otherwise
     */
    public boolean isDebugEnabled() {
        return DEBUG_ENABLED;
    }
    
    /**
     * Gets the logger name.
     *
     * 获取日志名称。
     *
     * @return the logger name / 日志名称
     */
    public String getName() {
        return name;
    }
}
