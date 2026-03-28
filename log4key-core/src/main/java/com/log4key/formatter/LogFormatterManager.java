/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter;

import com.log4key.api.LogEvent;
import com.log4key.api.spi.ExtensionManager;
import com.log4key.api.spi.LogFormatter;
import com.log4key.config.Log4KeyConfiguration;
import com.log4key.config.model.FormatterConfig;
import com.log4key.config.model.Log4KeyConfig;
import com.log4key.internal.InternalLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Log formatter manager.
 *
 * 日志格式化器管理器。
 */
public class LogFormatterManager {
    
    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(LogFormatterManager.class);

    private static final LogFormatterManager INSTANCE = new LogFormatterManager();
    
    // 存储所有注册的格式化器，key为格式化器名称
    private final Map<String, LogFormatter> formatters = new ConcurrentHashMap<>();
    
    // 存储链式调用的格式化器列表（按优先级排序）
    private final List<LogFormatter> chainedFormatters = new ArrayList<>();
    
    private LogFormatterManager() {
        // 初始化：加载SPI扩展的格式化器
        loadFormattersFromSpi();
    }
    
    /**
     * 获取单例实例
     * @return 格式化器管理器实例
     */
    public static LogFormatterManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 从SPI加载所有格式化器
     */
    private void loadFormattersFromSpi() {
        List<Class<? extends LogFormatter>> formatterClasses = ExtensionManager.discover(LogFormatter.class);
        for (Class<? extends LogFormatter> formatterClass : formatterClasses) {
            LogFormatter formatter = ExtensionManager.instantiate(formatterClass);
            if (formatter != null) {
                registerFormatter(formatter);
            }
        }
    }
    
    /**
     * 注册一个格式化器
     * @param formatter 要注册的格式化器
     */
    public void registerFormatter(LogFormatter formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter cannot be null");
        }
        
