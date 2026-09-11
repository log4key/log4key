/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.autoconfigure;

import com.log4key.LogManager;
import com.log4key.config.Log4KeyConfiguration;
import com.log4key.spring.query.LogQueryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Log4Key Spring Boot auto-configuration.
 *
 * Log4Key Spring Boot 自动配置（核心部分，适用于任意 Spring Boot 应用类型）。
 *
 * 职责（轻量）：
 * <ul>
 *   <li>{@link Log4KeyLifecycle}：Spring 上下文启动时初始化 Log4Key（复用 core 既有配置加载机制），
 *       上下文优雅关闭时关闭 Log4Key（排空 + close）；</li>
 *   <li>{@link LogQueryService}：以 core 配置的 rootDirectory / defaultCharset 构造本地日志查询服务
 *       （Bean 工厂内先 ensureInitialized，保证读取到的是配置加载后的真实 rootDirectory）。</li>
 * </ul>
 * Servlet 查询端点的注册见 {@code Log4KeyWebAutoConfiguration}（仅 servlet Web 应用生效）。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(LogManager.class)
public class Log4KeyAutoConfiguration {

    /**
     * 注册 Log4Key 生命周期管理 Bean。
     *
     * @return Log4KeyLifecycle 实例
     */
    @Bean
    @ConditionalOnMissingBean(Log4KeyLifecycle.class)
    public Log4KeyLifecycle log4KeyLifecycle() {
        return new Log4KeyLifecycle();
    }

    /**
     * 注册本地日志查询服务 Bean。
     *
     * <p>构造前先触发 {@link LogManager#ensureInitialized}，确保读取到的是 Log4Key 配置加载后的
     * 真实 rootDirectory 与 defaultCharset（而非未加载时的默认值）。
     *
     * @return LogQueryService 实例
     */
    @Bean
    @ConditionalOnMissingBean(LogQueryService.class)
    public LogQueryService logQueryService() {
        LogManager.ensureInitialized(null);
        LogManager manager = LogManager.getInstance();
        Log4KeyConfiguration config = manager != null ? manager.getConfig() : Log4KeyConfiguration.getInstance();

        String rootDirectory = config.getDefaultDirectory();
        if (rootDirectory == null || rootDirectory.trim().isEmpty()) {
            rootDirectory = "./logs";
        }

        String charsetName = config.getDefaultCharset();
        Charset charset;
        if (charsetName != null && !charsetName.trim().isEmpty()) {
            charset = Charset.forName(charsetName.trim());
        } else {
            charset = StandardCharsets.UTF_8;
        }
        return new LogQueryService(Paths.get(rootDirectory), charset);
    }
}
