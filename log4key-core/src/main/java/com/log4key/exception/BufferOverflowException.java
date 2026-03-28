/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.exception;

/**
 * Buffer overflow exception.
 *
 * 缓冲区溢出异常。
 */
public class BufferOverflowException extends RuntimeException {
    
    /**
     * Creates a new BufferOverflowException.
     *
     * 创建缓冲区溢出异常。
     *
     * @param message the exception message / 异常消息
     */
    public BufferOverflowException(String message) {
        super(message);
    }
    
    /**
     * Creates a new BufferOverflowException with a cause.
     *
     * 创建带根本原因的缓冲区溢出异常。
     *
     * @param message the exception message / 异常消息
     * @param cause the underlying cause / 根本原因
     */
    public BufferOverflowException(String message, Throwable cause) {
        super(message, cause);
    }
}