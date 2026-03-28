package com.log4key.api;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * 测试LogEvent Builder模式的功能
 * 验证Builder模式的基本功能、不可变对象特性、mdc的不可变性、logContext的readonly特性、属性分类的正确性以及与现有系统的兼容性
 */
public class LogEventBuilderTest {

    private static final String TEST_LEVEL = "INFO";
    private static final String TEST_LOGGER_NAME = "test-logger";
    private static final String TEST_MESSAGE = "Test log message";

    @Before
    public void setUp() {
        // 清理当前线程的LogContext，确保测试环境干净
        LogContext.current().clear();
    }

    /**
     * 测试Builder模式的基本功能
     * 验证可以通过Builder创建LogEvent实例，并且所有属性都能正确设置
     */
    @Test
    public void testBuilderBasicFunctionality() {
        // 使用Builder创建LogEvent实例
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .build();

        // 验证必须属性
        assertNotNull(event);
        assertEquals(TEST_LEVEL, event.getLevel());
        assertEquals(TEST_LOGGER_NAME, event.getLoggerName());
        assertEquals(TEST_MESSAGE, event.getMessage());

        // 验证默认值
        assertNotNull(event.getThreadName());
        assertTrue(System.currentTimeMillis() >= event.getTimestampMillis());
        assertNotNull(event.getMdc());
        assertNotNull(event.getLogContext());
    }

    /**
     * 测试LogEvent的不可变对象特性
     * 验证LogEvent实例创建后无法修改属性
     */
    @Test
    public void testLogEventImmutability() {
        // 使用Builder创建LogEvent实例
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .build();

        // 验证LogEvent没有setter方法（通过编译检查）
        // 注意：Java的不可变性通过没有setter方法和final字段来保证
        // 这里我们主要验证属性的getter方法返回正确的值，且无法修改

        // 验证属性值无法修改
        long originalTimestamp = event.getTimestampMillis();
        
        // 创建另一个实例，验证不同实例的属性相互独立
        LogEvent anotherEvent = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("another-logger")
                .message("Another message")
                .build();

        // 验证两个实例的属性相互独立
        assertNotEquals(event.getLevel(), anotherEvent.getLevel());
        assertNotEquals(event.getMessage(), anotherEvent.getMessage());
        
        // 验证原始实例的属性没有被修改
        assertEquals(originalTimestamp, event.getTimestampMillis());
    }

    /**
     * 测试mdc的不可变性
     * 验证LogEvent中的mdc是不可变的，无法修改
     */
    @Test
    public void testMdcImmutability() {
        // 创建测试mdc
        Map<String, Object> testMdc = new HashMap<>();
        testMdc.put("key1", "value1");
        testMdc.put("key2", 123);

        // 使用Builder创建LogEvent实例
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .mdc(testMdc)
                .build();

        // 获取LogEvent中的mdc
        Map<String, Object> eventMdc = event.getMdc();

        // 验证mdc内容正确
        assertEquals("value1", eventMdc.get("key1"));
        assertEquals(123, eventMdc.get("key2"));
        assertEquals(2, eventMdc.size());

        // 验证mdc不可修改
        assertThrows(UnsupportedOperationException.class, () -> {
            eventMdc.put("key3", "value3");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            eventMdc.remove("key1");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            eventMdc.clear();
        });

        // 验证原始mdc修改不会影响LogEvent中的mdc
        testMdc.put("key4", "value4");
        assertEquals(2, eventMdc.size());
        assertNull(eventMdc.get("key4"));
    }

    /**
     * 测试logContext的readonly特性
     * 验证LogEvent中的logContext是只读的，无法修改
     */
    @Test
    public void testLogContextReadonly() {
        // 获取并填充当前线程的LogContext
        LogContext currentContext = LogContext.current();
        currentContext.put("contextKey1", "contextValue1");
        currentContext.put("contextKey2", 456);

        // 使用Builder创建LogEvent实例，不设置logContext，使用默认的readonly上下文
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .build();

        // 获取LogEvent中的logContext
        LogContext eventContext = event.getLogContext();

        // 验证logContext内容正确
        assertEquals("contextValue1", eventContext.get("contextKey1"));
        assertEquals(Integer.valueOf(456), eventContext.get("contextKey2"));

        // 验证logContext只读，无法修改
        try {
            eventContext.put("contextKey3", "contextValue3");
            fail("Expected UnsupportedOperationException was not thrown");
        } catch (UnsupportedOperationException e) {
            // 预期的异常，测试通过
        }

        try {
            eventContext.remove("contextKey1");
            fail("Expected UnsupportedOperationException was not thrown");
        } catch (UnsupportedOperationException e) {
            // 预期的异常，测试通过
        }

        try {
            eventContext.clear();
            fail("Expected UnsupportedOperationException was not thrown");
        } catch (UnsupportedOperationException e) {
            // 预期的异常，测试通过
        }
    }

