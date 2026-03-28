/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key;

import com.log4key.api.LogEvent;
import com.log4key.api.appender.AppenderProvider;
import com.log4key.api.appender.AppenderType;
import com.log4key.api.spi.ExtensionManager;
import com.log4key.appender.FileAppender;
import com.log4key.config.Log4KeyConfiguration;
import com.log4key.config.model.AppenderConfig;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.formatter.LogFormatterManager;
import com.log4key.appender.BuiltinAppenderType;
import com.log4key.internal.InternalLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.log4key.metrics.LogMetrics;
import com.log4key.util.LogExecutor;
import com.log4key.util.LogExecutorFactory;
import com.log4key.util.ExecutorController;

/**
 * LogManager is the central manager of Log4Key logging system.
 *
 * LogManager是Log4Key日志系统的中央管理器。
 */
public class LogManager {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(LogManager.class);

    /**
     * 单例实例
     */
    private static volatile LogManager INSTANCE;

    /**
     * 系统启动时间，用于计算相对时间
     */
    public static final long startTime = System.currentTimeMillis();

    /**
     * 配置管理器
     */
    private final Log4KeyConfiguration config;

    /**
     * Appender列表
     */
    private final List<AppenderProvider> appenders = new ArrayList<>();

    /**
     * Appender名称到实例的映射，用于快速查找特定名称的Appender
     */
    private final Map<String, AppenderProvider> appenderByNameMap = new HashMap<>();

    /**
     * Logger名称到Appender实例列表的映射缓存
     */
    private final Map<String, List<AppenderProvider>> loggerAppenderCache = new ConcurrentHashMap<>();

    /**
     * 类名到Logger名称的映射缓存
     */
    private final Map<String, String> classLoggerCache = new ConcurrentHashMap<>();

    /**
     * Appender名称到配置对象的映射缓存
     */
    private final Map<String, AppenderConfig> appenderConfigCache = new ConcurrentHashMap<>();

    /**
     * 执行器控制器，负责管理主执行器和降级执行器
     */
    private ExecutorController executorController;

    /**
     * 日志清理器执行器
     */
    private ScheduledExecutorService CLEANER_EXECUTOR;

    /**
     * 关闭钩子线程
     */
    private Thread shutdownHook;

    /**
     * 关闭标志，用于标记LogManager是否正在关闭或已关闭
     */
    private volatile boolean isShuttingDown = false;

    /**
     * 关闭标志的锁，用于保护并发访问
     */
    private final Object shutdownLock = new Object();

    /**
     * 初始化标志，确保初始化工作只执行一次
     */
    private volatile boolean initialized = false;

    /**
     * 构造函数（轻量初始化）
     */
    private LogManager() {
        // 初始化配置管理器
        this.config = Log4KeyConfiguration.getInstance();
    }

    /**
     * 获取单例实例。
     *
     * 获取单例实例。
     *
     * @return the LogManager singleton instance / LogManager单例实例
     */
    public static LogManager getInstance() {
        return INSTANCE;
    }

    /**
     * Ensures the logging system is initialized.
     *
     * 确保日志系统已初始化。
     *
     * @param resolver configuration resolver / 配置解析器
     */
    public static void ensureInitialized(ConfigResolver resolver) {
        if (INSTANCE == null || !INSTANCE.initialized) {
            synchronized (LogManager.class) {
                if (INSTANCE == null || !INSTANCE.initialized) {
                    if (INSTANCE == null) {
                        INSTANCE = new LogManager();
                    }
                    INSTANCE.doInitializeComponents(resolver);
                }
            }
        }
    }

    /**
     * Resets the LogManager instance immediately (for testing purposes only).
     *
     * 立即重置LogManager实例（仅供测试使用）。
     */
    public static synchronized void reset() {
        if (INSTANCE != null) {
            INSTANCE.shutdownNow();
        }
        INSTANCE = new LogManager();
    }

    /**
     * 获取配置管理器。
     *
     * 获取配置管理器。
     *
     * @return the configuration manager instance / 配置管理器实例
     */
    public Log4KeyConfiguration getConfig() {
        return config;
    }

    /**
     * Gets all registered appenders.
     *
     * 获取所有注册的Appender。
     *
     * @return list of all registered appenders / 所有注册的Appender列表
     */
    public List<AppenderProvider> getAppenders() {
        return new ArrayList<>(appenders);
    }

