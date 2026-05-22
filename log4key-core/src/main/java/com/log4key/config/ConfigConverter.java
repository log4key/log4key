package com.log4key.config;

import com.log4key.config.model.AppenderConfig;
import com.log4key.config.model.FormatterConfig;
import com.log4key.config.model.Log4KeyConfig;
import com.log4key.config.model.LoggerConfig;
import com.log4key.config.model.OutputLevelPolicy;
import com.log4key.config.model.RootLoggerConfig;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.config.key.ConfigKey;
import com.log4key.internal.InternalLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置转换器
 * 用于在扁平化配置Map和结构化配置模型之间进行转换
 */
public class ConfigConverter {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(ConfigConverter.class);

    // ========================== public static 方法 ==========================

    /**
     * 将配置解析器转换为结构化的Log4KeyConfig模型
     * @param resolver 配置解析器
     * @return 结构化的Log4KeyConfig模型
     */
    public static Log4KeyConfig fromResolver(ConfigResolver resolver) {
        Log4KeyConfig config = new Log4KeyConfig(resolver);

        // 设置Root Logger配置
        RootLoggerConfig rootLoggerConfig = new RootLoggerConfig();

        // 解析和验证root logger的appender引用
        String rootAppendersStr = resolver.get(ConfigKeys.ROOT_LOGGER_APPENDERS_KEY);
        String[] rootAppenderNames = rootAppendersStr.split(",");

        // 收集所有已定义的appender名称（用于引用验证）
        Set<String> definedAppenderNames = collectDefinedAppenderNames(resolver);

        // 验证每个appender引用
        List<String> validRootAppenderNames = findValidAppender(rootAppenderNames, definedAppenderNames);

        // 设置验证后的appender引用
        rootLoggerConfig.setAppenders(validRootAppenderNames.toArray(new String[0]));

        // 设置Root Logger级别
        String rootLoggerLevel = resolver.get(ConfigKeys.ROOT_LOGGER_LEVEL_KEY);
        rootLoggerConfig.setLevel(rootLoggerLevel);
        config.setRootLoggerConfig(rootLoggerConfig);

        // 提取并设置非Root Logger配置
        extractNonRootLoggersConfig(resolver, config);

        // 提取并设置Appenders配置
        extractAppendersConfig(resolver, config);

        // 提取并设置Formatters配置
        extractFormattersConfig(resolver, config);

        // 计算并缓存所有appender的合并配置
        computeAndCacheMergedAppenderConfigs(config);

        return config;
    }

    // ========================== private static 方法 ==========================

