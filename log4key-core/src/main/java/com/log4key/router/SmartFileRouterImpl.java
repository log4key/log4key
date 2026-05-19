/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.router;

import com.log4key.api.ILogKey;
import com.log4key.api.LogEvent;
import com.log4key.api.router.SmartFileRouter;
import com.log4key.config.model.OutputLevelPolicy;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Smart file router implementation.
 *
 * 智能文件路由器的实现类。
 */
public class SmartFileRouterImpl implements SmartFileRouter {

    /**
     * 默认日志目录
     */
    private static final String DEFAULT_DEFAULT_DIRECTORY = "./logs";

    /**
     * 默认日志目录
     */
    private volatile String defaultDirectory = DEFAULT_DEFAULT_DIRECTORY;

    /**
     * 初始化状态
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 初始化锁
     */
    private final ReentrantLock initLock = new ReentrantLock();

    /**
     * 日期格式化器
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    /**
     * 每日缓存，包含日期与路径
     */
    private static class DayCache {
        /**
         * 缓存日期
         */
        final String date;

        /**
         * 路径缓存
         * [缓存键，实际路径]
         */
        final Map<String, String> pathCache = new ConcurrentHashMap<>();

        public DayCache(String date) {
            this.date = date;
        }
    }

    /**
     * 每日路径缓存
     */
    private final AtomicReference<DayCache> dayCacheRef = new AtomicReference<>(new DayCache(dateFormat.format(new Date())));

    /**
     * Appender输出准入级别
     * (路由使用小写)
     */
    private String outputAdmissionLevel = "info";

    /**
     * Appender输出级别策略
     */
    private OutputLevelPolicy outputLevelPolicy = OutputLevelPolicy.AT_LEAST;


    private static final Map<String, Integer> LEVEL_PRIORITY = new HashMap<>();
    private static final String[] STANDARD_LEVELS = {"error", "warn", "info", "debug", "trace"};

    static {
        LEVEL_PRIORITY.put("error", 50000);
        LEVEL_PRIORITY.put("warn", 40000);
        LEVEL_PRIORITY.put("info", 30000);
        LEVEL_PRIORITY.put("debug", 20000);
        LEVEL_PRIORITY.put("trace", 10000);
    }

    /**
     * 构造函数
     */
    public SmartFileRouterImpl() {
    }

    /**
     * Determines log file path based on log key.
     *
     * 根据日志主键确定日志文件路径。
     *
     * @param key the log key / 日志主键
     * @return the log file path / 日志文件路径
     */
    @Override
    public String determineLogFilePath(ILogKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Log key cannot be null");
        }

        // 确保已初始化
        ensureInitialized();

        // 尝试从缓存获取路径
        String keyValue = key.value();
        // 默认日志级别为INFO
        String level = "info";
        String dateStr = dateFormat.format(new Date());

