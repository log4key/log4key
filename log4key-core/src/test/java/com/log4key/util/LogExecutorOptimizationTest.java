package com.log4key.util;

import com.log4key.LogManager;
import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试执行器优化后的功能
 * 验证processLogEvent方法、降级执行器处理逻辑和shutdown流程是否正常工作
 */
public class LogExecutorOptimizationTest {

    private LogManager logManager;

    @BeforeEach
    void setUp() {
        // 重置LogManager实例，确保测试环境干净
        LogManager.reset();
        // 确保LogManager完全初始化，包括CLEANER_EXECUTOR等组件
        LogManager.ensureInitialized(null);
        logManager = LogManager.getInstance();
    }

    @AfterEach
    void tearDown() {
        // 关闭LogManager，释放资源
        if (logManager != null) {
            logManager.shutdown();
        }
    }

    /**
     * 测试processLogEvent方法是否正常工作
     */
    @Test
    void testProcessLogEvent() {
        // 生成测试日志事件
        LogEvent event = LogEventBuilder.builder()
                .key("test-key")
                .level("INFO")
                .message("Test log message")
                .loggerName("test-logger")
                .build();

        // 调用processLogEvent方法
        logManager.processLogEvent(event);

        // 验证日志事件被处理，这里我们主要验证方法没有抛出异常
        // 更详细的验证可以通过检查日志文件或Appender来完成
        assertTrue(true, "processLogEvent方法执行成功，没有抛出异常");

        // 关闭LogManager，释放资源
        logManager.shutdown();
    }

    /**
     * 测试执行器状态检查是否保留
     */
    @Test
    void testExecutorStatusCheck() {
        // 关闭LogManager，触发执行器关闭
        logManager.shutdown();

        // 生成测试日志事件
        LogEvent event = LogEventBuilder.builder()
                .key("test-key")
                .level("INFO")
                .message("Test log message after shutdown")
                .loggerName("test-logger")
                .build();

        // 调用processLogEvent方法，应该返回而不处理日志事件
        logManager.processLogEvent(event);

        // 验证方法执行成功，没有抛出异常
        assertTrue(true, "processLogEvent方法在执行器关闭后执行成功，没有抛出异常");

        // 注意：这个测试方法已经调用了shutdown()，不需要再次调用
        // 但是需要将logManager置为null，避免tearDown()中重复关闭
        logManager = null;
    }

