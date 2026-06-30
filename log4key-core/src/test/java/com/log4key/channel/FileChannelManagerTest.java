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
 * FileChannelManager 单元测试
 */
public class FileChannelManagerTest {

    private static final String TEST_DIR = "target/test-channel-manager";
    private static final String TEST_FILE_NAME = "test.log";
    private Path testDir;
    private FileChannelManager manager;

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

        // 创建 manager：maxOpenChannels=3, idleTimeout=100ms, batchSize=4096, flushInterval=1000, highWaterMark=32768
        manager = new FileChannelManager(3, 100, 4096, 1000, 32768, 10 * 1024 * 1024, "UTF-8", 4096);
    }

    @After
    public void tearDown() {
        if (manager != null) {
            manager.closeAll();
        }
        try {
            if (Files.exists(testDir)) {
                Files.walk(testDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 测试 getOrCreate 基本功能：首次创建和缓存命中。
     */
    @Test
    public void testGetOrCreate_Basic() throws IOException {
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");

        // 首次创建
        FileChannel ch1 = manager.getOrCreate(pk1);
        assertNotNull("首次 getOrCreate 应返回非 null Channel", ch1);
        assertEquals("首次创建后 size 应为 1", 1, manager.size());

        // 缓存命中：应返回相同实例
        FileChannel ch2 = manager.getOrCreate(pk1);
        assertSame("缓存命中应返回相同实例", ch1, ch2);
        assertEquals("缓存命中后 size 仍为 1", 1, manager.size());

        // 验证 IoMetrics.recordFileTouched() 被调用
        assertEquals("创建新 Channel 时 FILE_TOUCHED 应为 1", 1L, IoMetrics.getFileTouched());
    }

    /**
     * 测试多个不同 PathKey 创建不同的 Channel。
     */
    @Test
    public void testGetOrCreate_MultiplePathKeys() throws IOException {
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");
        PathKey pk2 = new PathKey(TEST_DIR, "test2.log");
        PathKey pk3 = new PathKey(TEST_DIR, "test3.log");

        FileChannel ch1 = manager.getOrCreate(pk1);
        FileChannel ch2 = manager.getOrCreate(pk2);
        FileChannel ch3 = manager.getOrCreate(pk3);

        assertNotSame("不同 PathKey 应返回不同 Channel", ch1, ch2);
        assertNotSame("不同 PathKey 应返回不同 Channel", ch2, ch3);
        assertEquals("创建 3 个 Channel 后 size 应为 3", 3, manager.size());
        assertEquals("创建 3 个 Channel 后 FILE_TOUCHED 应为 3", 3L, IoMetrics.getFileTouched());
    }

    /**
     * 测试 LRU 淘汰：创建超过 maxOpenChannels 的 Channel 时触发淘汰。
     */
    @Test
    public void testLRUEviction() throws IOException {
        // maxOpenChannels=3，创建 3 个 Channel
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");
        PathKey pk2 = new PathKey(TEST_DIR, "test2.log");
        PathKey pk3 = new PathKey(TEST_DIR, "test3.log");

        FileChannel ch1 = manager.getOrCreate(pk1);
        FileChannel ch2 = manager.getOrCreate(pk2);
        FileChannel ch3 = manager.getOrCreate(pk3);
        assertEquals("创建 3 个 Channel 后 size 应为 3", 3, manager.size());

        // 先访问 pk1，使其成为最近访问的（LRU 顺序：pk2 → pk3 → pk1）
        manager.getOrCreate(pk1);

        // 创建第 4 个 Channel，应触发 LRU 淘汰（淘汰 pk2，最久未访问）
        PathKey pk4 = new PathKey(TEST_DIR, "test4.log");
        FileChannel ch4 = manager.getOrCreate(pk4);
        assertNotNull("第 4 个 Channel 应创建成功", ch4);
        assertEquals("淘汰后 size 应为 3", 3, manager.size());

        // pk2 应被淘汰，pk1、pk3、pk4 应存在
        FileChannel ch1Again = manager.getOrCreate(pk1);
        assertSame("pk1 应仍存在", ch1, ch1Again);

        // pk2 被淘汰后重新创建，应返回新实例
        IoMetrics.reset();
        FileChannel ch2Again = manager.getOrCreate(pk2);
        assertNotSame("pk2 被淘汰后重新创建应为新实例", ch2, ch2Again);
        assertEquals("淘汰后重新创建 FILE_TOUCHED 应为 1", 1L, IoMetrics.getFileTouched());
    }

    /**
     * 测试 LRU 访问顺序更新：getOrCreate 命中缓存时更新 LRU 顺序。
     */
    @Test
    public void testLRU_AccessOrderUpdate() throws IOException {
        // maxOpenChannels=3
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");
        PathKey pk2 = new PathKey(TEST_DIR, "test2.log");
        PathKey pk3 = new PathKey(TEST_DIR, "test3.log");

        manager.getOrCreate(pk1); // LRU: pk1
        manager.getOrCreate(pk2); // LRU: pk1, pk2
        manager.getOrCreate(pk3); // LRU: pk1, pk2, pk3

        // 多次访问 pk1，使其始终在最近访问位置
        manager.getOrCreate(pk1); // LRU: pk2, pk3, pk1
        manager.getOrCreate(pk1); // LRU: pk2, pk3, pk1

        // 创建第 4 个，应淘汰 pk2（最久未访问）
        PathKey pk4 = new PathKey(TEST_DIR, "test4.log");
        manager.getOrCreate(pk4);

        // pk2 应被淘汰，pk1、pk3 应存在
        FileChannel ch1 = manager.getOrCreate(pk1);
        assertNotNull("pk1 应仍存在", ch1);

        FileChannel ch3 = manager.getOrCreate(pk3);
        assertNotNull("pk3 应仍存在", ch3);
    }

    /**
     * 测试 idleScan：空闲超时的 Channel 被释放。
     */
    @Test
    public void testIdleScan_ReleasesIdleChannels() throws IOException, InterruptedException {
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");
        PathKey pk2 = new PathKey(TEST_DIR, "test2.log");

        manager.getOrCreate(pk1);
        manager.getOrCreate(pk2);
        assertEquals("创建后 size 应为 2", 2, manager.size());

        // 等待超过 idleTimeoutMs（100ms）
        Thread.sleep(150);

        int removed = manager.idleScan();
        assertEquals("应释放 2 个空闲 Channel", 2, removed);
        assertEquals("释放后 size 应为 0", 0, manager.size());
    }

    /**
     * 测试 idleScan：最近访问的 Channel 不被释放。
     */
    @Test
    public void testIdleScan_KeepsRecentlyAccessed() throws IOException, InterruptedException {
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");
        PathKey pk2 = new PathKey(TEST_DIR, "test2.log");

        manager.getOrCreate(pk1);
        manager.getOrCreate(pk2);
        assertEquals("创建后 size 应为 2", 2, manager.size());

        // 等待 50ms 后访问 pk1（idleTimeout=100ms，pk1 不应超时）
        Thread.sleep(50);
        manager.getOrCreate(pk1);

        // 再等待 60ms，此时 pk1 最近访问过（距上次访问 60ms），pk2 空闲 110ms
        Thread.sleep(60);

        int removed = manager.idleScan();
        assertEquals("应只释放 1 个空闲 Channel（pk2）", 1, removed);
        assertEquals("释放后 size 应为 1", 1, manager.size());

        // pk1 应仍存在
        FileChannel ch1 = manager.getOrCreate(pk1);
        assertNotNull("pk1 应仍存在", ch1);
    }

    /**
     * 测试 closeAll：关闭所有 Channel 并清空映射表。
     */
    @Test
    public void testCloseAll() throws IOException {
        PathKey pk1 = new PathKey(TEST_DIR, "test1.log");
        PathKey pk2 = new PathKey(TEST_DIR, "test2.log");

        FileChannel ch1 = manager.getOrCreate(pk1);
        FileChannel ch2 = manager.getOrCreate(pk2);
        assertEquals("关闭前 size 应为 2", 2, manager.size());

        assertFalse("关闭前 ch1 writer 不应为 null", ch1.isWriterClosed());
        assertFalse("关闭前 ch2 writer 不应为 null", ch2.isWriterClosed());

        manager.closeAll();

        assertEquals("关闭后 size 应为 0", 0, manager.size());
        assertTrue("关闭后 ch1 writer 应为 null", ch1.isWriterClosed());
        assertTrue("关闭后 ch2 writer 应为 null", ch2.isWriterClosed());
    }

    /**
     * 测试 calculateGlobalLimit 静态方法。
     */
    @Test
    public void testCalculateGlobalLimit() {
        // 测试基本计算逻辑
        int limit = FileChannelManager.calculateGlobalLimit(64);
        // ulimit 默认 1024，globalMaxOpenChannels = 1024 * 0.2 = 204
        // min(204, 64) = 64
        assertTrue("globalLimit 应 > 0", limit > 0);
        assertTrue("globalLimit 应 <= maxFileWriters", limit <= 64);
    }

    /**
     * 测试 calculateGlobalLimit 至少返回 1。
     */
    @Test
    public void testCalculateGlobalLimit_MinimumOne() {
        // 即使 maxFileWriters=1，也应至少返回 1
        int limit = FileChannelManager.calculateGlobalLimit(1);
        assertTrue("globalLimit 至少为 1", limit >= 1);
    }

    /**
     * 测试 getMaxOpenChannels 返回构造时设置的值。
     */
    @Test
    public void testGetMaxOpenChannels() {
        assertEquals("maxOpenChannels 应为构造时设置的值", 3, manager.getMaxOpenChannels());
    }

    /**
     * 测试 getOrCreate 在 IoMetrics.refuse() 时不记录统计。
     */
    @Test
    public void testGetOrCreate_IoMetricsRefuse() throws IOException {
        IoMetrics.reset();
        IoMetrics.refuse();

        PathKey pk1 = new PathKey(TEST_DIR, "test_refuse.log");
        manager.getOrCreate(pk1);

        assertEquals("IoMetrics 拒绝采集时 FILE_TOUCHED 应为 0", 0L, IoMetrics.getFileTouched());
    }
}