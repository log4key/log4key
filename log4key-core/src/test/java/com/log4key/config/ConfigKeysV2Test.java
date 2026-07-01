package com.log4key.config;

import com.log4key.config.key.ConfigKey;
import com.log4key.config.model.Log4KeyConfig;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * V2 架构配置键单元测试。
 *
 * 验证 V2 新增配置键的默认值、ALL_KEYS 注册、Log4KeyConfig 默认配置。
 */
public class ConfigKeysV2Test {

    /**
     * 测试新配置键 BATCH_SIZE_KEY 默认值为 4096
     */
    @Test
    public void testBatchSizeKeyDefault() {
        ConfigKey<Integer> key = ConfigKeys.BATCH_SIZE_KEY;
        assertEquals("BATCH_SIZE_KEY 名称应为 batchSize", "batchSize", key.name());
        assertEquals("BATCH_SIZE_KEY 类型应为 Integer.class", Integer.class, key.type());
        assertEquals("BATCH_SIZE_KEY 默认值应为 4096", Integer.valueOf(4096), key.defaultValue());
    }

    /**
     * 测试新配置键 FLUSH_INTERVAL_KEY 默认值为 1000L
     */
    @Test
    public void testFlushIntervalKeyDefault() {
        ConfigKey<Long> key = ConfigKeys.FLUSH_INTERVAL_KEY;
        assertEquals("FLUSH_INTERVAL_KEY 名称应为 flushInterval", "flushInterval", key.name());
        assertEquals("FLUSH_INTERVAL_KEY 类型应为 Long.class", Long.class, key.type());
        assertEquals("FLUSH_INTERVAL_KEY 默认值应为 1000L", Long.valueOf(1000L), key.defaultValue());
    }

    /**
     * 测试新配置键 HIGH_WATER_MARK_KEY 默认值为 32768
     */
    @Test
    public void testHighWaterMarkKeyDefault() {
        ConfigKey<Integer> key = ConfigKeys.HIGH_WATER_MARK_KEY;
        assertEquals("HIGH_WATER_MARK_KEY 名称应为 highWaterMark", "highWaterMark", key.name());
        assertEquals("HIGH_WATER_MARK_KEY 类型应为 Integer.class", Integer.class, key.type());
        assertEquals("HIGH_WATER_MARK_KEY 默认值应为 32768", Integer.valueOf(32768), key.defaultValue());
    }

    /**
     * 测试新配置键 INITIAL_BUFFER_SIZE_KEY 默认值为 4096
     */
    @Test
    public void testInitialBufferSizeKeyDefault() {
        ConfigKey<Integer> key = ConfigKeys.INITIAL_BUFFER_SIZE_KEY;
        assertEquals("INITIAL_BUFFER_SIZE_KEY 名称应为 initialBufferSize", "initialBufferSize", key.name());
        assertEquals("INITIAL_BUFFER_SIZE_KEY 类型应为 Integer.class", Integer.class, key.type());
        assertEquals("INITIAL_BUFFER_SIZE_KEY 默认值应为 4096", Integer.valueOf(4096), key.defaultValue());
    }

    /**
     * 测试 MAX_OPEN_FILES_KEY 默认值已改为 64
     */
    @Test
    public void testMaxOpenFilesKeyDefault() {
        ConfigKey<Integer> key = ConfigKeys.MAX_OPEN_FILES_KEY;
        assertEquals("MAX_OPEN_FILES_KEY 默认值应为 64", Integer.valueOf(64), key.defaultValue());
    }

    /**
     * 测试 WRITER_IDLE_TIMEOUT_KEY 默认值已改为 1800000L（30分钟）
     */
    @Test
    public void testWriterIdleTimeoutKeyDefault() {
        ConfigKey<Long> key = ConfigKeys.WRITER_IDLE_TIMEOUT_KEY;
        assertEquals("WRITER_IDLE_TIMEOUT_KEY 默认值应为 1800000L", Long.valueOf(1800000L), key.defaultValue());
    }

    /**
     * 测试 ALL_KEYS 包含所有 V2 新配置键
     */
    @Test
    public void testAllKeysContainsV2Keys() {
        assertTrue("ALL_KEYS 应包含 BATCH_SIZE_KEY", ConfigKeys.ALL_KEYS.containsKey("batchSize"));
        assertTrue("ALL_KEYS 应包含 FLUSH_INTERVAL_KEY", ConfigKeys.ALL_KEYS.containsKey("flushInterval"));
        assertTrue("ALL_KEYS 应包含 HIGH_WATER_MARK_KEY", ConfigKeys.ALL_KEYS.containsKey("highWaterMark"));
        assertTrue("ALL_KEYS 应包含 INITIAL_BUFFER_SIZE_KEY", ConfigKeys.ALL_KEYS.containsKey("initialBufferSize"));
        assertTrue("ALL_KEYS 应包含 MAX_OPEN_FILES_KEY", ConfigKeys.ALL_KEYS.containsKey("maxOpenFiles"));
    }

    /**
     * 测试 Log4KeyConfig 中 initDefaultConfig() 包含 V2 新配置默认值
     */
    @Test
    public void testLog4KeyConfigContainsV2Defaults() {
        Log4KeyConfig config = new Log4KeyConfig();

        assertEquals("Log4KeyConfig 中 BATCH_SIZE 默认值应为 4096",
                Integer.valueOf(4096), config.getGlobalConfig(ConfigKeys.BATCH_SIZE_KEY));
        assertEquals("Log4KeyConfig 中 FLUSH_INTERVAL 默认值应为 1000L",
                Long.valueOf(1000L), config.getGlobalConfig(ConfigKeys.FLUSH_INTERVAL_KEY));
        assertEquals("Log4KeyConfig 中 HIGH_WATER_MARK 默认值应为 32768",
                Integer.valueOf(32768), config.getGlobalConfig(ConfigKeys.HIGH_WATER_MARK_KEY));
        assertEquals("Log4KeyConfig 中 INITIAL_BUFFER_SIZE 默认值应为 4096",
                Integer.valueOf(4096), config.getGlobalConfig(ConfigKeys.INITIAL_BUFFER_SIZE_KEY));
    }
}