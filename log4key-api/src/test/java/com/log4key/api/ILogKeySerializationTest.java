package com.log4key.api;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

/**
 * ILogKey序列化测试类
 * 验证ILogKey接口及其实现类的序列化功能
 */
public class ILogKeySerializationTest {

    /**
     * 测试ILogKey接口的序列化功能
     */
    @Test
    public void testILogKeySerialization() throws IOException, ClassNotFoundException {
        // 创建ILogKey实例
        ILogKey originalKey = DefaultLogKey.of("test-key", 12345L);
        
        // 序列化ILogKey对象
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalKey);
        oos.close();
        
        // 反序列化ILogKey对象
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ILogKey deserializedKey = (ILogKey) ois.readObject();
        ois.close();
        
        // 验证序列化和反序列化的对象是否相等
        assertNotNull(deserializedKey);
        assertEquals(originalKey.value(), deserializedKey.value());
        assertEquals(originalKey.getNodeId(), deserializedKey.getNodeId());
        assertEquals(originalKey, deserializedKey);
        assertEquals(originalKey.hashCode(), deserializedKey.hashCode());
    }
    
    /**
     * 测试LogEvent类中logKey字段的序列化功能
     */
    @Test
    public void testLogEventLogKeySerialization() throws IOException, ClassNotFoundException {
        // 创建LogEvent实例
        DefaultLogKey logKey = DefaultLogKey.of("test-key-123");
        LogEvent originalEvent = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test-logger")
                .message("Test log message")
                .logKey(logKey)
                .build();
        
        // 序列化LogEvent对象，捕获详细的堆栈跟踪
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(originalEvent);
            oos.close();
        } catch (NotSerializableException e) {
            System.out.println("序列化失败，详细信息：");
            e.printStackTrace();
            // 找出不可序列化的字段
            System.out.println("不可序列化的对象：" + e.getMessage());
            
            // 打印LogEvent类的所有字段
            System.out.println("LogEvent类的字段：");
            java.lang.reflect.Field[] fields = LogEvent.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(originalEvent);
                    boolean isTransient = (field.getModifiers() & java.lang.reflect.Modifier.TRANSIENT) != 0;
                    boolean isSerializable = value instanceof Serializable;
                    System.out.printf("%s: transient=%b, value=%s, serializable=%b\n", 
                            field.getName(), isTransient, value, isSerializable);
                } catch (IllegalAccessException ex) {
                    System.out.printf("%s: 无法访问该字段，原因：%s\n", field.getName(), ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (oos != null) {
                oos.close();
            }
        }
        
        // 反序列化LogEvent对象
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        LogEvent deserializedEvent = (LogEvent) ois.readObject();
        ois.close();
        
        // 验证序列化和反序列化的对象是否相等
        assertNotNull(deserializedEvent);
        assertNotNull(deserializedEvent.getLogKey());
        assertEquals(originalEvent.getLogKey().value(), deserializedEvent.getLogKey().value());
        assertEquals(originalEvent.getLogKey().getNodeId(), deserializedEvent.getLogKey().getNodeId());
        assertEquals(originalEvent.getLevel(), deserializedEvent.getLevel());
        assertEquals(originalEvent.getLoggerName(), deserializedEvent.getLoggerName());
        assertEquals(originalEvent.getMessage(), deserializedEvent.getMessage());
    }
    
    /**
     * 测试LogEvent类的key和nodeId字段是否能正确从logKey中获取
     */
    @Test
    public void testLogEventKeyAndNodeIdFromLogKey() {
        // 创建LogEvent实例
        ILogKey logKey = DefaultLogKey.of("test-key", 54321L);
        LogEvent event = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test-logger")
                .message("Test log message")
                .logKey(logKey)
                .build();
        
        // 验证key和nodeId字段是否正确设置
        assertEquals(logKey.value(), event.getKey());
        assertEquals(String.valueOf(logKey.getNodeId()), event.getNodeId());
        
        // 修改logKey的nodeId
        logKey.setNodeId(98765L);
        // 注意：由于LogEvent是不可变的，所以需要重新创建LogEvent实例
        LogEvent updatedEvent = LogEventBuilder.builder()
                .level("INFO")
                .loggerName("test-logger")
                .message("Test log message")
                .logKey(logKey)
                .build();
        
        // 验证key和nodeId字段是否正确更新
        assertEquals(logKey.value(), updatedEvent.getKey());
        assertEquals(String.valueOf(logKey.getNodeId()), updatedEvent.getNodeId());
    }
}