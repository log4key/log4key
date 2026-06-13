/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.worker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * WorkerGroup 类的单元测试。
 *
 * 覆盖 WorkerGroup 的创建、启停、任务投递、背压、shutdown 状态和 LogExecutor 接口实现。
 */
public class WorkerGroupTest {

    /** 测试用临时目录，供 FileChannelManager 使用 */
    private static final String TEST_DIR = "target/test-worker-group";

    private Path testDir;
    private WorkerGroup workerGroup;

    /** 测试参数（按推荐值） */
    private static final int WORKER_COUNT = 2;
    private static final int QUEUE_CAPACITY = 8;
    private static final int MAX_FILE_WRITERS = 64;
    private static final long IDLE_TIMEOUT_MS = 100L;
    private static final long BATCH_SIZE = 4096L;
    private static final long FLUSH_INTERVAL_MS = 1000L;
    private static final long HIGH_WATER_MARK = 32768L;
    private static final int INITIAL_BUFFER_SIZE = 4096;
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String CHARSET = "UTF-8";

    @Before
    public void setUp() throws IOException {
        // 创建临时目录
        testDir = Paths.get(TEST_DIR);
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectories(testDir);

        // 创建 WorkerGroup 实例
        workerGroup = new WorkerGroup(
                WORKER_COUNT, QUEUE_CAPACITY, MAX_FILE_WRITERS,
                IDLE_TIMEOUT_MS, BATCH_SIZE, FLUSH_INTERVAL_MS,
                HIGH_WATER_MARK, INITIAL_BUFFER_SIZE, MAX_FILE_SIZE, CHARSET);
    }

