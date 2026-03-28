/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.appender.AppenderProvider;
import com.log4key.api.appender.AppenderType;
import com.log4key.config.model.OutputLevelPolicy;

/**
 * Abstract appender provider base class.
 *
 * 抽象Appender提供者。
 */
public abstract class AbstractAppenderProvider implements AppenderProvider {

    /**
     * Appender类型
     */
    private final AppenderType type;

    protected AbstractAppenderProvider(AppenderType type) {
        this.type = type;
    }

    @Override
    public AppenderType getType() {
        return type;
    }

    /**
     * Sets the output admission level for this appender.
     *
     * 设置Appender输出准入级别。
     *
     * @param outputAdmissionLevel the output admission level / 输出准入级别
     */
    public void setOutputAdmissionLevel(String outputAdmissionLevel) {
        if (outputAdmissionLevel != null) {
            this.outputAdmissionLevel = outputAdmissionLevel.toUpperCase();
        }
    }

    /**
     * Sets the output level policy for this appender.
     *
     * 设置Appender输出级别策略。
     *
     * @param outputLevelPolicy the output level policy / 输出级别策略
     */
    public void setOutputLevelPolicy(OutputLevelPolicy outputLevelPolicy) {
        if (outputLevelPolicy != null) {
            this.outputLevelPolicy = outputLevelPolicy;
        }
    }

    /**
     * Checks if the log event should be output.
     *
     * 判断是否应该输出日志事件。
     *
     * @param event the log event / 日志事件
     * @return true if should output / 是否应该输出
     */
    protected boolean shouldOutput(LogEvent event) {
        if (event == null) {
            return false;
        }

        int eventPriority = getLevelPriority(event.getLevel());
        int outputPriority = getLevelPriority(outputAdmissionLevel);

        switch (outputLevelPolicy) {
            case EXACT:
                return eventPriority == outputPriority;
            case AT_LEAST:
                return eventPriority >= outputPriority;
            default:
                return true;
        }
    }

    // ========================== ConfigResolver 支持（Phase 5） ==========================

    /**
     * 使用ConfigResolver初始化输出提供者（新版本）
     * 默认实现将ConfigResolver转换为Map后调用原有initialize方法
     * @param config 配置解析器
     */
    public void initialize(com.log4key.config.resolver.ConfigResolver config) {
        if (config == null) {
            initialize((java.util.Map<String, Object>) null);
            return;
        }

        // 将ConfigResolver转换为Map<String, Object>
        java.util.Map<String, Object> mapConfig = new java.util.HashMap<>();
        for (com.log4key.config.key.ConfigKey<?> key : config.keys()) {
            Object value = config.get(key);
            if (value != null) {
                mapConfig.put(key.name(), value);
            }
        }

        initialize(mapConfig);
    }

    // ========================== 输出级别控制字段 ==========================

    /**
     * Appender输出准入级别
     */
    protected String outputAdmissionLevel = "INFO";

    /**
     * Appender输出级别策略
     */
    protected OutputLevelPolicy outputLevelPolicy = OutputLevelPolicy.AT_LEAST;

    /**
     * 获取日志级别的优先级
     * @param level 日志级别名称
     * @return 优先级数值（数值越小优先级越高）
     */
    private int getLevelPriority(String level) {
        if (level == null) {
            return getLevelPriority("INFO");
        }

        String upperLevel = level.toUpperCase();
        switch (upperLevel) {
            case "TRACE":
                return 0;
            case "DEBUG":
                return 1;
            case "INFO":
                return 2;
            case "WARN":
                return 3;
            case "ERROR":
                return 4;
            default:
                return getLevelPriority("INFO");
        }
    }
}
