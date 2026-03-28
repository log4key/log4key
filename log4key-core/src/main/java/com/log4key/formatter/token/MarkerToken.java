/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Marker token.
 *
 * Marker Token。
 */
public class MarkerToken implements Token {

    public MarkerToken() {
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        String markerName = event.getMarkerName();
        if (markerName != null) {
            out.append(markerName);
        }
    }
}
