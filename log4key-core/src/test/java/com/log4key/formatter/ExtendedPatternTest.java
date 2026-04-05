package com.log4key.formatter;

import com.log4key.LogManager;
import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class ExtendedPatternTest {

    @Test
    public void testRelativeTime() {
        // 确保LogManager已初始化
        long start = LogManager.startTime;
        
        PatternFormatter formatter = new PatternFormatter("%r");
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg")
                .build();
        
        String output = formatter.format(event, null);
        assertTrue("Relative time should be a number, got: " + output, output.matches("-?\\d+"));
    }

    @Test
    public void testMdc() {
        Map<String, Object> mdc = new HashMap<>();
        mdc.put("userId", "123");
        mdc.put("reqId", "abc");
        
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg")
                .mdc(mdc)
                .build();

        // Test %X
        PatternFormatter formatter1 = new PatternFormatter("%X");
        String output1 = formatter1.format(event, null);
        assertTrue(output1.contains("userId=123"));
        assertTrue(output1.contains("reqId=abc"));

        // Test %X{userId}
        PatternFormatter formatter2 = new PatternFormatter("%X{userId}");
        String output2 = formatter2.format(event, null);
        assertTrue(output2.equals("123"));
    }

    @Test
    public void testMarker() {
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg")
                .markerName("SECURITY")
                .build();

        PatternFormatter formatter = new PatternFormatter("%marker");
        String output = formatter.format(event, null);
        assertTrue(output.equals("SECURITY"));
    }

    @Test
    public void testThrowableExplicit() {
        Exception ex = new RuntimeException("Test Error");
        LogEvent event = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("test")
                .message("msg")
                .throwable(ex)
                .build();

        // %ex{short}
        PatternFormatter formatter = new PatternFormatter("%ex{short}");
        String output = formatter.format(event, null);
        assertTrue(output.contains("java.lang.RuntimeException: Test Error"));
        assertFalse(output.contains("at com.log4key")); // Short shouldn't have stack trace
    }

    @Test
    public void testThrowableAutomatic() {
        Exception ex = new RuntimeException("Test Error");
        LogEvent event = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("test")
                .message("msg")
                .throwable(ex)
                .build();

        // Pattern WITHOUT %ex
        PatternFormatter formatter = new PatternFormatter("%m");
        String output = formatter.format(event, null);
        
        assertTrue(output.startsWith("msg"));
        assertTrue("Should automatically append stack trace", output.contains("java.lang.RuntimeException: Test Error"));
        assertTrue(output.contains("at com.log4key"));
    }
    
    @Test
    public void testThrowableAutomaticNotAppendedIfPresent() {
        Exception ex = new RuntimeException("Test Error");
        LogEvent event = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("test")
                .message("msg")
                .throwable(ex)
                .build();

        // Pattern WITH %ex
        PatternFormatter formatter = new PatternFormatter("%m %ex{short}");
        String output = formatter.format(event, null);
        
        assertTrue(output.contains("msg"));
        assertTrue(output.contains("java.lang.RuntimeException: Test Error"));
        assertFalse("Should NOT contain full stack trace", output.contains("at com.log4key"));
    }
}
