/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.router;

import com.log4key.api.ILogKey;
import com.log4key.api.router.ShardingStrategy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Time-based sharding strategy.
 * Reserved for future extension.
 *
 * 时间分片策略。
 * 预留扩展点，当前版本未启用。
 */
public class TimeShardingStrategy implements ShardingStrategy {

    /**
     * 分片参数
     */
    private final Map<String, Object> parameters = new ConcurrentHashMap<>();

    /**
     * 时间格式映射
     */
    private final Map<String, SimpleDateFormat> formatters = new ConcurrentHashMap<>();

    /**
     * 默认时间粒度
     */
    private static final String DEFAULT_GRANULARITY = "day";

    /**
     * 时间格式映射
     */
    private static final Map<String, String> DATE_FORMATS = new ConcurrentHashMap<>();

    static {
        DATE_FORMATS.put("hour", "yyyy-MM-dd-HH");
        DATE_FORMATS.put("day", "yyyy-MM-dd");
        DATE_FORMATS.put("week", "yyyy-ww");
        DATE_FORMATS.put("month", "yyyy-MM");
        DATE_FORMATS.put("year", "yyyy");
    }

    @Override
    public String getName() {
        return "timeSharding";
    }

    @Override
    public String getShardId(ILogKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Log key cannot be null");
        }

        // 获取时间粒度参数
        String granularity = (String) getParameterOrDefault("granularity", DEFAULT_GRANULARITY);

        // 获取对应的时间格式化器
        SimpleDateFormat formatter = formatters.computeIfAbsent(granularity, g -> {
            String format = DATE_FORMATS.getOrDefault(g, DATE_FORMATS.get(DEFAULT_GRANULARITY));
            return new SimpleDateFormat(format);
        });

        // 生成时间分片标识
        String timeStr = formatter.format(new Date());

        // 可选：结合哈希分片以防止单个时间分片过大
        if (Boolean.TRUE.equals(getParameterOrDefault("combineWithHash", false))) {
            int bucketCount = (int) getParameterOrDefault("hashBucketCount", 8);
            int hashCode = Math.abs(key.hashCode());
            int bucketIndex = hashCode % bucketCount;
            return timeStr + "/bucket_" + bucketIndex;
        }

        return timeStr;
    }

    @Override
    public void setParameter(String paramName, Object paramValue) {
        if (paramName == null || paramName.isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        parameters.put(paramName, paramValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            throw new IllegalArgumentException("Parameter name cannot be null or empty");
        }
        return (T) parameters.get(paramName);
    }

    /**
     * 获取参数，如果不存在则返回默认值
     *
     * @param paramName 参数名称
     * @param defaultValue 默认值
     * @return 参数值
     */
    private Object getParameterOrDefault(String paramName, Object defaultValue) {
        Object value = getParameter(paramName);
        return value != null ? value : defaultValue;
    }
}