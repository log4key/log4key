/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config.converter;

import com.log4key.config.key.ConfigKey;
import com.log4key.internal.InternalLogger;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration converter utilities.
 *
 * 配置转换器工具类。
 */
public final class ConfigConverterUtils {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(ConfigConverterUtils.class);

    /**
     * 私有构造函数，防止实例化
     */
    private ConfigConverterUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========================== public static 方法 ==========================

    /**
     * 将旧版Map<String, Object>转换为新版Map<ConfigKey<?>, Object>
     * 注意：此方法仅转换已知的ConfigKey，未知的键将被忽略
     * @param oldMap 旧版配置Map
     * @return 新版配置Map
     */
    public static Map<ConfigKey<?>, Object> fromLegacyMap(Map<String, Object> oldMap) {
        if (oldMap == null || oldMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<ConfigKey<?>, Object> newMap = new HashMap<>();

        // 遍历所有已知的ConfigKey
        for (ConfigKey<?> key : com.log4key.config.ConfigKeys.ALL_KEYS.values()) {
            String keyName = key.name();
            if (oldMap.containsKey(keyName)) {
                Object value = oldMap.get(keyName);
                // 尝试进行类型转换
                if (value != null && key.type().isInstance(value)) {
                    newMap.put(key, value);
                } else if (value != null) {
                    // 类型不匹配，尝试字符串转换
                    try {
                        ConfigConverter converter = new PropertiesConfigConverter();
                        Object converted = converter.convert(String.valueOf(value), key.type());
                        newMap.put(key, converted);
                    } catch (IllegalArgumentException e) {
                            // 转换失败，跳过此配置项
                            logger.warn("Cannot convert value '" + value +
                                      "' for key '" + keyName + "' to type " +
                                      key.type().getSimpleName());
                        }
                }
            }
        }

        return newMap;
    }
}
