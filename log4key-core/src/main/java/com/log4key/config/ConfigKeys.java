/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config;

import com.log4key.appender.BuiltinAppenderType;
import com.log4key.config.key.ConfigKey;
import com.log4key.config.model.OutputLevelPolicy;

import java.util.*;




/**
 * Configuration key constants class.
 *
 * 配置键名常量类。
 */
public final class ConfigKeys {

    // 私有构造函数，防止实例化
    private ConfigKeys() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }




    // ========================== 全局配置键 ==========================

    /**
     * 全局默认准入级别
     */
    public static final String DEFAULT_ADMISSION_LEVEL = "defaultLevel";

    /**
     * 执行器配置
     */
    public static final String EXECUTOR = "executor";

    /**
     * 执行器类型
     */
    public static final String EXECUTOR_TYPE = "executorType";

    /**
     * 核心线程数
     */
    public static final String EXECUTOR_THREADS_SIZE = "executor.threads";

    /**
     * 执行器队列大小
     */
    public static final String EXECUTOR_QUEUE_SIZE = "executor.queueSize";

    /**
     * 缓冲区大小
     */
    public static final String BUFFER_SIZE = "bufferSize";

    /**
     * 最大文件大小（MB）
     */
    public static final String MAX_FILE_SIZE_MB = "maxFileSizeMB";

    /**
     * 最大备份索引
     */
    public static final String MAX_BACKUP_INDEX = "maxBackupIndex";

    /**
     * 默认字符集
     */
    public static final String DEFAULT_CHARSET = "defaultCharset";

    /**
     * 根目录
     */
    public static final String ROOT_DIRECTORY = "rootDirectory";

    /**
     * 控制台启用开关
     */
    public static final String CONSOLE_ENABLED = "consoleEnabled";

    // ========================== Root Logger配置键 ==========================

    /**
     * Root Logger配置前缀
     */
    public static final String ROOT_LOGGER_PREFIX = "rootLogger.";

    /**
     * Root Logger级别
     */
    public static final String ROOT_LOGGER_LEVEL = ROOT_LOGGER_PREFIX + "level";

    /**
     * Root Logger的Appenders
     */
    public static final String ROOT_LOGGER_APPENDERS = ROOT_LOGGER_PREFIX + "appenders";

    // ========================== Appender配置键 ==========================

    /**
     * Appender配置前缀
     */
    public static final String APPENDERS_PREFIX = "appenders.";

    /**
     * Appender类型
     */
    public static final String APPENDER_TYPE = "type";

    /**
     * Console Appender类型值
     */
    public static final String APPENDER_TYPE_CONSOLE = BuiltinAppenderType.CONSOLE.getId();

    /**
     * File Appender类型值
     */
    public static final String APPENDER_TYPE_FILE = BuiltinAppenderType.FILE.getId();

    /**
     * Appender使用的Formatter
     */
    public static final String APPENDER_FORMATTER = "formatter";

    /**
     * Appender输出准入级别
     */
    public static final String APPENDER_OUTPUT_ADMISSION_LEVEL = "level";

    /**
     * Appender输出级别策略
     */
    public static final String APPENDER_OUTPUT_LEVEL_POLICY = "levelPolicy";

    /**
     * Appender目录
     */
    public static final String APPENDER_DIRECTORY = "directory";

    /**
     * Appender文件名
     */
    public static final String APPENDER_FILE_NAME = "fileName";

    /**
     * Appender字符集
     */
    public static final String APPENDER_CHARSET = "charset";

    // ========================== Formatter配置键 ==========================

    /**
     * Formatter配置前缀
     */
    public static final String FORMATTERS_PREFIX = "formatters.";

    /**
     * Formatter模式
     */
    public static final String FORMATTER_PATTERN = "pattern";

    // ========================== Logger配置键 ==========================

    /**
     * Logger配置前缀
     */
    public static final String LOGGERS_PREFIX = "loggers.";

    /**
     * Logger级别后缀
     */
    public static final String LOGGER_LEVEL_SUFFIX = ".level";

    /**
     * Logger Appenders后缀
     */
    public static final String LOGGER_APPENDERS_SUFFIX = ".appenders";

    // ========================== 其他配置键 ==========================

    /**
     * 包含位置信息开关
     */
    public static final String INCLUDE_LOCATION = "includeLocation";

    /**
     * 写入器空闲超时时间
     */
    public static final String WRITER_IDLE_TIMEOUT = "writerIdleTimeout";

    /**
     * 最大批量大小
     */
    public static final String MAX_BATCH_SIZE = "maxBatchSize";

    /**
     * 最大文件写入器数量
     */
    public static final String MAX_FILE_WRITERS = "maxFileWriters";

    /**
     * Appender名称
     */
    public static final String APPENDER_NAME = "appenderName";

    // ========================== ConfigKey 语义化键（新增） ==========================

    /**
     * 核心线程池大小（语义化键）
     */
    public static final ConfigKey<Integer> EXECUTOR_THREADS_SIZE_KEY =
        new ConfigKey<>(EXECUTOR_THREADS_SIZE, Integer.class, 4);

    /**
     * 执行器类型（语义化键）
     */
    public static final ConfigKey<String> EXECUTOR_TYPE_KEY =
        new ConfigKey<>(EXECUTOR_TYPE, String.class, "KEY_BASED");

    /**
     * 执行器队列大小（语义化键）
     */
    public static final ConfigKey<Integer> EXECUTOR_QUEUE_SIZE_KEY =
        new ConfigKey<>(EXECUTOR_QUEUE_SIZE, Integer.class, 8192);

    /**
     * 缓冲区大小（语义化键）
     */
    public static final ConfigKey<Integer> BUFFER_SIZE_KEY =
        new ConfigKey<>(BUFFER_SIZE, Integer.class, 1024);

     /**
     * 最大文件大小（MB）（语义化键）
     */
    public static final ConfigKey<Integer> MAX_FILE_SIZE_MB_KEY =
        new ConfigKey<>(MAX_FILE_SIZE_MB, Integer.class, 100);

    /**
     * 最大备份索引（语义化键）
     */
    public static final ConfigKey<Integer> MAX_BACKUP_INDEX_KEY =
        new ConfigKey<>(MAX_BACKUP_INDEX, Integer.class, 7);

    /**
     * 默认字符集（语义化键）
     */
    public static final ConfigKey<String> DEFAULT_CHARSET_KEY =
        new ConfigKey<>(DEFAULT_CHARSET, String.class, "UTF-8");

    /**
     * 全局默认准入级别（语义化键）
     */
    public static final ConfigKey<String> DEFAULT_ADMISSION_LEVEL_KEY =
        new ConfigKey<>(DEFAULT_ADMISSION_LEVEL, String.class, "INFO");

    /**
     * 根目录（语义化键）
     */
    public static final ConfigKey<String> ROOT_DIRECTORY_KEY =
        new ConfigKey<>(ROOT_DIRECTORY, String.class, "./logs");

    /**
     * 包含位置信息开关（语义化键）
     */
    public static final ConfigKey<Boolean> INCLUDE_LOCATION_KEY =
        new ConfigKey<>(INCLUDE_LOCATION, Boolean.class, true);

    /**
     * Appender类型（语义化键）
     */
    public static final ConfigKey<String> APPENDER_TYPE_KEY =
        new ConfigKey<>(APPENDER_TYPE, String.class, "Console");

    /**
     * Appender使用的Formatter（语义化键）
     */
    public static final ConfigKey<String> APPENDER_FORMATTER_KEY =
        new ConfigKey<>(APPENDER_FORMATTER, String.class, "text");

    /**
     * Appender目录（语义化键）
     */
    public static final ConfigKey<String> APPENDER_DIRECTORY_KEY =
        new ConfigKey<>(APPENDER_DIRECTORY, String.class, null);

    /**
     * Appender文件名（语义化键）
     */
    public static final ConfigKey<String> APPENDER_FILE_NAME_KEY =
        new ConfigKey<>(APPENDER_FILE_NAME, String.class, "{key}.log");

    /**
     * Appender字符集（语义化键）
     */
    public static final ConfigKey<String> APPENDER_CHARSET_KEY =
        new ConfigKey<>(APPENDER_CHARSET, String.class, null);

    /**
     * Appender名称（语义化键）
     */
    public static final ConfigKey<String> APPENDER_NAME_KEY =
        new ConfigKey<>(APPENDER_NAME, String.class, "file");

    /**
     * 写入器空闲超时时间（毫秒）（语义化键）
     */
    public static final ConfigKey<Long> WRITER_IDLE_TIMEOUT_KEY =
        new ConfigKey<>(WRITER_IDLE_TIMEOUT, Long.class, 3600000L);

    /**
     * 最大批量大小（语义化键）
     */
    public static final ConfigKey<Integer> MAX_BATCH_SIZE_KEY =
        new ConfigKey<>(MAX_BATCH_SIZE, Integer.class, 100);

    /**
     * 最大文件写入器数量（语义化键）
     */
    public static final ConfigKey<Integer> MAX_FILE_WRITERS_KEY =
        new ConfigKey<>(MAX_FILE_WRITERS, Integer.class, 1000);

    /**
     * 级别包含开关（语义化键）
     */
    public static final String LEVEL_INCLUSION = "levelInclusion";
    public static final ConfigKey<Boolean> LEVEL_INCLUSION_KEY =
        new ConfigKey<>(LEVEL_INCLUSION, Boolean.class, false);


    /**
     * Appender输出准入级别（语义化键）
     */
    public static final ConfigKey<String> APPENDER_LEVEL_KEY =
        new ConfigKey<>(APPENDER_OUTPUT_ADMISSION_LEVEL, String.class, "INFO");

    /**
     * 输出级别策略（语义化键）
     */
    public static final ConfigKey<String> APPENDER_OUTPUT_LEVEL_POLICY_KEY =
            new ConfigKey<>(APPENDER_OUTPUT_LEVEL_POLICY, String.class, OutputLevelPolicy.AT_LEAST.name());

    /**
     * Formatter模式（语义化键）
     */
    public static final ConfigKey<String> FORMATTER_PATTERN_KEY =
        new ConfigKey<>(FORMATTER_PATTERN, String.class, null);

    /**
     * 控制台启用开关（语义化键）
     */
    public static final ConfigKey<Boolean> CONSOLE_ENABLED_KEY =
        new ConfigKey<>(CONSOLE_ENABLED, Boolean.class, true);

    /**
     * 包含级别信息开关（语义化键）
     */
    public static final String INCLUDE_LEVEL = "includeLevel";
    public static final ConfigKey<Boolean> INCLUDE_LEVEL_KEY =
        new ConfigKey<>(INCLUDE_LEVEL, Boolean.class, true);

    /**
     * 包含Logger名称开关（语义化键）
     */
    public static final String INCLUDE_LOGGER = "includeLogger";
    public static final ConfigKey<Boolean> INCLUDE_LOGGER_KEY =
        new ConfigKey<>(INCLUDE_LOGGER, Boolean.class, true);

    /**
     * 包含线程信息开关（语义化键）
     */
    public static final String INCLUDE_THREAD = "includeThread";
    public static final ConfigKey<Boolean> INCLUDE_THREAD_KEY =
        new ConfigKey<>(INCLUDE_THREAD, Boolean.class, true);

    /**
     * 包含MDC信息开关（语义化键）
     */
    public static final String INCLUDE_MDC = "includeMdc";
    public static final ConfigKey<Boolean> INCLUDE_MDC_KEY =
        new ConfigKey<>(INCLUDE_MDC, Boolean.class, false);

    /**
     * 包含时间戳开关（语义化键）
     */
    public static final String INCLUDE_TIMESTAMP = "includeTimestamp";
    public static final ConfigKey<Boolean> INCLUDE_TIMESTAMP_KEY =
        new ConfigKey<>(INCLUDE_TIMESTAMP, Boolean.class, true);

    /**
     * Root Logger级别（语义化键）
     */
    public static final ConfigKey<String> ROOT_LOGGER_LEVEL_KEY =
            new ConfigKey<>(ROOT_LOGGER_LEVEL, String.class, "INFO");

    /**
     * Root Logger Appenders（语义化键）
     */
    public static final ConfigKey<String> ROOT_LOGGER_APPENDERS_KEY =
            new ConfigKey<>(ROOT_LOGGER_APPENDERS, String.class, "CONSOLE");

    /**
     * 关闭钩子开关（语义化键）
     */
    public static final String SHUTDOWN_HOOK = "shutdownHook";
    public static final ConfigKey<Boolean> SHUTDOWN_HOOK_KEY =
        new ConfigKey<>(SHUTDOWN_HOOK, Boolean.class, true);

    /**
     * 所有ConfigKey的集合（按添加顺序）
     */
    public static final Map<String, ConfigKey<?>> ALL_KEYS;

    static {
        Map<String, ConfigKey<?>> allKeys = new LinkedHashMap<>();
        // 全局配置键
        allKeys.put(DEFAULT_CHARSET_KEY.name(), DEFAULT_CHARSET_KEY);
        allKeys.put(DEFAULT_ADMISSION_LEVEL_KEY.name(), DEFAULT_ADMISSION_LEVEL_KEY);
        allKeys.put(ROOT_DIRECTORY_KEY.name(), ROOT_DIRECTORY_KEY);
        allKeys.put(EXECUTOR_THREADS_SIZE_KEY.name(), EXECUTOR_THREADS_SIZE_KEY);
        allKeys.put(EXECUTOR_TYPE_KEY.name(), EXECUTOR_TYPE_KEY);
        allKeys.put(EXECUTOR_QUEUE_SIZE_KEY.name(), EXECUTOR_QUEUE_SIZE_KEY);
        allKeys.put(BUFFER_SIZE_KEY.name(), BUFFER_SIZE_KEY);
        allKeys.put(MAX_FILE_SIZE_MB_KEY.name(), MAX_FILE_SIZE_MB_KEY);
        allKeys.put(MAX_BACKUP_INDEX_KEY.name(), MAX_BACKUP_INDEX_KEY);
        allKeys.put(INCLUDE_LOCATION_KEY.name(), INCLUDE_LOCATION_KEY);
        allKeys.put(CONSOLE_ENABLED_KEY.name(), CONSOLE_ENABLED_KEY);
        allKeys.put(SHUTDOWN_HOOK_KEY.name(), SHUTDOWN_HOOK_KEY);

        // Appender配置键
        allKeys.put(APPENDER_TYPE_KEY.name(), APPENDER_TYPE_KEY);
        allKeys.put(APPENDER_NAME_KEY.name(), APPENDER_NAME_KEY);
        allKeys.put(APPENDER_DIRECTORY_KEY.name(), APPENDER_DIRECTORY_KEY);
        allKeys.put(APPENDER_FILE_NAME_KEY.name(), APPENDER_FILE_NAME_KEY);
        allKeys.put(APPENDER_CHARSET_KEY.name(), APPENDER_CHARSET_KEY);
        allKeys.put(APPENDER_FORMATTER_KEY.name(), APPENDER_FORMATTER_KEY);
        allKeys.put(WRITER_IDLE_TIMEOUT_KEY.name(), WRITER_IDLE_TIMEOUT_KEY);
        allKeys.put(MAX_BATCH_SIZE_KEY.name(), MAX_BATCH_SIZE_KEY);
        allKeys.put(MAX_FILE_WRITERS_KEY.name(), MAX_FILE_WRITERS_KEY);
        allKeys.put(LEVEL_INCLUSION_KEY.name(), LEVEL_INCLUSION_KEY);
        allKeys.put(APPENDER_LEVEL_KEY.name(), APPENDER_LEVEL_KEY);
        allKeys.put(APPENDER_OUTPUT_LEVEL_POLICY_KEY.name(), APPENDER_OUTPUT_LEVEL_POLICY_KEY);

        // Formatter配置键
        allKeys.put(FORMATTER_PATTERN_KEY.name(), FORMATTER_PATTERN_KEY);
        allKeys.put(INCLUDE_LEVEL_KEY.name(), INCLUDE_LEVEL_KEY);
        allKeys.put(INCLUDE_LOGGER_KEY.name(), INCLUDE_LOGGER_KEY);
        allKeys.put(INCLUDE_THREAD_KEY.name(), INCLUDE_THREAD_KEY);
        allKeys.put(INCLUDE_MDC_KEY.name(), INCLUDE_MDC_KEY);
        allKeys.put(INCLUDE_TIMESTAMP_KEY.name(), INCLUDE_TIMESTAMP_KEY);

        // ROOT配置键
        allKeys.put(ROOT_LOGGER_LEVEL_KEY.name(), ROOT_LOGGER_LEVEL_KEY);
        allKeys.put(ROOT_LOGGER_APPENDERS_KEY.name(), ROOT_LOGGER_APPENDERS_KEY);

        ALL_KEYS = Collections.unmodifiableMap(allKeys);
    }
}