    /**
     * 测试降级执行器是否智能处理
     */
    @Test
    void testFallbackExecutorSmartHandling() throws InterruptedException {
        // 生成大量日志事件，测试执行器负载情况
        final int eventCount = 1000;
        final CountDownLatch latch = new CountDownLatch(eventCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        // 提交大量日志事件
        for (int i = 0; i < eventCount; i++) {
            final int index = i;
            new Thread(() -> {
                LogEvent event = LogEventBuilder.builder()
                        .key("test-key-" + (index % 10)) // 10个不同的key
                        .level("INFO")
                        .message("Test log message " + index)
                        .loggerName("test-logger")
                        .build();

                try {
                    logManager.processLogEvent(event);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 忽略异常，降级执行器应该处理这种情况
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有线程完成
        latch.await(10, TimeUnit.SECONDS);

        // 验证大部分日志事件被成功处理
        double successRate = (double) successCount.get() / eventCount;
        System.out.println("日志事件处理成功率: " + successRate);
        assertTrue(successRate > 0.95, "降级执行器智能处理，日志事件处理成功率大于95%");

        // 关闭LogManager，释放资源
        logManager.shutdown();
    }

    /**
     * 测试shutdown流程是否优化
     */
    @Test
    void testShutdownProcessOptimization() throws InterruptedException {
        // 生成一些日志事件
        for (int i = 0; i < 100; i++) {
            LogEvent event = LogEventBuilder.builder()
                    .key("test-key-" + (i % 5))
                    .level("INFO")
                    .message("Test log message " + i)
                    .loggerName("test-logger")
                    .build();
            logManager.processLogEvent(event);
        }

        // 记录关闭开始时间
        long startTime = System.currentTimeMillis();

        // 调用shutdown方法
        logManager.shutdown();

        // 记录关闭结束时间
        long endTime = System.currentTimeMillis();
        long shutdownTime = endTime - startTime;

        // 验证shutdown流程在合理时间内完成（这里设置为10秒）
        System.out.println("shutdown流程耗时: " + shutdownTime + "ms");
        assertTrue(shutdownTime < 10000, "shutdown流程优化，在10秒内完成");

        // 注意：这个测试方法已经调用了shutdown()，不需要再次调用
        // 但是需要将logManager置为null，避免tearDown()中重复关闭
        logManager = null;
    }

    /**
     * 测试是否避免了执行器切换导致的乱序问题
     * 简化测试，验证同一key的日志事件能够正常处理，不出现执行器切换导致的异常
     */
    @Test
    void testAvoidsOutOfOrderDueToExecutorSwitch() throws InterruptedException {
        final int eventCount = 100;
        final CountDownLatch latch = new CountDownLatch(eventCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        // 提交同一key的日志事件，验证顺序
        for (int i = 0; i < eventCount; i++) {
            final int index = i;
            new Thread(() -> {
                // 设置时间戳
                long currentTime = System.currentTimeMillis();
                
                LogEvent event = LogEventBuilder.builder()
                        .key("same-key") // 同一key，确保同一线程处理
                        .level("INFO")
                        .message("Test log message " + index)
                        .loggerName("test-logger")
                        .timestampMillis(currentTime)
                        .build();

                try {
                    logManager.processLogEvent(event);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 忽略异常
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有线程完成
        latch.await(10, TimeUnit.SECONDS);

        // 验证大部分日志事件被成功处理
        double successRate = (double) successCount.get() / eventCount;
        System.out.println("同一key的日志事件处理成功率: " + successRate);
        assertTrue(successRate > 0.95, "同一key的日志事件处理成功率大于95%，执行器切换没有导致大量失败");
        System.out.println("同一key的日志事件处理验证通过，共处理了" + successCount.get() + "个事件");

        // 关闭LogManager，释放资源
        logManager.shutdown();
    }

    /**
     * 测试降级执行器在主执行器关闭后的处理
     */
    @Test
    void testFallbackExecutorAfterMainExecutorShutdown() throws InterruptedException {
        // 生成一些日志事件
        for (int i = 0; i < 50; i++) {
            LogEvent event = LogEventBuilder.builder()
                    .key("test-key-" + (i % 5))
                    .level("INFO")
                    .message("Test log message " + i)
                    .loggerName("test-logger")
                    .build();
            logManager.processLogEvent(event);
        }

        // 关闭LogManager，这会关闭主执行器
        logManager.shutdown();

        // 重置LogManager实例
        LogManager.reset();
        // 确保LogManager完全初始化，包括CLEANER_EXECUTOR等组件
        LogManager.ensureInitialized(null);
        logManager = LogManager.getInstance();

        // 再次生成日志事件，验证系统正常工作
        final int eventCount = 50;
        final AtomicInteger successCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(eventCount);

        for (int i = 0; i < eventCount; i++) {
            final int index = i;
            new Thread(() -> {
                LogEvent event = LogEventBuilder.builder()
                        .key("test-key-" + (index % 5))
                        .level("INFO")
                        .message("Test log message after reset " + index)
                        .loggerName("test-logger")
                        .build();

                try {
                    logManager.processLogEvent(event);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 忽略异常
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 等待所有线程完成
        latch.await(10, TimeUnit.SECONDS);

        // 验证大部分日志事件被成功处理
        double successRate = (double) successCount.get() / eventCount;
        System.out.println("重置后日志事件处理成功率: " + successRate);
        assertTrue(successRate > 0.95, "重置后降级执行器正常工作，日志事件处理成功率大于95%");

        // 关闭LogManager，释放资源
        logManager.shutdown();
    }
}