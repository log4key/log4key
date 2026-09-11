/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.autoconfigure;

import com.log4key.LogManager;
import org.springframework.context.SmartLifecycle;

/**
 * Binds the Log4Key lifecycle to the Spring context lifecycle.
 *
 * 将 Log4Key 生命周期与 Spring 上下文生命周期绑定。
 *
 * <ul>
 *   <li>start：上下文启动时触发 Log4Key 既有初始化机制（{@link LogManager#ensureInitialized}），
 *       由其内部的配置加载器按 Log4Key 既有规则加载默认 / 用户配置文件；调用幂等，已初始化时为空操作。</li>
 *   <li>stop：上下文优雅关闭时主动调用 {@link LogManager#shutdown()}（排空未写日志并关闭全部 Appender），
 *       保证“Spring Boot shutdown → Log4Key shutdown → flush → close”。core 注册的 JVM 钩子仅作为进程退出兜底。</li>
 * </ul>
 *
 * 说明：LogManager 为 JVM 级单例，一旦在本 JVM 内被关闭（上下文关闭），同一 JVM 内再次启动新上下文不会重新初始化；
 * 这是 core 既有生命周期语义，本类不做绕过。
 */
public class Log4KeyLifecycle implements SmartLifecycle {

    /**
     * 运行状态标记
     */
    private volatile boolean running = false;

    /**
     * 上下文启动：触发 Log4Key 既有初始化机制（幂等）。
     */
    @Override
    public void start() {
        // 触发 Log4Key 既有初始化：创建/获取 LogManager 单例并初始化组件（配置加载幂等）
        LogManager.ensureInitialized(null);
        this.running = true;
    }

    /**
     * 上下文关闭：主动调用 LogManager.shutdown（排空 + 关闭全部 Appender，幂等）。
     */
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        this.running = false;

        LogManager manager = LogManager.getInstance();
        if (manager != null) {
            // shutdown()：等待已提交任务排空（flush 语义）并关闭全部 Appender（close 语义）；幂等
            manager.shutdown();
        }
    }

    /**
     * 当前是否处于启动状态。
     *
     * @return true 表示已启动且尚未关闭 / true if started and not yet stopped
     */
    @Override
    public boolean isRunning() {
        return running;
    }
}
