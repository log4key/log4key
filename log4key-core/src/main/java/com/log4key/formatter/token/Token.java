/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Log template token interface.
 *
 * 日志模板Token接口。
 */
public interface Token {
    /**
     * 将Token渲染到StringBuilder中
     * 
     * @param event   日志事件对象
     * @param out     用于输出渲染结果的StringBuilder
     */
    void render(LogEvent event, StringBuilder out);
}
