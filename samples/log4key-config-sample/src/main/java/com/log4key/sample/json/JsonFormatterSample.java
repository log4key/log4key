/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.sample.json;

import com.log4key.api.ILogKey;
import com.log4key.slf4j.Log4KeyLogger;
import com.log4key.slf4j.Log4KeyLoggerFactory;

/**
 * JSON formatted log example demonstrating the SPI-based JsonSerializer integration.
 *
 * JSON格式化日志示例，演示基于SPI机制的JsonSerializer集成方式。
 * 
 * 使用前需确保正确配置：
 * 1. 在classpath中提供JsonSerializer的实现类（如本示例中的GsonJsonSerializer）
 * 2. 在 META-INF/services/com.log4key.spi.JsonSerializer 文件中声明实现类的全限定名
 *    文件内容示例：com.log4key.sample.json.GsonJsonSerializer
 * 3. 确保对应的JSON库依赖已添加到项目中
 */
public class JsonFormatterSample {

    /** 日志记录器 */
    private static final Log4KeyLogger logger  = Log4KeyLoggerFactory.getLog4KeyLogger(JsonFormatterSample.class);


    /**
     * Logs a message using the default logger.
     *
     * 使用默认Logger输出日志。
     *
     * @param message the log message / 日志消息
     */
    public static void log(String message) {

        logger.info("JsonFormatterSample message: {}", message);
        logger.warn("JsonFormatterSample warn message: {}", message);

    }

    /**
     * Logs a message with a log key using the default logger.
     *
     * 使用默认Logger输出带日志Key的日志。
     *
     * @param logKey the log key for categorization / 用于分类的日志Key
     * @param message the log message / 日志消息
     */
    public static void log(ILogKey logKey, String message) {

        logger.info(logKey, "JsonFormatterSample.key message: {}", message);
        logger.warn(logKey, "JsonFormatterSample.key warn message: {}", message);

    }

}
