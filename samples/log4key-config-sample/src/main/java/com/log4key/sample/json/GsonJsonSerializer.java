/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.sample.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.log4key.spi.JsonSerializer;


import java.util.Date;
import java.util.Map;

/**
 * Gson-based JsonSerializer implementation for demonstration.
 *
 * 基于Gson的JsonSerializer实现示例。
 * 此实现展示了如何将自定义JSON库集成到Log4Key中。
 */
public class GsonJsonSerializer implements JsonSerializer {

    /** Gson序列化实例 */
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Serializes the log map to a JSON string using Gson.
     *
     * 使用Gson将日志Map序列化为JSON字符串。
     *
     * @param map the log content map / 日志内容Map
     * @return the serialized JSON string / 序列化后的JSON字符串
     */
    @Override
    public String serialize(Map<String, Object> map) {
        if (map.containsKey("timestamp")) {
            map.put("time", new Date((Long) map.get("timestamp")));
        }
        return gson.toJson(map);
    }

}