    @After
    public void tearDown() {
        // 确保 WorkerGroup 被关闭
        if (workerGroup != null) {
            workerGroup.shutdownNow();
        }
        // 清理临时目录
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

    // ==================== 测试用例1：WorkerGroup 创建 ====================

    /**
     * 创建 WorkerGroup，验证 workerCount 和 Mailbox 数量。
     */
    @Test
    public void testWorkerGroupCreation() {
        assertEquals("workerCount 应为配置值", WORKER_COUNT, workerGroup.getWorkerCount());

        // 验证每个 Worker 的 Mailbox 存在
        Mailbox mailbox0 = workerGroup.getMailbox(0);
        Mailbox mailbox1 = workerGroup.getMailbox(1);
        assertNotNull("Worker 0 的 Mailbox 不应为 null", mailbox0);
        assertNotNull("Worker 1 的 Mailbox 不应为 null", mailbox1);
        assertNotSame("不同 Worker 的 Mailbox 应不同", mailbox0, mailbox1);

        // 验证 Mailbox 容量
        assertEquals("Mailbox 容量应为配置值", QUEUE_CAPACITY, mailbox0.capacity());
    }

    // ==================== 测试用例2：WorkerGroup 启停 ====================

    /**
     * 启动 WorkerGroup，验证 Worker 线程创建，然后停止。
     */
    @Test
    public void testWorkerGroupStartStop() throws InterruptedException {
        Worker[] workers = getWorkers();

        // 启动前：Worker 线程不应存在
        assertNull("启动前 Worker 0 线程应为 null", workers[0].getWorkerThread());
        assertNull("启动前 Worker 1 线程应为 null", workers[1].getWorkerThread());

        // 启动
        workerGroup.start();

        // 启动后：Worker 线程应已创建且存活
        Thread t0 = workers[0].getWorkerThread();
        Thread t1 = workers[1].getWorkerThread();
        assertNotNull("启动后 Worker 0 线程不应为 null", t0);
        assertNotNull("启动后 Worker 1 线程不应为 null", t1);
        assertTrue("Worker 0 线程应存活", t0.isAlive());
        assertTrue("Worker 1 线程应存活", t1.isAlive());

        // 线程名称应符合命名规范
        assertTrue("线程名称应包含 log4key-worker-0", t0.getName().contains("log4key-worker-0"));
        assertTrue("线程名称应包含 log4key-worker-1", t1.getName().contains("log4key-worker-1"));

        // 停止
        workerGroup.stop();

        // 停止后：Worker 应标记为 stopped
        assertTrue("Worker 0 应已停止", workers[0].isStopped());
        assertTrue("Worker 1 应已停止", workers[1].isStopped());
    }

    // ==================== 测试用例3：workerCount 非 2 的幂自动修正 ====================

    /**
     * 传入非 2 的幂的 workerCount（如 3），验证自动修正为 4。
     */
    @Test
    public void testWorkerCountPowerOfTwoCorrection() {
        WorkerGroup wg = new WorkerGroup(
                3, QUEUE_CAPACITY, MAX_FILE_WRITERS,
                IDLE_TIMEOUT_MS, BATCH_SIZE, FLUSH_INTERVAL_MS,
                HIGH_WATER_MARK, INITIAL_BUFFER_SIZE, MAX_FILE_SIZE, CHARSET);

        assertEquals("workerCount=3 应修正为 4", 4, wg.getWorkerCount());

        // 验证 Mailbox 数量也为 4
        assertNotNull("Worker 0 的 Mailbox 应存在", wg.getMailbox(0));
        assertNotNull("Worker 3 的 Mailbox 应存在", wg.getMailbox(3));

        wg.shutdownNow();
    }

    /**
     * 测试 roundUpToPowerOfTwo 静态方法的各种边界情况。
     */
    @Test
    public void testRoundUpToPowerOfTwo() {
        assertEquals("0 → 1", 1, WorkerGroup.roundUpToPowerOfTwo(0));
        assertEquals("1 → 1", 1, WorkerGroup.roundUpToPowerOfTwo(1));
        assertEquals("2 → 2", 2, WorkerGroup.roundUpToPowerOfTwo(2));
        assertEquals("3 → 4", 4, WorkerGroup.roundUpToPowerOfTwo(3));
        assertEquals("5 → 8", 8, WorkerGroup.roundUpToPowerOfTwo(5));
        assertEquals("7 → 8", 8, WorkerGroup.roundUpToPowerOfTwo(7));
        assertEquals("8 → 8", 8, WorkerGroup.roundUpToPowerOfTwo(8));
        assertEquals("9 → 16", 16, WorkerGroup.roundUpToPowerOfTwo(9));
        assertEquals("16 → 16", 16, WorkerGroup.roundUpToPowerOfTwo(16));
        assertEquals("1024 → 1024", 1024, WorkerGroup.roundUpToPowerOfTwo(1024));
    }

    // ==================== 测试用例4：execute 和 submit 投递任务 ====================

    /**
     * 通过 execute() 和 submit() 投递任务到指定 Worker，验证任务被执行。
     */
    @Test
    public void testExecuteAndSubmit() throws Exception {
        workerGroup.start();

        // ---- execute() 测试 ----
        final AtomicBoolean executed = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        Runnable executeTask = () -> {
            executed.set(true);
            latch.countDown();
        };
        workerGroup.execute("0", executeTask);

        // 等待任务执行
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        assertTrue("execute() 投递的任务应在超时前执行完毕", completed);
        assertTrue("execute() 投递的任务应被执行", executed.get());

        // ---- submit() 测试 ----
        final AtomicBoolean submitted = new AtomicBoolean(false);
        Runnable submitTask = () -> submitted.set(true);

        Future<?> future = workerGroup.submit("1", submitTask);
        assertNotNull("submit() 应返回非 null Future", future);

        // 等待 Future 完成
        future.get(3, TimeUnit.SECONDS);
        assertTrue("submit() 投递的任务应被执行", submitted.get());
    }

    /**
     * 测试 submit() 返回的 Future 在任务异常时携带异常信息。
     */
    @Test
    public void testSubmitTaskException() throws Exception {
        workerGroup.start();

        Runnable failingTask = () -> {
            throw new RuntimeException("test exception");
        };

        Future<?> future = workerGroup.submit("0", failingTask);

        try {
            future.get(3, TimeUnit.SECONDS);
            fail("应抛出异常");
        } catch (Exception e) {
            // 预期行为：Future 携带任务异常
            assertTrue("异常信息应包含 test exception",
                    e.getMessage() != null && e.getMessage().contains("test exception"));
        }
    }

    /**
     * 测试 submit() 在 Mailbox 满时返回异常 Future。
     */
    @Test
    public void testSubmitMailboxFull() {
        // 不启动 Worker，这样 Mailbox 不会被消费
        // 填满 Worker 0 的 Mailbox（容量 8）
        for (int i = 0; i < QUEUE_CAPACITY; i++) {
            workerGroup.execute("0", () -> {});
        }

        // 第 9 次 submit 应返回异常 Future
        Future<?> future = workerGroup.submit("0", () -> {});

        try {
            future.get(3, TimeUnit.SECONDS);
            fail("Mailbox 满时 submit 应返回异常 Future");
        } catch (Exception e) {
            assertTrue("异常信息应包含 Mailbox full",
                    e.getMessage() != null && e.getMessage().contains("Mailbox full"));
        }
    }

    // ==================== 测试用例5：shutdown 后 execute 拒绝任务 ====================

    /**
     * shutdown 后 execute() 应拒绝任务（不抛异常，仅忽略）。
     */
    @Test
    public void testExecuteWithShutdown() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        Runnable task = () -> executed.set(true);

        // 先 shutdown
        workerGroup.shutdown();

        // execute 不应抛异常，但任务不应被执行
        workerGroup.execute("0", task);
        assertFalse("shutdown 后 execute() 投递的任务不应被执行", executed.get());
    }