        return getLogFilePath(keyValue, level, dateStr);
    }

    /**
     * 根据日志事件确定日志文件路径
     *
     * @param event 日志事件
     * @return 日志文件路径
     */
    @Override
    public String determineLogFilePath(LogEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }

        // 确保已初始化
        ensureInitialized();

        // 尝试从缓存获取路径
        String originalKeyValue = event.getKey();
        // 获取日志级别，默认INFO
        String originalLevel = event.getLevel();
        String finalLevel = (originalLevel != null) ? originalLevel.toLowerCase() : "info";

        // 如果没有Key，则使用日志级别作为Key，这样生成的文件名就是 info.log, warn.log 等
        String finalKeyValue = (originalKeyValue == null || originalKeyValue.isEmpty()) ? finalLevel : originalKeyValue;

        // 获取日期字符串
        String finalDateStr = dateFormat.format(event.getTimestampMillis());

        return getLogFilePath(finalKeyValue, finalLevel, finalDateStr);
    }

    /**
     * Determines all log file paths for the log event.
     *
     * 根据日志事件确定所有需要写入的日志文件路径。
     *
     * @param event the log event / 日志事件
     * @return the list of log file paths / 日志文件路径列表
     */
    @Override
    public List<String> determineLogFilePaths(LogEvent event) {
        // 仅输出指定级别的日志
        if (outputLevelPolicy == OutputLevelPolicy.EXACT) {
            String path = determineLogFilePath(event);
            return path != null ? java.util.Collections.singletonList(path) : java.util.Collections.emptyList();
        }

        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }

        ensureInitialized();

        String originalKeyValue = event.getKey();
        // 判断是否使用默认Key
        boolean isDefaultKey = (originalKeyValue == null || originalKeyValue.isEmpty());
        String finalKeyValue = isDefaultKey ? null : originalKeyValue;

        String originalLevel = event.getLevel();
        String eventLevel = (originalLevel != null) ? originalLevel.toLowerCase() : "info";
        String dateStr = dateFormat.format(event.getTimestampMillis());

        Integer eventPriority = LEVEL_PRIORITY.get(eventLevel);
        // 如果是未知级别，退化为只写该级别的日志
        if (eventPriority == null) {
             String currentKey = isDefaultKey ? eventLevel : finalKeyValue;
             return java.util.Collections.singletonList(getLogFilePath(currentKey, eventLevel, dateStr));
        }

        List<String> paths = new ArrayList<>();
        // 获取最小包含级别的优先级
        Integer minPriority = LEVEL_PRIORITY.get(outputAdmissionLevel);
        if (minPriority == null) {
            minPriority = 0; // 如果配置错误，默认全部包含
        }

        for (String level : STANDARD_LEVELS) {
            Integer currentPriority = LEVEL_PRIORITY.get(level);

            // 检查是否低于最小包含级别
            if (currentPriority != null && currentPriority < minPriority) {
                continue;
            }

            // 如果事件优先级 >= 当前级别优先级，则包含该级别
            // 例如：WARN(40000) >= INFO(30000)，所以WARN日志会写入info.log
            if (currentPriority != null && eventPriority >= currentPriority) {
                // 如果是默认Key，使用当前级别作为文件名（例如 info.log）
                // 否则使用原始Key
                String currentKey = isDefaultKey ? level : finalKeyValue;
                paths.add(getLogFilePath(currentKey, level, dateStr));
            }
        }

        return paths;
    }

    /**
     * 设置输出级别
     * @param level 最小输出级别
     */
    public void setOutputAdmissionLevel(String level) {
        if (level != null && LEVEL_PRIORITY.containsKey(level.toLowerCase())) {
            this.outputAdmissionLevel = level.toLowerCase();
        }
    }

    /**
     * Sets the output level policy.
     *
     * 设置输出级别策略。
     *
     * @param policy the output level policy / 输出级别策略
     */
    public void setOutputLevelPolicy(OutputLevelPolicy policy) {
        this.outputLevelPolicy = policy;
    }

    /**
     * Sets the base log directory.
     *
     * 设置基础日志目录。
     *
     * @param baseDirectory the base log directory / 基础日志目录
     */
    @Override
    public void setBaseDirectory(String baseDirectory) {
        if (baseDirectory == null || baseDirectory.isEmpty()) {
            throw new IllegalArgumentException("Base directory cannot be null or empty");
        }
        this.defaultDirectory = baseDirectory;
        // 清空路径缓存，因为日志目录改变了
        dayCacheRef.get().pathCache.clear();
    }

    /**
     * 初始化路由器
     */
    @Override
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            try {
                initLock.lock();
                // 创建默认日志目录
                ensureDirectoryExists(Paths.get(defaultDirectory));
            } finally {
                initLock.unlock();
            }
        }
    }

    /**
     * 关闭路由器，释放资源
     */
    @Override
    public void shutdown() {
        initialized.set(false);
    }

    /**
     * 获取日志文件路径（带缓存）
     */
    private String getLogFilePath(String key, String level, String dateStr) {
        // 净化文件名，移除非法字符
        String sanitizedKey = sanitizeFileName(key);

        // 使用日志级别和事件时间戳构建缓存键
        String cacheKey = level + "_" + dateStr + "_" + sanitizedKey;

        // 获取日志文件路径缓存
        DayCache cache = dayCacheRef.get();
        if (!cache.date.equals(dateStr)) {
            // 创建新的缓存
            DayCache newCache = new DayCache(dateStr);

            // CAS 只允许一个线程成功
            dayCacheRef.compareAndSet(cache, newCache);

            // 重新获取
            cache = dayCacheRef.get();
        }

        // 使用当前缓存
        return cache.pathCache.computeIfAbsent(cacheKey, k -> {
            // 构建路径：defaultDirectory/日志级别/yyyyMMdd/key.log
            // 确保使用绝对路径并规范化
            Path basePath = Paths.get(defaultDirectory).toAbsolutePath().normalize();
            Path filePath = basePath.resolve(level)
                    .resolve(dateStr)
                    .resolve(sanitizedKey + ".log");

            // 确保目录存在
            ensureDirectoryExists(filePath.getParent());

            // 返回文件路径
            return filePath.toString();
        });
    }

    /**
     * Sanitizes file name by replacing illegal characters with underscores.
     *
     * 净化文件名，将非法字符替换为下划线。
     *
     * @param fileName the original file name / 原始文件名
     * @return the sanitized file name / 净化后的文件名
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "null";
        }
        // 替换 Windows 和 Linux 文件系统中的非法字符
        // < > : " / \ | ? *
        return fileName.replaceAll("[<>:\"/\\\\|?*\\[\\]]", "_");
    }

    /**
     * 确保路由器已初始化
     */
    private void ensureInitialized() {
        if (!initialized.get()) {
            initialize();
        }
    }

    /**
     * 确保目录存在，如果不存在则创建
     *
     * @param directoryPath 目录路径
     */
    private void ensureDirectoryExists(Path directoryPath) {
        File directory = directoryPath.toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
