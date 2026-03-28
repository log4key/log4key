/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Node ID token.
 *
 * 节点ID Token。
 */
public class NodeIdToken implements Token {
    @Override
    public void render(LogEvent event, StringBuilder out) {
        out.append(event.getNodeId() != null ? event.getNodeId() : "unknown");
    }
}
