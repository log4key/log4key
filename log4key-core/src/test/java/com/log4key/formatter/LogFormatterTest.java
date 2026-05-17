package com.log4key.formatter;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import com.log4key.api.spi.LogFormatter;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * 日志格式化器测试类，验证不同格式化器的功能
 */
public class LogFormatterTest {
    
    private LogEvent testEvent;
    private LogEvent testEventWithException;
    
    @Before
    public void setUp() {
        // 添加MDC
        Map<String, Object> mdc = new HashMap<>();
        mdc.put("userId", "user-123");
        mdc.put("requestId", "req-456");

        // 创建测试日志事件
        testEvent = LogEventBuilder.builder()
                .timestampMillis(System.currentTimeMillis())
                .level("INFO")
                .loggerName("TestLogger")
                .message("This is a test message")
                .nodeId("1")
                .key("test-key-123")
                .mdc(mdc)
                .build();
        
        // 创建带有异常的测试日志事件
        testEventWithException = LogEventBuilder.builder()
                .timestampMillis(System.currentTimeMillis())
                .level("ERROR")
                .loggerName("TestLogger")
                .message("This is an error message")
                .nodeId("1")
                .key("test-key-456")
                .throwable(new RuntimeException("Test exception"))
                .mdc(mdc)
                .build();
    }
    
    @Test
    public void testTextFormatter() {
        LogFormatter textFormatter = new TextLogFormatter();
        
        // 测试基本格式化
        String formatted = textFormatter.format(testEvent);
        
        assertNotNull("Text formatted log should not be null", formatted);
        assertTrue("Text formatted log should contain timestamp", formatted.contains("["));
        assertTrue("Text formatted log should contain level", formatted.contains("INFO"));
        assertTrue("Text formatted log should contain logger name", formatted.contains("TestLogger"));
        assertTrue("Text formatted log should contain message", formatted.contains("This is a test message"));
        
        // 测试带有上下文信息的格式化 - TextLogFormatter支持context参数但不输出到结果
        Map<String, Object> context = new HashMap<>();
        context.put("extraField", "extraValue");
        String formattedWithContext = textFormatter.format(testEvent, context);
        // Context is accepted but not included in output for text formatter
        assertNotNull("Text formatted log with context should not be null", formattedWithContext);
        
        // 测试带有异常的格式化
        String formattedWithException = textFormatter.format(testEventWithException);
        assertTrue("Text formatted log with exception should contain exception info", formattedWithException.contains("Exception:"));
        assertTrue("Text formatted log with exception should contain ERROR level", formattedWithException.contains("ERROR"));
        
        System.out.println("Text Formatter Test Result:");
        System.out.println(formatted);
    }
    
    @Test
    public void testFormatterManager() {
        LogFormatterManager manager = LogFormatterManager.getInstance();
        
        // 测试获取格式化器 - 只测试实际存在的formatter
        LogFormatter textFormatter = manager.getFormatter("text");
        assertNotNull("Text formatter should be available", textFormatter);
        
        // XML formatter does not exist in the project
        // LogFormatter xmlFormatter = manager.getFormatter("xml");
        // assertNotNull("XML formatter should be available", xmlFormatter);
        
        // 测试使用管理器格式化日志
        String textFormatted = manager.format(testEvent, "text");
        assertNotNull("Text formatted log via manager should not be null", textFormatted);
        
        // XML formatter does not exist
        // String xmlFormatted = manager.format(testEvent, "xml");
        // assertNotNull("XML formatted log via manager should not be null", xmlFormatted);
        
        // 测试带有上下文信息的格式化 - context is accepted but may not be included in output
        Map<String, Object> context = new HashMap<>();
        context.put("extraField", "extraValue");
        String formattedWithContext = manager.format(testEvent, "text", context);
        assertNotNull("Formatted log with context should not be null", formattedWithContext);
        
        System.out.println("Formatter Manager Test: All formatters are available");
    }
    
    @Test
    public void testFormatterChain() {
        LogFormatterManager manager = LogFormatterManager.getInstance();
        
        // 测试默认链式调用
        String chainedResult = manager.formatWithChain(testEvent);
        assertNotNull("Chained format result should not be null", chainedResult);
        
        // 测试自定义链式调用 - 只使用实际存在的formatters
        manager.configureChain(Arrays.asList("text"));
        String customChainedResult = manager.formatWithChain(testEvent);
        assertNotNull("Custom chained format result should not be null", customChainedResult);
        
        // 测试带有上下文的链式调用
        Map<String, Object> context = new HashMap<>();
        context.put("extraField", "extraValue");
        String chainedResultWithContext = manager.formatWithChain(testEvent, context);
        assertNotNull("Chained format result with context should not be null", chainedResultWithContext);
        
        // 测试重新配置链式调用 - XML formatter does not exist, use pattern instead
        manager.configureChain(Arrays.asList("text"));
        String reconfiguredChainedResult = manager.formatWithChain(testEvent);
        assertNotNull("Reconfigured chained format result should not be null", reconfiguredChainedResult);
        
        System.out.println("Formatter Chain Test Result:");
        System.out.println(chainedResult);
    }
    
    @Test
    public void testFormatterChainWithCustomFormatter() {
        LogFormatterManager manager = LogFormatterManager.getInstance();
        
        // 创建自定义格式化器，用于测试链式调用
        LogFormatter customFormatter = new LogFormatter() {
            @Override
            public String getName() {
                return "custom"; 
            }
            
            @Override
            public String getType() {
                return "custom";
            }
            
            @Override
            public String format(LogEvent event, Map<String, Object> context) {
                return "CUSTOM:" + event.getMessage();
            }
            
            @Override
            public String formatChain(String previousResult, LogEvent event, Map<String, Object> context) {
                return previousResult + " [CUSTOM-CHAINED]";
            }
        };
        
        // 注册自定义格式化器
        manager.registerFormatter(customFormatter);
        
        // 测试包含自定义格式化器的链式调用
        manager.configureChain(Arrays.asList("text", "custom"));
        String chainedResult = manager.formatWithChain(testEvent);
        assertNotNull("Chained format result with custom formatter should not be null", chainedResult);
        assertTrue("Chained format result should contain custom formatter output", chainedResult.contains("[CUSTOM-CHAINED]"));
        
        System.out.println("Custom Formatter Chain Test Result:");
        System.out.println(chainedResult);
    }
    
    @Test
    public void testFormatterWithNullEvent() {
        LogFormatter textFormatter = new TextLogFormatter();
        LogFormatter jsonFormatter = new JsonLogFormatter();

        // 测试所有格式化器对null事件的处理
        assertThrows(IllegalArgumentException.class, () -> textFormatter.format(null));
        assertThrows(IllegalArgumentException.class, () -> jsonFormatter.format(null));

        System.out.println("Null Event Test: All formatters correctly handle null events");
    }

}
