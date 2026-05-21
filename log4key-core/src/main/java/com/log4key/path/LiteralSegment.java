/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;

/**
 * Literal path segment.
 *
 * 字面量路径片段，直接输出构造时传入的固定字符串。
 */
public class LiteralSegment implements Segment {

    private final String literal;

    /**
     * 构造 LiteralSegment。
     *
     * @param literal 固定字符串内容，不能为 null
     */
    public LiteralSegment(String literal) {
        this.literal = literal;
    }

    @Override
    public void append(StringBuilder sb, LogEvent e) {
        sb.append(literal);
    }

    /**
     * Appends the literal string to the StringBuilder.
     * Ignores overrideLevel since literal segments are not affected by log level.
     *
     * 将固定字符串追加到 StringBuilder 中，忽略 overrideLevel（字面量片段不受日志级别影响）。
     *
     * @param sb the StringBuilder to append to
     * @param e  the log event
     * @param overrideLevel ignored / 忽略此参数
     */
    @Override
    public void append(StringBuilder sb, LogEvent e, String overrideLevel) {
        sb.append(literal);
    }
}