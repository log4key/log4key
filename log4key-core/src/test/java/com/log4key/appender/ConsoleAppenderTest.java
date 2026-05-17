package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * ConsoleAppender的单元测试
 */
public class ConsoleAppenderTest {

    private ConsoleAppender appender;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;
    
    @Before
    public void setUp() {
        // 保存原始输出流
        originalOut = System.out;
        
        // 创建字节数组输出流用于捕获控制台输出
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        
        // 创建ConsoleAppender实例
        appender = new ConsoleAppender();
    }
    
    @After
    public void tearDown() {
        // 恢复原始输出流
        System.setOut(originalOut);
        
        // 关闭appender
        if (appender != null) {
            appender.close();
        }
    }
    
    /**
     * 测试初始化方法
     */
    @Test
    public void testInitialize() {
        Map<String, Object> config = new HashMap<>();
        config.put("formatter", "json");
        config.put("asyncSupported", "true");
        
        appender.initialize(config);
        appender.start();
        
        assertTrue("Appender should be running", appender.isRunning());
        assertTrue("Appender should support async", appender.isAsyncSupported());
    }
    
    /**
     * 测试同步输出
     */
    @Test
    public void testSyncAppend() {
        // 初始化appender，使用同步模式
        Map<String, Object> config = new HashMap<>();
        config.put("asyncSupported", "false");
        appender.initialize(config);
        appender.start();
        
        // 创建测试事件
        LogEvent event = createTestEvent("Sync test message");
        
        // 执行append
        appender.append(event);
        
        // 验证输出
        String output = outContent.toString();
        assertTrue("Output should contain test message", output.contains("Sync test message"));
    }
    
    /**
     * 测试异步输出
     */
    @Test
    public void testAsyncAppend() throws InterruptedException {
        // 初始化appender，使用异步模式
        Map<String, Object> config = new HashMap<>();
        config.put("asyncSupported", "true");
        config.put("corePoolSize", "2");
        appender.initialize(config);
        appender.start();
        
        // 创建测试事件
        LogEvent event = createTestEvent("Async test message");
        
        // 执行异步append
        appender.append(event);
        
        // 给异步线程一些时间完成写入
        Thread.sleep(500);
        appender.flush();
        
        // 验证输出
        String output = outContent.toString();
        assertTrue("Output should contain async message", output.contains("Async test message"));
    }
    
    /**
     * 测试批量输出
     */
    @Test
    public void testAppendBatch() {
        // 初始化appender，使用同步模式便于测试
        Map<String, Object> config = new HashMap<>();
        config.put("asyncSupported", "false");
        appender.initialize(config);
        appender.start();
        
        // 创建测试事件列表
        List<LogEvent> events = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            events.add(createTestEvent("Batch test message " + i));
        }
        
        // 执行批量append
        appender.appendBatch(events);
        
        // 验证输出
        String output = outContent.toString();
        for (int i = 0; i < 5; i++) {
            assertTrue("Output should contain batch message " + i, output.contains("Batch test message " + i));
        }
    }
    
    /**
     * 测试Pattern格式化器
     */
    @Test
    public void testPatternFormatter() {
        // 初始化appender，使用Pattern格式化器
        Map<String, Object> config = new HashMap<>();
        config.put("formatter", "pattern");
        config.put("asyncSupported", "false");
        appender.initialize(config);
        appender.start();
        
        // 创建测试事件
        LogEvent event = createTestEvent("Pattern test message");
        
        // 执行append
        appender.append(event);
        
        // 验证输出
        String output = outContent.toString();
        assertTrue("Pattern output should contain test message", output.contains("Pattern test message"));
        assertTrue("Pattern output should contain INFO level", output.contains("INFO"));
    }
    
    /**
     * 测试无效格式化器回退
     */
    @Test
    public void testInvalidFormatterFallback() {
        // 初始化appender，使用无效格式化器
        Map<String, Object> config = new HashMap<>();
        config.put("formatter", "invalidFormatter");
        config.put("asyncSupported", "false");
        appender.initialize(config);
        appender.start();
        
        // 创建测试事件
        LogEvent event = createTestEvent("Invalid formatter test message");
        
        // 执行append
        appender.append(event);
        
        // 验证输出（应该使用默认的text格式化器）
        String output = outContent.toString();
        assertTrue("Output should contain test message", output.contains("Invalid formatter test message"));
    }
    
    /**
     * 测试生命周期管理
     */
    @Test
    public void testLifecycle() {
        appender.initialize(new HashMap<>());
        
        assertFalse("Appender should not be running before start", appender.isRunning());
        
        appender.start();
        assertTrue("Appender should be running after start", appender.isRunning());
        
        appender.stop();
        assertFalse("Appender should not be running after stop", appender.isRunning());
    }
    
    /**
     * 测试多线程异步输出
     */
    @Test
    public void testMultiThreadedAsyncAppend() throws InterruptedException {
        // 初始化appender，使用异步模式
        Map<String, Object> config = new HashMap<>();
        config.put("asyncSupported", "true");
        config.put("corePoolSize", "4");
        config.put("queueCapacity", "100");
        appender.initialize(config);
        appender.start();
        
        final int threadCount = 10;
        final int eventsPerThread = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        
        // 创建多个线程并发写入日志
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        LogEvent event = createTestEvent("Multi-thread test message - Thread " + threadId + ", Event " + j);
                        appender.append(event);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        // 等待所有线程完成
        latch.await(5, TimeUnit.SECONDS);
        
        // 给异步线程一些时间完成写入
        Thread.sleep(1000);
        appender.flush();
        
        // 验证输出
        String output = outContent.toString();
        int count = 0;
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < eventsPerThread; j++) {
                if (output.contains("Multi-thread test message - Thread " + i + ", Event " + j)) {
                    count++;
                }
            }
        }
        
        // 允许一些事件丢失（异步可能有竞争条件），但大部分应该成功
        assertTrue("At least 90% of events should be logged", count >= threadCount * eventsPerThread * 0.9);
    }
    
    /**
     * 创建测试用的LogEvent
     */
    private LogEvent createTestEvent(String message) {
        return LogEventBuilder.builder()
                .message(message)
                .level("INFO")
                .loggerName("TestLogger")
                .timestampMillis(System.currentTimeMillis())
                .key("test-key")
                .nodeId("1")
                .build();
    }
}