package com.log4key.channel;

import com.log4key.metrics.IoMetrics;
import com.log4key.path.PathKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * FileChannel 单元测试
 */
public class FileChannelTest {

    private static final String TEST_DIR = "target/test-channel";
    private static final String TEST_FILE_NAME = "test.log";
    private Path testDir;
    private FileChannel channel;

    @Before
    public void setUp() throws IOException {
        // 重置 IoMetrics 计数器
        IoMetrics.reset();

        testDir = Paths.get(TEST_DIR);
        // 清理并创建测试目录
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(testDir);

        PathKey pathKey = new PathKey(TEST_DIR, TEST_FILE_NAME);
        channel = new FileChannel(pathKey, testDir, TEST_FILE_NAME, "UTF-8", 10 * 1024 * 1024);
    }

    @After
    public void tearDown() throws IOException {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
                // 忽略关闭异常
            }
        }
        // 清理测试目录
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * 测试 append 后 estimatedBytes 正确增长。
     *
     * estimatedBytes 使用近似统计：message.length() * 2。
     */
    @Test
    public void testAppend_EstimatedBytesGrowth() {
        String msg = "Hello";
        // estimatedBytes 初始为 0
        assertEquals("初始 estimatedBytes 应为 0", 0L, channel.getEstimatedBytes());

        channel.append(msg);
        // msg.length() = 5, 5 * 2 = 10
        assertEquals("append 后 estimatedBytes 应为 message.length() * 2",
                10L, channel.getEstimatedBytes());

        channel.append("World");
        // "World".length() = 5, 5 * 2 = 10, 累计 20
        assertEquals("第二次 append 后 estimatedBytes 应累加",
                20L, channel.getEstimatedBytes());
    }

    /**
     * 测试 shouldFlush 三个条件分别触发。
     */
    @Test
    public void testShouldFlush_ThreeConditions() throws InterruptedException, IOException {
        // 条件1: estimatedBytes >= batchSize
        channel.append("12345"); // 5 chars * 2 = 10 bytes
        assertTrue("estimatedBytes >= batchSize(10) 应触发 flush",
                channel.shouldFlush(10, 10000, 100000));

        // 条件2: (now - lastFlushTime) >= flushIntervalMs
        // 先 flush 重置 lastFlushTime
        IoMetrics.reset();
        channel = createFreshChannel();
        long lastFlush = channel.getLastFlushTime();
        Thread.sleep(2); // 等待 2ms，确保时间差 >= 1ms
        assertTrue("时间间隔 >= flushIntervalMs(1) 应触发 flush",
                channel.shouldFlush(100000, 1, 100000));

        // 条件3: estimatedBytes >= highWaterMark
        IoMetrics.reset();
        channel = createFreshChannel();
        channel.append("12345"); // 10 bytes
        assertTrue("estimatedBytes >= highWaterMark(10) 应触发 flush",
                channel.shouldFlush(100000, 10000, 10));
    }

    /**
     * 测试 flush 后 buffer 清空且 estimatedBytes 归零。
     */
    @Test
    public void testFlush_ClearsBufferAndResetsEstimatedBytes() throws IOException {
        channel.append("Hello, World!");
        assertTrue("buffer 应有内容", channel.getBufferContent().length() > 0);
        assertTrue("estimatedBytes 应 > 0", channel.getEstimatedBytes() > 0);

        channel.flush(100, 10000, 100000, 4096);

        assertEquals("flush 后 buffer 应清空", 0, channel.getBufferContent().length());
        assertEquals("flush 后 estimatedBytes 应归零", 0L, channel.getEstimatedBytes());
    }

    /**
     * 测试 buffer 容量超过 highWaterMark 后新建 vs setLength(0) 复用。
     */
    @Test
    public void testFlush_BufferCapacityHighWaterMark() throws IOException {
        // 获取当前 buffer 引用
        int initialCapacity = channel.getBufferCapacity();

        // 场景1: highWaterMark 大于当前 capacity，应复用（setLength(0)）
        channel.append("test");
        channel.flush(100, 10000, initialCapacity + 100, 4096);
        // capacity 未超过 highWaterMark，应保持原有 capacity
        int capacityAfterFlush = channel.getBufferCapacity();
        assertEquals("highWaterMark 大于 capacity 时应复用 buffer，capacity 不变",
                initialCapacity, capacityAfterFlush);

        IoMetrics.reset();
        channel = createFreshChannel();

        // 场景2: highWaterMark 小于当前 capacity，应新建
        // 先让 buffer 容量膨胀（append 大量数据）
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            largeData.append("x");
        }
        channel.append(largeData.toString());
        int grownCapacity = channel.getBufferCapacity();
        assertTrue("追加大量数据后 capacity 应增长", grownCapacity > 4096);

        // 以很小的 highWaterMark flush，应触发新建
        channel.flush(100, 10000, 100, 4096);
        assertEquals("highWaterMark 小于原 capacity 时应新建 buffer，capacity 回到 initialBufferSize",
                4096, channel.getBufferCapacity());
    }

    /**
     * 测试 rolling 文件命名格式正确。
     *
     * 格式: 原名.yyyyMMdd.HHmmss.扩展名，例如: test.log → test.20260606.143012.log
     */
    @Test
    public void testRolling_FileNameFormat() throws IOException {
        // 先写入一些内容，使文件存在
        channel.append("content");
        channel.flush(100, 10000, 100000, 4096);

        File currentFile = channel.getCurrentFile();
        assertTrue("当前文件应存在", currentFile.exists());

        // 执行滚动
        channel.rollFile();

        // 验证滚动后的文件命名格式
        File[] rolledFiles = testDir.toFile().listFiles(
                (dir, name) -> name.startsWith("test.") && name.endsWith(".log") && !name.equals(TEST_FILE_NAME));
        assertNotNull("应有滚动文件", rolledFiles);
        assertTrue("至少有一个滚动文件", rolledFiles.length > 0);

        String rolledName = rolledFiles[0].getName();
        // 格式: test.yyyyMMdd.HHmmss.log
        assertTrue("滚动文件名应以 'test.' 开头", rolledName.startsWith("test."));
        assertTrue("滚动文件名应以 '.log' 结尾", rolledName.endsWith(".log"));

        // 去掉 "test." 前缀和 ".log" 后缀，中间部分应为时间戳格式
        String middle = rolledName.substring("test.".length(), rolledName.length() - ".log".length());
        // 时间戳格式: yyyyMMdd.HHmmss，共 15 字符
        assertEquals("时间戳应为 15 字符", 15, middle.length());
        assertTrue("时间戳应包含 '.'", middle.contains("."));
        // 验证格式: 8位日期 + '.' + 6位时间
        String[] parts = middle.split("\\.");
        assertEquals("时间戳应分为两部分（日期.时间）", 2, parts.length);
        assertEquals("日期部分应为 8 位", 8, parts[0].length());
        assertEquals("时间部分应为 6 位", 6, parts[1].length());
    }

    /**
     * 测试 rolling 后文件内容完整性。
     */
    @Test
    public void testRolling_ContentIntegrity() throws IOException {
        // 写入内容并 flush
        String message = "Before roll content";
        channel.append(message);
        channel.flush(100, 10000, 100000, 4096);

        // 读取当前文件内容
        File currentFile = channel.getCurrentFile();
        String contentBefore = new String(Files.readAllBytes(currentFile.toPath()), "UTF-8");
        assertTrue("当前文件应包含写入内容", contentBefore.contains(message));

        // 执行滚动
        channel.rollFile();

        // 滚动后原文件被重命名，检查滚动文件内容
        File[] rolledFiles = testDir.toFile().listFiles(
                (dir, name) -> name.startsWith("test.") && name.endsWith(".log") && !name.equals(TEST_FILE_NAME));
        assertNotNull("应有滚动文件", rolledFiles);
        assertEquals("应有一个滚动文件", 1, rolledFiles.length);

        String rolledContent = new String(Files.readAllBytes(rolledFiles[0].toPath()), "UTF-8");
        assertEquals("滚动文件应包含原内容", message, rolledContent);

        // 新文件应为空（或仅包含滚动后写入的内容）
        String newContent = new String(Files.readAllBytes(currentFile.toPath()), "UTF-8");
        assertEquals("滚动后新文件应为空", 0, newContent.length());
    }

    /**
     * 测试 close 后 writer 已关闭。
     */
    @Test
    public void testClose_WriterClosed() throws IOException {
        assertFalse("关闭前 writer 不应为 null", channel.isWriterClosed());

        channel.close();

        assertTrue("关闭后 writer 应为 null", channel.isWriterClosed());
    }

    /**
     * 测试 IoMetrics 统计计数正确递增。
     */
    @Test
    public void testIoMetrics_CountersIncrement() throws IOException {
        // 重置 IoMetrics
        IoMetrics.reset();

        assertEquals("初始 WRITE_CALLS 应为 0", 0L, IoMetrics.getWriteCalls());
        assertEquals("初始 FLUSH_CALLS 应为 0", 0L, IoMetrics.getFlushCalls());
        assertEquals("初始 BYTES_WRITTEN 应为 0", 0L, IoMetrics.getBytesWritten());
        assertEquals("初始 FILE_SWITCHES 应为 0", 0L, IoMetrics.getFileSwitches());

        // 写入并 flush 两次
        String msg1 = "Hello";
        channel.append(msg1);
        channel.flush(100, 10000, 100000, 4096);

        assertEquals("第一次 flush 后 WRITE_CALLS 应为 1", 1L, IoMetrics.getWriteCalls());
        assertEquals("第一次 flush 后 FLUSH_CALLS 应为 1", 1L, IoMetrics.getFlushCalls());
        long bytes1 = msg1.getBytes("UTF-8").length;
        assertEquals("第一次 flush 后 BYTES_WRITTEN 应等于 Hello 的字节数",
                bytes1, IoMetrics.getBytesWritten());

        String msg2 = "World";
        channel.append(msg2);
        channel.flush(100, 10000, 100000, 4096);

        assertEquals("第二次 flush 后 WRITE_CALLS 应为 2", 2L, IoMetrics.getWriteCalls());
        assertEquals("第二次 flush 后 FLUSH_CALLS 应为 2", 2L, IoMetrics.getFlushCalls());
        long bytes2 = msg2.getBytes("UTF-8").length;
        assertEquals("第二次 flush 后 BYTES_WRITTEN 应累加",
                bytes1 + bytes2, IoMetrics.getBytesWritten());

        // 执行 rolling（不记录 FILE_SWITCHES，因为 recordFileSwitch 语义是 Worker 切换 pathKey）
        channel.rollFile();
        assertEquals("rolling 不应增加 FILE_SWITCHES", 0L, IoMetrics.getFileSwitches());
    }

    /**
     * 创建全新的 FileChannel，重置 IoMetrics。
     */
    private FileChannel createFreshChannel() throws IOException {
        PathKey pathKey = new PathKey(TEST_DIR, TEST_FILE_NAME);
        return new FileChannel(pathKey, testDir, TEST_FILE_NAME, "UTF-8", 10 * 1024 * 1024);
    }
}