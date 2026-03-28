/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Message token.
 *
 * 日志消息Token。
 */
public class MessageToken implements Token {
    @Override
    public void render(LogEvent event, StringBuilder out) {
        if (event.getMessage() != null) {
            out.append(event.getMessage());
        }
    }
}
