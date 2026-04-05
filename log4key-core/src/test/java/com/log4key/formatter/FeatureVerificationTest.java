package com.log4key.formatter;

import com.log4key.LogManager;
import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class FeatureVerificationTest {

    @Before
    public void setup() {
        // Ensure LogManager is initialized to set startTime
        // Accessing a static field or method should trigger initialization
        long ignored = LogManager.startTime;
    }

    @Test
    public void verifyRelativeTimeIncreasing() throws InterruptedException {
        PatternFormatter formatter = new PatternFormatter("%r");
        
        // Event 1
        LogEvent event1 = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg1")
                .build();
        String output1 = formatter.format(event1, null);
        assertTrue("Relative time 1 should be a number", output1.matches("-?\\d+"));
        long time1 = Long.parseLong(output1);

        // Wait a bit to ensure time increase
        Thread.sleep(50);

        // Event 2
        LogEvent event2 = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg2")
                .build();
        String output2 = formatter.format(event2, null);
        assertTrue("Relative time 2 should be a number", output2.matches("-?\\d+"));
        long time2 = Long.parseLong(output2);

        assertTrue("Relative time should increase. Time1: " + time1 + ", Time2: " + time2, time2 >= time1);
    }

    @Test
    public void verifyMdcOutput() {
        Map<String, Object> mdc = new HashMap<>();
        mdc.put("userId", "user123");
        mdc.put("requestId", "req-001");

        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg")
                .mdc(mdc)
                .build();

        // 1. Test specific key %X{userId}
        PatternFormatter formatterKey = new PatternFormatter("%X{userId}");
        String outputKey = formatterKey.format(event, null);
        assertEquals("user123", outputKey);

        // 2. Test full map %X
        PatternFormatter formatterMap = new PatternFormatter("%X");
        String outputMap = formatterMap.format(event, null);
        // Map toString format usually {key=value, key2=value2} but implementation details may vary
        // Just check if it contains the keys and values
        assertTrue(outputMap.contains("userId=user123"));
        assertTrue(outputMap.contains("requestId=req-001"));
        
        // 3. Test non-existent key
        PatternFormatter formatterMissing = new PatternFormatter("%X{missing}");
        String outputMissing = formatterMissing.format(event, null);
        assertEquals("", outputMissing); // Or null? usually empty string for missing keys in log patterns
    }

    @Test
    public void verifyMarkerOutput() {
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg")
                .markerName("SECURITY_AUDIT")
                .build();

        PatternFormatter formatter = new PatternFormatter("%marker");
        String output = formatter.format(event, null);
        assertEquals("SECURITY_AUDIT", output);
        
        // Test null marker
        LogEvent eventNoMarker = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test")
                .message("msg")
                .build();
        String outputNoMarker = formatter.format(eventNoMarker, null);
        assertEquals("", outputNoMarker); // Expect empty string for no marker
    }

    @Test
    public void verifyExceptionExplicit() {
        Exception ex = new RuntimeException("Explicit Error");
        LogEvent event = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("test")
                .message("msg")
                .throwable(ex)
                .build();

        PatternFormatter formatter = new PatternFormatter("Ex: %ex");
        String output = formatter.format(event, null);
        
        assertTrue(output.contains("Ex: java.lang.RuntimeException: Explicit Error"));
        assertTrue(output.contains("at com.log4key.formatter.FeatureVerificationTest"));
    }

    @Test
    public void verifyExceptionImplicitAppendAndNewLine() {
        Exception ex = new RuntimeException("Implicit Error");
        LogEvent event = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("test")
                .message("msg")
                .throwable(ex)
                .build();

        // Pattern without %ex
        PatternFormatter formatter = new PatternFormatter("Msg: %m");
        String output = formatter.format(event, null);
        
        String expectedStart = "Msg: msg" + System.lineSeparator();
        assertTrue("Output should start with message followed by newline", output.startsWith(expectedStart));
        assertTrue("Output should contain exception class and message", output.contains("java.lang.RuntimeException: Implicit Error"));
        assertTrue("Output should contain stack trace", output.contains("at com.log4key.formatter.FeatureVerificationTest"));
    }

    @Test
    public void verifyExceptionTruncation() {
        Exception ex = new RuntimeException("Truncated Error");
        // Fill stack trace
        ex.fillInStackTrace();
        
        LogEvent event = LogEventBuilder.builder()
                .level("ERROR")
                .loggerName("test")
                .message("msg")
                .throwable(ex)
                .build();

        // %ex{2} - should output first line (exception msg) + 2 lines of stack trace? 
        // Or 2 lines total?
        // Usually %ex{n} means n lines of stack trace. Let's verify standard behavior.
        // If the requirement says "%ex{2} output first two lines", it usually implies the exception line + 1 stack trace line, OR exception line + 2 stack trace lines.
        // Let's check strict output line count.
        
        PatternFormatter formatter = new PatternFormatter("%ex{2}");
        String output = formatter.format(event, null);
        
        String[] lines = output.split(System.lineSeparator());
        // Depending on implementation:
        // Case A: 2 lines of stack trace (excluding the exception name/message line)
        // Case B: 2 lines total
        
        // Let's assume based on common log libraries (Log4j2), %ex{n} prints the first n lines of the stack trace.
        // However, usually line 0 is the exception toString(), followed by stack trace elements.
        
        // Let's assert based on what we see.
        assertTrue("Output should not be empty", output.length() > 0);
        assertTrue("Should contain exception message", output.contains("Truncated Error"));
        
        // Count lines that start with "at " (stack trace lines)
        int atCount = 0;
        for (String line : lines) {
            if (line.trim().startsWith("at ")) {
                atCount++;
            }
        }
        
        // If %ex{2} means 2 lines of output (including the exception message line):
        // Line 1: java.lang.RuntimeException: Truncated Error
        // Line 2: at ...
        
        int lineCount = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                lineCount++;
            }
        }

        if (lineCount != 2) {
             System.out.println("DEBUG: Actual output for %ex{2}:");
             System.out.println(output);
             System.out.println("DEBUG: lineCount = " + lineCount);
        }

        assertEquals("Should have exactly 2 lines of output", 2, lineCount);
        assertTrue("Should contain exception message", lines[0].contains("Truncated Error"));
        assertTrue("Second line should be stack trace", lines[1].trim().startsWith("at "));
    }
}
