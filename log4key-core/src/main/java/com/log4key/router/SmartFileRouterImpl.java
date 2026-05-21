/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.router;

import com.log4key.api.LogEvent;
import com.log4key.api.router.SmartFileRouter;
import com.log4key.config.model.OutputLevelPolicy;
import com.log4key.path.PathKey;
import com.log4key.path.PathTemplate;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Smart file router implementation.
 *
 * 智能文件路由器的实现类。
 * 使用 PathTemplate 构建目录和文件名路径，替代硬编码的路径拼接逻辑。
 */
public class SmartFileRouterImpl implements SmartFileRouter {

    /**
     * 默认根日志目录
     */
    private static final String DEFAULT_ROOT_DIRECTORY = "./logs";

    /**
     * 根日志目录
     */
    private volatile String rootDirectory = DEFAULT_ROOT_DIRECTORY;

    /**
     * 目录路径模板，默认 "{level}/{date}"
     */
    private volatile PathTemplate directoryTemplate = PathTemplate.compile("{level}/{date}");

    /**
     * 文件名模板，默认 "{key}.log"
     */
    private volatile PathTemplate fileNameTemplate = PathTemplate.compile("{key}.log");

    /**
     * 初始化状态
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 初始化锁
     */
    private final ReentrantLock initLock = new ReentrantLock();

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
     * 根据日志事件确定日志文件路径
     *
     * @param event 日志事件
     * @return 日志文件路径键
     */
    @Override
    public PathKey determineLogFilePath(LogEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }
        ensureInitialized();
        return buildPath(event);
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
    public List<PathKey> determineLogFilePaths(LogEvent event) {
        if (outputLevelPolicy == OutputLevelPolicy.EXACT) {
            return java.util.Collections.singletonList(determineLogFilePath(event));
        }

        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }

        ensureInitialized();

        String originalKeyValue = event.getKey();
        boolean isDefaultKey = (originalKeyValue == null || originalKeyValue.isEmpty());
        String sanitizedKey = isDefaultKey ? null : sanitizeFileName(originalKeyValue);

        String originalLevel = event.getLevel();
        String eventLevel = (originalLevel != null) ? originalLevel.toLowerCase() : "info";

        Integer eventPriority = LEVEL_PRIORITY.get(eventLevel);
        if (eventPriority == null) {
            String currentKey = isDefaultKey ? eventLevel : sanitizedKey;
            return java.util.Collections.singletonList(buildPath(event, currentKey, eventLevel));
        }

        List<PathKey> paths = new ArrayList<>();
        Integer minPriority = LEVEL_PRIORITY.get(outputAdmissionLevel);
        if (minPriority == null) {
            minPriority = 0;
        }

        for (String level : STANDARD_LEVELS) {
            Integer currentPriority = LEVEL_PRIORITY.get(level);

            if (currentPriority != null && currentPriority < minPriority) {
                continue;
            }

            if (currentPriority != null && eventPriority >= currentPriority) {
                String currentKey = isDefaultKey ? level : sanitizedKey;
                paths.add(buildPath(event, currentKey, level));
            }
        }

        return paths;
    }

    /**
     * 设置输出级别
     *
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
     * Sets the root log directory.
     *
     * 设置根日志目录。自动移除结尾的路径分隔符（/ 或 \）。
     *
     * @param rootDirectory the root log directory / 根日志目录
     */
    public void setRootDirectory(String rootDirectory) {
        if (rootDirectory == null || rootDirectory.isEmpty()) {
            throw new IllegalArgumentException("Root directory cannot be null or empty");
        }
        while (rootDirectory.endsWith("/") || rootDirectory.endsWith("\\")) {
            rootDirectory = rootDirectory.substring(0, rootDirectory.length() - 1);
        }
        this.rootDirectory = rootDirectory;
    }

    /**
     * Sets the directory path template.
     *
     * 设置目录路径模板。自动确保模板编译后的路径以分隔符开头，以便与 rootDirectory 拼接。
     *
     * @param template 路径模板（实际类型为 PathTemplate）
     */
    public void setDirectoryTemplate(PathTemplate template) {
        this.directoryTemplate = template;
    }

    /**
     * Sets the file name template.
     *
     * 设置文件名模板。
     *
     * @param template 文件名模板（实际类型为 PathTemplate）
     */
    public void setFileNameTemplate(PathTemplate template) {
        this.fileNameTemplate = template;
    }

    /**
     * 初始化路由器
     */
    @Override
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            try {
                initLock.lock();
                // 创建根日志目录
                ensureDirectoryExists(Paths.get(rootDirectory));
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
     * 使用目录模板从日志事件构建目录路径。
     *
     * @param event 日志事件
     * @return 相对目录路径（不包含 rootDirectory）
     */
    private String buildDir(LogEvent event) {
        return directoryTemplate.apply(event);
    }

    /**
     * 使用目录模板从日志事件构建目录路径，支持覆盖日志级别。
     *
     * 用于在 AT_LEAST 策略下为不同级别生成不同的目录路径。
     *
     * @param event 日志事件
     * @param overrideLevel 覆盖的日志级别（为 null 时使用 event 中的级别）
     * @return 相对目录路径（不包含 rootDirectory）
     */
    private String buildDir(LogEvent event, String overrideLevel) {
        return directoryTemplate.apply(event, overrideLevel);
    }

    /**
     * 使用文件名模板从日志事件构建文件名。
     *
     * @param event 日志事件
     * @return 文件名（含扩展名）
     */
    private String buildFile(LogEvent event) {
        return fileNameTemplate.apply(event);
    }

    /**
     * 使用文件名模板从日志事件构建文件名，支持覆盖 key 值。
     *
     * 用于在 AT_LEAST 策略下为不同级别生成不同的文件名。
     *
     * @param event 日志事件
     * @param overrideLevel 覆盖的日志级别（为 null 时使用 event 中的级别）
     * @return 文件名（含扩展名）
     */
    private String buildFile(LogEvent event, String overrideLevel) {
        return fileNameTemplate.apply(event, overrideLevel);
    }

    /**
     * 从日志事件构建完整的日志文件绝对路径。
     * 包含目录创建和路径拼接。
     *
     * @param event 日志事件
     * @return 完整的日志文件绝对路径
     */
    private PathKey buildPath(LogEvent event) {
        String dir = buildDir(event);
        Path fullDir = Paths.get(rootDirectory, dir);
        String filePath = buildFile(event);

        ensureDirectoryExists(fullDir);

        return new PathKey(fullDir.toString(), filePath);
    }

    /**
     * 从日志事件构建完整的日志文件绝对路径，支持覆盖 key 和 level。
     *
     * 用于在 AT_LEAST 策略下为不同级别生成不同的路径，避免创建多余的 LogEvent 对象。
     *
     * @param event 日志事件（提供 timestamp 等非关键信息）
     * @param key 日志键值（已净化，用于 fileName 模板中的 {key} 占位符）
     * @param level 日志级别（用于 directory/file 模板中的 {level}/{key} fallback）
     * @return 完整的日志文件绝对路径
     */
    private PathKey buildPath(LogEvent event, String key, String level) {
        String dir = buildDir(event, level);
        while (dir.startsWith("/") || dir.startsWith("\\")) {
            dir = dir.substring(1);
        }
        Path fullDir = Paths.get(rootDirectory, dir);
        String filePath = buildFile(event, key);

        ensureDirectoryExists(fullDir);

        return new PathKey(fullDir.toString(), filePath);
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