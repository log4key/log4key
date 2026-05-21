/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config;

import com.log4key.api.LogConfig;
import com.log4key.api.exception.ConfigurationException;
import com.log4key.config.key.ConfigKey;
import com.log4key.config.resolver.ConfigAccumulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default configuration validator.
 *
 * 默认配置验证器。
 */
public class DefaultConfigValidator implements ConfigValidator {

    // 有效的日志级别列表
    private static final List<String> VALID_LOG_LEVELS = Collections.unmodifiableList(
            Arrays.asList("TRACE", "DEBUG", "INFO", "WARN", "ERROR")
    );

    // 有效的formatter类型列表
    private static final List<String> VALID_FORMATTER_TYPES = Collections.unmodifiableList(
            Arrays.asList("Text", "Json", "Xml")
    );

    // 有效的appender类型列表
    private static final List<String> VALID_APPENDER_TYPES = Collections.unmodifiableList(
            Arrays.asList(ConfigKeys.APPENDER_TYPE_CONSOLE, ConfigKeys.APPENDER_TYPE_FILE)
    );

    // ========================== public 方法 ==========================

    @Override
    public List<ValidationResult> validateAndReturnResults(LogConfig config) {
        List<ValidationResult> results = new ArrayList<>();

        // 验证日志级别
        validateLogLevel(config, results);

        // 验证线程池配置
        validateThreadPoolConfig(config, results);

        // 验证文件路径配置
        validateFilePathConfig(config, results);

        // 验证缓存配置
        validateCacheConfig(config, results);

        return results;
    }

