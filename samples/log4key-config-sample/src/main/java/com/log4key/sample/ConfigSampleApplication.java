/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.sample;

import com.log4key.LogManager;
import com.log4key.api.DefaultLogKey;
import com.log4key.api.ILogKey;
import com.log4key.sample.business.WarnSample;
import com.log4key.sample.json.JsonFormatterSample;
import com.log4key.slf4j.Log4KeyLogger;
import com.log4key.slf4j.Log4KeyLoggerFactory;

/**
 * Log4Key configuration sample application.
 *
 * Log4Key 配置示例应用
 */
public class ConfigSampleApplication {

    private static final Log4KeyLogger logger = Log4KeyLoggerFactory.getLog4KeyLogger(ConfigSampleApplication.class);

    public static void main(String[] args) {

        System.out.println("=== Log4Key Configuration Sample ===");
        System.out.println("Expected:");
        System.out.println("- Default logs → ./logs/default");
        System.out.println("- Business WARN logs → ./logs/business");

        // 普通日志
        logger.info("This is an default log without key");

        // key-based routing (按 orderId 路由日志)
        ILogKey orderKey = DefaultLogKey.of("order-12345");

        // 测试日志级别
        String message = "This is an order message";
        RuntimeException testException = new RuntimeException("Exception for ConfigSampleApplication.");

        // 使用主键记录日志
        // DEBUG 将不会输出，当前配置准入等级为 INFO
        logger.debug(orderKey, "Debug level message : {}", message);
        logger.info(orderKey, "Info level message : {}", message);
        logger.warn(orderKey, "Warn level message : {}", message);
        logger.error(orderKey, testException, "Error level message : {}", message);

        // 测试非 RootLogger
        WarnSample.log(message);
        WarnSample.log(orderKey, message);

        // Json 日志格式
        JsonFormatterSample.log(message);
        JsonFormatterSample.log(orderKey, message);

        System.out.println("Logs written. Check ./logs/default and ./logs/business and ./logs/json ");

        // Flush & shutdown (required for standalone apps).
        // In frameworks like Spring Boot or when using JVM shutdown hooks, this is handled automatically.
        LogManager.getInstance().shutdown();
    }
}