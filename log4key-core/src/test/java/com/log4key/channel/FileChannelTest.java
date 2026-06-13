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
import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * FileChannel 单元测试
 *
 * V2: 测试 write/flush 分离后的行为。
 */
public class FileChannelTest {

    private static final String TEST_DIR = "target/test-channel";
    private static final String TEST_FILE_NAME = "test.log";
    private static final int HWM = 4096;
    private Path testDir;
    private FileChannel channel;

    @Before
    public void setUp() throws IOException {
        IoMetrics.reset();

        testDir = Paths.get(TEST_DIR);
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
            }
        }
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * 测试 append 后 estimatedBytes 正确增长（message.length() * 2 近似统计）。
     */
    @Test
    public void testAppend_EstimatedBytesGrowth() {
        String msg = "Hello";
        assertEquals("初始 estimatedBytes 应为 0", 0L, channel.getEstimatedBytes());

        channel.append(msg);
        assertEquals("append 后 estimatedBytes 应为 message.length() * 2",
                10L, channel.getEstimatedBytes());

        channel.append("World");
        assertEquals("第二次 append 后 estimatedBytes 应累加",
                20L, channel.getEstimatedBytes());
    }

    /**
     * 测试 shouldWrite（batchSize 触发条件）。
     */
    @Test
    public void testShouldWrite_BatchSizeCondition() {
        channel.append("12345"); // 10 bytes
        assertTrue("estimatedBytes >= batchSize(10) 应触发 shouldWrite",
                channel.shouldWrite(10));
        assertFalse("estimatedBytes >= batchSize(20) 不应触发 shouldWrite",
                channel.shouldWrite(20));
    }

    /**
     * 测试 shouldFlush（flushInterval 和 highWaterMark 触发条件）。
     */
    @Test
    public void testShouldFlush_IntervalAndHighWaterMark() throws InterruptedException, IOException {
        // 条件1: (now - lastFlushTime) >= flushIntervalMs
        IoMetrics.reset();
        channel = createFreshChannel();
        Thread.sleep(2);
        assertTrue("时间间隔 >= flushIntervalMs(1) 应触发 flush",
                channel.shouldFlush(1, 100000));

        // 条件2: estimatedBytes >= highWaterMark
        IoMetrics.reset();
        channel = createFreshChannel();
        channel.append("12345"); // 10 bytes
        assertTrue("estimatedBytes >= highWaterMark(10) 应触发 flush",
                channel.shouldFlush(10000, 10));
    }

    /**
     * 测试 write + flush 后 buffer 清空且 estimatedBytes 归零。
     */
    @Test
    public void testWrite_ClearsBufferAndResetsEstimatedBytes() throws IOException {
        channel.append("Hello, World!");
        assertTrue("write 前 buffer 应有内容", channel.getBufferContent().length() > 0);
        assertTrue("write 前 estimatedBytes 应 > 0", channel.getEstimatedBytes() > 0);

        channel.write(100000, 4096);
        channel.flush();

        assertEquals("write 后 buffer 应清空", 0, channel.getBufferContent().length());
        assertEquals("write 后 estimatedBytes 应归零", 0L, channel.getEstimatedBytes());
    }

    /**
     * 测试 buffer 容量超过 highWaterMark 后新建 vs setLength(0) 复用。
     */
    @Test
    public void testWrite_BufferCapacityHighWaterMark() throws IOException {
        int initialCapacity = channel.getBufferCapacity();

        // 场景1: highWaterMark 大于当前 capacity，应复用（setLength(0)）
        channel.append("test");
        channel.write(initialCapacity + 100, 4096);
        channel.flush();
        assertEquals("highWaterMark > capacity 时应复用 buffer",
                initialCapacity, channel.getBufferCapacity());

        IoMetrics.reset();
        channel = createFreshChannel();

        // 场景2: highWaterMark 小于当前 capacity，应新建
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            largeData.append("x");
        }
        channel.append(largeData.toString());
        int grownCapacity = channel.getBufferCapacity();
        assertTrue("追加大量数据后 capacity 应增长", grownCapacity > 4096);

        channel.write(100, 4096);
        channel.flush();
        assertEquals("highWaterMark < 原 capacity 时应新建 buffer",
                4096, channel.getBufferCapacity());
    }

    /**
     * 测试 rolling 文件命名格式: 原名.yyyyMMdd.HHmmss.扩展名。
     */
    @Test
    public void testRolling_FileNameFormat() throws IOException {
        channel.append("content");
        channel.write(100000, 4096);
        channel.flush();

        File currentFile = channel.getCurrentFile();
        assertTrue("当前文件应存在", currentFile.exists());

        channel.rollFile();

        File[] rolledFiles = testDir.toFile().listFiles(
                (dir, name) -> name.startsWith("test.") && name.endsWith(".log") && !name.equals(TEST_FILE_NAME));
        assertNotNull("应有滚动文件", rolledFiles);
        assertTrue("至少有一个滚动文件", rolledFiles.length > 0);

        String rolledName = rolledFiles[0].getName();
        assertTrue("滚动文件名应以 'test.' 开头", rolledName.startsWith("test."));
        assertTrue("滚动文件名应以 '.log' 结尾", rolledName.endsWith(".log"));

        String middle = rolledName.substring("test.".length(), rolledName.length() - ".log".length());
        assertEquals("时间戳应为 15 字符", 15, middle.length());
        assertTrue("时间戳应包含 '.'", middle.contains("."));

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
        String message = "Before roll content";
        channel.append(message);
        channel.write(100000, 4096);
        channel.flush();

        File currentFile = channel.getCurrentFile();
        String contentBefore = new String(Files.readAllBytes(currentFile.toPath()), "UTF-8");
        assertTrue("当前文件应包含写入内容", contentBefore.contains(message));

        channel.rollFile();

        File[] rolledFiles = testDir.toFile().listFiles(
                (dir, name) -> name.startsWith("test.") && name.endsWith(".log") && !name.equals(TEST_FILE_NAME));
        assertNotNull("应有滚动文件", rolledFiles);
        assertEquals("应有一个滚动文件", 1, rolledFiles.length);

        String rolledContent = new String(Files.readAllBytes(rolledFiles[0].toPath()), "UTF-8");
        assertEquals("滚动文件应包含原内容", message, rolledContent);

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
     * 测试 IoMetrics 统计计数：write() 记录 WRITE_CALLS，flush() 记录 FLUSH_CALLS。
     */
    @Test
    public void testIoMetrics_CountersIncrement() throws IOException {
        IoMetrics.reset();

        assertEquals("初始 WRITE_CALLS 应为 0", 0L, IoMetrics.getWriteCalls());
        assertEquals("初始 FLUSH_CALLS 应为 0", 0L, IoMetrics.getFlushCalls());
        assertEquals("初始 BYTES_WRITTEN 应为 0", 0L, IoMetrics.getBytesWritten());

        // 第一轮: append → write → flush
        String msg1 = "Hello";
        channel.append(msg1);
        channel.write(100000, 4096);
        channel.flush();

        assertEquals("第一次 write 后 WRITE_CALLS 应为 1", 1L, IoMetrics.getWriteCalls());
        assertEquals("第一次 flush 后 FLUSH_CALLS 应为 1", 1L, IoMetrics.getFlushCalls());
        long bytes1 = msg1.getBytes("UTF-8").length;
        assertEquals("第一次 write 后 BYTES_WRITTEN 应等于 Hello 的字节数",
                bytes1, IoMetrics.getBytesWritten());

        // 第二轮: append → write → flush
        String msg2 = "World";
        channel.append(msg2);
        channel.write(100000, 4096);
        channel.flush();

        assertEquals("第二次 write 后 WRITE_CALLS 应为 2", 2L, IoMetrics.getWriteCalls());
        assertEquals("第二次 flush 后 FLUSH_CALLS 应为 2", 2L, IoMetrics.getFlushCalls());
        long bytes2 = msg2.getBytes("UTF-8").length;
        assertEquals("第二次 write 后 BYTES_WRITTEN 应累加",
                bytes1 + bytes2, IoMetrics.getBytesWritten());
    }

    /**
     * 测试 shouldWrite 与 shouldFlush 独立触发：先 append → write → flush 完整流程。
     */
    @Test
    public void testWriteFlushSeparation_CompleteFlow() throws IOException {
        channel.append("1234");  // 8 bytes, batchSize=10 不触发
        assertFalse("8 bytes < batchSize(10)，shouldWrite 应为 false", channel.shouldWrite(10));

        channel.append("5678");  // +8 = 16 bytes, batchSize=10 触发
        assertTrue("16 bytes >= batchSize(10)，shouldWrite 应为 true", channel.shouldWrite(10));

        channel.write(100000, 4096);
        assertEquals("write 后 buffer 应清空", 0, channel.getBufferContent().length());
        assertEquals("write 后 estimatedBytes 应归零", 0L, channel.getEstimatedBytes());

        // flushInterval=0 触发 shouldFlush
        assertTrue("flushInterval=0 应触发 shouldFlush", channel.shouldFlush(0, 100000));
        channel.flush();

        // 验证文件内容完整
        File currentFile = channel.getCurrentFile();
        String content = new String(Files.readAllBytes(currentFile.toPath()), "UTF-8");
        assertEquals("文件内容应包含两次 append 的全部数据", "12345678", content);
    }

    private FileChannel createFreshChannel() throws IOException {
        PathKey pathKey = new PathKey(TEST_DIR, TEST_FILE_NAME);
        return new FileChannel(pathKey, testDir, TEST_FILE_NAME, "UTF-8", 10 * 1024 * 1024);
    }
}
