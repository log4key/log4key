/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.spi;

import java.util.Map;

/**
 * SPI extension point lifecycle interface that defines lifecycle management for extension points.
 *
 * SPI扩展点生命周期接口，定义扩展点的生命周期管理。
 */
public interface Lifecycle {
    /**
     * Initializes the extension point.
     *
     * 初始化扩展点。
     *
     * @param config configuration parameters / 配置参数
     */
    void initialize(Map<String, Object> config);

    /**
     * Starts the extension point.
     *
     * 启动扩展点。
     */
    void start();

    /**
     * Stops the extension point.
     *
     * 停止扩展点。
     */
    void stop();

    /**
     * Checks if the extension point is currently running.
     *
     * 检查扩展点是否正在运行。
     *
     * @return true if running / 是否正在运行
     */
    boolean isRunning();
}