    /**
     * shutdown 后 submit() 应抛出 IllegalStateException。
     */
    @Test(expected = IllegalStateException.class)
    public void testSubmitWithShutdown() {
        workerGroup.shutdown();
        workerGroup.submit("0", () -> {});
    }

    // ==================== 测试用例6：Mailbox 满时拒绝任务 ====================

    /**
     * 投递超过 Mailbox 容量的任务，验证 offer 返回 false 时行为。
     * execute() 不抛异常，仅记录告警。
     */
    @Test
    public void testMailboxFullRejection() {
        // 不启动 Worker，这样 Mailbox 不会被消费

        // 填满 Worker 0 的 Mailbox（容量 8）
        for (int i = 0; i < QUEUE_CAPACITY; i++) {
            workerGroup.execute("0", () -> {});
        }

        // 验证 Mailbox 已满
        Mailbox mailbox0 = workerGroup.getMailbox(0);
        assertTrue("Worker 0 的 Mailbox 应已满", mailbox0.isFull());
        assertEquals("Mailbox size 应为容量", QUEUE_CAPACITY, mailbox0.size());

        // 第 9 次 execute 不应抛异常
        final AtomicBoolean executed = new AtomicBoolean(false);
        workerGroup.execute("0", () -> executed.set(true));

        // 任务不应被执行（Mailbox 满，offer 返回 false）
        assertFalse("Mailbox 满时投递的任务不应被执行", executed.get());
        assertEquals("拒绝次数应为 1", 1, mailbox0.getRejectedCount());
    }

    // ==================== 测试用例7：shutdownNow 收集剩余任务 ====================

    /**
     * shutdownNow() 应收集 Mailbox 中剩余未处理的任务。
     */
    @Test
    public void testShutdownNowCollectsRemainingTasks() throws InterruptedException {
        workerGroup.start();

        // 投递任务到两个 Worker 的 Mailbox（不等待执行）
        for (int i = 0; i < 3; i++) {
            workerGroup.execute("0", () -> {});
        }
        for (int i = 0; i < 2; i++) {
            workerGroup.execute("1", () -> {});
        }

        // 立即 shutdownNow，收集剩余任务
        List<Runnable> remaining = workerGroup.shutdownNow();

        // 由于 Worker 线程可能已消费部分任务，剩余任务数 <= 5
        assertNotNull("剩余任务列表不应为 null", remaining);
        assertTrue("剩余任务数应 <= 5（总投递 5 个）", remaining.size() <= 5);
    }

