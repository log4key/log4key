/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.LogManager;
import com.log4key.api.LogEvent;

/**
 * Relative time token.
 *
 * 相对时间Token。
 */
public class RelativeTimeToken implements Token {

    public RelativeTimeToken() {
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        long relativeTime = event.getTimestampMillis() - LogManager.startTime;
        out.append(relativeTime);
    }
}
