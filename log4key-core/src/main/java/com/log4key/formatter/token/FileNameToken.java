/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * File name token.
 *
 * 文件名Token。
 */
public class FileNameToken implements Token {
    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(event.getFileName() != null ? event.getFileName() : "unknown");
    }
}
