package com.log4key.storage;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.api.storage.LogQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 本地文件存储提供者测试类
 */
public class LocalFileStorageProviderTest {
    
    private LocalFileStorageProvider storageProvider;
    private Map<String, Object> config;
    private String testLogPath;
    
    @Before
    public void setUp() {
        // 初始化本地文件存储提供者
        storageProvider = new LocalFileStorageProvider();
        
        // 创建测试配置
        testLogPath = "target/test-logs-local";
        config = new HashMap<>();
        config.put("logPath", testLogPath);
        config.put("formatterName", "text");
    }
    
    @After
    public void tearDown() {
        // 关闭存储提供者
        storageProvider.close();
        
        // 删除测试日志文件
        try {
            Files.deleteIfExists(Paths.get(testLogPath + "/local-storage.log"));
            Files.deleteIfExists(Paths.get(testLogPath));
        } catch (IOException e) {
            // 忽略删除异常
        }
    }
    
    /**
     * 测试本地文件存储提供者初始化
     */
    @Test
    public void testInitialize() {
        // 初始化存储提供者
        storageProvider.initialize(config);
        
        // 验证日志文件已创建
        File logFile = new File(testLogPath + "/local-storage.log");
        assertTrue(logFile.exists());
    }
    
    /**
     * 测试存储单条日志事件
     */
    @Test
    public void testStore() {
        // 初始化存储提供者
        storageProvider.initialize(config);
        
        // 创建测试日志事件
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("LocalFileTestLogger")
                .message("Single log test message")
                .timestampMillis(System.currentTimeMillis())
                .nodeId("test-node")
                .build();
        
        // 存储日志事件
        storageProvider.store(event);
        
        // 验证日志文件大小增加
        File logFile = new File(testLogPath + "/local-storage.log");
        assertTrue(logFile.length() > 0);
    }
    
    /**
     * 测试批量存储日志事件
     */
    @Test
    public void testStoreBatch() {
        // 初始化存储提供者
        storageProvider.initialize(config);
        
        // 创建测试日志事件列表
        int eventCount = 10;
        List<LogEvent> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            LogEvent event = LogEventBuilder.builder()
                    .level("INFO")
                    .loggerName("LocalFileTestLogger")
                    .message("Batch log test message " + i)
                    .timestampMillis(System.currentTimeMillis())
                    .nodeId("test-node")
                    .build();
            events.add(event);
        }
        
        // 批量存储日志事件
        storageProvider.storeBatch(events);
        
        // 验证日志文件大小增加
        File logFile = new File(testLogPath + "/local-storage.log");
        assertTrue(logFile.length() > 0);
    }
    
    /**
     * 测试查询日志事件（本地文件存储暂不支持查询，应返回空列表）
     */
    @Test
    public void testQuery() {
        // 初始化存储提供者
        storageProvider.initialize(config);
        
        // 创建查询条件
        LogQuery query = new LogQuery();
        query.setMessageKeyword("test");
        
        // 查询日志事件
        List<LogEvent> results = storageProvider.query(query);
        
        // 验证查询结果为空列表
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
    
    /**
     * 测试关闭存储提供者
     */
    @Test
    public void testClose() {
        // 初始化并关闭存储提供者
        storageProvider.initialize(config);
        storageProvider.close();
    }
    
    /**
     * 测试获取存储提供者名称
     */
    @Test
    public void testGetName() {
        // 验证存储提供者名称
        assertEquals("localFile", storageProvider.getName());
    }
    
    /**
     * 测试获取存储提供者优先级
     */
    @Test
    public void testGetPriority() {
        // 验证存储提供者优先级
        assertEquals(0, storageProvider.getPriority());
    }
}
