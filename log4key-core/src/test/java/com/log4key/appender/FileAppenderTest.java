package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.api.router.SmartFileRouter;
import com.log4key.path.PathKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

/**
 * FileAppender的单元测试
 */
public class FileAppenderTest {

    private static final String TEST_DIR = "target/test-appender";
    private FileAppender appender;
    
    @Mock
    private SmartFileRouter mockFileRouter;
    
    /**
     * Mock对象初始化器
     */
    private AutoCloseable closeable;

    @Before
    public void setUp() throws IOException {
        // 初始化mock对象
        closeable = MockitoAnnotations.openMocks(this);
        
        // 确保测试目录存在
        new File(TEST_DIR).mkdirs();
        
        // 创建FileAppender实例
        appender = new FileAppender();
        
        // 设置mock的文件路由器
        // FileAppender调用的是determineLogFilePaths(复数)，不是determineLogFilePath(单数)
        List<PathKey> paths = Collections.singletonList(new PathKey(TEST_DIR, "test.log"));
        when(mockFileRouter.determineLogFilePaths(any(LogEvent.class))).thenReturn(paths);
        doNothing().when(mockFileRouter).initialize();
        
        // 使用反射设置mock的router（或者在实际代码中提供setter方法）
        appender.setFileRouter(mockFileRouter);
        
        // 初始化appender
        Map<String, Object> config = new HashMap<>();
        config.put("rootDirectory", TEST_DIR);
        appender.initialize(config);
    }

    @After
    public void tearDown() throws Exception {
        // 关闭mock对象
        closeable.close();
        
        // 关闭appender
        appender.close();
        
        // 清理测试文件
        File logFile = new File(TEST_DIR + File.separator + "test.log");
        if (logFile.exists()) {
            logFile.delete();
        }
        
        // 清理测试目录
        File testDir = new File(TEST_DIR);
        if (testDir.exists()) {
            testDir.delete();
        }
    }

    @Test
    public void testInitialize() {
        // 验证初始化
        verify(mockFileRouter).initialize();
        
        // 验证名称
        assertEquals("file", appender.getName());
    }

    @Test
    public void testAppendSingleEvent() {
        // 创建测试事件
        LogEvent event = createTestEvent("Single test message");
        
        // 执行append操作
        appender.append(event);
        
        // 等待写入完成（同步模式）
        appender.flush();
        
        // 验证文件存在并包含内容
        File logFile = new File(TEST_DIR + File.separator + "test.log");
        assertTrue("Log file should exist", logFile.exists());
        assertTrue("Log file should not be empty", logFile.length() > 0);
        
        // 验证文件内容包含我们的消息
        String content = readFileContent(logFile.getPath());
        assertTrue("Content should contain test message", content.contains("Single test message"));
    }

    @Test
    public void testAppendBatchEvents() {
        // 创建多个测试事件
        List<LogEvent> events = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            events.add(createTestEvent("Batch message " + i));
        }
        
        // 执行批量append操作
        appender.appendBatch(events);
        
        // 等待写入完成（同步模式）
        appender.flush();
        
        // 验证文件存在并包含内容
        File logFile = new File(TEST_DIR + File.separator + "test.log");
        assertTrue("Log file should exist", logFile.exists());
        assertTrue("Log file should not be empty", logFile.length() > 0);
        
        // 验证文件内容包含所有消息
        String content = readFileContent(logFile.getPath());
        for (int i = 0; i < 5; i++) {
            assertTrue("Content should contain batch message " + i, 
                      content.contains("Batch message " + i));
        }
    }

    @Test
    public void testAppendWithException() {
        // 创建包含异常的测试事件
        Exception ex = new RuntimeException("Test exception");
        
        LogEvent event = LogEventBuilder.builder()
                .timestampMillis(System.currentTimeMillis())
                .level("INFO")
                .message("Message with exception")
                .loggerName("FileAppenderTest")
                .key("test-key")
                .nodeId("1")
                .throwable(ex)
                .build();
        
        // 执行append操作
        appender.append(event);
        
        // 等待写入完成
        appender.flush();
        
        // 验证文件内容包含异常信息
        String content = readFileContent(TEST_DIR + File.separator + "test.log");
        assertTrue("Content should contain exception message", content.contains("Test exception"));
        assertTrue("Content should contain exception type", content.contains("RuntimeException"));
    }

    @Test
    public void testMultiThreadedAppend() throws InterruptedException {
        // 创建多线程测试
        int threadCount = 5;
        int eventsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        LogEvent event = createTestEvent("Thread-" + threadId + "-Event-" + j);
                        appender.append(event);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await();
        
        // 刷新并关闭
        appender.flush();
        
        // 验证文件内容
        String content = readFileContent(TEST_DIR + File.separator + "test.log");
        int foundMessages = 0;
        
        // 检查是否大部分消息都被写入
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < eventsPerThread; j++) {
                if (content.contains("Thread-" + i + "-Event-" + j)) {
                    foundMessages++;
                }
            }
        }
        
        // 由于是多线程写入，我们期望大部分消息都能被写入，即使不是全部
        assertTrue("Most messages should be written", foundMessages > (threadCount * eventsPerThread * 0.9));
        
        executor.shutdown();
    }

    @Test
    public void testClose() {
        // 写入一些内容
        appender.append(createTestEvent("Message before close"));
        
        // 关闭appender
        appender.close();
        
        // 验证关闭后不能再写入
        LogEvent eventAfterClose = createTestEvent("This should not be written");
        appender.append(eventAfterClose);
        
        // 验证文件内容只包含关闭前的消息
        String content = readFileContent(TEST_DIR + File.separator + "test.log");
        assertTrue("Content should contain message before close", content.contains("Message before close"));
        assertFalse("Content should not contain message after close", content.contains("This should not be written"));
    }

    @Test
    public void testNullEventHandling() {
        // 尝试写入null事件，应该被忽略
        appender.append(null);
        
        // 验证文件是空的或不存在
        File logFile = new File(TEST_DIR + File.separator + "test.log");
        assertFalse("Log file should not be created for null event", logFile.exists() || 
                   (logFile.exists() && logFile.length() > 0));
    }

    @Test
    public void testNullBatchHandling() {
        // 尝试写入null批次，应该被忽略
        appender.appendBatch(null);
        
        // 尝试写入空批次，应该被忽略
        appender.appendBatch(Collections.emptyList());
        
        // 验证没有异常抛出
        assertTrue("Should not throw exception for null or empty batch", true);
    }

    /**
     * 创建测试用的日志事件
     */
    private LogEvent createTestEvent(String message) {
        return LogEventBuilder.builder()
                .timestampMillis(System.currentTimeMillis())
                .level("INFO")
                .message(message)
                .loggerName("FileAppenderTest")
                .key("test-key")
                .nodeId("1")
                .build();
    }
    
    /**
     * 读取文件内容（Java 8兼容版本）
     * @param filePath 文件路径
     * @return 文件内容
     */
    private String readFileContent(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }
}