    /**
     * 验证并返回有效的 appender名称列表
     * @param waitValidAppenders 等待验证的appender名称列表
     * @param definedAppenders 已定义的appender名称列表
     * @return 验证后的appender名称列表
     */
    private static List<String> findValidAppender(String[] waitValidAppenders, Set<String> definedAppenders) {
        List<String> validAppenderNames = new ArrayList<>();
        for (String appenderName : waitValidAppenders) {
            String trimmedName = appenderName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }

            // 检查appender是否存在
            if (definedAppenders.contains(trimmedName)) {
                validAppenderNames.add(trimmedName);
            } else {
                // 尝试大小写兼容性查找
                String foundName = null;
                for (String definedName : definedAppenders) {
                    if (definedName.equalsIgnoreCase(trimmedName)) {
                        foundName = definedName;
                        break;
                    }
                }

                if (foundName != null) {
                    validAppenderNames.add(foundName);
                }
            }
        }
        return validAppenderNames;
    }

    /**
     * 从配置解析器中收集所有已定义的appender名称
     * @param resolver 配置解析器
     * @return 已定义的appender名称集合
     */
    private static Set<String> collectDefinedAppenderNames(ConfigResolver resolver) {
        Set<String> definedAppenderNames = new HashSet<>();
        Pattern appenderPattern = Pattern.compile("^" + ConfigKeys.APPENDERS_PREFIX + "(.+?)\\..+$");

        for (ConfigKey<?> key : resolver.keys()) {
            String keyName = key.name();
            Matcher appenderMatcher = appenderPattern.matcher(keyName);
            if (appenderMatcher.matches()) {
                String appenderName = appenderMatcher.group(1);
                definedAppenderNames.add(appenderName);
            }
        }

        return definedAppenderNames;
    }

    /**
     * 从配置解析器中提取Appenders配置
     * @param resolver 配置解析器
     * @param config Log4KeyConfig模型
     */
    private static void extractAppendersConfig(ConfigResolver resolver, Log4KeyConfig config) {
        Pattern appenderPattern = Pattern.compile("^" + ConfigKeys.APPENDERS_PREFIX + "(\\w+)\\.(.+)$");
        Map<String, Map<String, Object>> appenderPropsMap = new HashMap<>();

        // 首先收集所有appender相关的属性
        for (ConfigKey<?> key : resolver.keys()) {
            String keyName = key.name();
            Matcher matcher = appenderPattern.matcher(keyName);
            if (matcher.matches()) {
                String appenderName = matcher.group(1);
                String propName = matcher.group(2);
                Object value = resolver.get(key);

                // 调试：打印匹配的键和appender名称
                logger.debug("[DEBUG] ConfigConverter.extractAppendersConfig: matched key='" + keyName + "', appenderName='" + appenderName + "', propName='" + propName + "'");

                // 修复：如果appender名称是"console"（小写），但XML中定义的是"CONSOLE"（大写），保持原始大小写
                // 注意：XML解析可能将appender名称转换为小写，我们需要保持原始大小写
                // 检查键中是否包含大写的CONSOLE
                if (keyName.contains("CONSOLE") && appenderName.equals("console")) {
                    appenderName = "CONSOLE";
                    logger.debug("[DEBUG] ConfigConverter.extractAppendersConfig: corrected appenderName from 'console' to 'CONSOLE'");
                }

                appenderPropsMap.computeIfAbsent(appenderName, k -> new HashMap<>())
                               .put(propName, value);
            }
        }

        // 然后为每个appender创建配置对象
        for (Map.Entry<String, Map<String, Object>> appenderEntry : appenderPropsMap.entrySet()) {
            String appenderName = appenderEntry.getKey();
            Map<String, Object> props = appenderEntry.getValue();

            // 调试：打印appender名称
            logger.debug("[DEBUG] ConfigConverter.extractAppendersConfig: processing appender name='" + appenderName + "'");

            AppenderConfig appenderConfig = new AppenderConfig();
            appenderConfig.setName(appenderName);
            appenderConfig.setType(getStringValue(props, ConfigKeys.APPENDER_TYPE, "Console"));
            appenderConfig.setFormatter(getStringValue(props, ConfigKeys.APPENDER_FORMATTER, "text"));
            appenderConfig.setConsoleEnabled(getBooleanValue(props, ConfigKeys.CONSOLE_ENABLED, true));

            if (props.containsKey(ConfigKeys.MAX_FILE_SIZE_MB)) {
                appenderConfig.setMaxFileSizeMB(getIntValue(props, ConfigKeys.MAX_FILE_SIZE_MB, ConfigKeys.MAX_FILE_SIZE_MB_KEY.defaultValue()));
            }
            if (props.containsKey(ConfigKeys.MAX_BACKUP_INDEX)) {
                appenderConfig.setMaxBackupIndex(getIntValue(props, ConfigKeys.MAX_BACKUP_INDEX, ConfigKeys.MAX_BACKUP_INDEX_KEY.defaultValue()));
            }
            if (props.containsKey(ConfigKeys.CONSOLE_ENABLED)) {
                appenderConfig.setConsoleEnabled(getBooleanValue(props, ConfigKeys.CONSOLE_ENABLED, ConfigKeys.CONSOLE_ENABLED_KEY.defaultValue()));
            }
            if (props.containsKey(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL)) {
                appenderConfig.setOutputAdmissionLevel(getStringValue(props, ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL, ConfigKeys.APPENDER_LEVEL_KEY.defaultValue()));
            }
            if (props.containsKey(ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY)) {
                String policyName = getStringValue(props, ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY,  ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY_KEY.defaultValue());
                appenderConfig.setOutputLevelPolicy(OutputLevelPolicy.valueOf(policyName));
            }
            if (props.containsKey(ConfigKeys.APPENDER_DIRECTORY)) {
                appenderConfig.setDirectory(getStringValue(props, ConfigKeys.APPENDER_DIRECTORY, ConfigKeys.APPENDER_DIRECTORY_KEY.defaultValue()));
            }
            if (props.containsKey(ConfigKeys.APPENDER_FILE_NAME)) {
                appenderConfig.setFileName(getStringValue(props, ConfigKeys.APPENDER_FILE_NAME, ConfigKeys.APPENDER_FILE_NAME_KEY.defaultValue()));
            }
            if (props.containsKey(ConfigKeys.APPENDER_CHARSET)) {
                appenderConfig.setCharset(getStringValue(props, ConfigKeys.APPENDER_CHARSET, ConfigKeys.APPENDER_CHARSET_KEY.defaultValue()));
            }

            // 添加到配置模型
            config.addAppender(appenderConfig);
        }
    }

    /**
     * 从配置解析器中提取非Root Logger配置
     * @param resolver 配置解析器
     * @param config Log4KeyConfig模型
     */
    private static void extractNonRootLoggersConfig(ConfigResolver resolver, Log4KeyConfig config) {
        // 收集所有非Root Logger相关的属性
        Pattern loggerPattern = Pattern.compile("^" + ConfigKeys.LOGGERS_PREFIX + "(.+?)" + "\\.+" + "(" + ConfigKeys.LOGGER_LEVEL_SUFFIX.replace(".", "") + "|" + ConfigKeys.LOGGER_APPENDERS_SUFFIX.replace(".", "") + ")$");
        Map<String, Map<String, Object>> loggerPropsMap = resolver.collect(loggerPattern);

        // 收集所有已定义的appender名称（用于引用验证）
        Set<String> definedAppenderNames = collectDefinedAppenderNames(resolver);

        // 然后为每个非Root Logger创建配置对象
        for (Map.Entry<String, Map<String, Object>> loggerEntry : loggerPropsMap.entrySet()) {
            String loggerName = loggerEntry.getKey();
            Map<String, Object> props = loggerEntry.getValue();

            LoggerConfig loggerConfig = new LoggerConfig();
            loggerConfig.setName(loggerName);
            loggerConfig.setAdmissionLevel(getStringValue(props, ConfigKeys.LOGGER_LEVEL_SUFFIX.replace(".", ""), "INFO"));

            // 解析appender引用并进行验证
            String appendersStr = getStringValue(props, ConfigKeys.LOGGER_APPENDERS_SUFFIX.replace(".", ""), "console");
            String[] appenderNames = appendersStr.split(",");

            // 验证每个appender引用
            List<String> validAppenderNames = findValidAppender(appenderNames, definedAppenderNames);

            // 设置验证后的appender引用
            loggerConfig.setAppenders(validAppenderNames.toArray(new String[0]));

            // 添加到配置模型
            config.addNonRootLogger(loggerConfig);
        }
    }

    /**
     * 从配置解析器中提取Formatters配置
     * @param resolver 配置解析器
     * @param config Log4KeyConfig模型
     */
    private static void extractFormattersConfig(ConfigResolver resolver, Log4KeyConfig config) {
        logger.debug("[ConfigConverter-DEBUG] extractFormattersConfig called with resolver size = " + resolver.size());

        // 调试：打印所有包含"formatter"的键
        int formatterKeyCount = 0;
        for (ConfigKey<?> key : resolver.keys()) {
            String keyName = key.name();
            if (keyName.contains("formatter")) {
                logger.debug("[ConfigConverter-DEBUG] Found formatter-related key: '" + keyName + "' = " + resolver.get(key));
                formatterKeyCount++;
            }
        }
        logger.debug("[ConfigConverter-DEBUG] Total formatter-related keys: " + formatterKeyCount);

        // 首先收集所有formatter相关的属性
        Pattern formatterPattern = Pattern.compile("^" + ConfigKeys.FORMATTERS_PREFIX + "(\\w+)\\.(.+)$");
        Map<String, Map<String, Object>> formatterPropsMap = resolver.collect(formatterPattern);

        // 然后为每个formatter创建配置对象
        for (Map.Entry<String, Map<String, Object>> formatterEntry : formatterPropsMap.entrySet()) {
            String formatterName = formatterEntry.getKey();
            Map<String, Object> props = formatterEntry.getValue();

            FormatterConfig formatterConfig = new FormatterConfig();
            formatterConfig.setName(formatterName);
            formatterConfig.setType(getStringValue(props, ConfigKeys.APPENDER_TYPE, "Text"));

            // 可选属性，只在有值时设置
            if (props.containsKey(ConfigKeys.FORMATTER_PATTERN)) {
                formatterConfig.setPattern(getStringValue(props, ConfigKeys.FORMATTER_PATTERN, null));
            }

            // 设置JSON formatter的特定属性
            if (props.containsKey("timestamp")) {
                formatterConfig.setTimestamp(getStringValue(props, "timestamp", null));
            }
            if (props.containsKey(ConfigKeys.INCLUDE_LEVEL)) {
                formatterConfig.setIncludeLevel(getBooleanValue(props, ConfigKeys.INCLUDE_LEVEL, true));
            }
            if (props.containsKey(ConfigKeys.INCLUDE_LOGGER)) {
                formatterConfig.setIncludeLogger(getBooleanValue(props, ConfigKeys.INCLUDE_LOGGER, true));
            }
            if (props.containsKey(ConfigKeys.INCLUDE_THREAD)) {
                formatterConfig.setIncludeThread(getBooleanValue(props, ConfigKeys.INCLUDE_THREAD, true));
            }
            if (props.containsKey(ConfigKeys.INCLUDE_MDC)) {
                formatterConfig.setIncludeMdc(getBooleanValue(props, ConfigKeys.INCLUDE_MDC, true));
            }
            if (props.containsKey(ConfigKeys.INCLUDE_TIMESTAMP)) {
                // 如果已经有timestamp属性，跳过includeTimestamp
                if (!props.containsKey("timestamp")) {
                    formatterConfig.setTimestamp(getStringValue(props, ConfigKeys.INCLUDE_TIMESTAMP, "ISO8601"));
                }
            }

            // 设置所有其他属性到additionalProperties
            for (Map.Entry<String, Object> propEntry : props.entrySet()) {
                String propName = propEntry.getKey();
                // 跳过已经处理的属性
                if (!propName.equals(ConfigKeys.APPENDER_TYPE) &&
                    !propName.equals(ConfigKeys.FORMATTER_PATTERN) &&
                    !propName.equals("timestamp") &&
                    !propName.equals(ConfigKeys.INCLUDE_LEVEL) &&
                    !propName.equals(ConfigKeys.INCLUDE_LOGGER) &&
                    !propName.equals(ConfigKeys.INCLUDE_THREAD) &&
                    !propName.equals(ConfigKeys.INCLUDE_MDC) &&
                    !propName.equals(ConfigKeys.INCLUDE_TIMESTAMP)) {
                    formatterConfig.setAdditionalProperty(propName, propEntry.getValue());
                }
            }

            // 调试：打印创建的formatter配置
            logger.debug("[ConfigConverter-DEBUG] extractFormattersConfig: created formatter '" + formatterName +
                             "' type='" + formatterConfig.getType() +
                             "' pattern='" + formatterConfig.getPattern() +
                             "' timestamp='" + formatterConfig.getTimestamp() +
                             "' includeLevel=" + formatterConfig.getIncludeLevel() +
                             "' includeLogger=" + formatterConfig.getIncludeLogger() +
                             "' includeThread=" + formatterConfig.getIncludeThread() +
                             "' includeMdc=" + formatterConfig.getIncludeMdc() +
                             "' additionalProperties=" + formatterConfig.getAdditionalProperties() + "'");

            // 添加到配置模型
            config.addFormatter(formatterConfig);
        }
    }

    /**
     * 计算并缓存所有appender的合并配置
     * @param config Log4KeyConfig配置模型
     */
    private static void computeAndCacheMergedAppenderConfigs(Log4KeyConfig config) {
        Map<String, AppenderConfig> appenders = config.getAppenders();
        if (appenders == null || appenders.isEmpty()) {
            return;
        }

        for (Map.Entry<String, AppenderConfig> entry : appenders.entrySet()) {
            String appenderName = entry.getKey();
            AppenderConfig appenderConfig = entry.getValue();
            Map<String, Object> mergedConfig = computeMergedConfig(config, appenderConfig);
            config.setMergedAppenderConfig(appenderName, mergedConfig);
        }
    }

    /**
     * 计算单个appender的合并配置
     * @param config 全局配置模型
     * @param appenderConfig appender配置
     * @return 合并后的完整配置
     */
    private static Map<String, Object> computeMergedConfig(Log4KeyConfig config, AppenderConfig appenderConfig) {
        Map<String, Object> mergedConfig = new HashMap<>();

        // 1. 首先添加基础配置作为默认值
        mergedConfig.put(ConfigKeys.MAX_FILE_SIZE_MB, config.getGlobalConfig(ConfigKeys.MAX_FILE_SIZE_MB_KEY));
        mergedConfig.put(ConfigKeys.MAX_BACKUP_INDEX, config.getGlobalConfig(ConfigKeys.MAX_BACKUP_INDEX_KEY));
        mergedConfig.put(ConfigKeys.BUFFER_SIZE, config.getGlobalConfig(ConfigKeys.BUFFER_SIZE_KEY));

        // 添加默认配置字段
        mergedConfig.put(ConfigKeys.DEFAULT_ADMISSION_LEVEL, config.getGlobalConfig(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY));
        mergedConfig.put(ConfigKeys.ROOT_DIRECTORY, config.getGlobalConfig(ConfigKeys.ROOT_DIRECTORY_KEY));
        mergedConfig.put(ConfigKeys.DEFAULT_CHARSET, config.getGlobalConfig(ConfigKeys.DEFAULT_CHARSET_KEY));

        // 2. 然后添加appender专用配置
        if (appenderConfig != null) {
            mergedConfig.put(ConfigKeys.APPENDER_TYPE, appenderConfig.getType());
            mergedConfig.put(ConfigKeys.APPENDER_FORMATTER, appenderConfig.getFormatter());
            mergedConfig.put(ConfigKeys.CONSOLE_ENABLED, appenderConfig.isConsoleEnabled());

            if (appenderConfig.getMaxFileSizeMB() > 0) {
                mergedConfig.put(ConfigKeys.MAX_FILE_SIZE_MB, appenderConfig.getMaxFileSizeMB());
            }
            if (appenderConfig.getMaxBackupIndex() > 0) {
                mergedConfig.put(ConfigKeys.MAX_BACKUP_INDEX, appenderConfig.getMaxBackupIndex());
            }
            if (appenderConfig.getOutputAdmissionLevel() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL, appenderConfig.getOutputAdmissionLevel());
            }
            if (appenderConfig.getOutputLevelPolicy() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY, appenderConfig.getOutputLevelPolicy().name());
            }
            if (appenderConfig.getDirectory() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_DIRECTORY, appenderConfig.getDirectory());
            }
            if (appenderConfig.getFileName() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_FILE_NAME, appenderConfig.getFileName());
            }
            if (appenderConfig.getCharset() != null) {
                mergedConfig.put(ConfigKeys.APPENDER_CHARSET, appenderConfig.getCharset());
            }
        }

        // 3. 应用默认值规则：如果appender配置未定义，则使用全局默认值
        if (!mergedConfig.containsKey(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL)) {
            mergedConfig.put(ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL, config.getGlobalConfig(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY));
        }
        if (!mergedConfig.containsKey(ConfigKeys.APPENDER_DIRECTORY)) {
            mergedConfig.put(ConfigKeys.APPENDER_DIRECTORY, config.getGlobalConfig(ConfigKeys.ROOT_DIRECTORY_KEY));
        }
        if (!mergedConfig.containsKey(ConfigKeys.APPENDER_CHARSET)) {
            mergedConfig.put(ConfigKeys.APPENDER_CHARSET, config.getGlobalConfig(ConfigKeys.DEFAULT_CHARSET_KEY));
        }
        if (!mergedConfig.containsKey(ConfigKeys.CONSOLE_ENABLED)) {
            mergedConfig.put(ConfigKeys.CONSOLE_ENABLED, true);
        }

        // 4. 添加appender名称到配置中
        if (appenderConfig != null && appenderConfig.getName() != null) {
            mergedConfig.put(ConfigKeys.APPENDER_NAME, appenderConfig.getName());
        }

        return mergedConfig;
    }

    // ========================== 辅助方法 ==========================

    /**
     * 从Map中获取String类型的值
     * @param map 配置Map
     * @param key 键
     * @param defaultValue 默认值
     * @return String值
     */
    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    /**
     * 从Map中获取int类型的值
     * @param map 配置Map
     * @param key 键
     * @param defaultValue 默认值
     * @return int值
     */
    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 从Map中获取boolean类型的值
     * @param map 配置Map
     * @param key 键
     * @param defaultValue 默认值
     * @return boolean值
     */
    private static boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value) || "yes".equalsIgnoreCase((String) value) || "1".equals(value);
        }
        return defaultValue;
    }
}
