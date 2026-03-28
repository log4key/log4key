/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.sample;

import com.log4key.LogManager;
import com.log4key.api.DefaultLogKey;
import com.log4key.api.ILogKey;
import com.log4key.slf4j.Log4KeyLogger;
import com.log4key.slf4j.Log4KeyLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickStart {

    private static final Logger slf4jLogger = LoggerFactory.getLogger(QuickStart.class);

    private static final Log4KeyLogger log4keyLogger = Log4KeyLoggerFactory.getLog4KeyLogger(QuickStart.class);

    public static void main(String[] args) {

        // Run this example and check the logs directory
        slf4jLogger.info("Application started");

        // normal log (no key)
        slf4jLogger.info("application log without key");

        // Create a log key (e.g., order ID, user ID, etc.)
        ILogKey order1001 = DefaultLogKey.of("order-1001");
        // Logs will be routed to different files based on the key

        // --- SLF4J style ---
        // Note: passing ILogKey as argument triggers IDEA warning
        // ("More arguments provided than placeholders"),
        // but it's required for Log4Key key-based routing
        slf4jLogger.info("create order", order1001);
        slf4jLogger.info("pay order", order1001);
        slf4jLogger.info("ship order：{}", "ship-001", order1001);

        // --- Log4Key standard API (recommended)  ---
        log4keyLogger.info(order1001, "ship order：{}", "ship-002");

        // order 1002
        ILogKey order1002 = DefaultLogKey.of("order-1002");
        slf4jLogger.info("create order", order1002);
        slf4jLogger.info("pay order", order1002);

        // Flush & shutdown (required for standalone apps).
        // In frameworks like Spring Boot or when using JVM shutdown hooks, this is handled automatically.
        LogManager.getInstance().shutdown();
    }

}
