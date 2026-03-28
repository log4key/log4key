package com.log4key.api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * ILogKey接口的单元测试
 */
public class ILogKeyTest {
    
    @Test
    public void testDefaultLogKeyCreation() {
        // 测试默认主键创建
        ILogKey logKey = DefaultLogKey.of("test-key");
        assertNotNull("LogKey should not be null", logKey);
        assertEquals("Key value should match", "test-key", logKey.value());
        assertEquals("Default node ID should be 0", 0, logKey.getNodeId());
    }
    
    @Test
    public void testLogKeyWithNodeId() {
        // 测试带节点ID的主键创建
        ILogKey logKey = DefaultLogKey.of("test-key", 1001);
        assertNotNull("LogKey should not be null", logKey);
        assertEquals("Key value should match", "test-key", logKey.value());
        assertEquals("Node ID should match", 1001, logKey.getNodeId());
    }
    
    @Test
    public void testNodeIdSetter() {
        // 测试节点ID设置器
        ILogKey logKey = DefaultLogKey.of("test-key");
        logKey.setNodeId(2002);
        assertEquals("Node ID should be updated", 2002, logKey.getNodeId());
    }
    
    @Test
    public void testHashCode() {
        // 测试哈希码生成
        ILogKey logKey1 = DefaultLogKey.of("test-key", 1001);
        ILogKey logKey2 = DefaultLogKey.of("test-key", 1001);
        ILogKey logKey3 = DefaultLogKey.of("test-key", 2002);
        ILogKey logKey4 = DefaultLogKey.of("different-key", 1001);
        
        assertEquals("Hash codes should be equal for identical keys", 
                logKey1.hashCode(), logKey2.hashCode());
        assertNotEquals("Hash codes should be different for different node IDs", 
                logKey1.hashCode(), logKey3.hashCode());
        assertNotEquals("Hash codes should be different for different key values", 
                logKey1.hashCode(), logKey4.hashCode());
    }
    
    @Test
    public void testEquals() {
        // 测试equals方法
        ILogKey logKey1 = DefaultLogKey.of("test-key", 1001);
        ILogKey logKey2 = DefaultLogKey.of("test-key", 1001);
        ILogKey logKey3 = DefaultLogKey.of("test-key", 2002);
        
        assertTrue("Identical keys should be equal", logKey1.equals(logKey2));
        assertFalse("Keys with different node IDs should not be equal", logKey1.equals(logKey3));
        assertFalse("Key should not equal null", logKey1.equals(null));
        assertFalse("Key should not equal different object type", logKey1.equals("test-key"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullKeyCreation() {
        // 测试创建null键时的异常
        DefaultLogKey.of(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyKeyCreation() {
        // 测试创建空键时的异常
        DefaultLogKey.of("");
    }
}