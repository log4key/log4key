package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.util.ExecutorController;
import com.log4key.worker.WorkerGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * FileAppender Level Inclusion Test
 * V2 架构：通过 WorkerGroup + ExecutorController 异步写入
 */
public class FileAppenderLevelInclusionTest {

    private static final String TEST_DIR = "target/test-level-inclusion";
    private FileAppender appender;
    private SimpleDateFormat dateFormat;

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
        // Ensure test directory exists and is clean
        cleanupTestDir();
        new File(TEST_DIR).mkdirs();

        // 创建 WorkerGroup 并启动
        workerGroup = new WorkerGroup(
                2,              // workerCount（必须是 2 的幂）
                8192,           // queueCapacity
                64,             // maxFileWriters
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

        // Create FileAppender instance
        appender = new FileAppender();

        // 注入 ExecutorController 和 WorkerCount
        appender.setExecutorController(executorController);
        appender.setWorkerCount(workerGroup.getWorkerCount());

        // Initialize appender with levelInclusion=true（默认 AT_LEAST 策略即为 level inclusion）
        Map<String, Object> config = new HashMap<>();
        config.put("rootDirectory", TEST_DIR);
        config.put("levelInclusion", true);
        config.put("appenderName", "testInclusion");
        config.put("asyncSupported", false); // V2 架构中不再使用此参数
        appender.initialize(config);

        dateFormat = new SimpleDateFormat("yyyyMMdd");
    }

    @After
    public void tearDown() throws IOException {
        // Close appender
        if (appender != null) {
            appender.close();
        }

        // 关闭 WorkerGroup（确保所有缓冲 flush 到磁盘）
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdown();
        }

        // Cleanup test directory
        cleanupTestDir();
    }

    /**
     * 等待 Worker 处理完任务并刷新到磁盘，然后关闭 WorkerGroup。
     */
    private void awaitWriteCompletion() throws InterruptedException {
        Thread.sleep(300); // 等待 Worker 从 Mailbox 取出任务并执行
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdown(); // shutdown 触发 closeAll()，flush 所有 FileChannel
        }
    }

    private void cleanupTestDir() throws IOException {
        Path path = Paths.get(TEST_DIR);
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Test
    public void testLevelInclusion() throws IOException, InterruptedException {
        long timestamp = System.currentTimeMillis();
        String dateStr = dateFormat.format(timestamp);
        String key = "testKey";

        // 1. Create a WARN level LogEvent
        LogEvent warnEvent = LogEventBuilder.builder()
                .timestampMillis(timestamp)
                .level("WARN")
                .message("This is a WARN message")
                .loggerName("com.test.Logger")
                .key(key)
                .build();

        // 2. Append the event
        appender.append(warnEvent);

        // 等待异步写入完成
        awaitWriteCompletion();

        // 3. Assert that files exist in both warn/... and info/... subdirectories
        Path warnFile = Paths.get(TEST_DIR, "warn", dateStr, key + ".log");
        Path infoFile = Paths.get(TEST_DIR, "info", dateStr, key + ".log");

        assertTrue("WARN log file should exist for WARN event", Files.exists(warnFile));
        assertTrue("INFO log file should exist for WARN event (level inclusion)", Files.exists(infoFile));

        // 4. Assert that the content of both files contains the log message
        String warnContent = new String(Files.readAllBytes(warnFile));
        String infoContent = new String(Files.readAllBytes(infoFile));

        assertTrue("WARN file content should contain message", warnContent.contains("This is a WARN message"));
        assertTrue("INFO file content should contain message", infoContent.contains("This is a WARN message"));

        // 5. Create an INFO level LogEvent
        LogEvent infoEvent = LogEventBuilder.builder()
                .timestampMillis(timestamp)
                .level("INFO")
                .message("This is an INFO message")
                .loggerName("com.test.Logger")
                .key(key)
                .build();

        // 6. Append the event
        appender.append(infoEvent);

        // 等待异步写入完成（需要重新创建 WorkerGroup，因为上一个 awaitWriteCompletion 已关闭）
        // 注意：这里 WorkerGroup 已被关闭，需要重建
        workerGroup = new WorkerGroup(
                2, 8192, 64, 1800000L,
                4096L, 1000L, 32768L, 4096,
                10 * 1024 * 1024, "UTF-8");
        workerGroup.start();
        executorController = new ExecutorController(workerGroup);
        appender.setExecutorController(executorController);
        appender.setWorkerCount(workerGroup.getWorkerCount());

        appender.append(infoEvent);

        awaitWriteCompletion();

        // 7. Assert that file exists in info/... but NOT in warn/... (or warn file content doesn't increase)
        // Since warnFile already exists, we check that it DOES NOT contain the new INFO message
        warnContent = new String(Files.readAllBytes(warnFile));
        infoContent = new String(Files.readAllBytes(infoFile));

        assertTrue("INFO file content should contain INFO message", infoContent.contains("This is an INFO message"));
        assertFalse("WARN file content should NOT contain INFO message", warnContent.contains("This is an INFO message"));
    }
}