    /**
     * Shuts down the LogManager synchronously and releases resources.
     *
     * 线程安全下同步关闭LogManager，释放资源。
     *
     * @see #shutdownNow()
     */
    public void shutdown() {
        shutdown(true);
    }

    /**
     * Shuts down the LogManager immediately without waiting for tasks to complete.
     *
     * 线程安全下立即关闭LogManager，不等待任务完成。
     *
     * @see #shutdown()
     */
    public void shutdownNow() {
        shutdown(false);
    }

    /**
     * Processes a log event.
     *
     * 处理日志事件。
     *
     * @param event the log event to process / 要处理的日志事件
     */
    public void processLogEvent(LogEvent event) {
        // 检查是否正在关闭
        if (event == null || isShuttingDown) {
            return;
        }

        // 记录日志事件数
        LogMetrics.recordEvent();

        // 获取日志主键
        String key = event.getLogKey() != null ? event.getLogKey().toString() : event.getLoggerName();

        // 使用执行器控制器执行任务
        if (executorController != null) {
            try {
                executorController.execute(key, () -> {
                    try {
                        dispatchEvent(event);
                    } catch (Exception e) {
                        // 异步任务中发生异常不会影响主线程
                    }
                });
                return;
            } catch (Exception e) {
                // 执行器控制器发生异常，直接同步处理
                logger.warn("Executor controller error, falling back to synchronous execution: {}", e.getMessage());
            }
        }

        // 执行器控制器不可用，直接同步处理
        try {
            dispatchEvent(event);
        } catch (Exception e) {
            // 同步处理也发生异常，记录错误
            logger.warn("Synchronous execution error, discarding log event: {}", e.getMessage());
        }
    }

