package com.log4key.util;

import com.log4key.internal.InternalLogger;

/**
 * InternalLogger重构测试类
 * 测试新的getLogger方法和日志输出格式
 */
public class InternalLoggerRefactorTest {
    
    public static void main(String[] args) {
        System.out.println("Testing refactored InternalLogger...");
        
        // 测试通过类获取日志实例
        InternalLogger logger1 = InternalLogger.getLogger(InternalLoggerRefactorTest.class);
        System.out.println("Logger 1 name: " + logger1.getName());
        
        // 测试相同类多次调用返回同一实例
        InternalLogger logger2 = InternalLogger.getLogger(InternalLoggerRefactorTest.class);
        System.out.println("Logger 1 == Logger 2: " + (logger1 == logger2));
        
        // 测试通过名称获取日志实例
        InternalLogger logger3 = InternalLogger.getLogger("custom-logger");
        System.out.println("Logger 3 name: " + logger3.getName());
        
        // 测试DEBUG级别
        System.out.println("Debug enabled: " + logger1.isDebugEnabled());
        logger1.debug("Debug message from test class");
        logger1.debug("Debug message with args: {}, {}", "arg1", "arg2");
        
        // 测试INFO级别
        logger1.info("Info message from test class");
        logger1.info("Info message with args: {}, {}", "arg1", "arg2");
        
        // 测试WARN级别
        logger1.warn("Warn message from test class");
        logger1.warn("Warn message with args: {}, {}", "arg1", "arg2");
        
        // 测试异常处理
        try {
            throw new RuntimeException("Test exception");
        } catch (Exception e) {
            logger1.warn("Exception occurred: {}", e.getMessage(), e);
        }
        
        // 测试不同类的日志输出
        InternalLogger otherLogger = InternalLogger.getLogger(OtherClass.class);
        otherLogger.info("Info message from other class");
        
        System.out.println("Testing completed.");
    }
    
    /**
     * 测试用的其他类
     */
    private static class OtherClass {
        // 空类，用于测试不同类的日志输出
    }
}
