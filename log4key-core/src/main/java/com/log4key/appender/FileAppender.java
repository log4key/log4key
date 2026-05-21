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
import com.log4key.metrics.IoMetrics;
import com.log4key.metrics.LogMetrics;
import com.log4key.router.SmartFileRouterImpl;
import com.log4key.io.LogFileWriter;
import com.log4key.path.PathKey;
import com.log4key.path.PathTemplate;
import com.log4key.util.ConfigUtils;
import com.log4key.config.model.OutputLevelPolicy;
import com.log4key.internal.InternalLogger;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * File-based log appender implementation.
 *
 * 文件输出目标实现。
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
     * 文件写入器映射，按文件路径缓存
     */
    private final Map<PathKey, LogFileWriter> fileWriters = new ConcurrentHashMap<>();

    /**
     * 文件打开的最大缓存数量，默认 1024
     * Linux 默认打开的文件描述符数量为1024，因此最多支持1024个文件写入器，
     * 当超过时需要打开限制：ulimit -n
     */
    private int maxFileWriters = 1024;

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
     * 是否自动刷新
     */
    private boolean autoFlush = true;

    /**
     * 刷新间隔（日志条数）
     */
    private int flushInterval = 100;

    /**
     * 刷新计数器
     */
    private final AtomicLong flushCounter = new AtomicLong(0);

    /**
     * 运行状态标志
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 文件写入器空闲超时时间（毫秒），默认15分钟
     */
    private long writerIdleTimeout = 15 * 60 * 1000L;

    /**
     * 错误计数器
     */
    private final AtomicLong errorCount = new AtomicLong(0);

    /**
     * 日志格式化器名称，默认使用文本格式
     */
    private String formatterName = "text";

    /**
     * 批量处理最大大小，默认1000
     */
    private int maxBatchSize = 1000;

    /**
     * 最后一次写入的文件路径
     */
    private PathKey lastFilePath;

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

            // 配置文件写入器空闲超时时间
            if (config.containsKey(ConfigKeys.WRITER_IDLE_TIMEOUT)) {
                this.writerIdleTimeout = ConfigUtils.parseLong(config.get(ConfigKeys.WRITER_IDLE_TIMEOUT), this.writerIdleTimeout);
            }

            // 配置批量处理最大大小
            if (config.containsKey(ConfigKeys.MAX_BATCH_SIZE)) {
                this.maxBatchSize = ConfigUtils.parseInt(config.get(ConfigKeys.MAX_BATCH_SIZE), this.maxBatchSize);
            }

            // 配置文件写入器最大缓存数量
            if (config.containsKey(ConfigKeys.MAX_FILE_WRITERS)) {
                this.maxFileWriters = ConfigUtils.parseInt(config.get(ConfigKeys.MAX_FILE_WRITERS), this.maxFileWriters);
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
        // 刷新所有文件写入器
        fileWriters.values().forEach(LogFileWriter::flush);
    }

    @Override
    public void close() {
        long startTime = System.currentTimeMillis();
        logger.debug(String.format("[%s] 开始关闭，当前时间: %s", appenderName, new java.util.Date(startTime)));

        // 立即设置关闭标志，防止新的日志事件被写入
        closed.set(true);
        // 停止运行状态
        running.set(false);

        // 关闭所有文件写入器
        try {
            shutdownAllFileWriters();
        } catch (Exception e) {
            logger.warn(String.format("[%s] 关闭所有文件写入器时发生异常: %s", appenderName, e.getMessage()));
        }

        long endTime = System.currentTimeMillis();
        logger.debug(String.format("[%s] 关闭完成，当前时间: %s，耗时: %d毫秒", appenderName, new java.util.Date(endTime), (endTime - startTime)));
    }

    /**
     * Cleans up idle file writers based on the given timestamp.
     *
     * 清理空闲的文件写入器。
     *
     * @param currentTimeMillis current timestamp in milliseconds / 当前时间戳（毫秒）
     */
    public void cleanIdleWriters(long currentTimeMillis) {
        // 先收集所有需要关闭的写入器，避免并发修改问题
        List<PathKey> idleFiles = new ArrayList<>();
        for (Map.Entry<PathKey, LogFileWriter> entry : fileWriters.entrySet()) {
            LogFileWriter writer = entry.getValue();
            if (writer.isIdle(currentTimeMillis, writerIdleTimeout)) {
                idleFiles.add(entry.getKey());
            }
        }

        // 关闭空闲的写入器并从map中移除
        for (PathKey pathKey : idleFiles) {
            try {
                LogFileWriter writer = fileWriters.remove(pathKey);
                if (writer != null) {
                    writer.close();
                    logger.debug("Cleaned idle log file writer for " + Paths.get(pathKey.getDir(), pathKey.getFile()).toString());
                }
            } catch (Exception e) {
                logger.warn("Error closing idle log file writer for " + Paths.get(pathKey.getDir(), pathKey.getFile()).toString() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Shuts down all file writers managed by this appender.
     *
     * 关闭所有文件写入器。
     */
    public void shutdownAllFileWriters() {
        logger.debug(String.format("[%s] 开始关闭所有文件写入器，共%d个...", appenderName, fileWriters.size()));
        // 创建副本以避免ConcurrentModificationException
        Map<PathKey, LogFileWriter> writersToClose = new java.util.HashMap<>(fileWriters);
        fileWriters.clear();

        // 关闭所有文件写入器
        for (Map.Entry<PathKey, LogFileWriter> entry : writersToClose.entrySet()) {
            try {
                PathKey pathKey = entry.getKey();
                LogFileWriter writer = entry.getValue();
                logger.debug(String.format("[%s] 关闭文件写入器: %s", appenderName, Paths.get(pathKey.getDir(), pathKey.getFile()).toString()));
                writer.shutdown();
                logger.debug(String.format("[%s] 文件写入器关闭成功: %s", appenderName, Paths.get(pathKey.getDir(), pathKey.getFile()).toString()));
            } catch (Exception e) {
                logger.warn(String.format("[%s] 关闭文件写入器失败: %s: %s", appenderName, Paths.get(entry.getKey().getDir(), entry.getKey().getFile()).toString(), e.getMessage()));
            }
        }

        // 清空映射
        fileWriters.clear();
        logger.debug(String.format("[%s] 所有文件写入器已关闭，文件写入器映射已清空", appenderName));
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
            this.writerIdleTimeout = ConfigKeys.WRITER_IDLE_TIMEOUT_KEY.defaultValue();
            this.maxBatchSize = ConfigKeys.MAX_BATCH_SIZE_KEY.defaultValue();
            this.maxFileWriters = ConfigKeys.MAX_FILE_WRITERS_KEY.defaultValue();

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

            // Long -> long (自动拆箱)
            this.writerIdleTimeout = config.get(ConfigKeys.WRITER_IDLE_TIMEOUT_KEY);

            // Integer -> int (自动拆箱)
            this.maxBatchSize = config.get(ConfigKeys.MAX_BATCH_SIZE_KEY);

            // 获取文件写入器最大缓存数量
            this.maxFileWriters = config.get(ConfigKeys.MAX_FILE_WRITERS_KEY);

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
     * 执行实际的日志写入操作（单条）
     */
    private void doAppend(LogEvent event) {
        List<PathKey> filePaths = determineFilePaths(event);
        LogMetrics.recordFile(filePaths.size());
        String formattedLog = formatLogEvent(event);
        for (PathKey pathKey : filePaths) {
            try {
                // 获取或创建文件写入器
                LogFileWriter writer = getOrCreateFileWriter(pathKey);
                if (writer != null) {
                    // 在此记录文件切换
                    if (!pathKey.equals(lastFilePath)) {
                        IoMetrics.recordFileSwitch();
                        lastFilePath = pathKey;
                    }

                    // 写入日志
                    writer.write(formattedLog);

                    // 根据配置决定是否刷新
                    if (autoFlush || flushCounter.incrementAndGet() % flushInterval == 0) {
                        writer.flush();
                    }
                }
            } catch (IOException e) {
                errorCount.incrementAndGet();
                logger.warn("Error writing log event to file " + Paths.get(pathKey.getDir(), pathKey.getFile()).toString() + ": " + e.getMessage());
            }
        }
    }

    /**
     * 执行实际的日志写入操作（批量）
     */
    private void doAppendBatch(List<LogEvent> events) {
        // 收集所有日志，不分 chunk
        Map<PathKey, List<String>> fileLogs = new LinkedHashMap<>(16);
        for (LogEvent event : events) {
            List<PathKey> filePaths = determineFilePaths(event);
            LogMetrics.recordFile(filePaths.size());
            String formattedLog = formatLogEvent(event);
            for (PathKey pathKey : filePaths) {
                fileLogs.computeIfAbsent(pathKey, k -> new ArrayList<>()).add(formattedLog);
            }
        }

        // 对每个文件批量写入（支持分片）
        for (Map.Entry<PathKey, List<String>> entry : fileLogs.entrySet()) {
            PathKey pathKey = entry.getKey();
            List<String> logs = entry.getValue();
            try {
                LogFileWriter writer = getOrCreateFileWriter(pathKey);
                if (writer == null) {
                    continue;
                }

                // 在此记录文件切换
                if (!pathKey.equals(lastFilePath)) {
                    IoMetrics.recordFileSwitch();
                    lastFilePath = pathKey;
                }

                // 批量分片写入
                StringBuilder sb = new StringBuilder(maxBatchSize);
                for (String log : logs) {
                    if (sb.length() + log.length() > maxBatchSize && sb.length() > 0) {
                        writer.write(sb.toString());
                        sb.setLength(0);
                    }
                    sb.append(log);
                }
                // 写入剩余
                if (sb.length() > 0) {
                    writer.write(sb.toString());
                }

            } catch (IOException e) {
                errorCount.incrementAndGet();
                logger.warn("Error writing batch to " + Paths.get(pathKey.getDir(), pathKey.getFile()).toString(), e);
            }
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
     * 获取或创建文件写入器
     */
    private LogFileWriter getOrCreateFileWriter(PathKey pathKey) {
        // 检查缓存大小，如果达到上限则清理空闲写入器
        if (fileWriters.size() >= maxFileWriters) {
            cleanIdleWriters(System.currentTimeMillis());
            // 如果清理后仍然达到上限，尝试清理最旧的写入器
            if (fileWriters.size() >= maxFileWriters) {
                evictOldestWriters();
            }
        }

        LogFileWriter writer = fileWriters.computeIfAbsent(pathKey, pk -> {
            try {
                IoMetrics.recordFileWrite();
                // 创建带有charset参数的LogFileWriter实例（PathKey版本）
                return new LogFileWriter(pk, 8192, 100 * 1024 * 1024, LogFileWriter.RollingPolicy.SIZE, 3600000, false, this.charset);
            } catch (IOException e) {
                errorCount.incrementAndGet();
                logger.warn("Failed to create log file writer for path: " + Paths.get(pk.getDir(), pk.getFile()).toString() + ": " + e.getMessage());
                return null; // 返回null，在调用处处理
            }
        });

        // 检查写入器是否已经关闭，如果已关闭则移除并创建新的
        if (writer != null && writer.isClosed()) {
            // 使用removeIf确保原子性操作
            fileWriters.entrySet().removeIf(entry ->
                entry.getKey().equals(pathKey) && entry.getValue().isClosed()
            );
            // 重新创建写入器
            return getOrCreateFileWriter(pathKey);
        }

        return writer;
    }

    /**
     * 清理最旧的文件写入器，当缓存达到上限时使用
     */
    private void evictOldestWriters() {
        // 计算需要清理的数量，保留90%的容量
        int targetSize = (int) (maxFileWriters * 0.9);
        int toEvict = fileWriters.size() - targetSize;

        if (toEvict > 0) {
            // 收集所有写入器
            List<Map.Entry<PathKey, LogFileWriter>> writers = new ArrayList<>(fileWriters.entrySet());

            // 随机排序，简单处理
            java.util.Collections.shuffle(writers);

            // 清理多余的写入器
            for (int i = 0; i < toEvict && i < writers.size(); i++) {
                Map.Entry<PathKey, LogFileWriter> entry = writers.get(i);
                try {
                    LogFileWriter writer = fileWriters.remove(entry.getKey());
                    if (writer != null) {
                        writer.close();
                        logger.debug("Evicted excess log file writer for " + Paths.get(entry.getKey().getDir(), entry.getKey().getFile()).toString());
                    }
                } catch (Exception e) {
                    logger.warn("Error closing excess log file writer for " + Paths.get(entry.getKey().getDir(), entry.getKey().getFile()).toString() + ": " + e.getMessage());
                }
            }
        }
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
