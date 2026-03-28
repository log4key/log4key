/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config;

import com.log4key.api.LogConfig;
import com.log4key.api.exception.ConfigurationException;
import com.log4key.config.resolver.ConfigAccumulator;

import java.util.List;
import java.util.Map;

/**
 * Configuration validator interface.
 *
 * 配置验证器接口。
 */
public interface ConfigValidator {

    /**
     * 验证配置
     * @param accumulator 要验证的ConfigAccumulator对象
     * @throws ConfigurationException 如果配置验证失败，抛出ConfigurationException
     */
    void validate(ConfigAccumulator accumulator) throws ConfigurationException;

    /**
     * 验证配置并返回验证结果列表
     * @param config 要验证的LogConfig对象
     * @return 验证结果列表，包含所有验证问题
     */
    List<ValidationResult> validateAndReturnResults(LogConfig config);

    /**
     * 验证配置并返回验证结果列表
     * @param accumulator 要验证的ConfigAccumulator对象
     * @return 验证结果列表，包含所有验证问题
     */
    List<ValidationResult> validateAndReturnResults(ConfigAccumulator accumulator);

    /**
     * 验证XML配置并返回验证结果列表
     * @param flatMap 扁平化的XML配置Map
     * @return 验证结果列表，包含所有验证问题
     */
    List<ValidationResult> validateXmlConfigAndReturnResults(Map<String, Object> flatMap);

    /**
     * 配置验证结果类，用于存储验证结果信息
     */
    class ValidationResult {
        private final boolean isValid;
        private final String message;
        private final String configKey;
        private final ValidationLevel level;

        public ValidationResult(boolean isValid, String message, String configKey, ValidationLevel level) {
            this.isValid = isValid;
            this.message = message;
            this.configKey = configKey;
            this.level = level;
        }

        public ValidationResult(boolean isValid, String message, String configKey) {
            this(isValid, message, configKey, ValidationLevel.ERROR);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getMessage() {
            return message;
        }

        public String getConfigKey() {
            return configKey;
        }

        public ValidationLevel getLevel() {
            return level;
        }

        /**
         * 验证级别枚举
         */
        public enum ValidationLevel {
            FATAL,  // 直接失败
            ERROR,  // 忽略该对象，其它继续
            WARN    // 跳过字段
        }
    }
}
