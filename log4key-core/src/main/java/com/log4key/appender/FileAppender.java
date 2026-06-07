/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.router.SmartFileRouter;
import com.log4key.config.ConfigKeys;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.formatter.LogFormatterManager;
import com.log4key.metrics.LogMetrics;
import com.log4key.router.SmartFileRouterImpl;
import com.log4key.path.PathKey;
import com.log4key.path.PathTemplate;
import com.log4key.util.ExecutorController;
import com.log4key.config.model.OutputLevelPolicy;
import com.log4key.internal.InternalLogger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File-based log appender implementation.
 *
 * V2 架构：format + route + shard 在 Business Thread 同步完成，
 * 仅最终 write 操作通过 executorController.executeWrite() 异步投递到 Worker。
 */
public class FileAppender extends AbstractAppenderProvider {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(FileAppender.class);

    /**
     * Appender名称，从配置前缀中提取
     */
    private String appenderName = ConfigKeys.APPENDER_TYPE_FILE;

    /**
     * 日志文件路由器
     */
    private SmartFileRouter fileRouter;

    /**
     * 执行器控制器，由 LogManager 初始化时注入
     */
    private ExecutorController executorController;

    /**
     * Worker 数量（用于 shard 计算），由 LogManager 初始化时注入
     */
    private int workerCount;

    /**
     * 字符编码
     */
    private String charset = "UTF-8";

    /**
     * 是否已初始化
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 是否已关闭
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 运行状态标志
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 错误计数器
     */
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * 日志格式化器名称，默认使用文本格式
     */
    private String formatterName = "text";

    /**
     * 构造函数
     */
    public FileAppender() {
        super(BuiltinAppenderType.FILE);
        // 创建默认的文件路由器
        this.fileRouter = new SmartFileRouterImpl();
    }

    @Override
    public String getName() {
        return appenderName;
    }

    /**
     * 设置 ExecutorController（由 LogManager 初始化时注入）。
     *
     * @param executorController 执行器控制器
     */
    public void setExecutorController(ExecutorController executorController) {
        this.executorController = executorController;
    }

    /**
     * 设置 Worker 数量（由 LogManager 初始化时注入）。
     *
     * @param workerCount Worker 数量
     */
    public void setWorkerCount(int workerCount) {
        this.workerCount = workerCount;
    }

    /**
     * 根据 PathKey 计算目标 Worker 编号。
     *
     * 使用 hashCode 取模分配，确保同一 PathKey 始终路由到同一 Worker。
     *
     * @param pathKey 路径键
     * @return Worker 编号（0 ~ workerCount-1）
     */
    private int shard(PathKey pathKey) {
        return Math.abs(pathKey.hashCode()) & (workerCount - 1);
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取错误计数。
     *
     * 获取错误计数。
     *
     * @return the number of errors encountered / 错误计数
     */
    public long getErrorCount() {
        return errorCount.get();
    }

    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized.getAndSet(true)) {
            return;
        }

        // 配置文件路由器
        if (config != null) {
            // 从配置中提取appender名称（如果存在）
            if (config.containsKey(ConfigKeys.APPENDER_NAME)) {
                this.appenderName = String.valueOf(config.get(ConfigKeys.APPENDER_NAME));
                logger.debug("initialize() called, appenderName from config: " + this.appenderName);
            }
            logger.debug("final appenderName: " + this.appenderName);
            // 设置 rootDirectory（全局配置）
            String rootDirectory = null;
            if (config.containsKey(ConfigKeys.ROOT_DIRECTORY)) {
                rootDirectory = String.valueOf(config.get(ConfigKeys.ROOT_DIRECTORY));
            }
            if (rootDirectory != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setRootDirectory(rootDirectory);
                }
            }

