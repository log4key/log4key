/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config;

import com.log4key.config.key.ConfigKey;
import com.log4key.config.model.AppenderConfig;
import com.log4key.config.model.LoggerConfig;
import com.log4key.config.model.Log4KeyConfig;
import com.log4key.config.model.RootLoggerConfig;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.internal.InternalLogger;
import java.util.*;

/**
 * Log4Key configuration management class.
 *
 * Log4Key配置管理类。
 */
public class Log4KeyConfiguration {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(Log4KeyConfiguration.class);

    /**
     * 单例实例
     */
    private static final Log4KeyConfiguration INSTANCE = new Log4KeyConfiguration();

    /**
     * 结构化配置模型 - 唯一的配置存储
     */
    private volatile Log4KeyConfig structuredConfig;

    /**
     * 代码配置是否已设置
     */
    private volatile boolean codeConfigSet = false;

    /**
     * 构造函数，初始化默认配置
     */
    private Log4KeyConfiguration() {
        // 初始化结构化配置
        structuredConfig = new Log4KeyConfig();
    }

    // ========================== public 方法 ==========================

    /**
     * Gets the singleton instance of configuration manager.
     *
     * 获取配置管理类的单例实例。
     *
     * @return the configuration manager instance / 配置管理类实例
     */
    public static Log4KeyConfiguration getInstance() {
        return INSTANCE;
    }

    /**
     * Loads the default configuration file.
     *
     * 加载默认配置文件。
     */
    public void loadConfigFile() {
        // 如果已经设置了代码配置，跳过文件配置加载
        if (codeConfigSet) {
            logger.debug("[CONFIG-LOAD-DEBUG] Code config has been set, skipping file config loading");
            return;
        }

        logger.debug("[CONFIG-LOAD-DEBUG] loadDefaultConfigFile() called");
        try {
            // 直接使用Log4KeyConfigurationLoader加载配置
            ConfigResolver resolver = Log4KeyConfigurationLoader.loadDefaultConfigAsResolver();
            logger.debug("============>>>> [CONFIG-LOAD-DEBUG] Log4KeyConfigurationLoader.loadDefaultConfigAsResolver() returned {} entries", resolver.size());

            // 直接使用ConfigResolver加载配置
            if (resolver.size() > 0) {
                // 输出所有键值对
                if (logger.isDebugEnabled()) {
                    int count = 0;
                    for (ConfigKey<?> key : resolver.keys()) {
                        logger.debug("[CONFIG-LOAD-DEBUG]   key[{}]: '{}' = '{}'", ++count, key.name(), resolver.get(key));
                    }
                }
                loadFromResolver(resolver);
                logger.debug("Loaded configuration using ConfigResolver, total properties: {}", resolver.size());
                logger.debug("[CONFIG-LOAD-DEBUG] loadFromResolver() completed");
            } else {
                logger.warn("Log4KeyConfigurationLoader returned empty configuration");
                logger.debug("[CONFIG-LOAD-DEBUG] WARNING: Empty configuration returned");
            }
        } catch (Exception e) {
            logger.warn("Failed to load default config file using Log4KeyConfigurationLoader: {}", e.getMessage());
            logger.debug("[CONFIG-LOAD-DEBUG] ERROR: {}", e.getMessage());
            logger.error("Failed to load default config file", e);
        }
        logger.debug("[CONFIG-LOAD-DEBUG] loadDefaultConfigFile() completed");
    }

