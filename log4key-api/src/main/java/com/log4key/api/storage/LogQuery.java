/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.storage;

import java.util.Date;
import java.util.Map;

/**
 * Log query condition class for specifying query parameters.
 *
 * 日志查询条件类，用于指定查询参数。
 */
public class LogQuery {
    /**
     * 日志级别
     */
    private String level;

    /**
     * 日志名称
     */
    private String loggerName;

    /**
     * 消息关键词
     */
    private String messageKeyword;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 键值
     */
    private String key;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * MDC过滤器
     */
    private Map<String, Object> mdcFilters;

    /**
     * 页码
     */
    private int page = 0;

    /**
     * 每页大小
     */
    private int size = 100;

    /**
     * Gets the log level filter.
     *
     * 获取日志级别过滤器。
     *
     * @return log level / 日志级别
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the log level filter.
     *
     * 设置日志级别过滤器。
     *
     * @param level log level / 日志级别
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * Gets the logger name filter.
     *
     * 获取日志名称过滤器。
     *
     * @return logger name / 日志名称
     */
    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Sets the logger name filter.
     *
     * 设置日志名称过滤器。
     *
     * @param loggerName logger name / 日志名称
     */
    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    /**
     * Gets the message keyword filter.
     *
     * 获取消息关键词过滤器。
     *
     * @return message keyword / 消息关键词
     */
    public String getMessageKeyword() {
        return messageKeyword;
    }

    /**
     * Sets the message keyword filter.
     *
     * 设置消息关键词过滤器。
     *
     * @param messageKeyword message keyword / 消息关键词
     */
    public void setMessageKeyword(String messageKeyword) {
        this.messageKeyword = messageKeyword;
    }

    /**
     * Gets the start time filter.
     *
     * 获取开始时间过滤器。
     *
     * @return start time / 开始时间
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time filter.
     *
     * 设置开始时间过滤器。
     *
     * @param startTime start time / 开始时间
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the end time filter.
     *
     * 获取结束时间过滤器。
     *
     * @return end time / 结束时间
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Sets the end time filter.
     *
     * 设置结束时间过滤器。
     *
     * @param endTime end time / 结束时间
     */
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the node ID filter.
     *
     * 获取节点ID过滤器。
     *
     * @return node ID / 节点ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the node ID filter.
     *
     * 设置节点ID过滤器。
     *
     * @param nodeId node ID / 节点ID
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Gets the page number.
     *
     * 获取页码。
     *
     * @return page number / 页码
     */
    public int getPage() {
        return page;
    }

    /**
     * Sets the page number.
     *
     * 设置页码。
     *
     * @param page page number / 页码
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Gets the page size.
     *
     * 获取每页大小。
     *
     * @return page size / 每页大小
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the page size.
     *
     * 设置每页大小。
     *
     * @param size page size / 每页大小
     */
    public void setSize(int size) {
        this.size = size;
    }
}