    /**
     * 执行完整初始化步骤
     */
    private void doInitializeComponents(ConfigResolver resolver) {
        if (initialized) {
            // 当用户试图在已初始化后设置代码配置时，给出明确提示
            if (resolver != null) {
                logger.warn("LogManager already initialized (likely via getLogger). " +
                        "Code configuration will be ignored. " +
                        "Call LogManager.initialize() BEFORE first getLogger().");
            }
            return;
        }

        if (resolver == null) {
            config.loadConfigFile();
        } else {
            config.setCodeConfig(resolver);
        }

        // 初始化格式化器
        initFormatters();

        // 初始化Appender
        initAppenders();

        // 初始化异步执行器和二级降级执行器管理器（在Appender初始化后）
        initExecutors();

        // 初始化文件关闭执行器
        if (CLEANER_EXECUTOR == null) {
            CLEANER_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "log4key-file-cleaner");
                t.setDaemon(true);
                return t;
            });
        }
        CLEANER_EXECUTOR.scheduleWithFixedDelay(this::cleanIdleWriters, 5, 5, TimeUnit.MINUTES);

        // 注册JVM关闭钩子
        registerShutdownHook();

        // 标记初始化完成
        initialized = true;
        logger.debug("LogManager components initialized successfully");
    }

    /**
     * 初始化执行器控制器
     */
    private void initExecutors() {
        // 初始化执行器控制器
        try {
            LogExecutor mainExecutor = LogExecutorFactory.createExecutorFromConfig(config);
            this.executorController = new ExecutorController(mainExecutor);
        } catch (Exception e) {
            // 如果执行器初始化失败，记录错误
            logger.error("Failed to initialize executor controller", e);
        }
    }

    /**
     * 初始化格式化器
     */
    private void initFormatters() {
        LogFormatterManager.getInstance().configureFormatters(config);
    }

    /**
     * 检查AppenderConfig是否为控制台Appender
     * @param config Appender配置对象
     * @return 是否为控制台Appender
     */
    private boolean isConsoleAppender(AppenderConfig config) {
        return config != null && BuiltinAppenderType.CONSOLE.getId().equals(config.getType());
    }

    /**
     * 检查AppenderProvider是否为控制台Appender
     * @param appender Appender提供者
     * @return 是否为控制台Appender
     */
    private boolean isConsoleAppender(AppenderProvider appender) {
        return appender != null && appender.getType() != null &&
               BuiltinAppenderType.CONSOLE.getId().equals(appender.getType().getId());
    }

    /**
     * 根据appender类型创建appender实例
     * @param appenderType appender类型
     * @return appender实例
     */
    private AppenderProvider createAppenderByType(String appenderType) {
        if (appenderType == null) {
            throw new IllegalArgumentException("Appender type cannot be null");
        }

        // 从SPI发现Appender实现类，然后实例化查找匹配类型的实例
        List<Class<? extends AppenderProvider>> appenderClasses = ExtensionManager.discover(AppenderProvider.class);
        for (Class<? extends AppenderProvider> appenderClass : appenderClasses) {
            // 创建一个临时实例来获取类型信息
            try {
                AppenderProvider tempAppender = appenderClass.getDeclaredConstructor().newInstance();
                AppenderType type = tempAppender.getType();
                if (type != null && appenderType.toLowerCase().equals(type.getId())) {
                    // 创建新的Appender实例
                    return ExtensionManager.instantiate(appenderClass);
                }
            } catch (Exception e) {
                // 忽略实例化失败的类
            }
        }

        throw new IllegalArgumentException("Unknown appender type: " + appenderType);
    }

    /**
     * 初始化所有配置的Appender
     */
    @SuppressWarnings("ConstantConditions")
    private void initAppenders() {
        try {
            logger.debug("[LogManager-DEBUG] Starting initAppenders()");

            // 缓存所有需要初始化的Appender配置对象
            // key: appender名称, value: 配置对象（null表示没有显式配置的console appender）
            boolean needConsoleAppender = false;
            boolean hasConsoleAppenderInRoot = false;

            // 1. 添加rootLogger配置的appenders（使用新的配置对象API）
            List<AppenderConfig> rootAppenderConfigs = config.getRootLoggerAppenderConfigs();
            logger.debug("[LogManager-DEBUG] Root logger appender configs count: {}", rootAppenderConfigs.size());
            for (AppenderConfig appenderConfig : rootAppenderConfigs) {
                if (appenderConfig != null && appenderConfig.getName() != null) {
                    String appenderName = appenderConfig.getName();
                    logger.debug("[LogManager-DEBUG] Processing root logger appender: {}, type: {}", appenderName, appenderConfig.getType());

                    // 将配置对象缓存起来，减少后续API调用
                    appenderConfigCache.put(appenderName, appenderConfig);

                    // 检查是否为console appender
                    if (isConsoleAppender(appenderConfig)) {
                        hasConsoleAppenderInRoot = true;
                        logger.debug("[LogManager-DEBUG] Found console appender in root logger: {}", appenderName);
                    }
                    // 检查consoleEnabled标志
                    if (appenderConfig.isConsoleEnabled()) {
                        needConsoleAppender = true;
                        logger.debug("[LogManager-DEBUG] Appender has consoleEnabled flag: {}", appenderName);
                    }
                }
            }

            // 2. 添加所有logger配置的appenders（使用新的配置对象API）
            List<String> loggerNames = config.getLoggerNames();
            logger.debug("[LogManager-DEBUG] Logger names from config: {} (count: {})", loggerNames, loggerNames.size());
            for (String loggerName : loggerNames) {
                Map<String, AppenderConfig> loggerAppenderConfigs = config.getLoggerAppenderConfigs(loggerName);
                logger.debug("[LogManager-DEBUG] Processing logger: {}, appender configs: {}",
                             loggerName, (loggerAppenderConfigs != null ? loggerAppenderConfigs.size() : 0));

                if (loggerAppenderConfigs != null && !loggerAppenderConfigs.isEmpty()) {
                    for (Map.Entry<String, AppenderConfig> entry : loggerAppenderConfigs.entrySet()) {
                        String appenderName = entry.getKey();
                        AppenderConfig appenderConfig = entry.getValue();
                        logger.debug("[LogManager-DEBUG] Logger '{}' references appender: {}, config: {}",
                                         loggerName, appenderName, (appenderConfig != null ? "present" : "null"));

                        // 配置验证：非console appender必须有配置对象
                        if (appenderConfig == null) {
                            // 记录配置错误警告
                            logger.warn("WARN: Logger '{}' references non-existent appender '{}'", loggerName, appenderName);
                            continue;
                        }

                        // 避免重复添加
                        if (!appenderConfigCache.containsKey(appenderName)) {
                            appenderConfigCache.put(appenderName, appenderConfig);
                            logger.debug("[LogManager-DEBUG] Added appender to cache: {}, type: {}", appenderName, appenderConfig.getType());

                            // 检查consoleEnabled标志
                            if (appenderConfig.isConsoleEnabled()) {
                                needConsoleAppender = true;
                                logger.debug("[LogManager-DEBUG] Logger appender has consoleEnabled: {}", appenderName);
                            }
                        } else {
                            logger.debug("[LogManager-DEBUG] Appender already in cache: {}", appenderName);
                        }
                    }
                }
            }

            // 3. 如果配置中没有 Console Appender 但 consoleEnabled 为 true
            if (!hasConsoleAppenderInRoot && needConsoleAppender) {
                // 不创建任何硬代码实例，仅记录警告
                logger.debug("[LogManager-DEBUG] Console appender not found in root logger, but consoleEnabled flag is true.");
            }

            // 4. 初始化每个appender
            logger.debug("[LogManager-DEBUG] Starting appender initialization loop, cache size: {}", appenderConfigCache.size());
            logger.debug("[LogManager-DEBUG] Appender cache keys: {}", appenderConfigCache.keySet());
            for (Map.Entry<String, AppenderConfig> entry : appenderConfigCache.entrySet()) {
                String appenderKey = entry.getKey();
                logger.debug("[LogManager-DEBUG] Processing appender: {}", appenderKey);
                try {
                    // 从缓存获取或加载appender配置对象
                    AppenderConfig appenderConfig = entry.getValue();
                    logger.debug("[LogManager-DEBUG] Appender config from cache: {}", (appenderConfig != null ? "present" : "null"));

                    // 创建appender实例
                    logger.debug("[LogManager-DEBUG] Creating appender instance, type: {}", appenderConfig.getType());
                    AppenderProvider appender = createAppenderByType(appenderConfig.getType());
                    logger.debug("[LogManager-DEBUG] Appender created: {}", appender.getClass().getName());

                    // 获取合并后的配置：appender配置 > 基础配置
                    logger.debug("[LogManager-DEBUG] Getting merged config for appender: {}", appenderKey);
                    Map<String, Object> mergedConfig = config.mergeAppenderConfig(appenderKey);
                    logger.debug("[LogManager-DEBUG] Merged config keys: {}", (mergedConfig != null ? mergedConfig.keySet() : "null"));

                    // 初始化appender
                    logger.debug("[LogManager-DEBUG] Initializing appender with merged config");
                    appender.initialize(mergedConfig);
                    logger.debug("[LogManager-DEBUG] Appender initialized, name: {}", appender.getName());

                    // 启动appender
                    logger.debug("[LogManager-DEBUG] Starting appender");
                    appender.start();

                    // 注册appender
                    logger.debug("[LogManager-DEBUG] Registering appender: {}", appenderKey);
                    registerAppender(appenderKey, appender);

                    // 绑定appender名称与config的关系
                    appenderConfigCache.put(appenderKey, appenderConfig);
                    logger.debug("[LogManager-DEBUG] Bound appender name to config: {}", appenderKey);

                    logger.debug("[LogManager-DEBUG] Appender registered successfully: {}", appenderKey);

                } catch (Exception e) {
                        // 记录初始化失败的appender
                        logger.error("Error initializing appender {}", appenderKey, e);
                }
            }

            // Appenders数量异常输出
            if (appenders.isEmpty()) {
                throw new RuntimeException("No appenders configured, adding default ConsoleAppender");
            }
        } catch (Exception e) {
            // 记录初始化失败的原因
            logger.error("Error initializing default appenders", e);
        }

        // 初始化loggerAppenderCache
        initializeLoggerAppenderCache();
    }

    /**
     * 初始化loggerAppenderCache
     */
    private void initializeLoggerAppenderCache() {
        try {
            logger.debug("[LogManager-DEBUG] Initializing loggerAppenderCache");

            // 清空现有缓存
            loggerAppenderCache.clear();

            // 获取所有配置的logger名称
            List<String> loggerNames = config.getLoggerNames();
            if (loggerNames != null && !loggerNames.isEmpty()) {
                for (String loggerName : loggerNames) {
                    // 获取该logger绑定的appenders
                    Map<String, AppenderConfig> appenderConfigs = config.getLoggerAppenderConfigs(loggerName);
                    List<AppenderProvider> appenderInstances = new ArrayList<>();
                    if (appenderConfigs != null && !appenderConfigs.isEmpty()) {
                        for (String appenderName : appenderConfigs.keySet()) {
                            AppenderProvider appender = appenderByNameMap.get(appenderName);
                            if (appender != null) {
                                appenderInstances.add(appender);
                            }
                        }
                    }
                    // 缓存结果
                    loggerAppenderCache.put(loggerName, appenderInstances);
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing loggerAppenderCache", e);
        }
    }

    /**
     * 注册Appender
     * @param configName Appender的配置名称
     * @param appender 要注册的Appender
     */
    private void registerAppender(String configName, AppenderProvider appender) {
        // 检查是否正在关闭
        if (isShuttingDown) {
            return;
        }

        // 双重检查，防止在获取锁期间状态发生变化
        if (!isShuttingDown && appender != null && !appenders.contains(appender)) {
            appenders.add(appender);
            // 将appender配置名称和实例添加到映射中
            appenderByNameMap.put(configName, appender);
        }
    }

    /**
     * 文件关闭执行器，用于关闭文件写入器中空闲的日志
     */
    private void cleanIdleWriters() {
        long currentTimeMillis = System.currentTimeMillis();
        for (AppenderProvider appender : appenders) {
            if (appender instanceof FileAppender) {
                ((FileAppender) appender).cleanIdleWriters(currentTimeMillis);
            }
        }
    }

    /**
     * 注册JVM关闭钩子
     */
    private void registerShutdownHook() {
        shutdownHook = new Thread(this::shutdown);
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * 线程安全下同步关闭　LogManager，释放资源。
     * 工作线程将等待所有任务完成，然后关闭所有执行器，释放所有资源。
     * @param wait 是否等待所有任务完成
     */
    private void shutdown(boolean wait) {
        // 设置关闭标志，防止新的appender注册或移除
        synchronized (shutdownLock) {
            if (isShuttingDown) {
                return;
            }
            isShuttingDown = true;
        }

        // 1. 先停止接受新的日志事件，确保所有执行器状态一致
        logger.debug("[LogManager] 开始关闭流程...");

        // 2. 关闭执行器控制器，协调主执行器和降级执行器的关闭
        if (executorController != null) {
            try {
                logger.debug("[LogManager] 关闭执行器控制器...");
                if (wait) {
                    executorController.shutdown();
                } else {
                    executorController.shutdownNow();
                }
                logger.debug("[LogManager] 执行器控制器已关闭");
            } catch (Exception e) {
                logger.warn("[LogManager] 关闭异常，强制关闭");
                executorController.shutdownNow();
            } finally {
                // 清空执行器控制器引用，避免后续使用
                executorController = null;
            }
        }

        // 3. 获取写锁，确保在关闭appender时没有其他线程修改appenders列表
        // 关闭所有Appender，创建副本进行遍历，避免在遍历过程中修改原列表导致的问题
        List<AppenderProvider> appendersCopy = new ArrayList<>(appenders);
        for (AppenderProvider appender : appendersCopy) {
            try {
                appender.close();
            } catch (Exception e) {
                // 忽略关闭异常，继续关闭其他appender
                logger.error("Error closing appender", e);
            }
        }

        // 清空 Appender 列表
        appenders.clear();

        // 关闭清理执行器
        try {
            CLEANER_EXECUTOR.shutdown();
            logger.debug("[LogManager] 关闭清理执行器...");
        } catch (Exception e) {
            CLEANER_EXECUTOR.shutdownNow();
            logger.warn("[LogManager] 关闭清理执行器时发生异常，强制关闭");
        } finally {
            CLEANER_EXECUTOR = null;
        }

        // 4. 关闭 JVM 钩子（如果存在）
        try {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
        } catch (IllegalStateException e) {
            // JVM 正在关闭，忽略异常
        }

        logger.debug("[LogManager] 关闭流程完成");
    }

    /**
     * 分发事件到logger配置的Appender
     * @param event 日志事件
     */
    private void dispatchEvent(LogEvent event) {
        // 1. 查找匹配的logger
        List<AppenderProvider> appenders = null;
        String matchingLogger = findMatchingLogger(event.getLoggerName());
        if (matchingLogger != null) {
            // 2. 从缓存获取该logger绑定的appenders
            appenders = loggerAppenderCache.get(matchingLogger);
        }

        // 3. 如果没有匹配的logger或匹配的logger没有绑定appenders，使用rootLogger
        if (matchingLogger == null || appenders == null || appenders.isEmpty()) {
            appenders = getRootLoggerAppenders();
        }

        // 4. 如果仍然没有appenders，直接返回
        if (appenders.isEmpty()) {
            return;
        }

        // 5. 处理所有appenders
        boolean hasConsoleOutput = false;
        boolean needConsoleOutput = false;

        // 单次遍历处理所有appender
        for (AppenderProvider appender : appenders) {
            // 判断是否是Console Appender
            boolean isConsole = isConsoleAppender(appender);

            // 处理appender
            // 直接从缓存获取appender对应的配置对象
            AppenderConfig configObj = appenderConfigCache.get(appender.getName());

            if (configObj != null) {
                // 获取是否需要输出到Console
                needConsoleOutput = configObj.isConsoleEnabled();
            }

            // 输出到当前appender（输出级别控制已移至appender内部）
            appender.append(event);

            // 当前是 Console Appender 则不再需要处理 isConsoleEnabled 配置
            if (isConsole) {
                hasConsoleOutput = true;
            }
        }

        // 如果需要控制台输出但还没有输出，尝试使用root中的console appender
        if (needConsoleOutput && !hasConsoleOutput) {
            AppenderProvider rootConsoleAppender = getRootConsoleAppender();
            if (rootConsoleAppender != null) {
                // 直接输出到root console appender
                rootConsoleAppender.append(event);
            }
        }

    }

    /**
     * 获取rootLogger绑定的appender实例列表
     * @return appender实例列表
     */
    private List<AppenderProvider> getRootLoggerAppenders() {
        List<AppenderProvider> appenders = new ArrayList<>();
        List<AppenderConfig> rootAppenderConfigs = config.getRootLoggerAppenderConfigs();
        if (rootAppenderConfigs != null) {
            for (AppenderConfig config : rootAppenderConfigs) {
                if (config != null && config.getName() != null) {
                    AppenderProvider appender = appenderByNameMap.get(config.getName());
                    if (appender != null) {
                        appenders.add(appender);
                    }
                }
            }
        }
        return appenders;
    }

    /**
     * 获取root logger中的console appender
     * @return console appender实例，如果没有则返回null
     */
    private AppenderProvider getRootConsoleAppender() {
        List<AppenderConfig> rootAppenderConfigs = config.getRootLoggerAppenderConfigs();
        if (rootAppenderConfigs != null) {
            for (AppenderConfig config : rootAppenderConfigs) {
                if (config != null && config.getName() != null && isConsoleAppender(config)) {
                    AppenderProvider appender = appenderByNameMap.get(config.getName());
                    if (appender != null) {
                        return appender;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 查找与类名匹配的logger
     * @param className 完整类名
     * @return 匹配的logger名称，如果没有匹配返回null
     */
    private String findMatchingLogger(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }

        // 1. 先尝试从缓存获取
        String cachedLogger = classLoggerCache.get(className);
        if (cachedLogger != null) {
            return cachedLogger;
        }

        // 2. 获取所有配置的logger名称
        List<String> loggerNames = config.getLoggerNames();
        if (loggerNames == null || loggerNames.isEmpty()) {
            return null;
        }

        // 3. 精确匹配优先
        if (loggerNames.contains(className)) {
            classLoggerCache.put(className, className);
            return className;
        }

        // 4. 通配匹配（从最长路径开始）
        String[] parts = className.split("\\.");
        for (int i = parts.length - 1; i >= 0; i--) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                sb.append(parts[j]).append(".");
            }
            sb.append("*");
            String pattern = sb.toString();

            if (loggerNames.contains(pattern)) {
                classLoggerCache.put(className, pattern);
                return pattern;
            }
        }

        // 5. 没有匹配的logger
        return null;
    }
}
