package com.log4key.slf4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J实现的测试类
 * 用于验证Log4Key的SLF4J实现是否正常工作
 */
public class SLF4JImplementationTest {
    
    @Test
    public void testSLF4JImplementation() {
        // 使用SLF4J的API获取Logger实例
        Logger logger = LoggerFactory.getLogger(SLF4JImplementationTest.class);
        
        // 测试不同级别的日志记录
        logger.trace("This is a TRACE level message");
        logger.debug("This is a DEBUG level message");
        logger.info("This is an INFO level message");
        logger.warn("This is a WARN level message");
        logger.error("This is an ERROR level message");
        
        // 测试参数化日志
        logger.info("Hello, {}! You are visitor number {}.", "World", 100);
        
        // 测试异常日志
        try {
            int result = 10 / 0;
        } catch (Exception e) {
            logger.error("An error occurred while dividing numbers", e);
        }
        
        // 测试Logger名称
        System.out.println("Logger name: " + logger.getName());
        
        // 验证Logger实例类型 - 使用JUnit断言替代Java内置assert
        assertTrue(logger instanceof Log4KeyLogger, "Logger should be an instance of Log4KeyLogger");
        System.out.println("SLF4J implementation test completed successfully!");
    }
}