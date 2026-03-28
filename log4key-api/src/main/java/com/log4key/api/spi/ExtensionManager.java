/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPI extension manager that discovers and provides instantiation capabilities for SPI extension classes.
 *
 * SPI扩展管理器，负责发现SPI扩展类并提供实例化能力。
 * 原则：只发现，不自动实例化。
 */
public final class ExtensionManager {

    /**
     * Discovers all SPI extension implementation classes for the specified type without creating instances.
     *
     * 发现指定类型的所有SPI扩展实现类（不创建实例）。
     *
     * @param serviceType service type / 服务类型
     * @param <T> service type generic / 服务类型泛型
     * @return list of implementation classes / 实现类列表
     */
    @SuppressWarnings("unchecked")
    public static <T> List<Class<? extends T>> discover(Class<T> serviceType) {
        String serviceName = serviceType.getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = serviceType.getClassLoader();
        }

        // 尝试从缓存获取
        List<Class<?>> classes = EXTENSION_REGISTRY.get(serviceType);
        if (classes != null) {
            // 安全转换：双重检查确保类型安全
            return (List<Class<? extends T>>) (List<?>) classes;
        }

        // JAVA8手动文件加载SPI服务，避免自动实例化
        classes = new ArrayList<>();
        try {
            Enumeration<URL> configs = classLoader.getResources("META-INF/services/" + serviceName);
            while (configs.hasMoreElements()) {
                URL url = configs.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            try {
                                Class<?> clazz = Class.forName(line, false, classLoader);
                                if (serviceType.isAssignableFrom(clazz)) {
                                    @SuppressWarnings("unchecked")
                                    Class<? extends T> implClass = (Class<? extends T>) clazz;
                                    classes.add(implClass);
                                }
                            } catch (ClassNotFoundException e) {
                                // 忽略或记录
                                System.err.println("Failed to load SPI class: " + line + ", error: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load service providers for " + serviceName, e);
        }

        return (List<Class<? extends T>>) (List<?>) Collections.unmodifiableList(classes);
    }

    /**
     * Instantiates the specified SPI extension class.
     *
     * 实例化指定的SPI扩展类。
     *
     * @param implClass SPI implementation class / SPI实现类
     * @param <T> service type generic / 服务类型泛型
     * @return instantiated object, null if failed / 实例化后的对象，如果失败则返回null
     */
    public static <T> T instantiate(Class<? extends T> implClass) {
        if (implClass == null) {
            throw new IllegalArgumentException("Implementation class cannot be null");
        }
        try {
            // 创建实例
            return implClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Failed to instantiate extension class " + implClass.getName() + ": " + e.getMessage());
            return null;
        }
    }

    // 扩展点注册表，使用ConcurrentHashMap确保线程安全
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final ConcurrentMap<Class<?>, List<Class<?>>> EXTENSION_REGISTRY = new ConcurrentHashMap<>();


    private ExtensionManager() {
        // 私有构造函数，防止实例化
    }

}
