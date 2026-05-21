package com.log4key.appender;

import com.log4key.api.LogEvent;
import com.log4key.api.LogEventBuilder;
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
 */
public class FileAppenderLevelInclusionTest {

    private static final String TEST_DIR = "target/test-level-inclusion";
    private FileAppender appender;
    private SimpleDateFormat dateFormat;

    @Before
    public void setUp() throws IOException {
        // Ensure test directory exists and is clean
        cleanupTestDir();
        new File(TEST_DIR).mkdirs();

        // Create FileAppender instance
        appender = new FileAppender();

        // Initialize appender with levelInclusion=true
        Map<String, Object> config = new HashMap<>();
        config.put("rootDirectory", TEST_DIR);
        config.put("levelInclusion", true);
        config.put("appenderName", "testInclusion");
        config.put("asyncSupported", false); // Sync mode for easier testing
        appender.initialize(config);

        dateFormat = new SimpleDateFormat("yyyyMMdd");
    }

    @After
    public void tearDown() throws IOException {
        // Close appender
        if (appender != null) {
            appender.close();
        }
        // Cleanup test directory
        cleanupTestDir();
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
    public void testLevelInclusion() throws IOException {
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
        appender.flush();

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
        appender.flush();

        // 7. Assert that file exists in info/... but NOT in warn/... (or warn file content doesn't increase)
        // Since warnFile already exists, we check that it DOES NOT contain the new INFO message
        warnContent = new String(Files.readAllBytes(warnFile));
        infoContent = new String(Files.readAllBytes(infoFile));

        assertTrue("INFO file content should contain INFO message", infoContent.contains("This is an INFO message"));
        assertFalse("WARN file content should NOT contain INFO message", warnContent.contains("This is an INFO message"));
    }
}