    /**
     * shutdownNow 在无剩余任务时返回空列表。
     */
    @Test
    public void testShutdownNowNoRemainingTasks() {
        // 不启动 Worker，不投递任务
        List<Runnable> remaining = workerGroup.shutdownNow();
        assertNotNull("剩余任务列表不应为 null", remaining);
        assertTrue("无任务时剩余列表应为空", remaining.isEmpty());
    }

    // ==================== 测试用例8：isShutdown 和 isTerminated 状态 ====================

    /**
     * 验证 shutdown 和 terminated 状态标记。
     */
    @Test
    public void testIsShutdownAndIsTerminated() {
        // 初始状态
        assertFalse("初始 isShutdown 应为 false", workerGroup.isShutdown());
        assertFalse("初始 isTerminated 应为 false", workerGroup.isTerminated());

        // shutdown 后
        workerGroup.shutdown();
        assertTrue("shutdown 后 isShutdown 应为 true", workerGroup.isShutdown());
        assertTrue("shutdown 后 isTerminated 应为 true", workerGroup.isTerminated());
    }

    /**
     * shutdownNow 后 isShutdown 和 isTerminated 也应为 true。
     */
    @Test
    public void testIsShutdownAndIsTerminatedAfterShutdownNow() {
        assertFalse("初始 isShutdown 应为 false", workerGroup.isShutdown());

        workerGroup.shutdownNow();
        assertTrue("shutdownNow 后 isShutdown 应为 true", workerGroup.isShutdown());
        assertTrue("shutdownNow 后 isTerminated 应为 true", workerGroup.isTerminated());
    }

    // ==================== 测试用例9：awaitTermination 超时行为 ====================

    /**
     * 验证 awaitTermination：Worker 运行中时超时返回 false。
     */
    @Test
    public void testAwaitTerminationTimeout() throws InterruptedException {
        // 启动 Worker，但不 shutdown
        workerGroup.start();

        // Worker 运行中，awaitTermination 应在超时后返回 false
        boolean result = workerGroup.awaitTermination(50, TimeUnit.MILLISECONDS);
        assertFalse("Worker 运行中 awaitTermination 应返回 false", result);

        // 清理
        workerGroup.shutdownNow();
    }

    /**
     * 验证 awaitTermination：shutdown 后返回 true。
     */
    @Test
    public void testAwaitTerminationAfterShutdown() throws InterruptedException {
        workerGroup.start();
        workerGroup.shutdown();

        boolean result = workerGroup.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue("shutdown 后 awaitTermination 应返回 true", result);
    }

    // ==================== 测试用例10：getMailbox 返回正确实例 ====================

    /**
     * 验证 getMailbox(workerId) 返回正确的 Mailbox 实例。
     */
    @Test
    public void testGetMailbox() {
        Mailbox m0 = workerGroup.getMailbox(0);
        Mailbox m1 = workerGroup.getMailbox(1);

        assertNotNull("Worker 0 的 Mailbox 不应为 null", m0);
        assertNotNull("Worker 1 的 Mailbox 不应为 null", m1);
        assertNotSame("不同 Worker 的 Mailbox 应不同", m0, m1);
        assertEquals("Mailbox 0 容量应为配置值", QUEUE_CAPACITY, m0.capacity());
        assertEquals("Mailbox 1 容量应为配置值", QUEUE_CAPACITY, m1.capacity());
    }

    /**
     * 测试 getMailbox 越界访问。
     */
    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetMailboxOutOfBounds() {
        // workerId=2 超出范围（workerCount=2）
        workerGroup.getMailbox(2);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取 WorkerGroup 内部的 Worker 数组（供测试使用）。
     * 通过反射或包级访问获取。
     *
     * @return Worker 数组
     */
    private Worker[] getWorkers() {
        // 通过 getMailbox 验证 Worker 存在，间接获取 Worker 引用
        // 实际测试中通过 getMailbox 和 getWorkerCount 验证
        try {
            java.lang.reflect.Field field = WorkerGroup.class.getDeclaredField("workers");
            field.setAccessible(true);
            return (Worker[]) field.get(workerGroup);
        } catch (Exception e) {
            throw new RuntimeException("无法获取 Worker 数组", e);
        }
    }
}