/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.sample.business;

import com.log4key.api.ILogKey;
import com.log4key.slf4j.Log4KeyLogger;
import com.log4key.slf4j.Log4KeyLoggerFactory;

/**
 * This class falls under logger:
 * com.log4key.sample.business.*
 * → Only WARN logs will be written to BUSINESS_FILE
 */
public class WarnSample {

    private static final Log4KeyLogger logger  = Log4KeyLoggerFactory.getLog4KeyLogger(WarnSample.class);


    public static void log(String message) {

        logger.info("WarnSample message: {}", message);
        logger.warn("WarnSample warn message: {}", message);

    }

    public static void log(ILogKey logKey, String message) {

        logger.info(logKey, "WarnSample.key message: {}", message);
        logger.warn(logKey, "WarnSample.key warn message: {}", message);

    }

}
