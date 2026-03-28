/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.exception;

/**
 * Base exception class for log components. All log component exceptions should extend this class.
 *
 * 日志组件基础异常类，所有日志组件的异常都应继承此类。
 */
public class LogComponentException extends RuntimeException {

    /**
     * Constructor.
     *
     * 构造函数。
     *
     * @param message exception message / 异常消息
     */
    public LogComponentException(String message) {
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
    public LogComponentException(String message, Throwable cause) {
        super(message, cause);
    }
}
