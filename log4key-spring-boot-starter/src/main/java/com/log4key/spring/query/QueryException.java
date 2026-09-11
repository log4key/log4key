/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

/**
 * Exception thrown by the log query layer.
 *
 * 日志查询层抛出的异常，携带语义化错误码（不依赖任何 HTTP/servlet 概念，
 * 由上层（Servlet）负责把错误码映射为 HTTP 状态）。
 */
public class QueryException extends RuntimeException {

    /**
     * 语义化错误码
     */
    public enum ErrorCode {
        /** 目标路径不存在（含根目录未创建） */
        NOT_FOUND,
        /** 路径段非法（穿越、分隔符、控制字符等） */
        INVALID_PATH,
        /** 参数非法（如 page &lt;= 0） */
        INVALID_ARGUMENT,
        /** 期望目录但目标是文件 */
        NOT_DIRECTORY,
        /** 期望文件但目标是目录或非普通文件 */
        NOT_FILE,
        /** 解析后越出日志根目录边界（如符号链接逃逸） */
        OUTSIDE_ROOT,
        /** 单页内容超过字节护栏 */
        PAGE_TOO_LARGE,
        /** IO 错误 */
        IO_ERROR
    }

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 语义化错误码
     */
    private final ErrorCode code;

    /**
     * Creates a query exception with an error code and message.
     *
     * 以错误码与消息构造查询异常。
     *
     * @param code error code / 语义化错误码
     * @param message error message / 错误消息
     */
    public QueryException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Creates a query exception with an error code, message and cause.
     *
     * 以错误码、消息与根因构造查询异常。
     *
     * @param code error code / 语义化错误码
     * @param message error message / 错误消息
     * @param cause root cause / 根因
     */
    public QueryException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取语义化错误码。
     *
     * @return 错误码
     */
    public ErrorCode getCode() {
        return code;
    }
}
