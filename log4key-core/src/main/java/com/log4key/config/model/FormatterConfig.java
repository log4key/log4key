/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.model;

import com.log4key.api.spi.LogFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * Formatter configuration model.
 *
 * Formatter配置模型。
 */
public class FormatterConfig {
    /**
     * 格式化器名称
     * 用于标识Formatter的唯一名称
     */
    private String name;
    
    /**
     * 格式化器类型
     * 用于指定Formatter的类型，如Pattern等
     */
    private String type;
    
    /**
     * 格式化模式
     * 用于指定日志格式化的模式字符串
     */
    private String pattern;
    
    /**
     * 时间戳格式
     * 用于指定JSON格式中的时间戳格式，如ISO8601
     */
    private String timestamp;
    
    /**
     * 是否包含日志级别
     * 用于指定JSON格式中是否包含level字段
     */
    private Boolean includeLevel;
    
    /**
     * 是否包含Logger名称
     * 用于指定JSON格式中是否包含loggerName字段
     */
    private Boolean includeLogger;
    
    /**
     * 是否包含线程信息
     * 用于指定JSON格式中是否包含thread字段
     */
    private Boolean includeThread;
    
    /**
     * 是否包含MDC上下文
     * 用于指定JSON格式中是否包含mdc字段
     */
    private Boolean includeMdc;
    
    /**
     * 额外属性映射
     * 用于存储其他自定义属性
     */
    private Map<String, Object> additionalProperties = new HashMap<>();
    
    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIncludeLevel() {
        return includeLevel;
    }

    public void setIncludeLevel(Boolean includeLevel) {
        this.includeLevel = includeLevel;
    }

    public Boolean getIncludeLogger() {
        return includeLogger;
    }

    public void setIncludeLogger(Boolean includeLogger) {
        this.includeLogger = includeLogger;
    }

    public Boolean getIncludeThread() {
        return includeThread;
    }

    public void setIncludeThread(Boolean includeThread) {
        this.includeThread = includeThread;
    }

    public Boolean getIncludeMdc() {
        return includeMdc;
    }

    public void setIncludeMdc(Boolean includeMdc) {
        this.includeMdc = includeMdc;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperty(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

    /**
     * 获取格式化器类型对应的Class对象
     * @return 格式化器类型对应的Class对象
     */
    public Class<? extends LogFormatter> getTypeClass() {
        if (type == null) {
            return com.log4key.formatter.TextLogFormatter.class;
        }
        switch (type.toLowerCase()) {
            case "json":
                return com.log4key.formatter.JsonLogFormatter.class;
            case "pattern":
                return com.log4key.formatter.PatternFormatter.class;
            default:
                return com.log4key.formatter.TextLogFormatter.class;
        }
    }

    @Override
    public String toString() {
        return "FormatterConfig{" +
                "name='" + name + "'" +
                ", type='" + type + "'" +
                ", pattern='" + pattern + "'" +
                ", timestamp='" + timestamp + "'" +
                ", includeLevel=" + includeLevel +
                ", includeLogger=" + includeLogger +
                ", includeThread=" + includeThread +
                ", includeMdc=" + includeMdc +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}