/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * Log4Key SLF4J ServiceProvider implementation.
 *
 * Log4Key的SLF4J ServiceProvider实现类。
 */
public class Log4KeyServiceProvider implements SLF4JServiceProvider {

    private static final String REQUESTED_API_VERSION = "2.0.9";

    /**
     * 日志工厂
     */
    private Log4KeyLoggerFactory loggerFactory;

    /**
     * Marker工厂
     */
    private Log4KeyMarkerFactory markerFactory;

    /**
     * MDC适配器
     */
    private Log4KeyMDCAdapter mdcAdapter;

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    @Override
    public void initialize() {
        loggerFactory = new Log4KeyLoggerFactory();
        markerFactory = new Log4KeyMarkerFactory(); // 使用Log4Key自己的MarkerFactory实现
        mdcAdapter = new Log4KeyMDCAdapter(); // 使用Log4Key自己的MDCAdapter实现
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }
}