            // 设置 directory 模板（Appender 子目录）
            String directory = null;
            if (config.containsKey(ConfigKeys.APPENDER_DIRECTORY)) {
                directory = String.valueOf(config.get(ConfigKeys.APPENDER_DIRECTORY));
            }
            if (directory != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    String normalizedDir = normalizeDirectoryPath(directory);
                    ((SmartFileRouterImpl) fileRouter).setDirectoryTemplate(PathTemplate.compile(normalizedDir));
                }
            }

            // 设置 fileName 模板
            String fileName = null;
            if (config.containsKey(ConfigKeys.APPENDER_FILE_NAME)) {
                fileName = String.valueOf(config.get(ConfigKeys.APPENDER_FILE_NAME));
            }
            if (fileName != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setFileNameTemplate(PathTemplate.compile(fileName));
                }
            }

            // 保存字符集配置
            if (config.containsKey(ConfigKeys.APPENDER_CHARSET)) {
                this.charset = String.valueOf(config.get(ConfigKeys.APPENDER_CHARSET));
            }

            // 配置日志格式化器
            if (config.containsKey(ConfigKeys.APPENDER_FORMATTER)) {
                this.formatterName = String.valueOf(config.get(ConfigKeys.APPENDER_FORMATTER));
                // 验证格式化器是否存在
                if (LogFormatterManager.getInstance().getFormatter(this.formatterName) == null) {
                    logger.warn("Formatter '" + this.formatterName + "' not found, falling back to 'text' formatter");
                    this.formatterName = "text";
                }
            }

            // 配置输出级别控制
            if (config.containsKey("level")) {
                setOutputAdmissionLevel(String.valueOf(config.get("level")));
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setOutputAdmissionLevel(this.outputAdmissionLevel);
                }
            }
            if (config.containsKey("levelPolicy")) {
                String policyName = String.valueOf(config.get("levelPolicy"));
                try {
                    setOutputLevelPolicy(OutputLevelPolicy.valueOf(policyName.toUpperCase()));
                    if (fileRouter instanceof SmartFileRouterImpl) {
                        ((SmartFileRouterImpl) fileRouter).setOutputLevelPolicy(this.outputLevelPolicy);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Warning: Invalid levelPolicy value '" + policyName + "', using default AT_LEAST");
                }
            }
        }

        // 初始化运行状态
        running.set(true);

        // 初始化文件路由器
        fileRouter.initialize();

    }

    @Override
    public void start() {
        // 启动扩展点，这里可以添加一些启动逻辑
        // 由于FileAppender在initialize方法中已经完成了主要初始化，start方法可以保持简单
        logger.debug("FileAppender启动: " + this.appenderName);
    }

    @Override
    public void stop() {
        // 停止扩展点，调用close方法释放资源
        logger.debug("FileAppender停止: " + this.appenderName);
        running.set(false);
        close();
    }

    @Override
    public void append(LogEvent event) {
        if (closed.get() || event == null) {
            return;
        }

        // 直接执行同步写入，由LogManager统一负责异步处理
        try {
            doAppend(event);
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.warn("Error writing log event: " + e.getMessage());
        }
    }

    @Override
    public void appendBatch(List<LogEvent> events) {
        if (closed.get() || events == null || events.isEmpty()) {
            return;
        }

        // 直接执行同步批量写入，由LogManager统一负责异步处理
        try {
            doAppendBatch(events);
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.warn("Error writing log events batch: " + e.getMessage());
        }
    }

    @Override
    public void flush() {
        // V2 架构：flush 由 Worker 内部管理，不再由 FileAppender 直接操作
    }

    @Override
    public void close() {
        long startTime = System.currentTimeMillis();
        logger.debug(String.format("[%s] 开始关闭，当前时间: %s", appenderName, new java.util.Date(startTime)));

        // 立即设置关闭标志，防止新的日志事件被写入
        closed.set(true);
        // 停止运行状态
        running.set(false);

        long endTime = System.currentTimeMillis();
        logger.debug(String.format("[%s] 关闭完成，当前时间: %s，耗时: %d毫秒", appenderName, new java.util.Date(endTime), (endTime - startTime)));
    }

    /**
     * Sets the file router (mainly for testing).
     *
     * 设置文件路由器（主要用于测试）。
     *
     * @param router the file router instance / 文件路由器实例
     */
    protected void setFileRouter(SmartFileRouter router) {
        this.fileRouter = router;
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

        // 如果config为null，使用默认值初始化
        if (config == null) {
            // 使用默认值
            this.appenderName = ConfigKeys.APPENDER_NAME_KEY.defaultValue();

            // 设置 rootDirectory
            String defaultRootDirectory = ConfigKeys.ROOT_DIRECTORY_KEY.defaultValue();
            if (defaultRootDirectory != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setRootDirectory(defaultRootDirectory);
                }
            }

            // 设置 directory 模板
            String defaultDirectory = ConfigKeys.APPENDER_DIRECTORY_KEY.defaultValue();
            if (defaultDirectory != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    String normalizedDir = normalizeDirectoryPath(defaultDirectory);
                    ((SmartFileRouterImpl) fileRouter).setDirectoryTemplate(PathTemplate.compile(normalizedDir));
                }
            }

            // 设置 fileName 模板
            String defaultFileName = ConfigKeys.APPENDER_FILE_NAME_KEY.defaultValue();
            if (defaultFileName != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setFileNameTemplate(PathTemplate.compile(defaultFileName));
                }
            }

            this.charset = ConfigKeys.APPENDER_CHARSET_KEY.defaultValue();
            this.formatterName = ConfigKeys.APPENDER_FORMATTER_KEY.defaultValue();

            // 验证格式化器是否存在
            if (LogFormatterManager.getInstance().getFormatter(this.formatterName) == null) {
                logger.warn("Formatter '" + this.formatterName + "' not found, falling back to 'text' formatter");
                this.formatterName = "text";
            }
        } else {
            // 使用类型安全的ConfigKey读取配置
            this.appenderName = config.get(ConfigKeys.APPENDER_NAME_KEY);

            // 设置 rootDirectory
            String rootDirectory = config.get(ConfigKeys.ROOT_DIRECTORY_KEY);
            if (rootDirectory != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setRootDirectory(rootDirectory);
                }
            }

            // 设置 directory 模板
            String directory = config.get(ConfigKeys.APPENDER_DIRECTORY_KEY);
            if (directory != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    String normalizedDir = normalizeDirectoryPath(directory);
                    ((SmartFileRouterImpl) fileRouter).setDirectoryTemplate(PathTemplate.compile(normalizedDir));
                }
            }

            // 设置 fileName 模板
            String fileName = config.get(ConfigKeys.APPENDER_FILE_NAME_KEY);
            if (fileName != null) {
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setFileNameTemplate(PathTemplate.compile(fileName));
                }
            }

            // 设置字符集
            this.charset = config.get(ConfigKeys.APPENDER_CHARSET_KEY);

            // 设置格式化器
            this.formatterName = config.get(ConfigKeys.APPENDER_FORMATTER_KEY);

            // 验证格式化器是否存在
            if (LogFormatterManager.getInstance().getFormatter(this.formatterName) == null) {
                logger.warn("Formatter '" + this.formatterName + "' not found, falling back to 'text' formatter");
                this.formatterName = "text";
            }

            // 设置输出级别控制
            String level = config.get(ConfigKeys.APPENDER_LEVEL_KEY);
            if (level != null) {
                setOutputAdmissionLevel(level);
                if (fileRouter instanceof SmartFileRouterImpl) {
                    ((SmartFileRouterImpl) fileRouter).setOutputAdmissionLevel(this.outputAdmissionLevel);
                }
            }
            String policyName = config.get(com.log4key.config.ConfigKeys.APPENDER_OUTPUT_LEVEL_POLICY_KEY);
            if (policyName != null) {
                try {
                    setOutputLevelPolicy(OutputLevelPolicy.valueOf(policyName.toUpperCase()));
                    if (fileRouter instanceof SmartFileRouterImpl) {
                        ((SmartFileRouterImpl) fileRouter).setOutputLevelPolicy(this.outputLevelPolicy);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn("Warning: Invalid levelPolicy value '" + policyName + "', using default AT_LEAST");
                }
            }
        }

        // 初始化运行状态
        running.set(true);

        // 初始化文件路由器
        fileRouter.initialize();
    }

    /**
     * 执行实际的日志写入操作（单条）。
     *
     * V2 架构：format + route + shard 在 Business Thread 同步完成，
     * 仅最终 write 通过 executorController.executeWrite() 异步投递到 Worker。
     */
    private void doAppend(LogEvent event) {
        // LevelFilter 检查
        if (!shouldOutput(event)) {
            return;
        }

        // executorController 为 null 时跳过写入（不抛 NPE）
        if (executorController == null) {
            logger.warn("executorController is null, skipping write");
            return;
        }

        // 格式化日志
        String formattedLog = formatLogEvent(event);

        // 确定文件路径
        List<PathKey> filePaths = determineFilePaths(event);
        LogMetrics.recordFile(filePaths.size());

        // 对每个路径键，shard 后异步投递到 Worker
        for (PathKey pathKey : filePaths) {
            int workerId = shard(pathKey);
            executorController.executeWrite(String.valueOf(workerId), pathKey, formattedLog);
        }
    }

    /**
     * 执行实际的日志写入操作（批量）。
     *
     * V2 架构：逐条处理后异步投递到 Worker。
     */
    private void doAppendBatch(List<LogEvent> events) {
        for (LogEvent event : events) {
            doAppend(event);
        }
    }

    /**
     * 确定日志文件路径列表
     */
    private List<PathKey> determineFilePaths(LogEvent event) {
        // 直接使用日志事件确定文件路径
        return fileRouter.determineLogFilePaths(event);
    }

    /**
     * 格式化日志事件
     */
    private String formatLogEvent(LogEvent event) {
        return LogFormatterManager.getInstance().format(event, formatterName);
    }

    /**
     * 规范化目录路径，确保与 rootDirectory 拼接后路径正确。
     * 
     * 规则：
     * - directory 开头必须有 / 符，如果没有则添加
     * - 保证 rootDirectory 与 directory 之间只有一个 / 符
     *
     * @param directory 原始目录路径模板
     * @return 规范化后的目录路径模板
     */
    private static String normalizeDirectoryPath(String directory) {
        if (directory == null || directory.isEmpty()) {
            return directory;
        }
        if (!directory.startsWith("/") && !directory.startsWith("\\")) {
            return "/" + directory;
        }
        return directory;
    }

}
