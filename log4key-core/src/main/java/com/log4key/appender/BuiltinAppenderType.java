/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.appender;

import com.log4key.api.appender.AppenderType;

/**
 * Built-in appender type enumeration.
 *
 * 内置Appender类型枚举。
 */
public enum BuiltinAppenderType implements AppenderType {

    /**
     * 控制台输出
     */
    CONSOLE {
        @Override
        public boolean supportsAsync() {
            return false;
        }

        @Override
        public boolean supportsKeyRouting() {
            return false;
        }
    },

    /**
     * 文件输出
     */
    FILE {
        @Override
        public boolean supportsKeyRouting() {
            return true;
        }
    };

    /**
     * Returns the unique identifier for this appender type.
     *
     * 返回此Appender类型的唯一标识符。
     *
     * @return the appender type identifier / Appender类型标识符
     */
    @Override
    public String getId() {
        return name().toLowerCase();
    }
}
