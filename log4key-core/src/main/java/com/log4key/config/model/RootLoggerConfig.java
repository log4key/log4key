/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.model;

/**
 * Root Logger configuration model.
 *
 * Root Logger配置模型。
 */
public class RootLoggerConfig {
    /**
     * 准入级别
     * 用于指定Root Logger的准入级别，如INFO, WARN, ERROR, DEBUG等
     */
    private String admissionLevel;
    
    /**
     * 追加器列表
     * 用于指定Root Logger使用的追加器名称数组，如["console", "file"]
     */
    private String[] appenders;

    // Getters and Setters
    public String getAdmissionLevel() {
        return admissionLevel;
    }

    public void setAdmissionLevel(String admissionLevel) {
        this.admissionLevel = admissionLevel;
    }
    
    // 兼容方法，用于支持测试代码
    public String getLevel() {
        return getAdmissionLevel();
    }
    
    // 兼容方法，用于支持测试代码
    public void setLevel(String level) {
        setAdmissionLevel(level);
    }

    public String[] getAppenders() {
        return appenders;
    }

    public void setAppenders(String[] appenders) {
        this.appenders = appenders;
    }

    @Override
    public String toString() {
        return "RootLoggerConfig{" +
                "admissionLevel='" + admissionLevel + "'" +
                ", appenders=" + java.util.Arrays.toString(appenders) +
                '}';
    }
}