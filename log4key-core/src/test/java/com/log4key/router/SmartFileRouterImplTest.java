/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.router;

import com.log4key.api.AbstractLogKey;
import com.log4key.api.DirectoryTemplateProvider;
import com.log4key.api.ILogKey;
import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.path.PathKey;
import com.log4key.path.PathTemplate;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SmartFileRouterImpl 目录覆盖用例测试。
 *
 * 验证 DirectoryTemplateProvider 主键可完全取代 appender.directory，其余主键不受影响。
 */
public class SmartFileRouterImplTest {

    /** 固定时间戳，避免日期断言依赖系统时钟 */
    private static final long TIMESTAMP = 1759257600000L; // 2026-10-01

    /** 测试用：带固定目录模板的业务主键 */
    private static class PlayerFundKey extends AbstractLogKey implements DirectoryTemplateProvider {
        PlayerFundKey(String key) {
            super(key);
        }

        @Override
        public String getDirectoryTemplate() {
            return "/playerFund/{date}";
        }
    }

    /** 测试用：返回 null，表示不覆盖 */
    private static class NullDirKey extends AbstractLogKey implements DirectoryTemplateProvider {
        NullDirKey(String key) {
            super(key);
        }

        @Override
        public String getDirectoryTemplate() {
            return null;
        }
    }

    /** 测试用：返回包含全部占位符的模板 */
    private static class FullPlaceholderKey extends AbstractLogKey implements DirectoryTemplateProvider {
        FullPlaceholderKey(String key) {
            super(key);
        }

        @Override
        public String getDirectoryTemplate() {
            return "{level}/{date}/{key}";
        }
    }

    private SmartFileRouterImpl router;

    @Before
    public void setUp() {
        router = new SmartFileRouterImpl();
        router.setRootDirectory("./logs");
        // appender.directory 模板：与默认一致，便于验证覆盖
        router.setDirectoryTemplate(PathTemplate.compile("/{level}/{date}"));
        router.setFileNameTemplate(PathTemplate.compile("{key}.log"));
        router.initialize();
    }

    private String expectedDate() {
        return new SimpleDateFormat("yyyyMMdd").format(new java.util.Date(TIMESTAMP));
    }

    private LogEvent event(ILogKey logKey) {
        return LogEventBuilder.builder()
                .logKey(logKey)
                .level("info")
                .loggerName("test")
                .message("msg")
                .timestampMillis(TIMESTAMP)
                .build();
    }

    @Test
    public void providerTemplateOverridesAppenderDirectory() {
        PathKey path = router.determineLogFilePath(event(new PlayerFundKey("user-1001")));
        String expectedDir = Paths.get("./logs", "playerFund", expectedDate()).toString();
        assertEquals(expectedDir, path.getDir());
        assertEquals("user-1001.log", path.getFile());
    }

    @Test
    public void providerTemplateExpandsAllPlaceholders() {
        PathKey path = router.determineLogFilePath(event(new FullPlaceholderKey("order-1")));
        String expectedDir = Paths.get("./logs", "info", expectedDate(), "order-1").toString();
        assertEquals(expectedDir, path.getDir());
    }

    @Test
    public void providerNullFallsBackToAppenderDirectory() {
        PathKey path = router.determineLogFilePath(event(new NullDirKey("user-1001")));
        String expectedDir = Paths.get("./logs", "info", expectedDate()).toString();
        assertEquals(expectedDir, path.getDir());
    }

    @Test
    public void directoryTemplateKeyIgnoresNodeIdSplit() {
        // 两个相同 key 的 PlayerFundKey 应路由到同一目录
        PathKey p1 = router.determineLogFilePath(event(new PlayerFundKey("user-1001")));
        PathKey p2 = router.determineLogFilePath(event(new PlayerFundKey("user-1001")));
        assertEquals(p1, p2);
    }

    @Test
    public void levelPolicyMultiPathUsesProviderTemplate() {
        router.setOutputLevelPolicy(com.log4key.config.model.OutputLevelPolicy.EXACT);
        PathKey path = router.determineLogFilePath(event(new PlayerFundKey("user-1001")));
        String expectedDir = Paths.get("./logs", "playerFund", expectedDate()).toString();
        assertEquals(expectedDir, path.getDir());
    }
}