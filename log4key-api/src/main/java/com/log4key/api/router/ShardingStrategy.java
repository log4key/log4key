/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api.router;

import com.log4key.api.ILogKey;

/**
 * Sharding strategy interface.
 *
 * 分片策略接口。
 * 定义日志文件分片的规则，支持哈希分片、时间分片等多种策略。
 */
public interface ShardingStrategy {

    /**
     * Gets the strategy name.
     *
     * 获取策略名称。
     *
     * @return strategy name / 策略名称
     */
    String getName();

    /**
     * Generates shard identifier based on the log key.
     *
     * 根据日志主键生成分片标识。
     *
     * @param key log key / 日志主键
     * @return shard identifier string / 分片标识字符串
     */
    String getShardId(ILogKey key);

    /**
     * Sets the shard parameter.
     *
     * 设置分片参数。
     *
     * @param paramName parameter name / 参数名称
     * @param paramValue parameter value / 参数值
     */
    void setParameter(String paramName, Object paramValue);

    /**
     * Gets the shard parameter.
     *
     * 获取分片参数。
     *
     * @param paramName parameter name / 参数名称
     * @param <T> parameter type / 参数类型
     * @return parameter value / 参数值
     */
    <T> T getParameter(String paramName);
}
