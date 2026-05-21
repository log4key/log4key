/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.resolver;

import com.log4key.config.ConfigKeys;
import com.log4key.config.key.ConfigKey;
import com.log4key.config.model.OutputLevelPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration accumulator and builder.
 *
 * 配置项的构建器。
 */
public final class ConfigAccumulator {

    private final Map<ConfigKey<?>, Object> values = new HashMap<>();

    // ========================== public 方法 ==========================

    /**
     * 设置配置值
     * 支持覆盖重复定义，后定义的值会覆盖先定义的值
     *
     * @param key 配置键
     * @param value 配置值
     * @param <T> 配置值类型
     */
    public <T> void set(ConfigKey<T> key, T value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    /**
     * 添加配置值
     * @param key 配置键
     * @param value 配置值
     * @param <T> 配置值类型
     * @return 构建器实例
     */
    public <T> ConfigAccumulator with(ConfigKey<T> key, T value) {
        if (value != null) {
            values.put(key, value);
        }
        return this;
    }

    /**
     * 获取当前构建器中的配置值数量
     * @return 配置值数量
     */
    public int size() {
        return values.size();
    }

    /**
     * 检查是否包含指定的配置键
     * @param key 配置键
     * @return 是否包含
     */
    public boolean contains(ConfigKey<?> key) {
        return values.containsKey(key);
    }

    /**
     * 获取内部配置值映射
     * 用于配置验证器提取配置信息
     * @return 配置值映射
     */
    public Map<ConfigKey<?>, Object> getValues() {
        return values;
    }

    /**
     * 冻结配置，生成ConfigResolver
     * 执行完整的结构校验，包括：
     * 1. 默认值填充
     * 2. 结构完整性校验
     * 3. 引用合法性校验
     * 4. 字段合法性校验
     *
     * 暂不考虑优化 freeze() 方法的性能，专注于功能正确性
     *
     * @return ConfigResolver 配置解析器
     */
    public ConfigResolver freeze() {
        // 首先补充缺少的必要配置项
        supplementMissingConfig();

        // 然后验证配置
        validateConfig();

        // 最后生成ConfigResolver
        return new ConfigResolver(values);
    }

    /**
     * 从另一个ConfigAccumulator合并配置
     * @param other 另一个ConfigAccumulator
     */
    public void mergeFrom(ConfigAccumulator other) {
        values.putAll(other.values);
    }

    /**
     * 从配置解析器合并其他配置项
     * @param resolver 配置解析器
     */
    public void merge(ConfigResolver resolver) {
        if (resolver == null || resolver.isEmpty()) return;
        for (ConfigKey<?> key : resolver.keys()) {
            values.put(key, resolver.get(key));
        }
    }

    /**
     * 配置全局参数
     *
     * @param key   配置键
     * @param value 配置值
     * @param <T>   配置值类型
     * @return ConfigAccumulator 配置累加器
     */
    public <T> ConfigAccumulator global(ConfigKey<T> key, T value) {
        set(key, value);
        return this;
    }

    /**
     * 配置全局参数
     *
     * @param name  配置名称
     * @param value 配置值
     * @param <T>   配置值类型
     * @return ConfigAccumulator 配置累加器
     */
    @SuppressWarnings("unchecked")
    public <T> ConfigAccumulator global(String name, T value) {
        set(new ConfigKey<>(name, (Class<T>) value.getClass(), null), value);
        return this;
    }

    /**
     * 配置格式化器
     *
     * @param name   格式化器名称
     * @param config 配置回调
     * @return ConfigAccumulator 配置累加器
     */
    public ConfigAccumulator formatter(String name, java.util.function.Consumer<FormatterConfig> config) {
        FormatterConfig formatterConfig = new FormatterConfig(this, name);
        config.accept(formatterConfig);
        return this;
    }

    /**
     * 配置Appender
     *
     * @param name   Appender名称
     * @param config 配置回调
     * @return ConfigAccumulator 配置累加器
     */
    public ConfigAccumulator appender(String name, java.util.function.Consumer<AppenderConfig> config) {
        AppenderConfig appenderConfig = new AppenderConfig(this, name);
        config.accept(appenderConfig);
        return this;
    }

    /**
     * 配置控制台Appender
     *
     * @param name   Appender名称
     * @param config 配置回调
     * @return ConfigAccumulator 配置累加器
     */
    public ConfigAccumulator consoleAppender(String name, java.util.function.Consumer<AppenderConfig> config) {
        AppenderConfig appenderConfig = new AppenderConfig(this, name);
        appenderConfig.type("Console");
        config.accept(appenderConfig);
        return this;
    }

    /**
     * 配置文件Appender
     *
     * @param name   Appender名称
     * @param config 配置回调
     * @return ConfigAccumulator 配置累加器
     */
    public ConfigAccumulator fileAppender(String name, java.util.function.Consumer<AppenderConfig> config) {
        AppenderConfig appenderConfig = new AppenderConfig(this, name);
        appenderConfig.type("File");
        config.accept(appenderConfig);
        return this;
    }

    /**
     * 配置Root Logger
     *
     * @param config 配置回调
     * @return ConfigAccumulator 配置累加器
     */
    public ConfigAccumulator rootLogger(java.util.function.Consumer<RootLoggerConfig> config) {
        RootLoggerConfig rootLoggerConfig = new RootLoggerConfig(this);
        config.accept(rootLoggerConfig);
        return this;
    }

    /**
     * 配置自定义Logger
     *
     * @param name   Logger名称
     * @param config 配置回调
     * @return ConfigAccumulator 配置累加器
     */
    public ConfigAccumulator logger(String name, java.util.function.Consumer<LoggerConfig> config) {
        LoggerConfig loggerConfig = new LoggerConfig(this, name);
        config.accept(loggerConfig);
        return this;
    }

    // ========================== private 方法 ==========================

    /**
     * 添加未类型化的配置值（内部使用）
     * 仅在类型检查通过时添加值
     *
     * @param key   配置键
     * @param value 配置值
     */
    private void withUntyped(ConfigKey<?> key, Object value) {
        if (value != null) {
            // 检查类型是否匹配
            if (key.type().isInstance(value)) {
                // 安全转换，因为我们已经检查了类型
                values.put(key, value);
            }
        }
    }

    /**
     * 验证配置
     * 使用DefaultConfigValidator对配置进行验证
     * 符合Fail-Fast原则，配置错误时立即抛出异常
     */
    private void validateConfig() {
        try {
            com.log4key.config.DefaultConfigValidator validator = new com.log4key.config.DefaultConfigValidator();
            validator.validate(this);
        } catch (com.log4key.api.exception.ConfigurationException e) {
            // 转换为运行时异常，保持向后兼容
            throw new RuntimeException("Configuration validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 补充缺少的必要配置项
     * 从ConfigKeys中读取默认值并添加缺少的必要配置项
     * 确保与ConfigKeys语义一致
     */
    public void supplementMissingConfig() {
        try {
            // 补充全局基础运行配置
            supplementGlobalConfig();

            // 补充rootLogger配置
            supplementRootLoggerConfig();

            // 补充formatter配置
            supplementFormatterConfig();

            // 补充appender配置（只有当没有任何有效appender时）
            supplementAppenderConfig();
        } catch (Exception e) {
            // 捕获异常，打印详细信息
            System.err.println("Error in supplementMissingConfig() method: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 补充全局基础运行配置
     */
    private void supplementGlobalConfig() {
        // 补充默认日志准入级别
        if (!contains(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY)) {
            with(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY, ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY.defaultValue());
        }

        // 补充默认日志文件输出目录
        if (!contains(ConfigKeys.ROOT_DIRECTORY_KEY)) {
            with(ConfigKeys.ROOT_DIRECTORY_KEY, ConfigKeys.ROOT_DIRECTORY_KEY.defaultValue());
        }

        // 补充默认字符集
        if (!contains(ConfigKeys.DEFAULT_CHARSET_KEY)) {
            with(ConfigKeys.DEFAULT_CHARSET_KEY, ConfigKeys.DEFAULT_CHARSET_KEY.defaultValue());
        }

        // 补充关闭钩子
        if (!contains(ConfigKeys.SHUTDOWN_HOOK_KEY)) {
            with(ConfigKeys.SHUTDOWN_HOOK_KEY, ConfigKeys.SHUTDOWN_HOOK_KEY.defaultValue());
        }

        // 补充核心线程数
        if (!contains(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY)) {
            with(ConfigKeys.EXECUTOR_THREADS_SIZE_KEY, ConfigKeys.EXECUTOR_THREADS_SIZE_KEY.defaultValue());
        }

        // 补充执行器队列大小
        if (!contains(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY)) {
            with(ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY, ConfigKeys.EXECUTOR_QUEUE_SIZE_KEY.defaultValue());
        }

        // 补充执行器类型
        if (!contains(ConfigKeys.EXECUTOR_TYPE_KEY)) {
            with(ConfigKeys.EXECUTOR_TYPE_KEY, ConfigKeys.EXECUTOR_TYPE_KEY.defaultValue());
        }

        // 补充缓冲区大小
        if (!contains(ConfigKeys.BUFFER_SIZE_KEY)) {
            with(ConfigKeys.BUFFER_SIZE_KEY, ConfigKeys.BUFFER_SIZE_KEY.defaultValue());
        }

        // 补充最大文件大小
        if (!contains(ConfigKeys.MAX_FILE_SIZE_MB_KEY)) {
            with(ConfigKeys.MAX_FILE_SIZE_MB_KEY, ConfigKeys.MAX_FILE_SIZE_MB_KEY.defaultValue());
        }

        // 补充最大备份索引
        if (!contains(ConfigKeys.MAX_BACKUP_INDEX_KEY)) {
            with(ConfigKeys.MAX_BACKUP_INDEX_KEY, ConfigKeys.MAX_BACKUP_INDEX_KEY.defaultValue());
        }

        // 补充包含位置信息开关
        if (!contains(ConfigKeys.INCLUDE_LOCATION_KEY)) {
            with(ConfigKeys.INCLUDE_LOCATION_KEY, ConfigKeys.INCLUDE_LOCATION_KEY.defaultValue());
        }
    }

    /**
     * 补充rootLogger配置
     */
    private void supplementRootLoggerConfig() {
        // 补充rootLogger级别
        if (!contains(ConfigKeys.ROOT_LOGGER_LEVEL_KEY)) {
            with(ConfigKeys.ROOT_LOGGER_LEVEL_KEY, ConfigKeys.ROOT_LOGGER_LEVEL_KEY.defaultValue());
        }

        // 补充rootLogger appenders
        if (!contains(ConfigKeys.ROOT_LOGGER_APPENDERS_KEY)) {
            with(ConfigKeys.ROOT_LOGGER_APPENDERS_KEY, ConfigKeys.ROOT_LOGGER_APPENDERS_KEY.defaultValue());
        }
    }

    /**
     * 补充formatter配置
     * 参考默认配置文件log4key-default.xml，补充formatter相关的配置
     * 仅在用户未定义同名formatter时才添加默认值，避免覆盖用户配置
     */
    private void supplementFormatterConfig() {
        // 补充CONSOLE_DEFAULT formatter（仅在不存在时）
        if (!hasFormatter("CONSOLE_DEFAULT")) {
            formatter("CONSOLE_DEFAULT", formatter -> {
                formatter.type("Text");
                formatter.pattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} : %msg");
            });
        }

        // 补充TEXT_DEFAULT formatter（仅在不存在时）
        if (!hasFormatter("TEXT_DEFAULT")) {
            formatter("TEXT_DEFAULT", formatter -> {
                formatter.type("Text");
                formatter.pattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} : %msg%n");
            });
        }

        // 补充JSON_DEFAULT formatter（仅在不存在时）
        if (!hasFormatter("JSON_DEFAULT")) {
            formatter("JSON_DEFAULT", formatter -> {
                formatter.type("Json");
                formatter.property("timestamp", "ISO8601");
                formatter.property("includeLevel", true);
                formatter.property("includeLogger", true);
                formatter.property("includeThread", true);
                formatter.property("includeMdc", true);
            });
        }
    }

    /**
     * 检查指定名称的formatter是否已经存在
     * 通过检查 formatters.{name}.type 键是否存在来判断
     *
     * @param name formatter名称
     * @return 如果已存在返回true，否则返回false
     */
    private boolean hasFormatter(String name) {
        return contains(new ConfigKey<>(ConfigKeys.FORMATTERS_PREFIX + name + ".type", String.class, null));
    }

    /**
     * 补充appender配置
     * 只有当没有任何有效appender时，才会补充默认的appender
     * 参考默认配置文件log4key-default.xml，补充appender相关的配置
     */
    private void supplementAppenderConfig() {
        // 检查是否已有有效appender
        boolean hasAppender = false;
        // 简单检查是否存在appender相关的配置
        for (ConfigKey<?> key : values.keySet()) {
            if (key.name().startsWith(ConfigKeys.APPENDERS_PREFIX)) {
                hasAppender = true;
                break;
            }
        }

        // 只有当没有任何有效appender时，才会补充默认的appender
        if (!hasAppender) {
            // 补充CONSOLE appender
            consoleAppender("CONSOLE", appender -> {
                appender.formatter("CONSOLE_DEFAULT");
                appender.level("INFO");
                appender.charset("UTF-8");
            });

            // 补充FILE appender
            fileAppender("FILE", appender -> {
                appender.formatter("TEXT_DEFAULT");
                appender.level("INFO");
                appender.directory("./logs/file");
                appender.charset("UTF-8");
                appender.consoleEnabled(true);
            });

            // 补充AUDIT_FILE appender
            fileAppender("AUDIT_FILE", appender -> {
                appender.formatter("JSON_DEFAULT");
                appender.level("INFO");
                appender.directory("./logs/json");
                appender.consoleEnabled(false);
            });
        }
    }

    // ========================== 内部类 ==========================

    /**
     * 格式化器配置类
     */
    public static class FormatterConfig {
        private final ConfigAccumulator accumulator;
        private final String name;

        public FormatterConfig(ConfigAccumulator accumulator, String name) {
            this.accumulator = accumulator;
            this.name = name;
        }

        /**
         * 设置格式化器类型
         *
         * @param type 格式化器类型
         * @return FormatterConfig 格式化器配置
         */
        public FormatterConfig type(String type) {
            accumulator.set(new ConfigKey<>(ConfigKeys.FORMATTERS_PREFIX + name + ".type", String.class, null), type);
            return this;
        }

        /**
         * 设置格式化器模式
         *
         * @param pattern 格式化器模式
         * @return FormatterConfig 格式化器配置
         */
        public FormatterConfig pattern(String pattern) {
            accumulator.set(new ConfigKey<>(ConfigKeys.FORMATTERS_PREFIX + name + "." + ConfigKeys.FORMATTER_PATTERN, String.class, null), pattern);
            return this;
        }

        /**
         * 设置其他属性
         *
         * @param key   属性名
         * @param value 属性值
         * @param <T>   属性值类型
         * @return FormatterConfig 格式化器配置
         */
        @SuppressWarnings("unchecked")
        public <T> FormatterConfig property(String key, T value) {
            accumulator.set(new ConfigKey<>(ConfigKeys.FORMATTERS_PREFIX + name + "." + key, (Class<T>) value.getClass(), null), value);
            return this;
        }
    }

    /**
     * Appender配置类
     */
    public static class AppenderConfig {
        private final ConfigAccumulator accumulator;
        private final String name;

        public AppenderConfig(ConfigAccumulator accumulator, String name) {
            this.accumulator = accumulator;
            this.name = name;
        }

        /**
         * 设置Appender类型
         *
         * @param type Appender类型
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig type(String type) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.APPENDER_TYPE, String.class, null), type);
            return this;
        }

        /**
         * 设置Appender使用的Formatter
         *
         * @param formatter Formatter名称
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig formatter(String formatter) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.APPENDER_FORMATTER, String.class, null), formatter);
            return this;
        }

        /**
         * 内联配置格式化器
         *
         * @param config 格式化器配置回调
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig formatter(java.util.function.Consumer<FormatterConfig> config) {
            // 生成唯一的格式化器名称，基于appender名称
            String formatterName = name + "_FORMatter";

            // 创建并配置格式化器
            FormatterConfig formatterConfig = new FormatterConfig(accumulator, formatterName);
            config.accept(formatterConfig);

            // 将appender关联到这个格式化器
            return formatter(formatterName);
        }

        /**
         * 设置Appender级别
         *
         * @param level 级别
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig level(String level) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.APPENDER_OUTPUT_ADMISSION_LEVEL, String.class, null), level);
            return this;
        }

        /**
         * 设置Appender目录
         *
         * @param directory 目录
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig directory(String directory) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.APPENDER_DIRECTORY, String.class, null), directory);
            return this;
        }

        /**
         * 设置Appender字符集
         *
         * @param charset 字符集
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig charset(String charset) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.APPENDER_CHARSET, String.class, null), charset);
            return this;
        }

        /**
         * 设置控制台启用开关
         *
         * @param enabled 是否启用
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig consoleEnabled(boolean enabled) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.CONSOLE_ENABLED, Boolean.class, null), enabled);
            return this;
        }

        /**
         * 配置输出级别策略
         *
         * @param levelPolicy 输出级别策略
         * @return AppenderConfig Appender配置
         */
        public AppenderConfig levelPolicy(OutputLevelPolicy levelPolicy) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY_KEY, String.class, null), levelPolicy.toString());
            return this;
        }

        /**
         * 设置其他属性
         *
         * @param key   属性名
         * @param value 属性值
         * @param <T>   属性值类型
         * @return AppenderConfig Appender配置
         */
        @SuppressWarnings("unchecked")
        public <T> AppenderConfig property(String key, T value) {
            accumulator.set(new ConfigKey<>(ConfigKeys.APPENDERS_PREFIX + name + "." + key, (Class<T>) value.getClass(), null), value);
            return this;
        }
    }

    /**
     * Root Logger配置类
     */
    public static class RootLoggerConfig {
        private final ConfigAccumulator accumulator;

        public RootLoggerConfig(ConfigAccumulator accumulator) {
            this.accumulator = accumulator;
        }

        /**
         * 设置Root Logger级别
         *
         * @param level 级别
         * @return RootLoggerConfig Root Logger配置
         */
        public RootLoggerConfig level(String level) {
            accumulator.set(com.log4key.config.ConfigKeys.ROOT_LOGGER_LEVEL_KEY, level);
            return this;
        }

        /**
         * 设置Root Logger使用的Appenders
         *
         * @param appenders Appender名称列表
         * @return RootLoggerConfig Root Logger配置
         */
        public RootLoggerConfig appenders(java.util.List<String> appenders) {
            accumulator.set(com.log4key.config.ConfigKeys.ROOT_LOGGER_APPENDERS_KEY, String.join(",", appenders));
            return this;
        }

        /**
         * 设置Root Logger使用的Appenders
         *
         * @param appenders Appender名称数组
         * @return RootLoggerConfig Root Logger配置
         */
        public RootLoggerConfig appenders(String... appenders) {
            accumulator.set(com.log4key.config.ConfigKeys.ROOT_LOGGER_APPENDERS_KEY, String.join(",", appenders));
            return this;
        }
    }

    /**
     * Logger配置类
     */
    public static class LoggerConfig {
        private final ConfigAccumulator accumulator;
        private final String name;

        public LoggerConfig(ConfigAccumulator accumulator, String name) {
            this.accumulator = accumulator;
            this.name = name;
        }

        /**
         * 设置Logger级别
         *
         * @param level 级别
         * @return LoggerConfig Logger配置
         */
        public LoggerConfig level(String level) {
            accumulator.set(new ConfigKey<>(ConfigKeys.LOGGERS_PREFIX + name + ConfigKeys.LOGGER_LEVEL_SUFFIX, String.class, null), level);
            return this;
        }

        /**
         * 设置Logger使用的Appenders
         *
         * @param appenders Appender名称列表
         * @return LoggerConfig Logger配置
         */
        public LoggerConfig appenders(java.util.List<String> appenders) {
            accumulator.set(new ConfigKey<>(ConfigKeys.LOGGERS_PREFIX + name + ConfigKeys.LOGGER_APPENDERS_SUFFIX, String.class, null), String.join(",", appenders));
            return this;
        }

        /**
         * 设置Logger使用的Appenders
         *
         * @param appenders Appender名称数组
         * @return LoggerConfig Logger配置
         */
        public LoggerConfig appenders(String... appenders) {
            accumulator.set(new ConfigKey<>(ConfigKeys.LOGGERS_PREFIX + name + ConfigKeys.LOGGER_APPENDERS_SUFFIX, String.class, null), String.join(",", appenders));
            return this;
        }
    }
}
