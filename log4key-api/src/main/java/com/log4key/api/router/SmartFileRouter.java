/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.router;

import com.log4key.api.ILogKey;
import com.log4key.api.LogEvent;

/**
 * Smart file router interface that routes logs to different files based on log key and supports sharding storage.
 *
 * 智能文件路由器接口
 * 根据日志主键将日志路由到不同的文件中，支持分片存储
 */
public interface SmartFileRouter {
    
    /**
     * 根据日志主键确定日志文件路径
     * 
     * @param key 日志主键
     * @return 日志文件路径
     */
    String determineLogFilePath(ILogKey key);
    
    /**
     * 根据日志事件确定日志文件路径
     * 
     * @param event 日志事件
     * @return 日志文件路径
     */
    String determineLogFilePath(LogEvent event);

    /**
     * 根据日志事件确定所有需要写入的日志文件路径
     * 默认实现仅返回单个路径
     * 
     * @param event 日志事件
     * @return 日志文件路径列表
     */
    default java.util.List<String> determineLogFilePaths(LogEvent event) {
        String path = determineLogFilePath(event);
        if (path == null) {
            return java.util.Collections.emptyList();
        }
        return java.util.Collections.singletonList(path);
    }
    
    /**
     * 设置基础日志目录
     * 
     * @param baseDirectory 基础日志目录
     */
    void setBaseDirectory(String baseDirectory);
    
    /**
     * 初始化路由器
     */
    void initialize();
    
    /**
     * 关闭路由器，释放资源
     */
    void shutdown();
    
    /**
     * 获取当前的分片策略
     * 
     * @return 分片策略实例
     */
    ShardingStrategy getShardingStrategy();
    
    /**
     * 设置分片策略
     * 
     * @param strategy 分片策略
     */
    void setShardingStrategy(ShardingStrategy strategy);
}