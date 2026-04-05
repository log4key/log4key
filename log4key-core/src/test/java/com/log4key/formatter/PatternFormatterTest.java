package com.log4key.formatter;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * PatternFormatter测试类，验证日志格式化功能正确性
 */
public class PatternFormatterTest {
    
    @Test
    public void testDefaultPatternFormat() {
        // 测试默认日志格式
        PatternFormatter formatter = new PatternFormatter();
        LogEvent event = createTestEvent();
        
        String result = formatter.format(event, null);
        assertNotNull(result);
        // 验证结果包含预期的日志元素
        assertTrue(result.contains(event.getLevel()));
        assertTrue(result.contains(event.getLoggerName()));
        assertTrue(result.contains(event.getMessage()));
    }
    
    @Test
    public void testCustomPatternFormat() {
        // 测试自定义日志格式
        String customPattern = "[%d] %p: %m %c %M %L [%t] %key %nodeId%n";
        PatternFormatter formatter = new PatternFormatter(customPattern);
        LogEvent event = createTestEvent();
        
        String result = formatter.format(event, null);
        assertNotNull(result);
        // 验证结果包含预期的日志元素
        assertTrue(result.contains(event.getLevel()));
        assertTrue(result.contains(event.getMessage()));
        assertTrue(result.contains(event.getMethodName()));
        assertTrue(result.contains(String.valueOf(event.getLineNumber())));
        assertTrue(result.contains(event.getKey()));
        assertTrue(result.contains(event.getNodeId()));
    }
    
    @Test
    public void testLog4j2Placeholders() {
        // 测试log4j2占位符兼容
        String log4j2Pattern = "%date{yyyy-MM-dd HH:mm:ss} %level %logger %message %thread%n";
        PatternFormatter formatter = new PatternFormatter(log4j2Pattern);
        LogEvent event = createTestEvent();
        
        String result = formatter.format(event, null);
        assertNotNull(result);
        // 验证结果包含预期的日志元素
        assertTrue(result.contains(event.getLevel()));
        assertTrue(result.contains(event.getLoggerName()));
        assertTrue(result.contains(event.getMessage()));
    }
    
    @Test
    public void testLogbackPlaceholders() {
        // 测试logback占位符兼容
        String logbackPattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
        PatternFormatter formatter = new PatternFormatter(logbackPattern);
        LogEvent event = createTestEvent();
        
        String result = formatter.format(event, null);
        assertNotNull(result);
        // 验证结果包含预期的日志元素
        assertTrue(result.contains(event.getLevel()));
        assertTrue(result.contains(event.getLoggerName()));
        assertTrue(result.contains(event.getMessage()));
    }
    
    @Test
    public void testPatternCache() {
        // 测试模板缓存功能
        String testPattern = "%d %p %m%n";
        
        // 第一次解析，应该生成新的Token列表
        PatternFormatter formatter1 = new PatternFormatter(testPattern);
        LogEvent event = createTestEvent();
        String result1 = formatter1.format(event, null);
        
        // 第二次解析，应该从缓存中获取Token列表
        PatternFormatter formatter2 = new PatternFormatter(testPattern);
        String result2 = formatter2.format(event, null);
        
        // 验证两次结果相同
        assertEquals(result1, result2);
    }
    
    @Test
    public void testSetPattern() {
        // 测试动态更新模板
        PatternFormatter formatter = new PatternFormatter();
        LogEvent event = createTestEvent();
        
        // 使用默认模板
        String result1 = formatter.format(event, null);
        
        // 更新模板
        String newPattern = "%p: %m%n";
        formatter.setPattern(newPattern);
        String result2 = formatter.format(event, null);
        
        // 验证结果不同
        assertNotEquals(result1, result2);
        // 验证新结果符合新模板
        assertTrue(result2.contains(event.getLevel()));
        assertTrue(result2.contains(event.getMessage()));
    }
    
    /**
     * 创建测试日志事件
     * @return 日志事件对象
     */
    private LogEvent createTestEvent() {
        return LogEventBuilder.builder()
                .level("INFO")
                .loggerName("com.log4key.test.TestClass")
                .message("Test log message")
                .className("TestClass")
                .methodName("testMethod")
                .lineNumber(42)
                .fileName("TestClass.java")
                .key("test-key")
                .nodeId("1")
                .build();
    }
    
    /**
     * 测试性能优化效果
     * 验证日志格式化的吞吐量
     */
    @Test
    public void testPerformance() {
        // 创建测试日志事件
        LogEvent event = createTestEvent();
        PatternFormatter formatter = new PatternFormatter();
        
        // 执行大量格式化操作，测试吞吐量
        int iterations = 100000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            formatter.format(event, null);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = iterations / (duration / 1000.0);
        
        System.out.println("Performance test results:");
        System.out.println("Iterations: " + iterations);
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " logs/second");
        
        // 验证吞吐量大于10,000 logs/second
        assertTrue("Throughput should be greater than 10,000 logs/second", throughput > 10000);
    }
    
    /**
     * 测试线程安全性
     * 验证多线程环境下PatternFormatter的正确性
     */
    @Test
    public void testThreadSafety() throws InterruptedException {
        final PatternFormatter formatter = new PatternFormatter();
        final LogEvent event = createTestEvent();
        final int threadCount = 10;
        final int iterationsPerThread = 10000;
        
        // 创建多个线程同时调用format方法
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    String result = formatter.format(event, null);
                    // 验证结果不为null且包含基本信息
                    assertNotNull(result);
                    assertTrue(result.contains(event.getLevel()));
                    assertTrue(result.contains(event.getMessage()));
                }
            });
        }
        
        // 启动所有线程
        long startTime = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Thread safety test results:");
        System.out.println("Threads: " + threadCount);
        System.out.println("Iterations per thread: " + iterationsPerThread);
        System.out.println("Total iterations: " + (threadCount * iterationsPerThread));
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Throughput: " + String.format("%.2f", (threadCount * iterationsPerThread) / (duration / 1000.0)) + " logs/second");
    }
}
