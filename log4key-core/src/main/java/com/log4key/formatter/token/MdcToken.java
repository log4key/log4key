/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;
import java.util.Map;

/**
 * MDC (Mapped Diagnostic Context) token.
 *
 * MDC Token。
 */
public class MdcToken implements Token {
    private final String key;

    public MdcToken() {
        this(null);
    }

    public MdcToken(String key) {
        this.key = key;
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        Map<String, Object> mdc = event.getMdc();
        if (mdc == null || mdc.isEmpty()) {
            return;
        }

        if (key != null && !key.isEmpty()) {
            Object val = mdc.get(key);
            if (val != null) {
                out.append(val.toString());
            }
        } else {
            // 输出所有MDC内容，格式为 {key=value, key2=value2}
            out.append(mdc.toString());
        }
    }
}