    /**
     * 从Map加载配置
     * @param properties 配置属性Map
     */
    public void loadFromMap(Map<String, Object> properties) {
        if (properties != null && !properties.isEmpty()) {
            // 将Map<String, Object>转换为ConfigResolver
            Map<com.log4key.config.key.ConfigKey<?>, Object> configMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                // 创建动态ConfigKey，类型根据值推断
                Class<?> type = inferValueType(entry.getValue());
                com.log4key.config.key.ConfigKey<?> key = new com.log4key.config.key.ConfigKey<>(entry.getKey(), type, null);
                configMap.put(key, entry.getValue());
            }
            ConfigResolver resolver = new ConfigResolver(configMap);
            loadFromProperties(resolver);
        }
    }

    /**
     * Clears all configuration.
     *
     * 清除所有配置。
     */
    public void clear() {
        // 重新初始化配置
        structuredConfig = new Log4KeyConfig();
    }

    /**
     * 获取缓冲区大小
     * @return 缓冲区大小
     */
    public int getBufferSize() {
        return structuredConfig.getGlobalConfig(ConfigKeys.BUFFER_SIZE_KEY);
    }

    /**
     * Gets max file size in MB.
     *
     * 获取最大文件大小（MB）。
     *
     * @return max file size in MB / 最大文件大小（MB）
     */
    public int getMaxFileSizeMB() {
        return structuredConfig.getGlobalConfig(ConfigKeys.MAX_FILE_SIZE_MB_KEY);
    }

    /**
     * 获取最大备份索引
     * @return 最大备份索引
     */
    public int getMaxBackupIndex() {
        return structuredConfig.getGlobalConfig(ConfigKeys.MAX_BACKUP_INDEX_KEY);
    }

    /**
     * Gets default charset.
     *
     * 获取默认字符集。
     *
     * @return default charset / 默认字符集
     */
    public String getDefaultCharset() {
        return structuredConfig.getGlobalConfig(ConfigKeys.DEFAULT_CHARSET_KEY);
    }

    /**
     * 获取全局默认准入级别
     * @return 全局默认准入级别
     */
    public String getDefaultAdmissionLevel() {
        return structuredConfig.getGlobalConfig(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY);
    }

    /**
     * 获取默认目录
     * @return 默认目录
     */
    public String getDefaultDirectory() {
        return structuredConfig.getGlobalConfig(ConfigKeys.ROOT_DIRECTORY_KEY);
    }

    /**
     * Gets whether to include location information.
     *
     * 获取是否包含位置信息。
     *
     * @return true if location info should be included / 是否包含位置信息
     */
    public boolean isIncludeLocation() {
        return structuredConfig.getGlobalConfig(ConfigKeys.INCLUDE_LOCATION_KEY);
    }

    /**
     * 获取核心线程池大小
     * @return 核心线程池大小
     */
    public int getCorePoolSize() {
        return structuredConfig.getGlobalConfig(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY);
    }

    /**
     * Gets executor type.
     *
     * 获取执行器类型。
     *
     * @return executor type / 执行器类型
     */
    public String getExecutorType() {
        return structuredConfig.getGlobalConfig(ConfigKeys.EXECUTOR_TYPE_KEY);
    }

    /**
     * 获取执行器队列大小
     * @return 执行器队列大小
     */
    public int getExecutorQueueSize() {
        return structuredConfig.getGlobalConfig(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY);
    }

    /**
     * 获取 Flush 字节阈值
     * @return Flush 字节阈值
     */
    public int getBatchSize() {
        return structuredConfig.getGlobalConfig(ConfigKeys.BATCH_SIZE_KEY);
    }

    /**
     * 获取 Flush 时间间隔（毫秒）
     * @return Flush 时间间隔（毫秒）
     */
    public long getFlushInterval() {
        return structuredConfig.getGlobalConfig(ConfigKeys.FLUSH_INTERVAL_KEY);
    }

    /**
     * 获取 Buffer 扩容回收阈值
     * @return Buffer 扩容回收阈值
     */
    public int getHighWaterMark() {
        return structuredConfig.getGlobalConfig(ConfigKeys.HIGH_WATER_MARK_KEY);
    }

    /**
     * 获取 StringBuilder 初始容量
     * @return StringBuilder 初始容量
     */
    public int getInitialBufferSize() {
        return structuredConfig.getGlobalConfig(ConfigKeys.INITIAL_BUFFER_SIZE_KEY);
    }

    /**
     * 获取最大打开文件数（作为 maxOpenChannels 上限值使用）
     * @return 最大打开文件数
     */
    public int getMaxOpenFiles() {
        return structuredConfig.getGlobalConfig(ConfigKeys.MAX_OPEN_FILES_KEY);
    }

    /**
     * 获取root logger关联的所有Appender配置对象
     * @return Appender配置对象列表，如果root logger没有配置appender则返回空列表
     */
    public List<AppenderConfig> getRootLoggerAppenderConfigs() {
        if (structuredConfig == null) {
            return Collections.emptyList();
        }

        RootLoggerConfig rootLoggerConfig = structuredConfig.getRootLoggerConfig();
        if (rootLoggerConfig == null) {
            return Collections.emptyList();
        }

        String[] appenderNames = rootLoggerConfig.getAppenders();
        if (appenderNames == null || appenderNames.length == 0) {
            return Collections.emptyList();
        }

        Map<String, AppenderConfig> allAppenders = structuredConfig.getAppenders();

        List<AppenderConfig> result = new ArrayList<>(appenderNames.length);

        for (String appenderName : appenderNames) {
            String trimmedName = appenderName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }

            AppenderConfig config = findIgnoreCaseConfig(allAppenders, trimmedName);
            if (config != null) {
                result.add(config);
            }
        }

        return result;
    }

    /**
     * 获取指定appender的配置对象
     *
     * @param appenderName appender名称
     * @return appender配置对象
     */
    public AppenderConfig getAppenderConfigObject(String appenderName) {
        return structuredConfig.getAppenders().get(appenderName);
    }

    /**
     * Gets all configured logger names.
     *
     * 获取所有配置的logger名称。
     *
     * @return list of logger names / logger名称列表
     */
    public List<String> getLoggerNames() {
        return new ArrayList<>(structuredConfig.getNonRootLoggers().keySet());
    }

    /**
     * 按logger名称前缀匹配获取logger的准入级别
     * @param loggerName logger名称
     * @return 匹配到的准入级别，未匹配到则返回null
     */
    public String getLoggerAdmissionLevel(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return null;
        }

        // 查找最具体的匹配级别（最长模式）
        String matchedLevel = null;
        int longestMatchLength = 0;
        String matchedPattern = null;

        for (Map.Entry<String, LoggerConfig> entry : structuredConfig.getNonRootLoggers().entrySet()) {
            String configLoggerPattern = entry.getKey();
            if (matchesLoggerPattern(loggerName, configLoggerPattern)) {
                // 记录最长匹配（最具体的模式）
                if (configLoggerPattern.length() > longestMatchLength) {
                    longestMatchLength = configLoggerPattern.length();
                    matchedPattern = configLoggerPattern;
                    matchedLevel = entry.getValue().getAdmissionLevel();
                }
            }
        }

        if (matchedLevel != null) {
            logger.debug("DEBUG: Logger \"{}\" level matched pattern \"{}\" with level: {}",
                              loggerName, matchedPattern, matchedLevel);
        }

        return matchedLevel;
    }

    /**
     * Gets all Appender configuration objects associated with the specified logger.
     *
     * 获取指定logger关联的所有Appender配置对象映射。
     *
     * @param loggerName logger name / logger名称
     * @return Appender configuration mapping (name to config) / Appender配置对象映射（名称到配置），如果logger没有配置appender则返回空映射
     */
    public Map<String, AppenderConfig> getLoggerAppenderConfigs(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, AppenderConfig> result = new LinkedHashMap<>();
        Map<String, AppenderConfig> allAppenders = structuredConfig.getAppenders();

        if (allAppenders == null) {
            return result;
        }

        // 查找最具体的匹配模式（最长模式）
        List<LoggerConfig> matchedConfigs = new ArrayList<>();
        int longestMatchLength = 0;

        for (Map.Entry<String, LoggerConfig> entry : structuredConfig.getNonRootLoggers().entrySet()) {
            String configLoggerPattern = entry.getKey();
            if (matchesLoggerPattern(loggerName, configLoggerPattern)) {
                int patternLength = configLoggerPattern.length();
                if (patternLength > longestMatchLength) {
                    // 找到更长的匹配，重置列表
                    longestMatchLength = patternLength;
                    matchedConfigs.clear();
                    matchedConfigs.add(entry.getValue());
                } else if (patternLength == longestMatchLength) {
                    // 找到相同长度的匹配，添加到列表
                    matchedConfigs.add(entry.getValue());
                }
            }
        }

        // 处理找到的最具体匹配
        for (LoggerConfig loggerConfig : matchedConfigs) {
            String[] patternAppenders = loggerConfig.getAppenders();

            if (patternAppenders != null && patternAppenders.length > 0) {
                // 添加到映射中，避免重复
                for (String appenderName : patternAppenders) {
                    String trimmedName = appenderName.trim();
                    if (trimmedName.isEmpty()) {
                        continue;
                    }

                    if (!result.containsKey(trimmedName)) {
                        AppenderConfig config = findIgnoreCaseConfig(allAppenders, trimmedName);
                        if (config != null) {
                            result.put(trimmedName, config);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Merges appender configuration with base configuration.
     *
     * 合并 appender 配置和基础配置。
     *
     * @param appenderName appender name / appender 名称
     * @return merged complete configuration / 合并后的完整配置
     */
    public Map<String, Object> mergeAppenderConfig(String appenderName) {
        // 首先尝试从结构化配置中获取预计算的合并配置
        if (structuredConfig != null && structuredConfig.hasMergedAppenderConfig(appenderName)) {
            Map<String, Object> cached = structuredConfig.getMergedAppenderConfig(appenderName);
            return new HashMap<>(cached); // 返回副本，防止外部修改
        }

        // 回退到原有的动态计算逻辑（用于处理运行时新增的appender等特殊情况）
        return computeMergedAppenderConfig(appenderName);
    }

    /**
     * 从配置解析器加载配置
     * @param resolver 配置解析器
     */
    public void loadFromProperties(ConfigResolver resolver) {
        if (resolver == null) {
            return;
        }
        // 使用ConfigConverter将ConfigResolver转换为结构化配置
        structuredConfig = ConfigConverter.fromResolver(resolver);
    }

    /**
     * Gets the structured configuration model.
     *
     * 获取结构化配置模型。
     *
     * @return structured configuration model / 结构化配置模型
     */
    public Log4KeyConfig getStructuredConfig() {
        return structuredConfig;
    }

    /**
     * 从配置解析器加载配置
     * @param resolver 配置解析器
     */
    public void loadFromResolver(ConfigResolver resolver) {
        if (resolver == null) {
            return;
        }

        logger.debug("=== [CONFIG-DEBUG-IMPORTANT] loadFromResolver called with {} entries ===", resolver.size());

        // 使用ConfigConverter将ConfigResolver转换为结构化配置
        structuredConfig = ConfigConverter.fromResolver(resolver);

        logger.debug("[Config-DEBUG] Structured config created: {}", (structuredConfig != null ? "yes" : "no"));
        if (structuredConfig != null) {
            logger.debug("[Config-DEBUG] Structured config root logger: {}",
                              (structuredConfig.getRootLoggerConfig() != null ? "present" : "null"));
            if (structuredConfig.getRootLoggerConfig() != null) {
                logger.debug("[Config-DEBUG] Root logger appenders: {}",
                                  java.util.Arrays.toString(structuredConfig.getRootLoggerConfig().getAppenders()));
            }
            logger.debug("[Config-DEBUG] Structured config appenders count: {}",
                              (structuredConfig.getAppenders() != null ? structuredConfig.getAppenders().size() : 0));
        }
    }

    /**
     * Sets code configuration.
     *
     * 设置代码配置。
     *
     * @param resolver configuration resolver / 配置解析器
     * @throws IllegalStateException if configuration has already been locked / 如果配置已经锁定则抛出异常
     */
    public void setCodeConfig(ConfigResolver resolver) {
        if (codeConfigSet) {
            logger.warn("Code configuration has already been set, ignoring new configuration");
            return;
        }

        if (resolver != null && resolver.size() > 0) {
            codeConfigSet = true;
            loadFromResolver(resolver);
            logger.debug("Code configuration set successfully");
        } else {
            logger.warn("Code configuration is empty, ignoring");
        }
    }

    // ========================== private 方法 ==========================

    /**
     * 检查logger名称是否匹配配置中的模式（支持通配符）
     * @param loggerName 具体的logger名称
     * @param pattern 配置中的模式（可能包含*）
     * @return 是否匹配
     */
    private boolean matchesLoggerPattern(String loggerName, String pattern) {
        if (loggerName == null || pattern == null) {
            return false;
        }

        // 精确匹配
        if (loggerName.equals(pattern)) {
            return true;
        }

        // 处理通配符
        if (pattern.contains("*")) {
            // 将*转换为正则表达式.*，并转义其他字符
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            // 确保匹配整个字符串
            regex = "^" + regex + "$";
            return loggerName.matches(regex);
        }

        // 如果没有通配符，检查是否是父级模式（loggerName以前缀开始）
        // 例如：pattern="com.example", loggerName="com.example.MyClass"
        return loggerName.startsWith(pattern + ".");
    }

    /**
     * 推断值的类型
     */
    private Class<?> inferValueType(Object value) {
        if (value == null) {
            return String.class;
        }
        if (value instanceof Integer) {
            return Integer.class;
        }
        if (value instanceof Long) {
            return Long.class;
        }
        if (value instanceof Boolean) {
            return Boolean.class;
        }
        if (value instanceof Double) {
            return Double.class;
        }
        return String.class;
    }

    /**
     * 忽略大小写查找指定名称的 appender 配置
     * @param allAppenders 所有 appender 配置
     * @param trimmedName 待查找的 appender 名称
     * @return 找到的 appender 配置，如果没有找到则返回 null
     */
    private AppenderConfig findIgnoreCaseConfig(Map<String, AppenderConfig> allAppenders, String trimmedName) {
        AppenderConfig config = allAppenders.get(trimmedName);
        if (config == null) {
            for (Map.Entry<String, AppenderConfig> entry : allAppenders.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(trimmedName)) {
                    return entry.getValue();
                }
            }
        }
        return config;
    }

    /**
     * 动态计算appender的合并配置
     * @param appenderName appender名称
     * @return 合并后的完整配置
     */
    private Map<String, Object> computeMergedAppenderConfig(String appenderName) {
        Map<String, Object> mergedConfig = new HashMap<>();

        // 获取 appender 配置对象
        AppenderConfig appenderConfig = getAppenderConfigObject(appenderName);

        // 1. 首先添加基础配置作为默认值
        mergedConfig.put(ConfigKeys.MAX_FILE_SIZE_MB, getMaxFileSizeMB());
        mergedConfig.put(ConfigKeys.MAX_BACKUP_INDEX, getMaxBackupIndex());
        mergedConfig.put(ConfigKeys.BUFFER_SIZE, getBufferSize());

        // 添加默认配置字段
        mergedConfig.put(ConfigKeys.DEFAULT_ADMISSION_LEVEL, getDefaultAdmissionLevel());
        mergedConfig.put(ConfigKeys.ROOT_DIRECTORY, getDefaultDirectory());

        // 添加appender名称（必须设置，以便appender知道自己的名称）
        mergedConfig.put(ConfigKeys.APPENDER_NAME, appenderName);

        // 2. 然后添加appender专用配置
        if (appenderConfig != null) {
            mergedConfig.put(ConfigKeys.APPENDER_TYPE, appenderConfig.getType());
            mergedConfig.put(ConfigKeys.APPENDER_FORMATTER, appenderConfig.getFormatter());
            mergedConfig.put(ConfigKeys.CONSOLE_ENABLED, appenderConfig.isConsoleEnabled());

            // 可选属性
            if (appenderConfig.getMaxFileSizeMB() > 0) {
                mergedConfig.put(ConfigKeys.MAX_FILE_SIZE_MB, appenderConfig.getMaxFileSizeMB());
            }
            if (appenderConfig.getMaxBackupIndex() > 0) {
                mergedConfig.put(ConfigKeys.MAX_BACKUP_INDEX, appenderConfig.getMaxBackupIndex());
            }
            if (appenderConfig.getOutputAdmissionLevel() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL, appenderConfig.getOutputAdmissionLevel());
            }
            mergedConfig.put(ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY, appenderConfig.getOutputLevelPolicy().name());
            if (appenderConfig.getDirectory() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_DIRECTORY, appenderConfig.getDirectory());
            }
            if (appenderConfig.getFileName() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_FILE_NAME, appenderConfig.getFileName());
            }
            if (appenderConfig.getCharset() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_CHARSET, appenderConfig.getCharset());
            }

            // 移除appender自定义属性添加，因为已不再支持自定义属性
        }

        // 3. 应用默认值规则：如果appender配置未定义，则使用全局默认值
        if (!mergedConfig.containsKey(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL)) {
            mergedConfig.put(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL, getDefaultAdmissionLevel());
        }
        if (!mergedConfig.containsKey(ConfigKeys.APPENDER_CHARSET)) {
            mergedConfig.put(ConfigKeys.APPENDER_CHARSET, getDefaultCharset());
        }
        if (!mergedConfig.containsKey(ConfigKeys.CONSOLE_ENABLED)) {
            mergedConfig.put(ConfigKeys.CONSOLE_ENABLED, true);
        }

        // 4. 添加appender名称到配置中
        mergedConfig.put(ConfigKeys.APPENDER_NAME, appenderName);

        return mergedConfig;
    }

}