package com.log4key.formatter;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * PatternFormatter 格式化测试类
 * 专门测试格式修饰符（对齐、截断等）
 */
public class PatternFormatterFormattingTest {

    @Test
    public void testLevelFormatting() {
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test.Logger")
                .message("test message")
                .build();

        // 1. %-5level: "INFO " (left align, min 5)
        verifyFormat("%-5level", event, "INFO ");

        // 2. %5level: " INFO" (right align, min 5)
        verifyFormat("%5level", event, " INFO");

        // 3. %.3level: "INF" (max 3, truncate)
        verifyFormat("%.3level", event, "INF");

        // 4. %10.10level: "      INFO" (min 10, max 10, right align)
        verifyFormat("%10.10level", event, "      INFO");
    }

    private void verifyFormat(String pattern, LogEvent event, String expected) {
        PatternFormatter formatter = new PatternFormatter(pattern);
        String result = formatter.format(event, null);
        assertEquals("Pattern '" + pattern + "' failed", expected, result);
    }
}
