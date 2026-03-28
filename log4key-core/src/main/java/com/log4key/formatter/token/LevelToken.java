/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Level token.
 *
 * 日志级别Token。
 */
public class LevelToken implements Token {
    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(event.getLevel() != null ? event.getLevel() : "UNKNOWN");
    }
}
