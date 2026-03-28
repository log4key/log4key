/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.model;

/**
 * Logger configuration model.
 *
 * Logger配置模型。
 */
public class LoggerConfig {
    /**
     * Logger名称
     * 用于标识Logger的名称，支持前缀匹配
     */
    private String name;
    
    /**
     * 日志准入级别(是否生成事件)
     * 用于指定Logger的准入级别，如INFO, WARN, ERROR, DEBUG等
     */
    private String admissionLevel;
    
    /**
     * 关联的Appenders名称数组
     * 用于指定Logger关联的Appender名称
     */
    private String[] appenders;
    
    /**
     * 获取Logger名称
     * @return Logger名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置Logger名称
     * @param name Logger名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取准入级别
     * @return 准入级别
     */
    public String getAdmissionLevel() {
        return admissionLevel;
    }
    
    /**
     * 设置准入级别
     * @param admissionLevel 准入级别
     */
    public void setAdmissionLevel(String admissionLevel) {
        this.admissionLevel = admissionLevel;
    }
    
    /**
     * 获取关联的Appenders名称数组
     * @return Appenders名称数组
     */
    public String[] getAppenders() {
        return appenders;
    }
    
    /**
     * 设置关联的Appenders名称数组
     * @param appenders Appenders名称数组
     */
    public void setAppenders(String[] appenders) {
        this.appenders = appenders;
    }
    
    @Override
    public String toString() {
        return "LoggerConfig{" +
                "name='" + name + "'" +
                ", admissionLevel='" + admissionLevel + "'" +
                ", appenders=" + java.util.Arrays.toString(appenders) +
                '}';
    }
}