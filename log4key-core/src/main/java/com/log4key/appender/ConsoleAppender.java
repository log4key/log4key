/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.config.ConfigKeys;
import com.log4key.config.model.OutputLevelPolicy;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.formatter.LogFormatterManager;
import com.log4key.internal.InternalLogger;
import com.log4key.util.ConfigUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Console output appender implementation.
 *
 * 控制台输出实现。
 */
public class ConsoleAppender extends AbstractAppenderProvider {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(ConsoleAppender.class);

    /**
     * Appender名称，从配置前缀中提取
     */
    private String appenderName = ConfigKeys.APPENDER_TYPE_CONSOLE;

    /**
     * 日志格式化器管理器
     */
    private final LogFormatterManager formatterManager = LogFormatterManager.getInstance();

    /**
     * 使用的格式化器名称
     */
    private String formatterName = "text";

    /**
     * 是否支持异步操作
     */
    private boolean asyncSupported = true;

    /**
     * 异步执行器
     */
    private ExecutorService asyncExecutor;

    /**
     * 核心线程池大小
     */
    private int corePoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程池大小
     */
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 线程存活时间（秒）
     */
    private long keepAliveTime = 60;

    /**
     * 队列容量
     */
    private int queueCapacity = 1024;

    /**
     * 是否已初始化
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 是否已关闭
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 运行状态
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 构造函数
     */
    public ConsoleAppender() {
        super(BuiltinAppenderType.CONSOLE);
    }

    @Override
    public String getName() {
        return appenderName;
    }

    @Override
    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    @Override
    public int getPriority() {
        return 50; // 优先级高于默认值，优先使用
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized.getAndSet(true)) {
            return;
        }

        // 内部日志输出，避免使用System.out直接输出
        logInternal("初始化控制台输出器");