    @Override
    public void validate(ConfigAccumulator accumulator) throws ConfigurationException {
        List<ValidationResult> results = validateAndReturnResults(accumulator);

        // 检查是否有验证失败的结果
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                throw new ConfigurationException(result.getMessage());
            }
        }
    }

    @Override
    public List<ValidationResult> validateAndReturnResults(ConfigAccumulator accumulator) {
        List<ValidationResult> results = new ArrayList<>();

        // 1. 结构完整性校验
        validateStructureIntegrity(accumulator, results);

        // 2. 引用合法性校验
        validateReferenceLegality(accumulator, results);

        // 3. 字段合法性校验
        validateFieldLegality(accumulator, results);

        return results;
    }

    @Override
    public List<ValidationResult> validateXmlConfigAndReturnResults(Map<String, Object> flatMap) {
        List<ValidationResult> results = new ArrayList<>();

        // 验证Root Logger是否存在
        validateRootLoggerExists(flatMap, results);

        // 验证Appender配置
        validateAppenders(flatMap, results);

        // 验证Formatter配置
        validateFormatters(flatMap, results);

        // 验证Logger配置
        validateLoggers(flatMap, results);

        return results;
    }

    // ========================== private 方法 ==========================

    /**
     * 验证结构完整性
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateStructureIntegrity(ConfigAccumulator accumulator, List<ValidationResult> results) {
        // 验证全局基础运行配置
        validateGlobalConfig(accumulator, results);

        // 验证rootLogger配置
        validateRootLoggerConfig(accumulator, results);

        // 验证至少存在一个appender
        validateAppenderExists(accumulator, results);

        // 验证至少存在一个formatter
        validateFormatterExists(accumulator, results);
    }

    /**
     * 验证引用合法性
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateReferenceLegality(ConfigAccumulator accumulator, List<ValidationResult> results) {
        // 提取所有Formatter名称
        Set<String> formatterNames = extractFormatterNames(accumulator);

        // 提取所有Appender名称
        Set<String> appenderNames = extractAppenderNames(accumulator);

        // 验证Appender对Formatter的引用
        validateAppenderFormatterReferences(accumulator, appenderNames, formatterNames, results);

        // 验证Logger对Appender的引用
        validateLoggerAppenderReferences(accumulator, appenderNames, results);
    }

    /**
     * 验证字段合法性
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateFieldLegality(ConfigAccumulator accumulator, List<ValidationResult> results) {
        // 验证日志级别
        validateLogLevels(accumulator, results);

        // 验证线程池配置
        validateThreadPoolConfig(accumulator, results);

        // 验证其他字段合法性
        validateOtherFields(accumulator, results);
    }

    /**
     * 提取所有Formatter名称
     * @param accumulator 配置累加器
     * @return Formatter名称集合
     */
    private Set<String> extractFormatterNames(ConfigAccumulator accumulator) {
        Set<String> formatterNames = new HashSet<>();
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 提取所有Formatter名称
        for (ConfigKey<?> key : values.keySet()) {
            String keyName = key.name();
            if (keyName.startsWith(ConfigKeys.FORMATTERS_PREFIX) && keyName.endsWith(".type")) {
                // 提取Formatter名称
                String formatterName = keyName.substring(ConfigKeys.FORMATTERS_PREFIX.length(), keyName.length() - ".type".length());
                formatterNames.add(formatterName);
            }
        }

        return formatterNames;
    }

    /**
     * 提取所有Appender名称
     * @param accumulator 配置累加器
     * @return Appender名称集合
     */
    private Set<String> extractAppenderNames(ConfigAccumulator accumulator) {
        Set<String> appenderNames = new HashSet<>();
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 提取所有Appender名称
        for (ConfigKey<?> key : values.keySet()) {
            String keyName = key.name();
            if (keyName.startsWith(ConfigKeys.APPENDERS_PREFIX) && keyName.endsWith(".type")) {
                // 提取Appender名称
                String appenderName = keyName.substring(ConfigKeys.APPENDERS_PREFIX.length(), keyName.length() - ".type".length());
                appenderNames.add(appenderName);
            }
        }

        return appenderNames;
    }

    /**
     * 验证Appender对Formatter的引用
     * @param accumulator 配置累加器
     * @param appenderNames Appender名称集合
     * @param formatterNames Formatter名称集合
     * @param results 验证结果列表
     */
    private void validateAppenderFormatterReferences(ConfigAccumulator accumulator, Set<String> appenderNames, Set<String> formatterNames, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 验证每个Appender对Formatter的引用
        for (String appenderName : appenderNames) {
            String formatterRefKey = ConfigKeys.APPENDERS_PREFIX + appenderName + ".formatter";

            for (ConfigKey<?> key : values.keySet()) {
                if (key.name().equals(formatterRefKey)) {
                    String formatterRef = (String) values.get(key);
                    if (formatterRef != null && !formatterNames.contains(formatterRef)) {
                        results.add(new ValidationResult(
                                false,
                                "Appender '" + appenderName + "' references non-existent formatter: " + formatterRef,
                                formatterRefKey,
                                ValidationResult.ValidationLevel.FATAL
                        ));
                    } else {
                        results.add(new ValidationResult(
                                true,
                                "Appender '" + appenderName + "' has valid formatter reference: " + formatterRef,
                                formatterRefKey
                        ));
                    }
                    break;
                }
            }
        }
    }

    /**
     * 验证Logger对Appender的引用
     * @param accumulator 配置累加器
     * @param appenderNames Appender名称集合
     * @param results 验证结果列表
     */
    private void validateLoggerAppenderReferences(ConfigAccumulator accumulator, Set<String> appenderNames, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 验证Root Logger对Appender的引用
        String rootAppendersKey = ConfigKeys.ROOT_LOGGER_APPENDERS_KEY.name();
        for (ConfigKey<?> key : values.keySet()) {
            if (key.name().equals(rootAppendersKey)) {
                String rootAppenders = (String) values.get(key);
                if (rootAppenders != null) {
                    for (String appender : rootAppenders.split(",")) {
                        String appenderName = appender.trim();
                        if (!appenderNames.contains(appenderName)) {
                            results.add(new ValidationResult(
                                    false,
                                    "Root logger references non-existent appender: " + appenderName,
                                    rootAppendersKey,
                                    ValidationResult.ValidationLevel.FATAL
                            ));
                        } else {
                            results.add(new ValidationResult(
                                    true,
                                    "Root logger has valid appender reference: " + appenderName,
                                    rootAppendersKey
                            ));
                        }
                    }
                }
                break;
            }
        }

        // 验证非Root Logger对Appender的引用
        for (ConfigKey<?> key : values.keySet()) {
            String keyName = key.name();
            if (keyName.startsWith(ConfigKeys.LOGGERS_PREFIX) && keyName.endsWith(".appenders")) {
                String loggersAppenders = (String) values.get(key);
                if (loggersAppenders != null) {
                    for (String appender : loggersAppenders.split(",")) {
                        String appenderName = appender.trim();
                        if (!appenderNames.contains(appenderName)) {
                            results.add(new ValidationResult(
                                    false,
                                    "Logger references non-existent appender: " + appenderName,
                                    keyName,
                                    ValidationResult.ValidationLevel.FATAL
                            ));
                        } else {
                            results.add(new ValidationResult(
                                    true,
                                    "Logger has valid appender reference: " + appenderName,
                                    keyName
                            ));
                        }
                    }
                }
            }
        }
    }

    /**
     * 验证日志级别
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateLogLevels(ConfigAccumulator accumulator, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 验证默认日志准入级别
        if (accumulator.contains(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY)) {
            String defaultLevel = (String) values.get(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY);
            if (defaultLevel != null && !VALID_LOG_LEVELS.contains(defaultLevel.toUpperCase())) {
                results.add(new ValidationResult(
                        false,
                        "Invalid defaultAdmissionLevel: " + defaultLevel + ", valid values are: " + VALID_LOG_LEVELS,
                        ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY.name(),
                        ValidationResult.ValidationLevel.ERROR
                ));
            } else {
                results.add(new ValidationResult(
                        true,
                        "Valid defaultAdmissionLevel: " + defaultLevel,
                        ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY.name()
                ));
            }
        }

        // 验证Root Logger级别
        if (accumulator.contains(ConfigKeys.ROOT_LOGGER_LEVEL_KEY)) {
            String rootLevel = (String) values.get(ConfigKeys.ROOT_LOGGER_LEVEL_KEY);
            if (rootLevel != null && !VALID_LOG_LEVELS.contains(rootLevel.toUpperCase())) {
                results.add(new ValidationResult(
                        false,
                        "Invalid rootLogger.level: " + rootLevel + ", valid values are: " + VALID_LOG_LEVELS,
                        ConfigKeys.ROOT_LOGGER_LEVEL_KEY.name(),
                        ValidationResult.ValidationLevel.ERROR
                ));
            } else {
                results.add(new ValidationResult(
                        true,
                        "Valid rootLogger.level: " + rootLevel,
                        ConfigKeys.ROOT_LOGGER_LEVEL_KEY.name()
                ));
            }
        }

        // 验证其他Logger级别
        for (ConfigKey<?> key : values.keySet()) {
            String keyName = key.name();
            if (keyName.startsWith(ConfigKeys.LOGGERS_PREFIX) && keyName.endsWith(".level")) {
                String loggerLevel = (String) values.get(key);
                if (loggerLevel != null && !VALID_LOG_LEVELS.contains(loggerLevel.toUpperCase())) {
                    results.add(new ValidationResult(
                            false,
                            "Invalid logger level: " + loggerLevel + ", valid values are: " + VALID_LOG_LEVELS,
                            keyName,
                            ValidationResult.ValidationLevel.ERROR
                    ));
                } else {
                    results.add(new ValidationResult(
                            true,
                            "Valid logger level: " + loggerLevel,
                            keyName
                    ));
                }
            }
        }
    }

    /**
     * 验证线程池配置
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateThreadPoolConfig(ConfigAccumulator accumulator, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 验证核心线程数
        if (accumulator.contains(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY)) {
            Integer corePoolSize = (Integer) values.get(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY);
            if (corePoolSize != null && corePoolSize < 0) {
                results.add(new ValidationResult(
                        false,
                        "Invalid corePoolSize: " + corePoolSize + ", must be >= 0",
                        ConfigKeys.EXECUTOR_THREADS_SIZE_KEY.name(),
                        ValidationResult.ValidationLevel.ERROR
                ));
            } else {
                results.add(new ValidationResult(
                        true,
                        "Valid corePoolSize: " + corePoolSize,
                        ConfigKeys.EXECUTOR_THREADS_SIZE_KEY.name()
                ));
            }
        }

        // 验证执行器队列大小
        if (accumulator.contains(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY)) {
            Integer executorQueueSize = (Integer) values.get(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY);
            if (executorQueueSize != null && executorQueueSize < 0) {
                results.add(new ValidationResult(
                        false,
                        "Invalid executorQueueSize: " + executorQueueSize + ", must be >= 0",
                        ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY.name(),
                        ValidationResult.ValidationLevel.ERROR
                ));
            } else {
                results.add(new ValidationResult(
                        true,
                        "Valid executorQueueSize: " + executorQueueSize,
                        ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY.name()
                ));
            }
        }
    }

    /**
     * 验证其他字段合法性
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateOtherFields(ConfigAccumulator accumulator, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();

        // 验证缓冲区大小
        if (accumulator.contains(ConfigKeys.BUFFER_SIZE_KEY)) {
            Integer bufferSize = (Integer) values.get(ConfigKeys.BUFFER_SIZE_KEY);
            if (bufferSize != null && bufferSize < 0) {
                results.add(new ValidationResult(
                        false,
                        "Invalid bufferSize: " + bufferSize + ", must be >= 0",
                        ConfigKeys.BUFFER_SIZE_KEY.name(),
                        ValidationResult.ValidationLevel.ERROR
                ));
            } else {
                results.add(new ValidationResult(
                        true,
                        "Valid bufferSize: " + bufferSize,
                        ConfigKeys.BUFFER_SIZE_KEY.name()
                ));
            }
        }
    }

    /**
     * 验证全局基础运行配置
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateGlobalConfig(ConfigAccumulator accumulator, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();
        boolean hasDefaultAdmissionLevel = false;
        boolean hasDefaultDirectory = false;
        boolean hasDefaultCharset = false;
        boolean hasShutdownHook = false;
        boolean hasCorePoolSize = false;
        boolean hasExecutorQueueSize = false;

        // 遍历所有配置键，检查是否存在所需的配置
        for (ConfigKey<?> key : values.keySet()) {
            String keyName = key.name();
            if (keyName.equals(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY.name())) {
                hasDefaultAdmissionLevel = true;
            } else if (keyName.equals(ConfigKeys.ROOT_DIRECTORY_KEY.name())) {
                hasDefaultDirectory = true;
            } else if (keyName.equals(ConfigKeys.DEFAULT_CHARSET_KEY.name())) {
                hasDefaultCharset = true;
            } else if (keyName.equals(ConfigKeys.SHUTDOWN_HOOK_KEY.name())) {
                hasShutdownHook = true;
            } else if (keyName.equals(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY.name())) {
                hasCorePoolSize = true;
            } else if (keyName.equals(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY.name())) {
                hasExecutorQueueSize = true;
            }
        }

        // 验证默认日志准入级别
        if (!hasDefaultAdmissionLevel) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: defaultAdmissionLevel",
                    "defaultAdmissionLevel"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid defaultAdmissionLevel configuration",
                    "defaultAdmissionLevel"
            ));
        }

        // 验证默认日志文件输出目录
        if (!hasDefaultDirectory) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: rootDirectory",
                    "rootDirectory"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid rootDirectory configuration",
                    "rootDirectory"
            ));
        }

        // 验证默认字符集
        if (!hasDefaultCharset) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: defaultCharset",
                    "defaultCharset"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid defaultCharset configuration",
                    "defaultCharset"
            ));
        }

        // 验证关闭钩子
        if (!hasShutdownHook) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: shutdownHook",
                    "shutdownHook"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid shutdownHook configuration",
                    "shutdownHook"
            ));
        }

        // 验证核心线程数
        if (!hasCorePoolSize) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: corePoolSize",
                    "corePoolSize"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid corePoolSize configuration",
                    "corePoolSize"
            ));
        }

        // 验证执行器队列大小
        if (!hasExecutorQueueSize) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: executorQueueSize",
                    "executorQueueSize"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid executorQueueSize configuration",
                    "executorQueueSize"
            ));
        }
    }

    /**
     * 验证rootLogger配置
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateRootLoggerConfig(ConfigAccumulator accumulator, List<ValidationResult> results) {
        Map<ConfigKey<?>, Object> values = accumulator.getValues();
        boolean hasRootLoggerLevel = false;
        boolean hasRootLoggerAppenders = false;

        // 遍历所有配置键，检查是否存在所需的配置
        for (ConfigKey<?> key : values.keySet()) {
            String keyName = key.name();
            if (keyName.equals(ConfigKeys.ROOT_LOGGER_LEVEL_KEY.name())) {
                hasRootLoggerLevel = true;
            } else if (keyName.equals(ConfigKeys.ROOT_LOGGER_APPENDERS_KEY.name())) {
                hasRootLoggerAppenders = true;
            }
        }

        // 验证rootLogger级别
        if (!hasRootLoggerLevel) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: rootLogger.level",
                    "rootLogger.level"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid rootLogger.level configuration",
                    "rootLogger.level"
            ));
        }

        // 验证rootLogger appenders
        if (!hasRootLoggerAppenders) {
            results.add(new ValidationResult(
                    false,
                    "Missing required configuration: rootLogger.appenders",
                    "rootLogger.appenders"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid rootLogger.appenders configuration",
                    "rootLogger.appenders"
            ));
        }
    }

    /**
     * 验证至少存在一个appender
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateAppenderExists(ConfigAccumulator accumulator, List<ValidationResult> results) {
        // 这里简化处理，实际应该检查是否有appender相关的配置
        // 暂时假设只要有默认的appender配置就认为有效
        results.add(new ValidationResult(
                true,
                "Appender configuration validation placeholder",
                "appenders"
        ));
    }

    /**
     * 验证至少存在一个formatter
     * @param accumulator 配置累加器
     * @param results 验证结果列表
     */
    private void validateFormatterExists(ConfigAccumulator accumulator, List<ValidationResult> results) {
        // 这里简化处理，实际应该检查是否有formatter相关的配置
        // 暂时假设只要有默认的formatter配置就认为有效
        results.add(new ValidationResult(
                true,
                "Formatter configuration validation placeholder",
                "formatters"
        ));
    }

    /**
     * 验证日志级别配置并将结果添加到列表中
     * @param config 要验证的LogConfig对象
     * @param results 验证结果列表
     */
    private void validateLogLevel(LogConfig config, List<ValidationResult> results) {
        String logLevel = config.getStringProperty("logLevel");
        if (logLevel != null && !VALID_LOG_LEVELS.contains(logLevel.toUpperCase())) {
            results.add(new ValidationResult(
                    false,
                    "Invalid log level: " + logLevel + ", valid values are: " + VALID_LOG_LEVELS,
                    "logLevel"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid log level: " + (logLevel != null ? logLevel : "not set"),
                    "logLevel"
            ));
        }
    }

    /**
     * 验证线程池配置
     * @param config 要验证的LogConfig对象
     * @param results 验证结果列表
     */
    private void validateThreadPoolConfig(LogConfig config, List<ValidationResult> results) {
        // 验证核心线程数
        Integer corePoolSize = config.getIntProperty("threadPool.coreSize");
        if (corePoolSize != null && corePoolSize < 0) {
            results.add(new ValidationResult(
                    false,
                    "Invalid corePoolSize: " + corePoolSize + ", must be >= 0",
                    "threadPool.coreSize"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid corePoolSize: " + (corePoolSize != null ? corePoolSize : "not set"),
                    "threadPool.coreSize"
            ));
        }

        // 验证最大线程数
        Integer maxPoolSize = config.getIntProperty("threadPool.maxSize");
        if (maxPoolSize != null && maxPoolSize < 0) {
            results.add(new ValidationResult(
                    false,
                    "Invalid maxPoolSize: " + maxPoolSize + ", must be >= 0",
                    "threadPool.maxSize"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid maxPoolSize: " + (maxPoolSize != null ? maxPoolSize : "not set"),
                    "threadPool.maxSize"
            ));
        }

        // 验证核心线程数是否小于等于最大线程数
        if (corePoolSize != null && maxPoolSize != null && corePoolSize > maxPoolSize) {
            results.add(new ValidationResult(
                    false,
                    "Invalid thread pool configuration: corePoolSize (" + corePoolSize + ") must be <= maxPoolSize (" + maxPoolSize + ")",
                    "threadPool"
            ));
        }
    }

    /**
     * 验证文件路径配置
     * @param config 要验证的LogConfig对象
     * @param results 验证结果列表
     */
    private void validateFilePathConfig(LogConfig config, List<ValidationResult> results) {
        // 验证日志文件路径
        String logFilePath = config.getStringProperty("file.path");
        if (logFilePath != null) {
            // 简单验证路径是否包含非法字符（Windows系统）
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                if (logFilePath.contains("?") || logFilePath.contains("*") ||
                    logFilePath.contains("<") || logFilePath.contains(">") ||
                    logFilePath.contains("|") || logFilePath.contains(":")) {
                    results.add(new ValidationResult(
                            false,
                            "Invalid file path: " + logFilePath + ", contains illegal characters",
                            "file.path"
                    ));
                    return;
                }
            }

            results.add(new ValidationResult(
                    true,
                    "Valid file path: " + logFilePath,
                    "file.path"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "File path not set, using default",
                    "file.path"
            ));
        }
    }

    /**
     * 验证缓存配置
     * @param config 要验证的LogConfig对象
     * @param results 验证结果列表
     */
    private void validateCacheConfig(LogConfig config, List<ValidationResult> results) {
        // 验证缓存大小
        Integer cacheSize = config.getIntProperty("cache.size");
        if (cacheSize != null && cacheSize < 0) {
            results.add(new ValidationResult(
                    false,
                    "Invalid cache size: " + cacheSize + ", must be >= 0",
                    "cache.size"
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid cache size: " + (cacheSize != null ? cacheSize : "not set"),
                    "cache.size"
            ));
        }
    }

    /**
     * 验证Root Logger是否存在
     * @param flatMap 扁平化的XML配置Map
     * @param results 验证结果列表
     */
    private void validateRootLoggerExists(Map<String, Object> flatMap, List<ValidationResult> results) {
        if (!flatMap.containsKey(ConfigKeys.ROOT_LOGGER_LEVEL) || !flatMap.containsKey(ConfigKeys.ROOT_LOGGER_APPENDERS)) {
            results.add(new ValidationResult(
                    false,
                    "Root logger configuration not found",
                    "rootLogger",
                    ValidationResult.ValidationLevel.FATAL
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Root logger configuration found",
                    "rootLogger"
            ));
        }
    }

    /**
     * 验证Appender配置
     * @param flatMap 扁平化的XML配置Map
     * @param results 验证结果列表
     */
    private void validateAppenders(Map<String, Object> flatMap, List<ValidationResult> results) {
        // 提取所有Appender名称
        Set<String> appenderNames = new HashSet<>();
        Pattern appenderPattern = Pattern.compile("^" + ConfigKeys.APPENDERS_PREFIX + "(\\w+)\\." + ConfigKeys.APPENDER_TYPE + "$");

        for (String key : flatMap.keySet()) {
            Matcher matcher = appenderPattern.matcher(key);
            if (matcher.matches()) {
                appenderNames.add(matcher.group(1));
            }
        }

        // 验证每个Appender
        for (String appenderName : appenderNames) {
            validateSingleAppender(flatMap, appenderName, results);
        }

        // 验证Appender引用
        validateAppenderReferences(flatMap, appenderNames, results);
    }

    /**
     * 验证单个Appender配置
     * @param flatMap 扁平化的XML配置Map
     * @param appenderName Appender名称
     * @param results 验证结果列表
     */
    private void validateSingleAppender(Map<String, Object> flatMap, String appenderName, List<ValidationResult> results) {
        String appenderPrefix = ConfigKeys.APPENDERS_PREFIX + appenderName + ".";

        // 验证Appender类型
        String appenderType = (String) flatMap.get(appenderPrefix + ConfigKeys.APPENDER_TYPE);
        if (appenderType == null || !VALID_APPENDER_TYPES.contains(appenderType)) {
            results.add(new ValidationResult(
                    false,
                    "Invalid appender type: " + appenderType + ", valid values are: " + VALID_APPENDER_TYPES,
                    appenderPrefix + ConfigKeys.APPENDER_TYPE,
                    ValidationResult.ValidationLevel.ERROR
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid appender type: " + appenderType,
                    appenderPrefix + ConfigKeys.APPENDER_TYPE
            ));
        }

        // 验证Appender的formatter引用
        String formatterRef = (String) flatMap.get(appenderPrefix + ConfigKeys.APPENDER_FORMATTER);
        if (formatterRef != null && !flatMap.containsKey(ConfigKeys.FORMATTERS_PREFIX + formatterRef + "." + ConfigKeys.APPENDER_TYPE)) {
            results.add(new ValidationResult(
                    false,
                    "Formatter reference not found: " + formatterRef,
                    appenderPrefix + ConfigKeys.APPENDER_FORMATTER,
                    ValidationResult.ValidationLevel.FATAL
            ));
        } else {
            results.add(new ValidationResult(
                    true,
                    "Valid formatter reference: " + formatterRef,
                    appenderPrefix + ConfigKeys.APPENDER_FORMATTER
            ));
        }
    }

    /**
     * 验证Appender引用
     * @param flatMap 扁平化的XML配置Map
     * @param appenderNames 所有Appender名称
     * @param results 验证结果列表
     */
    private void validateAppenderReferences(Map<String, Object> flatMap, Set<String> appenderNames, List<ValidationResult> results) {
        // 验证Root Logger的Appender引用
        String rootAppenders = (String) flatMap.get(ConfigKeys.ROOT_LOGGER_APPENDERS);
        if (rootAppenders != null) {
            for (String appender : rootAppenders.split(",")) {
                if (!appenderNames.contains(appender.trim())) {
                    results.add(new ValidationResult(
                            false,
                            "Root logger appender reference not found: " + appender.trim(),
                            ConfigKeys.ROOT_LOGGER_APPENDERS,
                            ValidationResult.ValidationLevel.FATAL
                    ));
                }
            }
        }

        // 验证非Root Logger的Appender引用
        Pattern loggerAppendersPattern = Pattern.compile("^" + ConfigKeys.LOGGERS_PREFIX + "(.+?)" + ConfigKeys.LOGGER_APPENDERS_SUFFIX + "$");
        for (String key : flatMap.keySet()) {
            Matcher matcher = loggerAppendersPattern.matcher(key);
            if (matcher.matches()) {
                String loggersAppenders = (String) flatMap.get(key);
                for (String appender : loggersAppenders.split(",")) {
                    if (!appenderNames.contains(appender.trim())) {
                        results.add(new ValidationResult(
                                false,
                                "Logger appender reference not found: " + appender.trim(),
                                key,
                                ValidationResult.ValidationLevel.FATAL
                        ));
                    }
                }
            }
        }
    }

    /**
     * 验证Formatter配置
     * @param flatMap 扁平化的XML配置Map
     * @param results 验证结果列表
     */
    private void validateFormatters(Map<String, Object> flatMap, List<ValidationResult> results) {
        // 提取所有Formatter名称
        Set<String> formatterNames = new HashSet<>();
        Pattern formatterPattern = Pattern.compile("^" + ConfigKeys.FORMATTERS_PREFIX + "(\\w+)\\." + ConfigKeys.APPENDER_TYPE + "$");

        for (String key : flatMap.keySet()) {
            Matcher matcher = formatterPattern.matcher(key);
            if (matcher.matches()) {
                formatterNames.add(matcher.group(1));
            }
        }

        // 验证每个Formatter
        for (String formatterName : formatterNames) {
            String formatterPrefix = ConfigKeys.FORMATTERS_PREFIX + formatterName + ".";
            String formatterType = (String) flatMap.get(formatterPrefix + ConfigKeys.APPENDER_TYPE);

            if (formatterType == null || !VALID_FORMATTER_TYPES.contains(formatterType)) {
                results.add(new ValidationResult(
                        false,
                        "Invalid formatter type: " + formatterType + ", valid values are: " + VALID_FORMATTER_TYPES,
                        formatterPrefix + ConfigKeys.APPENDER_TYPE,
                        ValidationResult.ValidationLevel.FATAL
                ));
            } else {
                results.add(new ValidationResult(
                        true,
                        "Valid formatter type: " + formatterType,
                        formatterPrefix + ConfigKeys.APPENDER_TYPE
                ));
            }
        }
    }

    /**
     * 验证Logger配置
     * @param flatMap 扁平化的XML配置Map
     * @param results 验证结果列表
     */
    private void validateLoggers(Map<String, Object> flatMap, List<ValidationResult> results) {
        // 验证非Root Logger配置
        Pattern loggerPattern = Pattern.compile("^" + ConfigKeys.LOGGERS_PREFIX + "(.+?)" + ConfigKeys.LOGGER_LEVEL_SUFFIX + "$");

        for (String key : flatMap.keySet()) {
            Matcher matcher = loggerPattern.matcher(key);
            if (matcher.matches()) {
                String loggerName = matcher.group(1);
                String loggerPrefix = ConfigKeys.LOGGERS_PREFIX + loggerName + ".";

                // 验证Logger级别
                String loggerLevel = (String) flatMap.get(loggerPrefix + ConfigKeys.LOGGER_LEVEL_SUFFIX.replace(".", ""));
                if (loggerLevel != null && !VALID_LOG_LEVELS.contains(loggerLevel.toUpperCase())) {
                    results.add(new ValidationResult(
                            false,
                            "Invalid logger level: " + loggerLevel + ", valid values are: " + VALID_LOG_LEVELS,
                            loggerPrefix + ConfigKeys.LOGGER_LEVEL_SUFFIX.replace(".", ""),
                            ValidationResult.ValidationLevel.ERROR
                    ));
                } else {
                    results.add(new ValidationResult(
                            true,
                            "Valid logger level: " + loggerLevel,
                            loggerPrefix + ConfigKeys.LOGGER_LEVEL_SUFFIX.replace(".", "")
                    ));
                }
            }
        }
    }
}