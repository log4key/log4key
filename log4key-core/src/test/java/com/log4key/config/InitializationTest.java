package com.log4key.config;

import com.log4key.LogManager;
import com.log4key.config.resolver.ConfigAccumulator;
import com.log4key.config.resolver.ConfigResolver;
import com.log4key.slf4j.Log4KeyLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 初始化时机测试
 */
public class InitializationTest {

    public static void main(String[] args) {
        // 测试场景1：通过配置文件的方式懒加载初始化
        testFileConfigInitialization();

        // 重置LogManager
        LogManager.reset();
        
        // 测试场景2：代码配置后手动初始化
        testCodeConfigInitialization();
    }

    /**
     * 测试通过配置文件的方式懒加载初始化
     */
    private static void testFileConfigInitialization() {
        System.out.println("\n=== Testing File Config Initialization ===");
        
        System.out.println("1. Before getLogger() - LogManager should not be initialized");
        
        // 第一次调用 getLogger()，应该触发初始化
        System.out.println("2. Calling LoggerFactory.getLogger() - should trigger initialization");
        Logger logger = Log4KeyLoggerFactory.getLogger(InitializationTest.class);
        
        System.out.println("3. After getLogger() - LogManager should be initialized");
        
        // 输出日志
        System.out.println("4. Outputting logs:");
        logger.info("Info message");
        logger.warn("Warn message");
        
        System.out.println("=== File Config Initialization Test Completed ===");
    }

    /**
     * 测试代码配置后手动初始化
     */
    private static void testCodeConfigInitialization() {
        System.out.println("\n=== Testing Code Config Initialization ===");
        
        System.out.println("1. Creating code config with new fluent API");
        // 创建配置解析器，使用新的链式代码配置
        ConfigAccumulator accumulator = new ConfigAccumulator();
        
        // 配置全局参数
        accumulator.global(ConfigKeys.DEFAULT_ADMISSION_LEVEL_KEY, "INFO");
        accumulator.global(ConfigKeys.ROOT_DIRECTORY_KEY, "logs");
        
        // 配置 formatter
        accumulator.formatter("TEXT_DEFAULT", formatter -> {
            formatter.type("Text");
            formatter.pattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %5level [%thread] %logger{36} : %msg%n");
        });
        
        // 配置 file appender
        accumulator.fileAppender("FILE", appender -> {
            appender.formatter("TEXT_DEFAULT");
            appender.level("INFO");
            appender.directory("./logs/code");
            appender.charset("UTF-8");
            appender.consoleEnabled(true);
        });
        
        // 配置 root logger
        accumulator.rootLogger(root -> {
            root.level("INFO");
            root.appenders("FILE");
        });

        System.out.println("2. Calling LogManager.initialize() - should initialize with code config");
        // 代码配置后手动初始化
        LogManager.ensureInitialized(accumulator.freeze());
        
        System.out.println("3. After initialize() - LogManager should be initialized");
        
        // 获取Logger实例
        System.out.println("4. Calling LoggerFactory.getLogger() - should use existing initialization");
        Logger logger = LoggerFactory.getLogger(InitializationTest.class);
        
        // 输出日志
        System.out.println("5. Outputting logs:");
        logger.info("Info message with code config");
        logger.warn("Warn message with code config");
        
        System.out.println("=== Code Config Initialization Test Completed ===");
    }
}
