/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.storage;

import com.log4key.api.LogEvent;
import com.log4key.api.storage.LogQuery;
import com.log4key.api.storage.StorageProvider;
import com.log4key.api.spi.ExtensionManager;
import com.log4key.internal.InternalLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Storage manager for managing storage providers.
 *
 * 存储策略管理类。
 */
public class StorageManager {
    private static final InternalLogger logger = InternalLogger.getLogger(StorageManager.class);

    private static final StorageManager INSTANCE = new StorageManager();

    private Map<String, StorageProvider> storageProviders;
    private List<StorageProvider> activeProviders;
    private boolean initialized = false;

    private StorageManager() {
        // 私有构造函数，单例模式
    }

    /**
     * Gets the singleton instance of StorageManager.
     *
     * 获取存储管理器单例实例。
     *
     * @return the StorageManager instance / 存储管理器实例
     */
    public static StorageManager getInstance() {
        return INSTANCE;
    }

    /**
     * Initializes the storage manager.
     *
     * 初始化存储管理器。
     *
     * @param config the configuration parameters / 配置参数
     */
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            logger.warn("StorageManager has already been initialized");
            return;
        }

        try {
            // 加载所有StorageProvider实现
            loadStorageProviders();

            // 初始化并激活存储提供者
            initializeActiveProviders(config);

            initialized = true;
            logger.info("StorageManager initialized successfully");
        } catch (Exception e) {
            logger.warn("Failed to initialize StorageManager", e);
            // 确保在初始化失败时关闭已初始化的存储提供者
            if (activeProviders != null) {
                for (StorageProvider provider : activeProviders) {
                    try {
                        provider.close();
                    } catch (Exception ex) {
                        logger.warn("Failed to close StorageProvider during initialization failure: {}", provider.getName(), ex);
                    }
                }
            }
            throw new RuntimeException("Failed to initialize StorageManager", e);
        }
    }

    /**
     * 存储单条日志事件
     * @param event 日志事件
     */
    public void store(LogEvent event) {
        if (!initialized) {
            throw new IllegalStateException("StorageManager has not been initialized");
        }

        if (event == null) {
            logger.warn("Attempt to store null log event");
            return;
        }

        // 遍历所有激活的存储提供者，存储日志事件
        for (StorageProvider provider : activeProviders) {
            try {
                provider.store(event);
            } catch (Exception e) {
                logger.warn("Failed to store log event with provider: {}", provider.getName(), e);
                // 继续尝试其他存储提供者
            }
        }
    }

    /**
     * Batch stores log events.
     *
     * 批量存储日志事件。
     *
     * @param events the list of log events / 日志事件列表
     */
    public void storeBatch(List<LogEvent> events) {
        if (!initialized) {
            throw new IllegalStateException("StorageManager has not been initialized");
        }

        if (events == null || events.isEmpty()) {
            return;
        }

        // 遍历所有激活的存储提供者，批量存储日志事件
        for (StorageProvider provider : activeProviders) {
            try {
                provider.storeBatch(events);
            } catch (Exception e) {
                logger.warn("Failed to store batch log events with provider: {}", provider.getName(), e);
                // 继续尝试其他存储提供者
            }
        }
    }

    /**
     * 查询日志事件
     * @param query 查询条件
     * @return 日志事件列表
     */
    public List<LogEvent> query(LogQuery query) {
        if (!initialized) {
            throw new IllegalStateException("StorageManager has not been initialized");
        }

        // 遍历所有激活的存储提供者，查询日志事件
        // 使用LinkedHashSet去重，保持插入顺序
        LinkedHashSet<LogEvent> uniqueResults = new LinkedHashSet<>();

        for (StorageProvider provider : activeProviders) {
            try {
                List<LogEvent> results = provider.query(query);
                if (results != null && !results.isEmpty()) {
                    uniqueResults.addAll(results);
                }
            } catch (Exception e) {
                logger.warn("Failed to query log events with provider: {}", provider.getName(), e);
                // 继续尝试其他存储提供者
            }
        }

        // 转换为列表并返回
        return new ArrayList<>(uniqueResults);
    }

    /**
     * Switches the storage provider.
     *
     * 切换存储策略。
     *
     * @param providerName the storage provider name / 存储提供者名称
     * @param config the configuration parameters / 配置参数
     * @return true if switch succeeded / 是否切换成功
     */
    public boolean switchStorageProvider(String providerName, Map<String, Object> config) {
        if (!initialized) {
            throw new IllegalStateException("StorageManager has not been initialized");
        }

        StorageProvider provider = storageProviders.get(providerName);
        if (provider == null) {
            logger.warn("StorageProvider not found: {}", providerName);
            return false;
        }

        try {
            // 1. 初始化新的存储提供者（预初始化）
            provider.initialize(config);

            // 2. 创建新的激活提供者列表
            List<StorageProvider> newActiveProviders = new CopyOnWriteArrayList<>();
            newActiveProviders.add(provider);

            // 3. 保存旧的激活提供者列表
            List<StorageProvider> oldActiveProviders = activeProviders;

            // 4. 原子切换激活提供者列表（平滑切换，避免日志丢失）
            activeProviders = newActiveProviders;

            // 5. 异步关闭旧的存储提供者
            Thread closeThread = new Thread(() -> {
                for (StorageProvider activeProvider : oldActiveProviders) {
                    try {
                        activeProvider.close();
                        logger.info("Closed old StorageProvider: {}", activeProvider.getName());
                    } catch (Exception e) {
                        logger.warn("Failed to close old StorageProvider: {}", activeProvider.getName(), e);
                    }
                }
            });
            closeThread.setName("StorageProvider-Close-Thread");
            closeThread.setDaemon(true);
            closeThread.start();

            logger.info("Switched to StorageProvider: {}", providerName);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to switch to StorageProvider: {}", providerName, e);
            // 切换失败，保持原有存储提供者不变
            return false;
        }
    }

    /**
     * Gets all loaded storage provider names.
     /**
     * 获取所有加载的存储提供者名称。
     *
     * @return the list of storage provider names / 存储提供者名称列表
     * @deprecated This method will be removed in future versions.
     */
    @Deprecated
    public List<String> getLoadedProviders() {
        return storageProviders.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 获取当前激活的存储提供者名称
     * @return 激活的存储提供者名称列表
     */
    public List<String> getActiveProviders() {
        if (!initialized) {
            return Collections.emptyList();
        }

        return activeProviders.stream()
                .map(StorageProvider::getName)
                .collect(Collectors.toList());
    }

    /**
     * 关闭存储管理器，释放资源
     */
    public void close() {
        if (initialized) {
            try {
                // 关闭所有激活的存储提供者
                for (StorageProvider provider : activeProviders) {
                    try {
                        provider.close();
                    } catch (Exception e) {
                        logger.warn("Failed to close StorageProvider: {}", provider.getName(), e);
                    }
                }

                initialized = false;
                logger.info("StorageManager closed successfully");
            } catch (Exception e) {
                logger.warn("Failed to close StorageManager", e);
            }
        }
    }

    /**
     * Checks if the storage manager is initialized.
     *
     * 检查存储管理器是否已初始化。
     *
     * @return true if initialized / 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 加载所有StorageProvider实现
     */
    private void loadStorageProviders() {
        storageProviders = new ConcurrentHashMap<>();

        // 使用ExtensionManager发现所有StorageProvider实现类，然后实例化
        List<Class<? extends StorageProvider>> providerClasses = ExtensionManager.discover(StorageProvider.class);

        for (Class<? extends StorageProvider> providerClass : providerClasses) {
            // 实例化SPI类
            StorageProvider provider = ExtensionManager.instantiate(providerClass);

            if (provider != null) {
                String name = provider.getName();
                if (storageProviders.containsKey(name)) {
                    logger.warn("Duplicate StorageProvider found: {}, skipping", name);
                    continue;
                }
                storageProviders.put(name, provider);
                logger.info("Loaded StorageProvider: {}", name);
            }
        }

        if (storageProviders.isEmpty()) {
            logger.warn("No StorageProvider implementations found");
        }
    }

    /**
     * 初始化并激活存储提供者
     * @param config 配置参数
     */
    private void initializeActiveProviders(Map<String, Object> config) {
        activeProviders = new CopyOnWriteArrayList<>();

        // 从配置中获取激活的存储提供者名称，默认为localFile
        String activeProviderNames = config.containsKey("activeProviders") ?
                config.get("activeProviders").toString() : "localFile";

        for (String providerName : activeProviderNames.split(",")) {
            providerName = providerName.trim();
            StorageProvider provider = storageProviders.get(providerName);

            if (provider != null) {
                try {
                    provider.initialize(config);
                    activeProviders.add(provider);
                    logger.info("Activated StorageProvider: {}", providerName);
                } catch (Exception e) {
                    logger.warn("Failed to initialize StorageProvider: {}", providerName, e);
                }
            } else {
                logger.warn("StorageProvider not found: {}, skipping", providerName);
            }
        }

        // 如果没有激活的存储提供者，使用默认的本地文件存储
        if (activeProviders.isEmpty()) {
            logger.warn("No active StorageProviders, using default LocalFileStorageProvider");
            StorageProvider defaultProvider = new LocalFileStorageProvider();
            defaultProvider.initialize(config);
            activeProviders.add(defaultProvider);
            storageProviders.put(defaultProvider.getName(), defaultProvider);
        }

        // 按优先级排序，优先级值越小优先级越高
        activeProviders.sort(Comparator.comparingInt(StorageProvider::getPriority));
    }
}