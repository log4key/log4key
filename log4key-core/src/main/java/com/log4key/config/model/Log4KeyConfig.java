/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.model;

import com.log4key.config.ConfigKeys;
import com.log4key.config.resolver.ConfigAccumulator;
import com.log4key.config.key.ConfigKey;
import com.log4key.config.resolver.ConfigResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Log4Key core configuration model.
 *
 * Log4Key核心配置模型。
 */
public class Log4KeyConfig {

    /**
     * 配置构造器
     */
    private ConfigAccumulator accumulator;

    /**
     * 配置解析器
     */
    private ConfigResolver resolver;

    /**
     * Root Logger配置
     * 用于指定Root Logger的配置信息
     */
    private RootLoggerConfig rootLoggerConfig;
    
    /**
     * 非Root Logger配置映射
     * 用于存储所有非Root Logger的配置信息，键为Logger名称
     */
    private final Map<String, LoggerConfig> nonRootLoggers = new HashMap<>();
    
    /**
     * Appenders配置映射
     * 用于存储所有Appender的配置信息，键为Appender名称
     */
    private Map<String, AppenderConfig> appenders = new HashMap<>();
    
    /**
     * Formatters配置映射
     * 用于存储所有Formatter的配置信息，键为Formatter名称
     */
    private Map<String, FormatterConfig> formatters = new HashMap<>();
    
    /**
     * Appender合并配置缓存
     * 用于缓存每个Appender的合并配置（全局默认值 + Appender专用配置）
     */
    private final Map<String, Map<String, Object>> mergedAppenderConfigs = new HashMap<>();
    
    /**
     * 构造函数 - 初始化默认配置
     */
    public Log4KeyConfig() {
        // 初始化默认配置
        initDefaultConfig();
    }


    /**
     * 构造函数 - 使用指定配置解析器
     * @param resolver 配置解析器
     */
    public Log4KeyConfig(ConfigResolver resolver) {
        // 初始化默认配置
        initDefaultConfig();
        // 合并配置
        accumulator.merge(resolver);
    }


    /**
     * 初始化默认配置
     */
    private void initDefaultConfig() {
        // 设置全局配置
        accumulator = new ConfigAccumulator();
        accumulator.with(ConfigKeys.ROOT_DIRECTORY_KEY, "./logs")
                .with(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY, "INFO")
                .with(ConfigKeys.DEFAULT_CHARSET_KEY, "UTF-8")
                .with(ConfigKeys.INCLUDE_LOCATION_KEY, true)
                .with(ConfigKeys.MAX_FILE_SIZE_MB_KEY, 100)
                .with(ConfigKeys.MAX_BACKUP_INDEX_KEY, 7)
                .with(ConfigKeys.EXECUTOR_TYPE_KEY, "KEY_BASED")
                .with(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY, 4)
                .with(ConfigKeys.BUFFER_SIZE_KEY, 1024)
                .with(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY, 8192);

//        accumulator = new ConfigAccumulator();
//        accumulator.mergeFrom(globalConfig);
//        globalConfig = new ConfigResolver(map);
    }


    public ConfigAccumulator builder() {
        return accumulator;
    }


    /**
     * 获取全局配置
     * @param key 配置项
     * @return 配置项的值
     */
    public <T> T getGlobalConfig(ConfigKey<T> key) {
//        return globalConfig.get(key);
//        return accumulator.get(key);

        if(resolver == null){
            resolver = accumulator.freeze();
        }
        return resolver.get(key);
    }


    public RootLoggerConfig getRootLoggerConfig() {
        return rootLoggerConfig;
    }

    public void setRootLoggerConfig(RootLoggerConfig rootLoggerConfig) {
        this.rootLoggerConfig = rootLoggerConfig;
    }

    public Map<String, LoggerConfig> getNonRootLoggers() {
        return nonRootLoggers;
    }

    public void addNonRootLogger(LoggerConfig loggerConfig) {
        this.nonRootLoggers.put(loggerConfig.getName(), loggerConfig);
    }

    public Map<String, AppenderConfig> getAppenders() {
        return appenders;
    }

    public void setAppenders(Map<String, AppenderConfig> appenders) {
        this.appenders = appenders;
    }

    public void addAppender(AppenderConfig appenderConfig) {
        this.appenders.put(appenderConfig.getName(), appenderConfig);
    }

    public Map<String, FormatterConfig> getFormatters() {
        return formatters;
    }

    public void setFormatters(Map<String, FormatterConfig> formatters) {
        this.formatters = formatters;
    }

    public void addFormatter(FormatterConfig formatterConfig) {
        this.formatters.put(formatterConfig.getName(), formatterConfig);
    }
    
    /**
     * 设置Appender的合并配置
     * @param appenderName Appender名称
     * @param mergedConfig 合并后的配置
     */
    public void setMergedAppenderConfig(String appenderName, Map<String, Object> mergedConfig) {
        this.mergedAppenderConfigs.put(appenderName, mergedConfig);
    }
    
    /**
     * 获取Appender的合并配置
     * @param appenderName Appender名称
     * @return 合并后的配置，如果不存在则返回null
     */
    public Map<String, Object> getMergedAppenderConfig(String appenderName) {
        return this.mergedAppenderConfigs.get(appenderName);
    }
    
    /**
     * 检查是否存在Appender的合并配置
     * @param appenderName Appender名称
     * @return 如果存在合并配置则返回true
     */
    public boolean hasMergedAppenderConfig(String appenderName) {
        return this.mergedAppenderConfigs.containsKey(appenderName);
    }

    @Override
    public String toString() {
        return "Log4KeyConfig{" + resolver.toString() + '}';
    }
}