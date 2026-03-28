/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.model;


/**
 * Appender configuration model.
 *
 * Appender配置模型。
 */
public class AppenderConfig {
    /**
     * 追加器名称
     * 用于标识Appender的唯一名称
     */
    private String name;
    
    /**
     * 追加器类型
     * 用于指定Appender的类型，如Console、File等
     */
    private String type;
    
    /**
     * 格式化器名称
     * 用于指定Appender使用的格式化器名称
     */
    private String formatter;
    
    /**
     * 是否支持异步
     * 用于指定Appender是否支持异步日志记录
     */
    private boolean asyncSupported;
    
    /**
     * 最大文件大小（MB）
     * 用于指定单个日志文件的最大大小，单位为MB
     */
    private int maxFileSizeMB;
    
    /**
     * 最大备份索引
     * 用于指定日志文件的最大备份数量
     */
    private int maxBackupIndex;
    
    /**
     * 是否启用控制台输出
     * 用于指定是否将日志额外输出到defaultConsoleAppender
     */
    private boolean consoleEnabled;
    
    /**
     * 允许输出级别
     * 用于指定Appender输出日志的最低级别
     */
    private String outputAdmissionLevel;
    
    /**
     * 输出级别策略
     * 用于指定Appender输出日志的策略，如EXACT（只输出指定级别）或AT_LEAST（输出指定级别及以上）
     */
    private OutputLevelPolicy outputLevelPolicy = OutputLevelPolicy.AT_LEAST;
    
    /**
     * 日志目录
     * 用于指定日志文件的存储目录
     */
    private String directory;
    
    /**
     * 字符编码
     * 用于指定日志文件的字符编码
     */
    private String charset;
    
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

    public String getFormatter() {
        return formatter;
    }

    public void setFormatter(String formatter) {
        this.formatter = formatter;
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    public int getMaxFileSizeMB() {
        return maxFileSizeMB;
    }

    public void setMaxFileSizeMB(int maxFileSizeMB) {
        this.maxFileSizeMB = maxFileSizeMB;
    }

    public int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    public void setMaxBackupIndex(int maxBackupIndex) {
        this.maxBackupIndex = maxBackupIndex;
    }

    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public void setConsoleEnabled(boolean consoleEnabled) {
        this.consoleEnabled = consoleEnabled;
    }

    public String getOutputAdmissionLevel() {
        return outputAdmissionLevel;
    }

    public void setOutputAdmissionLevel(String outputAdmissionLevel) {
        this.outputAdmissionLevel = outputAdmissionLevel;
    }

    public OutputLevelPolicy getOutputLevelPolicy() {
        return outputLevelPolicy;
    }

    public void setOutputLevelPolicy(OutputLevelPolicy outputLevelPolicy) {
        this.outputLevelPolicy = outputLevelPolicy;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public String toString() {
        return "AppenderConfig{" +
                "name='" + name + "'" +
                ", type='" + type + "'" +
                ", formatter='" + formatter + "'" +
                ", asyncSupported=" + asyncSupported +
                ", maxFileSizeMB=" + maxFileSizeMB +
                ", maxBackupIndex=" + maxBackupIndex +
                ", consoleEnabled=" + consoleEnabled +
                ", outputAdmissionLevel='" + outputAdmissionLevel + "'" +
                ", outputLevelPolicy=" + outputLevelPolicy +
                ", directory='" + directory + "'" +
                ", charset='" + charset + "'" +
                '}';
    }
}