        // 从配置中读取参数
        if (config != null) {
            // 从配置中提取appender名称（如果存在）
            if (config.containsKey(ConfigKeys.APPENDER_NAME)) {
                this.appenderName = String.valueOf(config.get(ConfigKeys.APPENDER_NAME));
            }
            // 配置格式化器
            if (config.containsKey("formatter")) {
                this.formatterName = String.valueOf(config.get("formatter"));
                // 验证格式化器是否存在
                if (formatterManager.getFormatter(this.formatterName) == null) {
                    logInternal("Warning: Formatter '" + this.formatterName + "' not found, falling back to 'text' formatter", true);
                    this.formatterName = "text";
                }
            }

            // 设置是否支持异步
            if (config.containsKey("asyncSupported")) {
                Object asyncValue = config.get("asyncSupported");
                if (asyncValue instanceof Boolean) {
                    this.asyncSupported = (Boolean) asyncValue;
                } else if (asyncValue instanceof String) {
                    this.asyncSupported = "true".equalsIgnoreCase((String) asyncValue);
                }
            }

            // 配置线程池参数
            if (config.containsKey("corePoolSize")) {
                this.corePoolSize = ConfigUtils.parseInt(config.get("corePoolSize"), this.corePoolSize);
            }
            if (config.containsKey("maxPoolSize")) {
                this.maxPoolSize = ConfigUtils.parseInt(config.get("maxPoolSize"), this.maxPoolSize);
            }
            if (config.containsKey("keepAliveTime")) {
                this.keepAliveTime = ConfigUtils.parseLong(config.get("keepAliveTime"), this.keepAliveTime);
            }
            if (config.containsKey("queueCapacity")) {
                this.queueCapacity = ConfigUtils.parseInt(config.get("queueCapacity"), this.queueCapacity);
            }

            // 配置输出级别控制
            if (config.containsKey("level")) {
                setOutputAdmissionLevel(String.valueOf(config.get("level")));
            }
            if (config.containsKey("levelPolicy")) {
                String policyName = String.valueOf(config.get("levelPolicy"));
                try {
                    setOutputLevelPolicy(OutputLevelPolicy.valueOf(policyName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    logInternal("Warning: Invalid levelPolicy value '" + policyName + "', using default AT_LEAST", true);
                }
            }
        }

        // 初始化异步执行器
        if (asyncSupported) {
            this.asyncExecutor = new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    keepAliveTime,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    new ThreadFactory() {
                        private final AtomicLong threadNumber = new AtomicLong(1);
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(r, "log4key-console-appender-" + threadNumber.getAndIncrement());
                            thread.setDaemon(true);
                            thread.setPriority(Thread.NORM_PRIORITY);
                            return thread;
                        }
                    },
                    new ThreadPoolExecutor.DiscardOldestPolicy() // 队列满时丢弃最旧的任务，避免阻塞调用者线程
            );
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            logInternal("启动控制台输出器");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logInternal("停止控制台输出器");
            close();
        }
    }

    @Override
    public void append(LogEvent event) {
        if (!running.get() || closed.get() || event == null) {
            return;
        }

        if (asyncSupported) {
            // 异步输出
            asyncExecutor.execute(() -> doAppend(event));
        } else {
            // 同步输出
            doAppend(event);
        }
    }

    @Override
    public void appendBatch(List<LogEvent> events) {
        if (!running.get() || closed.get() || events == null || events.isEmpty()) {
            return;
        }

        if (asyncSupported) {
            // 异步批量输出
            asyncExecutor.execute(() -> doAppendBatch(events));
        } else {
            // 同步批量输出
            doAppendBatch(events);
        }
    }

    @Override
    public void flush() {
        // 控制台输出刷新
        System.out.flush();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            logInternal("关闭控制台输出器");

            // 关闭异步执行器
            if (asyncExecutor != null) {
                try {
                    asyncExecutor.shutdown();
                    if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                        asyncExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    asyncExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            running.set(false);
        }
    }

    /**
     * Internal log output method to avoid using System.out directly.
     *
     * 内部日志输出方法，避免使用System.out直接输出。
     *
     * @param message the log message / 日志消息
     */
    private void logInternal(String message) {
        logInternal(message, false);
    }

    /**
     * 内部日志输出方法，避免使用System.out直接输出
     * @param message 日志消息
     * @param isError 是否为错误消息
     */
    private void logInternal(String message, boolean isError) {
        if (isError) {
            logger.warn(message);
        } else {
            // 避免在控制台输出器初始化时递归调用自身
            if (!appenderName.equalsIgnoreCase(ConfigKeys.APPENDER_TYPE_CONSOLE)) {
                logger.debug(message);
            }
        }
    }

    /**
     * Actually performs the log output.
     *
     * 实际执行日志输出。
     *
     * @param event the log event / 日志事件
     */
    private void doAppend(LogEvent event) {
        try {
            String formattedLog = formatterManager.format(event, formatterName);
            System.out.println(formattedLog);
        } catch (Exception e) {
            logger.warn("ConsoleAppender: Failed to append log event: {}", e.getMessage());
        }
    }

    /**
     * Actually performs the batch log output.
     *
     * 实际执行批量日志输出。
     *
     * @param events the list of log events / 日志事件列表
     */
    private void doAppendBatch(List<LogEvent> events) {
        try {
            // 优化批量输出，减少System.out.println调用次数
            final int MAX_BATCH_SIZE = 1000; // 最大批次大小
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < events.size(); i++) {
                LogEvent event = events.get(i);
                String formattedLog = formatterManager.format(event, formatterName);
                sb.append(formattedLog).append(System.lineSeparator());

                // 达到最大批次大小或最后一条记录时输出
                if (sb.length() > MAX_BATCH_SIZE || i == events.size() - 1) {
                    System.out.print(sb.toString());
                    sb.setLength(0); // 重置StringBuilder
                }
            }
        } catch (Exception e) {
            logger.warn("ConsoleAppender: Failed to append batch log events: {}", e.getMessage());
        }
    }

    // ========================== ConfigResolver 支持（Phase 5） ==========================

    /**
     * Initializes the appender using ConfigResolver.
     *
     * 使用ConfigResolver初始化输出提供者。
     *
     * @param config the configuration resolver / 配置解析器
     */
    @Override
    public void initialize(ConfigResolver config) {
        if (initialized.getAndSet(true)) {
            return;
        }

        // 内部日志输出，避免使用System.out直接输出
        logInternal("初始化控制台输出器");

        // 如果config为null，使用默认值初始化
        if (config == null) {
            // 使用默认值
            this.appenderName = ConfigKeys.APPENDER_NAME_KEY.defaultValue();
            this.formatterName = ConfigKeys.APPENDER_FORMATTER_KEY.defaultValue();
        } else {
            // 使用类型安全的ConfigKey读取配置
            this.appenderName = config.get(ConfigKeys.APPENDER_NAME_KEY);

            // 设置格式化器
            this.formatterName = config.get(ConfigKeys.APPENDER_FORMATTER_KEY);

            // 设置输出级别控制
            String level = config.get(ConfigKeys.APPENDER_LEVEL_KEY);
            if (level != null) {
                setOutputAdmissionLevel(level);
            }
            String policyName = config.get(com.log4key.config.ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY_KEY);
            if (policyName != null) {
                try {
                    setOutputLevelPolicy(OutputLevelPolicy.valueOf(policyName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    logInternal("Warning: Invalid levelPolicy value '" + policyName + "', using default AT_LEAST", true);
                }
            }
        }

        // 验证格式化器是否存在
        if (formatterManager.getFormatter(this.formatterName) == null) {
            logInternal("Warning: Formatter '" + this.formatterName + "' not found, falling back to 'text' formatter", true);
            this.formatterName = "text";
        }

        // 初始化异步执行器
        if (asyncSupported) {
            this.asyncExecutor = new ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    keepAliveTime,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(queueCapacity),
                    new ThreadFactory() {
                        private final AtomicLong threadNumber = new AtomicLong(1);
                        @Override
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(r, "log4key-console-appender-" + threadNumber.getAndIncrement());
                            thread.setDaemon(true);
                            thread.setPriority(Thread.NORM_PRIORITY);
                            return thread;
                        }
                    },
                    new ThreadPoolExecutor.DiscardOldestPolicy() // 队列满时丢弃最旧的任务，避免阻塞调用者线程
            );
        }
    }

}
