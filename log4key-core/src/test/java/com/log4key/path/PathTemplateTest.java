/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * PathTemplate 单元测试。
 *
 * 验证路径模板引擎的编译和运行时行为，包括合法的占位符、非法的占位符、
 * 字面量模板、以及各种运行时场景（有 key、无 key、LevelSegment 和 KeySegment 的 fallback 行为）。
 */
public class PathTemplateTest {

    /**
     * 创建测试用 LogEvent 的快捷方法。
     *
     * @param level           日志级别（非空，LogEventBuilder.build() 要求 level 必填）
     * @param key             日志键值（可为 null）
     * @param timestampMillis 时间戳（毫秒）
     * @return 构建完成的 LogEvent
     */
    private static LogEvent createTestEvent(String level, String key, long timestampMillis) {
        LogEventBuilder builder = LogEventBuilder.builder()
                .level(level)
                .loggerName("TestLogger")
                .message("test message");

        if (key != null) {
            builder.key(key);
        }
        builder.timestampMillis(timestampMillis);

        return builder.build();
    }

    /**
     * 测试编译包含所有合法占位符的模板。
     * 预期：segments 数量为 5（LevelSegment, LiteralSegment("/"), DateSegment, LiteralSegment("/"), KeySegment）。
     */
    @Test
    public void testCompileWithAllPlaceholders() {
        PathTemplate template = PathTemplate.compile("{level}/{date}/{key}");
        assertNotNull(template);
        assertEquals("segments count should be 5", 5, template.getSegments().size());
    }

    /**
     * 测试编译纯字面量模板。
     */
    @Test
    public void testCompileWithLiteralOnly() {
        PathTemplate template = PathTemplate.compile("my/path");
        assertNotNull(template);
    }

    /**
     * 测试编译包含非法占位符 {user} 的模板，预期抛出 IllegalArgumentException。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCompileWithInvalidPlaceholder() {
        PathTemplate.compile("{date}/{user}");
    }

    /**
     * 测试编译包含未知占位符 {unknown} 的模板，预期抛出 IllegalArgumentException。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCompileWithUnknownPlaceholder() {
        PathTemplate.compile("{date}/{unknown}");
    }

    /**
     * 测试 {key} 占位符在有 key 值时的运行时行为。
     */
    @Test
    public void testApplyWithKey() {
        PathTemplate template = PathTemplate.compile("{key}");
        LogEvent event = createTestEvent("INFO", "order-1001", System.currentTimeMillis());
        String result = template.apply(event);
        assertEquals("order-1001", result);
    }

    /**
     * 测试 {level} 占位符的运行时行为，验证输出为小写。
     */
    @Test
    public void testApplyWithLevel() {
        PathTemplate template = PathTemplate.compile("{level}");
        LogEvent event = createTestEvent("WARN", null, System.currentTimeMillis());
        String result = template.apply(event);
        assertEquals("warn", result);
    }

    /**
     * 测试 {date} 占位符的运行时行为，验证输出为 yyyyMMdd 格式。
     */
    @Test
    public void testApplyWithDate() {
        PathTemplate template = PathTemplate.compile("{date}");
        long timestamp = 1716153600000L; // 2024-05-20
        LogEvent event = createTestEvent("INFO", null, timestamp);
        String result = template.apply(event);
        assertEquals("20240520", result);
    }

    /**
     * 测试复合模板 {level}/{key} 的运行时行为。
     */
    @Test
    public void testApplyCompositePath() {
        PathTemplate template = PathTemplate.compile("{level}/{key}");
        LogEvent event = createTestEvent("INFO", "test-key", System.currentTimeMillis());
        String result = template.apply(event);
        assertEquals("info/test-key", result);
    }

    /**
     * 测试 {key} 占位符在 key 为 null 时的 fallback 行为：应降级为 level 的小写形式。
     */
    @Test
    public void testApplyWithNullKey() {
        PathTemplate template = PathTemplate.compile("{key}");
        LogEvent event = createTestEvent("INFO", null, System.currentTimeMillis());
        String result = template.apply(event);
        assertEquals("info", result); // fallback to level
    }

    /**
     * 测试 {level} 占位符输出 level 的小写形式。
     *
     * 注：LogEventBuilder.build() 要求 level 非空，因此无法通过正常构造链路触发
     * LevelSegment 的 null-level fallback（输出 "info"）。
     * 该 fallback 路径作为内建防御逻辑存在于 LevelSegment 中，在 Log4KeyLogger
     * 的正常使用场景下始终有 level 值传入。
     */
    @Test
    public void testApplyLevelLowercase() {
        PathTemplate template = PathTemplate.compile("{level}");
        LogEvent event = createTestEvent("DEBUG", null, System.currentTimeMillis());
        String result = template.apply(event);
        assertEquals("debug", result);
    }

    /**
     * 测试空模板的运行时行为：应返回空字符串。
     */
    @Test
    public void testApplyEmptyTemplate() {
        PathTemplate template = PathTemplate.compile("");
        LogEvent event = createTestEvent("INFO", "key1", System.currentTimeMillis());
        String result = template.apply(event);
        assertEquals("", result);
    }
}