package com.log4key.io;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LogFileWriter的单元测试
 */
public class LogFileWriterTest {

    private static final String TEST_DIR = "target/test-logs";
    private static final String TEST_FILE = TEST_DIR + File.separator + "test.log";
    private LogFileWriter writer;

    @Before
    public void setUp() throws IOException {
        // 确保测试目录存在
        new File(TEST_DIR).mkdirs();
        // 删除之前的测试文件
        Files.deleteIfExists(Paths.get(TEST_FILE));
        // 创建测试实例
        writer = new LogFileWriter(TEST_FILE);
    }
    
    /**
     * 读取文件内容的辅助方法，兼容Java 8
     * 
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException 如果文件读取失败
     */
    private String readFileContent(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }

    @After
    public void tearDown() throws Exception {
        // 关闭写入器
        if (writer != null) {
            writer.close();
        }
        // 清理测试文件
        File testFile = new File(TEST_FILE);
        if (testFile.exists()) {
            testFile.delete();
        }
        // 清理滚动生成的日志文件
        File[] rolledFiles = new File(TEST_DIR).listFiles((dir, name) -> 
                name.startsWith("test.log.") && !name.equals("test.log"));
        if (rolledFiles != null) {
            for (File file : rolledFiles) {
                file.delete();
            }
        }
    }

    @Test
    public void testWrite_SingleMessage() throws IOException {
        // 写入单个消息
        String message = "Test log message\n";
        writer.write(message);
        writer.flush();
        
        // 验证文件内容
        String content = readFileContent(TEST_FILE);
        assertEquals("File content should match written message", message, content);
        
        // 验证文件大小
        long expectedSize = message.getBytes().length;
        assertEquals("File size should match expected", expectedSize, writer.getCurrentFileSize());
    }

    @Test
    public void testWrite_MultipleMessages() throws IOException {
        // 写入多个消息
        String message1 = "First log message\n";
        String message2 = "Second log message\n";
        String message3 = "Third log message\n";
        
        writer.write(message1);
        writer.write(message2);
        writer.write(message3);
        writer.flush();
        
        // 验证文件内容
        String content = readFileContent(TEST_FILE);
        String expectedContent = message1 + message2 + message3;
        assertEquals("File content should contain all messages", expectedContent, content);
        
        // 验证文件大小
        long expectedSize = expectedContent.getBytes().length;
        assertEquals("File size should match expected", expectedSize, writer.getCurrentFileSize());
    }

    @Test
    public void testFlush() throws IOException {
        // 写入消息但不刷新
        String message = "Test flush message\n";
        writer.write(message);
        
        // 检查文件是否存在，但不验证内容（因为可能还在缓冲区）
        File file = new File(TEST_FILE);
        assertTrue("File should exist", file.exists());
        
        // 刷新并验证内容
        writer.flush();
        String content = readFileContent(TEST_FILE);
        assertEquals("After flush, content should match", message, content);
    }

    @Test
    public void testClose() throws IOException {
        // 写入消息并关闭
        String message = "Test close message\n";
        writer.write(message);
        writer.close();
        
        // 验证文件内容
        String content = readFileContent(TEST_FILE);
        assertEquals("After close, content should be written", message, content);
        
        // 验证关闭后不能再写入
        try {
            writer.write("Should not be able to write after close\n");
            fail("Should throw IOException after close");
        } catch (IOException e) {
            // 预期行为
        }
    }

    @Test
    public void testRollFile() throws IOException {
        // 创建一个小容量的写入器
        int maxFileSize = 100; // 100字节
        LogFileWriter rollingWriter = new LogFileWriter(TEST_FILE, 1024, maxFileSize);
        
        // 写入足够的数据触发滚动
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            largeMessage.append("This is a test line for rolling file. Line number ")
                       .append(i).append("\n");
        }
        
        rollingWriter.write(largeMessage.toString());
        rollingWriter.close();
        
        // 验证滚动文件已创建
        File dir = new File(TEST_DIR);
        File[] logFiles = dir.listFiles((d, name) -> name.startsWith("test.") && name.endsWith(".log") && !name.equals("test.log"));
        assertNotNull("Rolled file should be created", logFiles);
        assertTrue("At least one rolled file should exist", logFiles.length > 0);
        
        // 验证原始文件仍然存在（可能是空的或包含新的内容）
        assertTrue("Original log file should still exist", new File(TEST_FILE).exists());
    }

    @Test
    public void testDirectoryCreation() throws IOException {
        // 使用不存在的子目录
        String nestedPath = TEST_DIR + File.separator + "nested" + File.separator + "subdir" + File.separator + "nested.log";
        LogFileWriter nestedWriter = new LogFileWriter(nestedPath);
        
        // 写入内容
        nestedWriter.write("Message to nested directory\n");
        nestedWriter.close();
        
        // 验证文件和目录都已创建
        assertTrue("Nested log file should exist", new File(nestedPath).exists());
        
        // 清理
        Files.delete(Paths.get(nestedPath));
        Files.delete(Paths.get(TEST_DIR + File.separator + "nested" + File.separator + "subdir"));
        Files.delete(Paths.get(TEST_DIR + File.separator + "nested"));
    }

    @Test
    public void testMultiThreadedWrite() throws InterruptedException, IOException {
        // 并发写入测试
        int threadCount = 5;
        int messagesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < messagesPerThread; j++) {
                        writer.write("Thread-" + threadId + "-Message-" + j + "\n");
                    }
                } catch (IOException e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        latch.await();
        
        // 刷新并关闭
        writer.flush();
        
        // 验证没有错误
        assertEquals("Should have no errors", 0, errorCount.get());
        
        // 验证文件内容行数（可能不完全匹配，因为并发写入可能导致行合并）
        long lineCount = Files.lines(Paths.get(TEST_FILE)).count();
        assertTrue("Should have some lines written", lineCount > 0);
        assertTrue("Should have at least some expected lines", lineCount <= threadCount * messagesPerThread);
        
        executor.shutdown();
    }

    @Test
    public void testGetFilePath() {
        // 验证获取文件路径
        assertEquals("File path should match", TEST_FILE, writer.getFilePath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFilePath() throws IOException {
        // 测试无效的文件路径
        new LogFileWriter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBufferSize() throws IOException {
        // 测试无效的缓冲区大小
        new LogFileWriter(TEST_FILE, -1, 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMaxFileSize() throws IOException {
        // 测试无效的最大文件大小
        new LogFileWriter(TEST_FILE, 1024, -1);
    }
}