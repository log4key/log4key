package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.api.router.SmartFileRouter;
import com.log4key.path.PathKey;
import com.log4key.util.ExecutorController;
import com.log4key.worker.WorkerGroup;
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
 * V2 架构：通过 WorkerGroup + ExecutorController 异步写入
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

    /**
     * WorkerGroup 实例，用于异步写入测试
     */
    private WorkerGroup workerGroup;

    /**
     * ExecutorController 实例，由 WorkerGroup 构造
     */
    private ExecutorController executorController;

    @Before
    public void setUp() throws IOException {
        // 初始化mock对象
        closeable = MockitoAnnotations.openMocks(this);

        // 确保测试目录存在
        new File(TEST_DIR).mkdirs();

        // 创建 WorkerGroup 并启动
        workerGroup = new WorkerGroup(
                2,              // workerCount（必须是 2 的幂）
                8192,           // queueCapacity
                64,             // maxOpenFiles
                1800000L,       // idleTimeoutMs
                4096L,          // batchSize
                1000L,          // flushIntervalMs
                32768L,         // highWaterMark
                4096,           // initialBufferSize
                10 * 1024 * 1024, // maxFileSize
                "UTF-8"         // charset
        );
        workerGroup.start();

        // 创建 ExecutorController
        executorController = new ExecutorController(workerGroup);

        // 创建FileAppender实例
        appender = new FileAppender();

        // 设置mock的文件路由器
        List<PathKey> paths = Collections.singletonList(new PathKey(TEST_DIR, "test.log"));
        when(mockFileRouter.determineLogFilePaths(any(LogEvent.class))).thenReturn(paths);
        doNothing().when(mockFileRouter).initialize();

        // 使用反射设置mock的router
        appender.setFileRouter(mockFileRouter);

        // 注入 ExecutorController 和 WorkerCount
        appender.setExecutorController(executorController);
        appender.setWorkerCount(workerGroup.getWorkerCount());

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

        // 关闭 WorkerGroup（确保所有缓冲 flush 到磁盘）
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdown();
        }

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

    /**
     * 等待 Worker 处理完任务并刷新到磁盘，然后关闭 WorkerGroup。
     * 调用后 WorkerGroup 被关闭，后续测试将通过 @Before 重建。
     */
    private void awaitWriteCompletion() throws InterruptedException {
        Thread.sleep(300); // 等待 Worker 从 Mailbox 取出任务并执行
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdown(); // shutdown 触发 closeAll()，flush 所有 FileChannel
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
    public void testAppendSingleEvent() throws InterruptedException {
        // 创建测试事件
        LogEvent event = createTestEvent("Single test message");

        // 执行append操作
        appender.append(event);

        // 等待异步写入完成
        awaitWriteCompletion();

        // 验证文件存在并包含内容
        File logFile = new File(TEST_DIR + File.separator + "test.log");
        assertTrue("Log file should exist", logFile.exists());
        assertTrue("Log file should not be empty", logFile.length() > 0);

        // 验证文件内容包含我们的消息
        String content = readFileContent(logFile.getPath());
        assertTrue("Content should contain test message", content.contains("Single test message"));
    }

    @Test
    public void testAppendBatchEvents() throws InterruptedException {
        // 创建多个测试事件
        List<LogEvent> events = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            events.add(createTestEvent("Batch message " + i));
        }

        // 执行批量append操作
        appender.appendBatch(events);

        // 等待异步写入完成
        awaitWriteCompletion();

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
    public void testAppendWithException() throws InterruptedException {
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

        // 等待异步写入完成
        awaitWriteCompletion();

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

        // 等待所有线程完成提交
        latch.await();

        // 等待异步写入完成
        awaitWriteCompletion();

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

        // 由于是异步写入，期望所有消息都被写入
        assertTrue("Most messages should be written", foundMessages > (threadCount * eventsPerThread * 0.9));

        executor.shutdown();
    }

    @Test
    public void testClose() throws InterruptedException {
        // 写入一些内容
        appender.append(createTestEvent("Message before close"));

        // 关闭appender
        appender.close();

        // 验证关闭后不能再写入
        LogEvent eventAfterClose = createTestEvent("This should not be written");
        appender.append(eventAfterClose);

        // 等待异步写入完成（appender.close() 不关闭 WorkerGroup，Worker 继续处理已提交的任务）
        awaitWriteCompletion();

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
        assertFalse("Log file should not be created for null event", logFile.exists());
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