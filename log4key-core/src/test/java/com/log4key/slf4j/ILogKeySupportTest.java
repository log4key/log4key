package com.log4key.slf4j;

import com.log4key.api.DefaultLogKey;
import com.log4key.api.ILogKey;
import org.junit.Test;

/**
 * 测试Log4KeyLogger对ILogKey参数的支持
 */
public class ILogKeySupportTest {
    
    @Test
    public void testILogKeySupport() {
        // 获取Logger实例
        Log4KeyLogger logger = new Log4KeyLogger("ILogKeySupportTest");
        
        // 创建ILogKey实例
        ILogKey logKey = DefaultLogKey.of("test-key-123");
        
        // 测试不同日志级别下的ILogKey参数支持
        logger.debug(logKey, "Debug message with ILogKey");
        logger.info(logKey, "Info message with ILogKey");
        logger.warn(logKey, "Warn message with ILogKey");
        logger.error(logKey, "Error message with ILogKey");
        
        // 测试带格式化参数的ILogKey支持
        logger.debug(logKey, "Debug message with ILogKey and param: {}", "test-param");
        logger.info(logKey, "Info message with ILogKey and params: {} {}", "param1", "param2");
        
        // 测试带异常的ILogKey支持
        logger.error(logKey, "Error message with ILogKey and exception", new RuntimeException("Test exception"));
        
        System.out.println("ILogKey support test completed successfully");
    }
}