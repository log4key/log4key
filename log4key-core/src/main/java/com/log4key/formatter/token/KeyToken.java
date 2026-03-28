/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Log key token.
 *
 * 日志主键Token。
 */
public class KeyToken implements Token {
    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(event.getKey() != null ? event.getKey() : "unknown");
    }
}
