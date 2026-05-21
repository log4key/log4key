/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.router;

import com.log4key.api.LogEvent;
import com.log4key.path.PathKey;

/**
 * Smart file router interface that routes logs to different files based on log key.
 *
 * 智能文件路由器接口
 * 根据日志主键将日志路由到不同的文件中
 */
public interface SmartFileRouter {
    
    /**
     * 根据日志事件确定日志文件路径
     * 
     * @param event 日志事件
     * @return 日志文件路径键
     */
    PathKey determineLogFilePath(LogEvent event);

    /**
     * 根据日志事件确定所有需要写入的日志文件路径
     * 默认实现仅返回单个路径
     * 
     * @param event 日志事件
     * @return 日志文件路径列表
     */
    default java.util.List<PathKey> determineLogFilePaths(LogEvent event) {
        return java.util.Collections.emptyList();
    }
    
    /**
     * 初始化路由器
     */
    void initialize();
    
    /**
     * 关闭路由器，释放资源
     */
    void shutdown();
}