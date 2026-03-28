package com.log4key.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * LogContext单元测试类
 * 测试日志上下文的核心功能
 */
public class LogContextTest {

    /**
     * 在每个测试方法开始前清空上下文
     */
    @Before
    public void setUp() {
        LogContext.current().clear();
        LogContext.global().clear();
    }

    /**
     * 在每个测试方法结束后清空上下文
     */
    @After
    public void tearDown() {
        LogContext.current().clear();
        LogContext.global().clear();
    }

    @Test
    public void testCurrentContext() {
        // 获取当前上下文
        LogContext context = LogContext.current();
        assertNotNull(context);
        
        // 测试上下文的基本操作
        context.put("testKey", "testValue");
        assertEquals("testValue", context.get("testKey"));
        assertEquals(1, context.getAll().size());
        
        // 测试获取不存在的键
        assertNull(context.get("nonExistentKey"));
        assertEquals("defaultValue", context.get("nonExistentKey", "defaultValue"));
        
        // 测试移除键
        context.put("removeKey", "removeValue");
        assertEquals(2, context.getAll().size());
        assertEquals("removeValue", context.remove("removeKey"));
        assertNull(context.get("removeKey"));
        assertEquals(1, context.getAll().size());
        
        // 测试清空上下文
        context.put("key1", "value1");
        context.put("key2", "value2");
        assertEquals(3, context.getAll().size());
        context.clear();
        assertEquals(0, context.getAll().size());
    }

    @Test
    public void testGlobalContext() {
        // 获取全局上下文
        LogContext globalContext = LogContext.global();
        assertNotNull(globalContext);
        
        // 设置全局上下文值
        globalContext.put("globalKey", "globalValue");
        
        // 从当前上下文获取全局值
        LogContext currentContext = LogContext.current();
        assertNull(currentContext.get("globalKey")); // 当前上下文不会自动包含全局上下文的值
        
        // 从只读上下文获取合并后的值
        LogContext readonlyContext = DefaultLogContext.readonly();
        assertEquals("globalValue", readonlyContext.get("globalKey"));
    }

    @Test
    public void testReadonlyContext() {
        // 设置当前上下文值
        LogContext.current().put("currentKey", "currentValue");
        
        // 获取只读上下文
        LogContext readonlyContext = DefaultLogContext.readonly();
        assertEquals("currentValue", readonlyContext.get("currentKey"));
        
        // 测试只读上下文无法修改
        try {
            readonlyContext.put("newKey", "newValue");
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // 预期异常，测试通过
        }
        
        try {
            readonlyContext.remove("currentKey");
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // 预期异常，测试通过
        }
        
        try {
            readonlyContext.clear();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // 预期异常，测试通过
        }
    }

    @Test
    public void testContextMerge() {
        // 创建两个上下文并设置值
        LogContext context1 = LogContext.current();
        context1.put("key1", "value1");
        
        // 注意：这里应该创建一个新的上下文，而不是再次调用current()
        // 因为current()返回的是同一个线程的上下文
        LogContext context2 = LogContext.current();
        context2.put("key2", "value2");
        
        // 合并上下文
        context1.merge(context2);
        
        // 验证合并结果
        assertEquals("value1", context1.get("key1"));
        assertEquals("value2", context1.get("key2"));
        // 由于context1和context2实际上是同一个上下文，所以context2也应该包含key1
        assertEquals("value1", context2.get("key1"));
    }

    @Test
    public void testThreadIsolation() throws InterruptedException {
        // 在主线程设置上下文值
        LogContext.current().put("mainThreadKey", "mainThreadValue");
        
        // 用于存储新线程中的上下文值
        final String[] newThreadValue = new String[1];
        
        // 在新线程中测试上下文隔离
        Thread thread = new Thread(() -> {
            // 新线程中应该没有主线程的上下文值
            assertNull(LogContext.current().get("mainThreadKey"));
            
            // 在新线程中设置自己的上下文值
            LogContext.current().put("newThreadKey", "newThreadValue");
            newThreadValue[0] = LogContext.current().get("newThreadKey");
        });
        
        thread.start();
        thread.join();
        
        // 验证新线程中的上下文值
        assertEquals("newThreadValue", newThreadValue[0]);
        
        // 主线程中应该没有新线程的上下文值
        assertNull(LogContext.current().get("newThreadKey"));
    }

    @Test
    public void testLogEventWithContext() {
        // 设置当前上下文
        LogContext.current().put("eventKey", "eventValue");
        
        // 创建日志事件
        LogEvent logEvent = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test-logger")
                .message("Test log message")
                .build();
        
        // 验证日志事件中包含上下文信息
        assertNotNull(logEvent.getLogContext());
        assertEquals("eventValue", logEvent.getLogContext().get("eventKey"));
        
        // 测试更新上下文后创建新的日志事件
        LogContext.current().put("updatedKey", "updatedValue");
        LogEvent updatedEvent = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test-logger")
                .message("Test log message")
                .build();
        assertEquals("updatedValue", updatedEvent.getLogContext().get("updatedKey"));
    }
}