    /**
     * 测试属性分类的正确性
     * 验证必须属性、补充属性和派生属性的处理正确
     */
    @Test
    public void testPropertyClassification() {
        // 创建测试数据
        Map<String, Object> testMdc = new HashMap<>();
        testMdc.put("testKey", "testValue");

        Throwable testThrowable = new RuntimeException("Test exception");
        ILogKey testLogKey = DefaultLogKey.of("test-key", 12345L);

        // 使用Builder创建LogEvent实例，设置所有属性
        LogEvent event = LogEventBuilder.builder()
                // 必须属性
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                // 补充属性
                .throwable(testThrowable)
                .className("TestClass")
                .methodName("testMethod")
                .fileName("TestClass.java")
                .lineNumber(42)
                .threadName("TestThread")
                .mdc(testMdc)
                // 派生属性
                .logKey(testLogKey)
                .build();

        // 验证必须属性
        assertEquals(TEST_LEVEL, event.getLevel());
        assertEquals(TEST_LOGGER_NAME, event.getLoggerName());
        assertEquals(TEST_MESSAGE, event.getMessage());

        // 验证补充属性
        assertEquals(testThrowable, event.getThrowable());
        assertEquals("TestClass", event.getClassName());
        assertEquals("testMethod", event.getMethodName());
        assertEquals("TestClass.java", event.getFileName());
        assertEquals(42, event.getLineNumber());
        assertEquals("TestThread", event.getThreadName());
        assertEquals("testValue", event.getMdc().get("testKey"));

        // 验证派生属性
        assertTrue(System.currentTimeMillis() >= event.getTimestampMillis());
        assertEquals("test-key", event.getKey());
        assertEquals("12345", event.getNodeId());
        assertEquals(testLogKey, event.getLogKey());

        // 验证logKey设置后自动更新key和nodeId
        ILogKey anotherLogKey = DefaultLogKey.of("another-key", 54321L);
        LogEvent anotherEvent = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .logKey(anotherLogKey)
                .build();

        assertEquals("another-key", anotherEvent.getKey());
        assertEquals("54321", anotherEvent.getNodeId());
    }

    /**
     * 测试必须属性验证
     * 验证缺少必须属性时，build()方法会抛出IllegalArgumentException
     */
    @Test
    public void testRequiredPropertiesValidation() {
        // 缺少level
        try {
            LogEventBuilder.builder()
                    .loggerName(TEST_LOGGER_NAME)
                    .message(TEST_MESSAGE)
                    .build();
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // 预期的异常，测试通过
        }

        // 缺少loggerName
        try {
            LogEventBuilder.builder()
                    .level(TEST_LEVEL)
                    .message(TEST_MESSAGE)
                    .build();
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // 预期的异常，测试通过
        }

        // 缺少message
        try {
            LogEventBuilder.builder()
                    .level(TEST_LEVEL)
                    .loggerName(TEST_LOGGER_NAME)
                    .build();
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // 预期的异常，测试通过
        }

        // 缺少所有必须属性
        try {
            LogEventBuilder.builder()
                    .build();
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // 预期的异常，测试通过
        }
    }

    /**
     * 测试与现有系统的兼容性
     * 验证LogEventBuilder创建的LogEvent实例可以被现有系统正确处理
     */
    @Test
    public void testCompatibilityWithExistingSystem() {
        // 创建一个简单的LogEvent实例
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .build();

        // 验证LogEvent实现了Serializable接口
        assertTrue(event instanceof java.io.Serializable);

        // 验证所有getter方法都存在且能正常工作
        assertNotNull(event.getLevel());
        assertNotNull(event.getLoggerName());
        assertNotNull(event.getMessage());
        assertNotNull(event.getTimestampMillis());
        assertNotNull(event.getThreadName());
        assertNotNull(event.getMdc());
        assertNotNull(event.getLogContext());

        // 验证可以正常访问所有属性，包括新增的属性
        event.getKey();
        event.getNodeId();
        event.getLogKey();
        event.getClassName();
        event.getMethodName();
        event.getFileName();
        event.getLineNumber();
        event.getThrowable();

        // 验证LogEvent可以被用于现有的日志处理流程
        // 这里我们模拟一个简单的日志处理流程
        StringBuilder sb = new StringBuilder();
        sb.append("[")
          .append(event.getLevel())
          .append("] [")
          .append(event.getLoggerName())
          .append("] ")
          .append(event.getMessage());

        String formattedLog = sb.toString();
        assertTrue(formattedLog.contains(TEST_LEVEL));
        assertTrue(formattedLog.contains(TEST_LOGGER_NAME));
        assertTrue(formattedLog.contains(TEST_MESSAGE));
    }

    /**
     * 测试LogContext的默认readonly特性
     * 验证未设置logContext时，LogEvent使用的是readonly的logContext
     */
    @Test
    public void testDefaultLogContextIsReadonly() {
        // 使用Builder创建LogEvent实例，不设置logContext
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .build();

        // 获取LogEvent中的logContext
        LogContext eventContext = event.getLogContext();

        // 验证logContext是只读的
        try {
            eventContext.put("testKey", "testValue");
            fail("Expected UnsupportedOperationException was not thrown");
        } catch (UnsupportedOperationException e) {
            // 预期的异常，测试通过
        }
    }

    /**
     * 测试logKey对key和nodeId的影响
     * 验证设置logKey后，key和nodeId会自动更新
     */
    @Test
    public void testLogKeyUpdatesKeyAndNodeId() {
        // 先设置key和nodeId，然后设置logKey，验证logKey会覆盖之前的设置
        LogEvent event = LogEventBuilder.builder()
                .level(TEST_LEVEL)
                .loggerName(TEST_LOGGER_NAME)
                .message(TEST_MESSAGE)
                .key("initial-key")
                .nodeId("initial-node")
                .logKey(DefaultLogKey.of("updated-key", 99999L))
                .build();

        // 验证logKey覆盖了之前的key和nodeId设置
        assertEquals("updated-key", event.getKey());
        assertEquals("99999", event.getNodeId());
    }
}