        String name = formatter.getName();
        registerFormatter(name, formatter);
    }

    /**
     * 注册一个格式化器，指定名称
     * @param name 格式化器名称
     * @param formatter 要注册的格式化器
     */
    public void registerFormatter(String name, LogFormatter formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter cannot be null");
        }

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Formatter name cannot be null or empty");
        }

        logger.debug("Registering formatter '" + name + "'");
        formatters.put(name, formatter);

        // 如果格式化器不在链式列表中，则添加并重新排序
        if (!chainedFormatters.contains(formatter)) {
            chainedFormatters.add(formatter);
            sortChainedFormatters();
        }
    }

    /**
     * 获取指定名称的格式化器
     * @param name 格式化器名称
     * @return 格式化器实例，如果不存在则返回null
     */
    public LogFormatter getFormatter(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Formatter name cannot be null or empty");
        }
        
        // 首先尝试精确匹配
        LogFormatter formatter = formatters.get(name);
        if (formatter != null) {
            return formatter;
        }
        
        // 如果精确匹配失败，尝试小写版本（解决大小写不一致问题）
        String lowerCaseName = name.toLowerCase();
        if (!lowerCaseName.equals(name)) {
            formatter = formatters.get(lowerCaseName);
            if (formatter != null) {
                logger.debug("Found formatter '" + name + "' via lowercase lookup as '" + lowerCaseName + "'");
                return formatter;
            }
        }
        
        // 最后尝试大写版本
        String upperCaseName = name.toUpperCase();
        if (!upperCaseName.equals(name)) {
            formatter = formatters.get(upperCaseName);
            if (formatter != null) {
                logger.debug("Found formatter '" + name + "' via uppercase lookup as '" + upperCaseName + "'");
                return formatter;
            }
        }
        
        // 如果都找不到，返回null
        logger.debug("Formatter '" + name + "' not found. Available formatters: " + formatters.keySet());
        return null;
    }
    
    /**
     * 使用指定名称的格式化器格式化日志事件
     * @param event 日志事件
     * @param formatterName 格式化器名称
     * @return 格式化后的日志字符串
     */
    public String format(LogEvent event, String formatterName) {
        return format(event, formatterName, null);
    }
    
    /**
     * 使用指定名称的格式化器格式化日志事件，支持上下文信息
     * @param event 日志事件
     * @param formatterName 格式化器名称
     * @param context 上下文信息
     * @return 格式化后的日志字符串
     */
    public String format(LogEvent event, String formatterName, Map<String, Object> context) {
        LogFormatter formatter = getFormatter(formatterName);
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter not found: " + formatterName);
        }

        return formatter.format(event, context);
    }

    /**
     * 使用链式调用机制格式化日志事件
     * 所有注册的格式化器将按优先级依次调用，前一个格式化器的输出作为后一个的输入
     * @param event 日志事件
     * @return 最终格式化后的日志字符串
     */
    public String formatWithChain(LogEvent event) {
        return formatWithChain(event, null);
    }

    /**
     * 使用链式调用机制格式化日志事件，支持上下文信息
     * 所有注册的格式化器将按优先级依次调用，前一个格式化器的输出作为后一个的输入
     * @param event 日志事件
     * @param context 上下文信息
     * @return 最终格式化后的日志字符串
     */
    public String formatWithChain(LogEvent event, Map<String, Object> context) {
        if (chainedFormatters.isEmpty()) {
            throw new IllegalStateException("No formatters registered in chain");
        }

        // 第一个格式化器直接处理原始事件
        String result = chainedFormatters.get(0).format(event, context);

        // 后续格式化器处理前一个的输出，实现真正的链式调用
        for (int i = 1; i < chainedFormatters.size(); i++) {
            result = chainedFormatters.get(i).formatChain(result, event, context);
        }

        return result;
    }

    /**
     * 配置链式调用的格式化器列表
     * @param formatterNames 格式化器名称列表
     */
    public void configureChain(List<String> formatterNames) {
        if (formatterNames == null || formatterNames.isEmpty()) {
            throw new IllegalArgumentException("Formatter names cannot be null or empty");
        }

        // 清空当前链式列表
        chainedFormatters.clear();

        // 添加指定的格式化器到链式列表
        for (String name : formatterNames) {
            LogFormatter formatter = getFormatter(name);
            if (formatter != null) {
                chainedFormatters.add(formatter);
            }
        }

        // 按优先级排序
        sortChainedFormatters();
    }

    /**
     * 根据配置对象配置格式化器
     * @param config Log4Key配置对象
     */
    public void configureFormatters(Log4KeyConfiguration config) {

        Log4KeyConfig structuredConfig = config.getStructuredConfig();
        if (structuredConfig == null || structuredConfig.getFormatters() == null) {
            logger.debug("Structured config formatters: null");
            return;
        }

        // 获取所有已注册的 Formatter 类型，用于创建新实例
        List<Class<? extends LogFormatter>> availableFormatterClasses = ExtensionManager.discover(LogFormatter.class);
        for (FormatterConfig formatterConfig : structuredConfig.getFormatters().values()) {
            // 尝试获取指定名称的 Formatter
            String name = formatterConfig.getName();
            LogFormatter formatter = getFormatter(name);

            // 如果找不到指定名称的Formatter，尝试根据类型创建新实例
            if (formatter == null) {
                String type = formatterConfig.getType();
                if (type == null) {
                    logger.warn("Formatter '" + name + "' not found. Please specify a valid type.");
                    continue;
                }
                for (Class<? extends LogFormatter> availableClass : availableFormatterClasses) {
                    try {
                        if (availableClass == formatterConfig.getTypeClass()) {
                            formatter = availableClass.getDeclaredConstructor().newInstance();
                            registerFormatter(name, formatter);
                            break;
                        }
                    } catch (Exception e) {
                        // 创建失败，忽略
                        logger.warn("Failed to create formatter '" + name + "' of type '" + type + "'");
                    }
                }
            }

            // 配置formatter
            if (formatter != null) {
                configureFormatterWithConfig(formatter, formatterConfig);
            }
        }
    }

    // -------------------- private --------------------

    /**
     * 按优先级排序链式格式化器列表
     */
    private void sortChainedFormatters() {
        chainedFormatters.sort(Comparator.comparingInt(LogFormatter::getPriority));
    }

    /**
     * 使用FormatterConfig配置Formatter实例
     * @param formatter Formatter实例
     * @param formatterConfig Formatter配置
     */
    private void configureFormatterWithConfig(LogFormatter formatter, FormatterConfig formatterConfig) {
        logger.debug("Configuring formatter '" + formatterConfig.getName() +
                   "' of type '" + formatterConfig.getType() + "'");

        // 尝试调用setPattern方法（兼容TextFormatter）
        if (formatterConfig.getPattern() != null) {
            try {
                java.lang.reflect.Method setPatternMethod = formatter.getClass().getMethod("setPattern", String.class);
                setPatternMethod.invoke(formatter, formatterConfig.getPattern());
                logger.debug("Called setPattern with: " + formatterConfig.getPattern());
            } catch (Exception e) {
                // 如果方法不存在或调用失败，忽略异常
            }
        }

        // 尝试调用setTimestampFormat方法（JsonFormatter）
        if (formatterConfig.getTimestamp() != null) {
            try {
                java.lang.reflect.Method setTimestampMethod = formatter.getClass().getMethod("setTimestampFormat", String.class);
                setTimestampMethod.invoke(formatter, formatterConfig.getTimestamp());
                logger.debug("Called setTimestampFormat with: " + formatterConfig.getTimestamp());
            } catch (Exception e) {
                // 如果方法不存在或调用失败，尝试setTimestamp
                try {
                    java.lang.reflect.Method setTimestampMethod = formatter.getClass().getMethod("setTimestamp", String.class);
                    setTimestampMethod.invoke(formatter, formatterConfig.getTimestamp());
                    logger.debug("Called setTimestamp with: " + formatterConfig.getTimestamp());
                } catch (Exception e2) {
                    // 忽略异常
                }
            }
        }

        // 尝试调用setIncludeLevel方法
        if (formatterConfig.getIncludeLevel() != null) {
            try {
                java.lang.reflect.Method setIncludeLevelMethod = formatter.getClass().getMethod("setIncludeLevel", boolean.class);
                setIncludeLevelMethod.invoke(formatter, formatterConfig.getIncludeLevel());
                logger.debug("Called setIncludeLevel with: " + formatterConfig.getIncludeLevel());
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 尝试调用setIncludeLogger方法
        if (formatterConfig.getIncludeLogger() != null) {
            try {
                java.lang.reflect.Method setIncludeLoggerMethod = formatter.getClass().getMethod("setIncludeLogger", boolean.class);
                setIncludeLoggerMethod.invoke(formatter, formatterConfig.getIncludeLogger());
                logger.debug("Called setIncludeLogger with: " + formatterConfig.getIncludeLogger());
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 尝试调用setIncludeThread方法
        if (formatterConfig.getIncludeThread() != null) {
            try {
                java.lang.reflect.Method setIncludeThreadMethod = formatter.getClass().getMethod("setIncludeThread", boolean.class);
                setIncludeThreadMethod.invoke(formatter, formatterConfig.getIncludeThread());
                logger.debug("Called setIncludeThread with: " + formatterConfig.getIncludeThread());
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 尝试调用setIncludeMdc方法
        if (formatterConfig.getIncludeMdc() != null) {
            try {
                java.lang.reflect.Method setIncludeMdcMethod = formatter.getClass().getMethod("setIncludeMdc", boolean.class);
                setIncludeMdcMethod.invoke(formatter, formatterConfig.getIncludeMdc());
                logger.debug("Called setIncludeMdc with: " + formatterConfig.getIncludeMdc());
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 处理additionalProperties
        if (formatterConfig.getAdditionalProperties() != null && !formatterConfig.getAdditionalProperties().isEmpty()) {
            for (Map.Entry<String, Object> entry : formatterConfig.getAdditionalProperties().entrySet()) {
                String propName = entry.getKey();
                Object propValue = entry.getValue();

                // 尝试找到对应的setter方法
                String setterName = "set" + Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
                try {
                    // 根据值类型尝试不同的方法签名
                    if (propValue instanceof Boolean) {
                        java.lang.reflect.Method setterMethod = formatter.getClass().getMethod(setterName, boolean.class);
                        setterMethod.invoke(formatter, (Boolean) propValue);
                        logger.debug("Called " + setterName + " with boolean: " + propValue);
                    } else if (propValue instanceof String) {
                        java.lang.reflect.Method setterMethod = formatter.getClass().getMethod(setterName, String.class);
                        setterMethod.invoke(formatter, (String) propValue);
                        logger.debug("Called " + setterName + " with String: " + propValue);
                    } else if (propValue instanceof Integer) {
                        java.lang.reflect.Method setterMethod = formatter.getClass().getMethod(setterName, int.class);
                        setterMethod.invoke(formatter, (Integer) propValue);
                        logger.debug("Called " + setterName + " with int: " + propValue);
                    }
                } catch (Exception e) {
                    // 忽略异常，该属性可能没有对应的setter
                }
            }
        }

        logger.debug("Finished configuring formatter '" + formatterConfig.getName() + "'");
    }

}
