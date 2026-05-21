/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;

/**
 * Key path segment.
 *
 * Key路径片段，输出日志事件的主键值，fallback 到日志级别的小写形式。
 */
public class KeySegment implements Segment {

    @Override
    public void append(StringBuilder sb, LogEvent e) {
        String key = e.getKey();
        if (key != null && !key.isEmpty()) {
            sb.append(key);
        } else {
            String level = e.getLevel();
            if (level != null) {
                sb.append(level.toLowerCase());
            } else {
                sb.append("info");
            }
        }
    }

    /**
     * Appends the key value to the StringBuilder, falling back to level (lowercase) if key is empty,
     * with an optional override for the level during fallback.
     *
     * 将 key 值追加到 StringBuilder 中，key 为空时 fallback 到日志级别（小写），
     * fallback 时支持覆盖日志级别。
     *
     * @param sb the StringBuilder to append to
     * @param e  the log event
     * @param overrideLevel override level for fallback; falls back to event.getLevel() if null
     */
    @Override
    public void append(StringBuilder sb, LogEvent e, String overrideLevel) {
        String key = e.getKey();
        if (key != null && !key.isEmpty()) {
            sb.append(key);
        } else {
            String level = overrideLevel != null ? overrideLevel : e.getLevel();
            if (level != null) {
                sb.append(level.toLowerCase());
            } else {
                sb.append("info");
            }
        }
    }
}