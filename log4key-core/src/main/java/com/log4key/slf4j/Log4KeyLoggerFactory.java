/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.slf4j;

import com.log4key.LogManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Log4Key SLF4J LoggerFactory implementation.
 *
 * Log4Key的SLF4J LoggerFactory实现类。
 */
public class Log4KeyLoggerFactory implements ILoggerFactory {

    /**
     * 单例实例
     */
    private static final Log4KeyLoggerFactory INSTANCE = new Log4KeyLoggerFactory();

    /**
     * 缓存Logger实例的ConcurrentHashMap，确保线程安全
     */
    private final ConcurrentMap<String, Logger> loggerMap = new ConcurrentHashMap<>();

    /**
     * 获取单例实例
     * @return Log4KeyLoggerFactory实例
     */
    public static Log4KeyLoggerFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 通过Class类型获取Logger实例
     * @param clazz 类对象
     * @return Logger实例
     */
    public static Logger getLogger(Class<?> clazz) {
        if (clazz == null) {
            return INSTANCE.getLogger((String) null);
        }
        return INSTANCE.getLogger(clazz.getName());
    }

    /**
     * 获取或创建指定名称的Logger实例
     * 如果Logger不存在，则创建新实例并缓存；如果已存在，则直接返回缓存实例
     *
     * @param name Logger名称
     * @return Logger实例
     */
    @Override
    public Logger getLogger(String name) {
        // 确保日志系统已初始化
        LogManager.ensureInitialized(null);

        // 如果name为null，使用默认名称
        if (name == null) {
            name = "ROOT";
        }

        // 使用computeIfAbsent确保线程安全的单例创建
        return loggerMap.computeIfAbsent(name, Log4KeyLogger::new);
    }

    /**
     * Gets a Log4KeyLogger instance by Class type.
     *
     * 通过Class类型获取Log4KeyLogger实例。
     *
     * @param clazz the Class object / 类对象
     * @return the Log4KeyLogger instance / Log4KeyLogger实例
     */
    public static Log4KeyLogger getLog4KeyLogger(Class<?> clazz) {
        Logger logger = getLogger(clazz);
        if (logger instanceof Log4KeyLogger) {
            return (Log4KeyLogger) logger;
        }
        throw new IllegalStateException("Logger is not a Log4KeyLogger instance");
    }

    /**
     * Gets a Log4KeyLogger instance by name.
     *
     * 通过名称获取Log4KeyLogger实例。
     *
     * @param name the name / 名称
     * @return the Log4KeyLogger instance / Log4KeyLogger实例
     */
    public static Log4KeyLogger getLog4KeyLogger(String name) {
        Logger logger = INSTANCE.getLogger(name);
        if (logger instanceof Log4KeyLogger) {
            return (Log4KeyLogger) logger;
        }
        throw new IllegalStateException("Logger is not a Log4KeyLogger instance");
    }

    /**
     * Clears all cached Logger instances.
     *
     * 清除所有缓存的Logger实例。
     */
    public void clear() {
        loggerMap.clear();
    }

    /**
     * 获取当前缓存的Logger数量
     *
     * @return Logger数量
     */
    public int getLoggerCount() {
        return loggerMap.size();
    }

    /**
     * Checks if a Logger with the specified name exists.
     *
     * 检查是否存在指定名称的Logger实例。
     *
     * @param name the Logger name / Logger名称
     * @return true if exists / 如果存在返回true
     */
    public boolean hasLogger(String name) {
        return loggerMap.containsKey(name);
    }
}
