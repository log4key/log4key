/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.spi;

/**
 * Named extension interface for supporting extension point lookup by name.
 *
 * 命名扩展接口，用于支持按名称查找扩展点。
 */
public interface NamedExtension {
    /**
     * Gets the name of the extension point.
     *
     * 获取扩展点名称。
     *
     * @return extension point name / 扩展点名称
     */
    String getName();
}
