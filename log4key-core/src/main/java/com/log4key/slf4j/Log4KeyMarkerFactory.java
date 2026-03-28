/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.slf4j;

import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMarkerFactory;

/**
 * Marker factory implementation.
 *
 * Marker工厂实现类。
 */
public class Log4KeyMarkerFactory implements IMarkerFactory {

    // 内部使用SLF4J的BasicMarkerFactory实现
    private final BasicMarkerFactory basicMarkerFactory;

    /**
     * 构造函数
     */
    public Log4KeyMarkerFactory() {
        this.basicMarkerFactory = new BasicMarkerFactory();
    }

    @Override
    public Marker getMarker(String name) {
        return basicMarkerFactory.getMarker(name);
    }

    @Override
    public boolean exists(String name) {
        return basicMarkerFactory.exists(name);
    }

    @Override
    public boolean detachMarker(String name) {
        return basicMarkerFactory.detachMarker(name);
    }

    @Override
    public Marker getDetachedMarker(String name) {
        return basicMarkerFactory.getDetachedMarker(name);
    }
}
