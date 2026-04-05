package com.log4key.formatter.token;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ClassNameTokenTest {

    private void assertAbbreviate(String loggerName, String targetLength, String expected) {
        ClassNameToken token = new ClassNameToken(targetLength);
        LogEvent event = LogEventBuilder.builder()
                .loggerName(loggerName)
                .level("INFO")
                .message("test")
                .build();
        StringBuilder sb = new StringBuilder();
        token.render(event, sb);
        assertEquals(expected, sb.toString());
    }

    @Test
    public void testScenario1_LengthEnough() {
        // 1. 长度足够时不缩写：`com.foo.Bar`, 20 -> `com.foo.Bar`
        assertAbbreviate("com.foo.Bar", "20", "com.foo.Bar");
    }

    @Test
    public void testScenario2_ProgressiveAbbreviation() {
        // 2. 逐级缩写：`com.foo.Bar`, 8 -> `c.f.Bar`
        // com.foo.Bar (11 chars)
        // target 8
        // 1. com -> c. (len 11 -> 9) > 8, continue
        // 2. foo -> f. (len 9 -> 7) <= 8, stop? No, logic is greedy based on currentLength > targetLength
        
        // Let's trace logic:
        // currentLength = 11
        // segment 1: "com", len=3. current(11) > target(8) && len > 1 -> abbreviate "c". current = 11 - 2 = 9.
        // segment 2: "foo", len=3. current(9) > target(8) && len > 1 -> abbreviate "f". current = 9 - 2 = 7.
        // segment 3 (class): Bar.
        // Result: c.f.Bar
        assertAbbreviate("com.foo.Bar", "8", "c.f.Bar");
    }

    @Test
    public void testScenario3_MixedAbbreviation() {
        // 3. 混合缩写：`org.apache.catalina.core.StandardWrapperValve`, 36 -> `o.a.c.core.StandardWrapperValve`
        // Original length: 46
        // Target: 36
        // 1. org -> o (46 -> 44) > 36
        // 2. apache -> a (44 -> 39) > 36
        // 3. catalina -> c (39 -> 32) <= 36. Next segments won't abbreviate because currentLength(32) <= targetLength(36) is false?
        // Wait, logic says: boolean shouldAbbreviate = (currentLength > targetLength) && (segmentLen > 1);
        // So once currentLength <= targetLength, it stops abbreviating.
        
        // Trace:
        // "org"(3) -> "o"(1). len 46->44.
        // "apache"(6) -> "a"(1). len 44->39.
        // "catalina"(8) -> "c"(1). len 39->32.
        // "core"(4). current(32) <= target(36). No abbreviate.
        // Result: o.a.c.core.StandardWrapperValve
        assertAbbreviate("org.apache.catalina.core.StandardWrapperValve", "36", "o.a.c.core.StandardWrapperValve");
    }

    @Test
    public void testScenario4_ClassNameNeverAbbreviated() {
        // 4. 类名永不缩写：`com.foo.VeryLongClassName`, 10 -> `c.f.VeryLongClassName`
        // com.foo.VeryLongClassName (25 chars)
        // target 10
        // com -> c (23)
        // foo -> f (21)
        // VeryLongClassName -> never touched
        // Result: c.f.VeryLongClassName (length 21)
        assertAbbreviate("com.foo.VeryLongClassName", "10", "c.f.VeryLongClassName");
    }

    @Test
    public void testScenario5_NoPackage() {
        // 5. 无包名：`SimpleClass`, 5 -> `SimpleClass`
        // Logic: if dotCount == 0 return className
        assertAbbreviate("SimpleClass", "5", "SimpleClass");
        
        // Case where class name is longer than target
        assertAbbreviate("VeryLongSimpleClass", "5", "VeryLongSimpleClass");
    }

    @Test
    public void testScenario6_EdgeCases() throws Exception {
        // 6. 边界情况：空字符串、null、长度参数为0或负数
        
        // Null logger name -> LogEventBuilder prevents null, so we use reflection to force it
        // to verify ClassNameToken robustness
        ClassNameToken token = new ClassNameToken("10");
        LogEvent event = LogEventBuilder.builder()
                .loggerName("placeholder")
                .level("INFO")
                .message("test")
                .build();
        
        java.lang.reflect.Field loggerNameField = LogEvent.class.getDeclaredField("loggerName");
        loggerNameField.setAccessible(true);
        loggerNameField.set(event, null);
        
        StringBuilder sb = new StringBuilder();
        token.render(event, sb);
        assertEquals("unknown", sb.toString()); // "unknown" length 7 <= 10

        // Empty string
        assertAbbreviate("", "10", "");

        // Target length 0 -> should probably behave like -1 (no abbreviation) or try to shorten everything?
        // Constructor:
        // parsedLength = Integer.parseInt("0") -> 0
        // render: if (targetLength > 0) -> 0 is not > 0.
        // So it behaves as full logger name.
        assertAbbreviate("com.foo.Bar", "0", "com.foo.Bar");

        // Negative length
        assertAbbreviate("com.foo.Bar", "-1", "com.foo.Bar");
        
        // Invalid number string
        assertAbbreviate("com.foo.Bar", "abc", "com.foo.Bar");
    }
    
    @Test
    public void testEdgeCase_JustEnoughLength() {
        // Length matches exactly
        assertAbbreviate("com.foo.Bar", "11", "com.foo.Bar");
    }
    
    @Test
    public void testEdgeCase_OneCharSegments() {
        // a.b.c.Bar (9 chars)
        // target 5
        // a -> a (len 1, cannot shorten). 
        // b -> b
        // c -> c
        // Result: a.b.c.Bar
        assertAbbreviate("a.b.c.Bar", "5", "a.b.c.Bar");
    }
}
