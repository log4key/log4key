/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Method name token.
 *
 * 方法名Token。
 */
public class MethodNameToken implements Token {
    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(event.getMethodName() != null ? event.getMethodName() : "unknown");
    }
}
