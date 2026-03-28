/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.storage;

import com.log4key.api.LogEvent;
import com.log4key.api.storage.LogQuery;
import com.log4key.api.storage.StorageProvider;
import com.log4key.io.LogFileWriter;
import com.log4key.formatter.LogFormatterManager;
import com.log4key.internal.InternalLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Local file storage provider implementation.
 *
 * 本地文件存储策略实现。
 */
public class LocalFileStorageProvider implements StorageProvider {
    private static final InternalLogger logger = InternalLogger.getLogger(LocalFileStorageProvider.class);
    
    // 默认配置常量
    private static final String DEFAULT_LOG_PATH = "target/test-logs";
    private static final String DEFAULT_FORMATTER_NAME = "text";
    
    /**
     * 日志文件写入器
     */
    private LogFileWriter logFileWriter;

    /**
     * 格式化管理器
     */
    private LogFormatterManager formatterManager;

    /**
     * 是否已初始化
     */
    private volatile boolean initialized = false;

    /**
     * 日志路径
     */
    private String logPath = DEFAULT_LOG_PATH;

    /**
     * 格式化器名称
     */
    private String formatterName = DEFAULT_FORMATTER_NAME;
    
    @Override
    public void initialize(Map<String, Object> config) {
        if (initialized) {
            logger.warn("LocalFileStorageProvider has already been initialized");
            return;
        }
        
        try {
            // 从配置中获取日志路径
            if (config.containsKey("logPath")) {
                logPath = config.get("logPath").toString();
            }
            
            // 从配置中获取格式化器名称
            if (config.containsKey("formatterName")) {
                formatterName = config.get("formatterName").toString();
            }
            
            // 初始化日志文件写入器
            String logFilePath = logPath + "/local-storage.log";
            logFileWriter = new LogFileWriter(logFilePath);
            
            // 获取格式化管理器实例（单例模式）
            formatterManager = LogFormatterManager.getInstance();
            
            initialized = true;
            logger.info("LocalFileStorageProvider initialized successfully with logPath: {}", logFilePath);
        } catch (IOException e) {
            logger.warn("Failed to initialize LocalFileStorageProvider", e);
            throw new RuntimeException("Failed to initialize LocalFileStorageProvider", e);
        }
    }
    
    @Override
    public void store(LogEvent event) {
        if (!initialized) {
            throw new IllegalStateException("LocalFileStorageProvider has not been initialized");
        }
        
        try {
            // 格式化日志事件
            String formattedLog = formatterManager.format(event, formatterName);
            // 写入文件
            logFileWriter.write(formattedLog + "\n");
            // 立即刷新到磁盘，确保测试中能检测到文件变化
            logFileWriter.flush();
        } catch (IOException e) {
            // 处理IOException，记录日志并返回，避免整个应用崩溃
            logger.warn("Failed to store log event to file: {}", e.getMessage(), e);
            // 考虑实现重试机制，这里简单返回
        } catch (Exception e) {
            logger.warn("Failed to store log event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store log event", e);
        }
    }
    
    @Override
    public void storeBatch(List<LogEvent> events) {
        if (!initialized) {
            throw new IllegalStateException("LocalFileStorageProvider has not been initialized");
        }
        
        if (events == null || events.isEmpty()) {
            return;
        }
        
        try {
            List<String> formattedLogs = new ArrayList<>(events.size());
            for (LogEvent event : events) {
                String formattedLog = formatterManager.format(event, formatterName);
                formattedLogs.add(formattedLog + "\n");
            }
            logFileWriter.writeBatch(formattedLogs);
            // 立即刷新到磁盘，确保测试中能检测到文件变化
            logFileWriter.flush();
        } catch (IOException e) {
            // 处理IOException，记录日志并返回，避免整个应用崩溃
            logger.warn("Failed to store batch log events to file: {}", e.getMessage(), e);
            // 考虑实现重试机制，这里简单返回
        } catch (Exception e) {
            logger.warn("Failed to store batch log events: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store batch log events", e);
        }
    }
    
    @Override
    public List<LogEvent> query(LogQuery query) {
        // 本地文件存储暂不支持查询功能，返回空列表
        // 实际实现中可以添加文件扫描和解析逻辑
        return Collections.emptyList();
    }
    
    @Override
    public void close() {
        if (initialized) {
            try {
                if (logFileWriter != null) {
                    logFileWriter.close();
                }
                initialized = false;
                logger.info("LocalFileStorageProvider closed successfully");
            } catch (Exception e) {
                logger.warn("Failed to close LocalFileStorageProvider", e);
            }
        }
    }
    
    @Override
    public String getName() {
        return "localFile";
    }
    
    @Override
    public int getPriority() {
        return 0; // 默认优先级，值越小优先级越高
    }
}
