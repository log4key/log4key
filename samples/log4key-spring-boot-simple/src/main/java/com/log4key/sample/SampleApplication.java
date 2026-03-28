/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.sample;

import com.log4key.api.DefaultLogKey;
import com.log4key.api.ILogKey;
import com.log4key.slf4j.Log4KeyLogger;
import com.log4key.slf4j.Log4KeyLoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Log4Key Spring Boot sample application.
 *
 * Log4Key Spring Boot示例应用
 */
@SpringBootApplication
@RestController
public class SampleApplication {

    private static final Log4KeyLogger logger = Log4KeyLoggerFactory.getLog4KeyLogger(SampleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }

    /**
     * Example:
     * http://localhost:8080/log/user-1001/hello
     */
    @GetMapping("/log/{userId}/{message}")
    public String log(@PathVariable String userId, @PathVariable String message) {

        // Create a key (e.g. userId / orderId)
        ILogKey key = DefaultLogKey.of(userId);

        // Key-based logging → routed to separate files
        logger.info(key, "Received message: {}", message);

        return "OK";
    }

    @GetMapping("/health")
    public String health() {
        // normal log (no key)
        logger.info("Spring-boot log without key");

        return "OK";
    }

}