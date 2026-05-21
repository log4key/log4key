/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;

/**
 * Level path segment.
 *
 * 级别路径片段，输出日志级别的小写形式。
 */
public class LevelSegment implements Segment {

    @Override
    public void append(StringBuilder sb, LogEvent e) {
        String level = e.getLevel();
        if (level != null) {
            sb.append(level.toLowerCase());
        } else {
            sb.append("info");
        }
    }

    /**
     * Appends the log level (lowercase) to the StringBuilder,
     * with an optional override for the level.
     *
     * 将日志级别（小写）追加到 StringBuilder 中，支持覆盖日志级别。
     *
     * @param sb the StringBuilder to append to
     * @param e  the log event
     * @param overrideLevel override level; falls back to event.getLevel() if null
     */
    @Override
    public void append(StringBuilder sb, LogEvent e, String overrideLevel) {
        String level = overrideLevel != null ? overrideLevel : e.getLevel();
        if (level != null) {
            sb.append(level.toLowerCase());
        } else {
            sb.append("info");
        }
    }
}