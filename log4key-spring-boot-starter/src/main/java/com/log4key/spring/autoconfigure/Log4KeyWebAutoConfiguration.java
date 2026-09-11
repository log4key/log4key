/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.autoconfigure;

import com.log4key.spring.query.LogQueryService;
import com.log4key.spring.web.LogsQueryServlet;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Log4Key servlet endpoint auto-configuration.
 *
 * Log4Key 查询 Servlet 端点自动配置（仅 servlet Web 应用生效，不引入任何 Spring MVC 组件）。
 *
 * <p>将 {@link LogsQueryServlet} 注册到内嵌 servlet 容器，映射 {@code /log4key} 与 {@code /log4key/*}。
 * 注册依赖 {@link Log4KeyAutoConfiguration} 提供的 {@link LogQueryService} Bean。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "javax.servlet.http.HttpServlet")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureAfter(Log4KeyAutoConfiguration.class)
public class Log4KeyWebAutoConfiguration {

    /**
     * 注册日志查询 Servlet。
     *
     * @param logQueryService 查询服务
     * @return servlet 注册 Bean
     */
    @Bean
    @ConditionalOnMissingBean(name = "logsQueryServletRegistration")
    public ServletRegistrationBean<LogsQueryServlet> logsQueryServletRegistration(
            LogQueryService logQueryService) {
        ServletRegistrationBean<LogsQueryServlet> registration =
                new ServletRegistrationBean<>(new LogsQueryServlet(logQueryService),
                        "/log4key/*", "/log4key");
        registration.setName("logsQueryServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }
}
