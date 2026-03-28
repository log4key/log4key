/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Newline token.
 *
 * 换行符Token。
 */
public class NewLineToken implements Token {
    // 换行符常量
    private static final String NEW_LINE = System.lineSeparator();
    
    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(NEW_LINE);
    }
}
