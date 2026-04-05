package com.log4key.config;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * YAML配置加载测试
 */
public class Log4KeyYamlConfigurationTest {

    @Test
    public void testLoadFromYamlFile() throws IOException {
        // 创建临时YAML配置文件
        String yamlContent = "logLevel: DEBUG\n" +
                            "baseDirectory: ./target/test-yaml-logs\n" +
                            "asyncEnabled: true\n" +
                            "threadPoolSize: 4\n" +
                            "fileAppender:\n" +
                            "  baseDirectory: ./target/test-yaml-logs\n" +
                            "  asyncSupported: true\n";
        
        Path tempFile = Files.createTempFile("log4key-test", ".yaml");
        Files.write(tempFile, yamlContent.getBytes());
        
        try {
            // 加载YAML配置
            Map<String, Object> configMap = Log4KeyConfigurationLoader.loadFromFile(tempFile.toString());
            
            // 验证配置加载结果
            assertNotNull("Config map should not be null", configMap);
            assertFalse("Config map should not be empty", configMap.isEmpty());
            
            // 验证简单属性
            assertEquals("logLevel should be DEBUG", "DEBUG", configMap.get("logLevel"));
            assertEquals("baseDirectory should be ./target/test-yaml-logs", "./target/test-yaml-logs", configMap.get("baseDirectory"));
            assertEquals("asyncEnabled should be true", Boolean.TRUE, configMap.get("asyncEnabled"));
            assertEquals("threadPoolSize should be 4", 4, configMap.get("threadPoolSize"));
            
            // 验证嵌套属性
            assertEquals("fileAppender.baseDirectory should be ./target/test-yaml-logs", "./target/test-yaml-logs", configMap.get("fileAppender.baseDirectory"));
            assertEquals("fileAppender.asyncSupported should be true", Boolean.TRUE, configMap.get("fileAppender.asyncSupported"));
            
            System.out.println("YAML configuration loaded successfully: " + configMap);
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }
    
    @Test
    public void testLoadFromYmlFile() throws IOException {
        // 创建临时YML配置文件
        String ymlContent = "logLevel: INFO\n" +
                           "baseDirectory: ./target/test-yml-logs\n" +
                           "asyncEnabled: false\n";
        
        Path tempFile = Files.createTempFile("log4key-test", ".yml");
        Files.write(tempFile, ymlContent.getBytes());
        
        try {
            // 加载YML配置
            Map<String, Object> configMap = Log4KeyConfigurationLoader.loadFromFile(tempFile.toString());
            
            // 验证配置加载结果
            assertNotNull("Config map should not be null", configMap);
            assertFalse("Config map should not be empty", configMap.isEmpty());
            
            // 验证属性
            assertEquals("logLevel should be INFO", "INFO", configMap.get("logLevel"));
            assertEquals("baseDirectory should be ./target/test-yml-logs", "./target/test-yml-logs", configMap.get("baseDirectory"));
            assertEquals("asyncEnabled should be false", Boolean.FALSE, configMap.get("asyncEnabled"));
            
            System.out.println("YML configuration loaded successfully: " + configMap);
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }
}
