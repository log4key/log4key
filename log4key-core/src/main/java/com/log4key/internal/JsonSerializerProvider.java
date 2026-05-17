/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.internal;

import com.log4key.LogManager;
import com.log4key.spi.JsonSerializer;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * JSON serializer provider that discovers and provides a JsonSerializer instance via SPI.
 *
 * JSON序列化器提供者，通过SPI机制发现并提供JsonSerializer实例。
 * 使用双重检查锁定确保线程安全的单例加载。
 */
public class JsonSerializerProvider {

    /** 内部日志记录器 */
    private static final InternalLogger logger = InternalLogger.getLogger(LogManager.class);

    /** JsonSerializer单例实例 */
    private static volatile JsonSerializer INSTANCE;

    /**
     * Gets the singleton JsonSerializer instance.
     *
     * 获取JsonSerializer单例实例。
     *
     * @return the JsonSerializer instance / JsonSerializer实例
     * @throws IllegalStateException if no JsonSerializer implementation found / 如果未找到JsonSerializer实现
     */
    public static JsonSerializer get() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        synchronized (JsonSerializerProvider.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }

            INSTANCE = load();
            return INSTANCE;
        }
    }

    /**
     * Loads the JsonSerializer via Java SPI mechanism.
     *
     * 通过Java SPI机制加载JsonSerializer实现。
     *
     * @return the loaded JsonSerializer instance / 加载到的JsonSerializer实例
     * @throws IllegalStateException if no implementation found in classpath / 如果在classpath中找不到实现
     */
    private static JsonSerializer load() {
        ServiceLoader<JsonSerializer> loader =
                ServiceLoader.load(JsonSerializer.class);

        Iterator<JsonSerializer> it = loader.iterator();

        if (it.hasNext()) {
            JsonSerializer serializer = it.next();

            if (it.hasNext()) {
                // 多实现警告
                logger.warn("Multiple JsonSerializer implementations found.");
            }

            return serializer;
        }

        throw new IllegalStateException(
                "No JsonSerializer found.\n" +
                        "Please add dependency: fastJson or gson or any other."
        );
    }
}