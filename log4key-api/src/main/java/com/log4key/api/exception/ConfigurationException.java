/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.exception;

/**
 * Configuration exception class that represents exceptions during configuration loading or parsing.
 *
 * 配置异常类，用于表示配置加载或解析过程中的异常。
 */
public class ConfigurationException extends LogComponentException {

    /**
     * Constructor.
     *
     * 构造函数。
     *
     * @param message exception message / 异常消息
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * 构造函数。
     *
     * @param message exception message / 异常消息
     * @param cause exception cause / 异常原因
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
