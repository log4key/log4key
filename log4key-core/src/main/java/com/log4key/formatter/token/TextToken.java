/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Text token.
 *
 * 文本Token。
 */
public class TextToken implements Token {
    private final String text;

    /**
     * 构造函数
     * 
     * @param text 文本内容
     */
    public TextToken(String text) {
        this.text = text;
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(text);
    }
}
