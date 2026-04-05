package com.log4key.config;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test config merge logic: appender config should override base config
 */
public class Log4KeyConfigurationMergeTest {
    
    private Log4KeyConfiguration config;
    
    @Before
    public void setUp() {
        // Get fresh config instance for each test
        config = Log4KeyConfiguration.getInstance();
        config.clear();
    }
    
    @Test
    public void testAppenderConfigOverrideBaseConfig() {
        // Load all config at once (loadFromMap replaces previous config, not merges)
        Map<String, Object> fullConfig = new HashMap<>();
        // Base config
        fullConfig.put("defaultDirectory", "./logs/base");
        fullConfig.put("maxFileSizeMB", 50);
        fullConfig.put("maxBackupIndex", 5);
        fullConfig.put("rootLogger.appenders", "file");
        // Appender config with override values
        fullConfig.put("appenders.file.type", "File");
        fullConfig.put("appenders.file.formatter", "json");
        fullConfig.put("appenders.file.directory", "./logs/appender"); // Override base config
        fullConfig.put("appenders.file.maxFileSizeMB", 100); // Override base config
        // Define formatter to avoid validation error
        fullConfig.put("formatters.json.type", "JsonLogFormatter");
        config.loadFromMap(fullConfig);
        
        // Test config merge
        Map<String, Object> mergedConfig = config.mergeAppenderConfig("file");
        assertNotNull("Merged config should not be null", mergedConfig);
        
        // Verify appender config overrides base config
        assertEquals("./logs/appender", mergedConfig.get("directory"));
        assertEquals(100, mergedConfig.get("maxFileSizeMB"));
        
        // Verify base config is used for missing appender config
        assertEquals(5, mergedConfig.get("maxBackupIndex"));
    }
    
    @Test
    public void testConsoleAppenderUsesBaseConfig() {
        // 1. Set base config with complete appender definition
        Map<String, Object> baseConfig = new HashMap<>();
        baseConfig.put("defaultDirectory", "./logs/base");
        baseConfig.put("maxFileSizeMB", 50);
        baseConfig.put("rootLogger.appenders", "console");
        // Define appender in base config
        baseConfig.put("appenders.console.type", "Console");
        baseConfig.put("appenders.console.formatter", "text");
        // Define formatter to avoid validation error
        baseConfig.put("formatters.text.type", "TextLogFormatter");
        config.loadFromMap(baseConfig);
        
        // 2. Test config merge - console appender should use base config for directory
        Map<String, Object> mergedConfig = config.mergeAppenderConfig("console");
        assertNotNull("Merged config should not be null", mergedConfig);
        
        // 3. Verify console appender uses base config for directory
        assertEquals("./logs/base", mergedConfig.get("directory"));
        assertEquals(50, mergedConfig.get("maxFileSizeMB"));
    }
}