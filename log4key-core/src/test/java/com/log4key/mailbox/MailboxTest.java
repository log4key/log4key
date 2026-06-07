/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.mailbox;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Mailbox 类的单元测试。
 */
public class MailboxTest {

    private Mailbox mailbox;

    @Before
    public void setUp() {
        mailbox = new Mailbox();
    }

    // ==================== 测试用例1：基本投递和消费 ====================

    /**
     * 测试基本投递和消费：offer 一个 task，poll 出来验证正确。
     */
    @Test
    public void testOfferAndPoll() {
        final String[] result = new String[1];
        Runnable task = () -> result[0] = "test-message";

        // 投递
        boolean offered = mailbox.offer(task);
        assertTrue("offer 应该成功", offered);

        // 消费并执行
        Runnable polled = mailbox.poll();
        assertNotNull("poll 不应返回 null", polled);
        polled.run();
        assertEquals("消费的任务应正确执行", "test-message", result[0]);
    }

    // ==================== 测试用例2：多生产者并发测试 ====================

    /**
     * 测试多生产者并发投递：10 线程并发 offer，单线程 poll，验证所有 task 都能消费到。
     */
    @Test
    public void testMultiProducerConcurrent() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 1000;
        int totalEvents = threadCount * eventsPerThread;
        Mailbox concurrentMailbox = new Mailbox(16384);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger offeredCount = new AtomicInteger(0);

        // 多生产者并发投递
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < eventsPerThread; i++) {
                        final String msg = "Thread-" + threadId + "-Event-" + i;
                        Runnable task = () -> {}; // 空任务，仅验证投递
                        if (concurrentMailbox.offer(task)) {
                            offeredCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // 验证投递数量
        assertEquals("所有事件都应投递成功", totalEvents, offeredCount.get());

        // 单消费者 poll 所有事件
        int consumedCount = 0;
        Runnable task;
        while ((task = concurrentMailbox.poll()) != null) {
            consumedCount++;
        }

        assertEquals("消费数量应等于投递数量", totalEvents, consumedCount);
        assertTrue("队列应为空", concurrentMailbox.isEmpty());
    }

    // ==================== 测试用例3：容量满时 offer 返回 false ====================

    /**
     * 测试容量满时 offer 返回 false。
     */
    @Test
    public void testOfferReturnsFalseWhenFull() {
        int capacity = 8;
        Mailbox smallMailbox = new Mailbox(capacity);

        // 填满容量
        for (int i = 0; i < capacity; i++) {
            assertTrue("前 " + capacity + " 次投递应成功", smallMailbox.offer(createTestTask("Event-" + i)));
        }

        assertTrue("队列应已满", smallMailbox.isFull());

        // 第 capacity+1 次投递应失败
        boolean offered = smallMailbox.offer(createTestTask("overflow-event"));
        assertFalse("容量满时 offer 应返回 false", offered);
        assertEquals("拒绝次数应为 1", 1, smallMailbox.getRejectedCount());
    }

    // ==================== 测试用例4：背压告警阈值 80% ====================

    /**
     * 测试背压告警阈值 80%：当 usageRate 达到 80% 时触发告警，但不拒绝投递。
     */
    @Test
    public void testBackpressureWarningAt80Percent() {
        int capacity = 8;
        Mailbox smallMailbox = new Mailbox(capacity);

        // 投递 6 个事件（75% 使用率），不触发告警
        for (int i = 0; i < 6; i++) {
            assertTrue("使用率 75% 时投递应成功", smallMailbox.offer(createTestTask("Event-" + i)));
        }
        assertEquals("拒绝次数应为 0", 0, smallMailbox.getRejectedCount());

        // 第 7 个事件使使用率达到 87.5%（>= 80%），应仍成功但触发告警
        assertTrue("使用率 >=80% 时投递仍应成功", smallMailbox.offer(createTestTask("Event-6")));
        assertEquals("拒绝次数仍应为 0", 0, smallMailbox.getRejectedCount());

        // 第 8 个事件使使用率达到 100%，应仍成功（填满缓冲区）
        assertTrue("使用率 100% 时投递仍应成功", smallMailbox.offer(createTestTask("Event-7")));
        assertEquals("拒绝次数仍应为 0", 0, smallMailbox.getRejectedCount());

        // 第 9 个事件，缓冲区已满，应拒绝
        assertFalse("缓冲区满后投递应失败", smallMailbox.offer(createTestTask("Event-8")));
        assertEquals("拒绝次数应为 1", 1, smallMailbox.getRejectedCount());
    }

    // ==================== 测试用例5：rejectedCount 累计正确 ====================

    /**
     * 测试 rejectedCount 累计正确：多次拒绝后计数应准确。
     */
    @Test
    public void testRejectedCountAccumulation() {
        int capacity = 4;
        Mailbox smallMailbox = new Mailbox(capacity);

        // 填满
        for (int i = 0; i < capacity; i++) {
            smallMailbox.offer(createTestTask("Event-" + i));
        }

        // 连续拒绝 5 次
        for (int i = 0; i < 5; i++) {
            smallMailbox.offer(createTestTask("overflow-" + i));
        }

        assertEquals("拒绝次数应累计为 5", 5, smallMailbox.getRejectedCount());

        // 消费一个事件后，应能继续投递
        smallMailbox.poll();
        assertTrue("消费后应能投递", smallMailbox.offer(createTestTask("new-event")));
        assertEquals("拒绝次数仍为 5", 5, smallMailbox.getRejectedCount());
    }

    // ==================== 测试用例6：空队列 poll 返回 null ====================

    /**
     * 测试空队列 poll 返回 null。
     */
    @Test
    public void testPollReturnsNullWhenEmpty() {
        assertNull("空队列 poll 应返回 null", mailbox.poll());
        assertTrue("空队列 isEmpty 应为 true", mailbox.isEmpty());
    }

    // ==================== 测试用例7：容量非2的幂自动向上取整 ====================

    /**
     * 测试容量非 2 的幂自动向上取整：传入 1000 应 -> 1024。
     */
    @Test
    public void testCapacityRoundUpToPowerOfTwo() {
        Mailbox m1 = new Mailbox(1000);
        assertEquals("1000 应向上取整为 1024", 1024, m1.capacity());

        Mailbox m2 = new Mailbox(500);
        assertEquals("500 应向上取整为 512", 512, m2.capacity());

        Mailbox m3 = new Mailbox(1);
        assertEquals("1 应保持 1", 1, m3.capacity());

        Mailbox m4 = new Mailbox(3);
        assertEquals("3 应向上取整为 4", 4, m4.capacity());

        Mailbox m5 = new Mailbox(8192);
        assertEquals("8192 本身是 2 的幂，应保持", 8192, m5.capacity());
    }

    // ==================== 测试用例8：usageRate 计算正确 ====================

    /**
     * 测试 usageRate 计算正确。
     */
    @Test
    public void testUsageRateCalculation() {
        int capacity = 8;
        Mailbox smallMailbox = new Mailbox(capacity);

        // 空队列 usageRate 应为 0
        assertEquals("空队列 usageRate 应为 0", 0.0f, smallMailbox.usageRate(), 0.001f);

        // 投递 4 个事件，usageRate 应为 0.5
        for (int i = 0; i < 4; i++) {
            smallMailbox.offer(createTestTask("Event-" + i));
        }
        assertEquals("4/8 usageRate 应为 0.5", 0.5f, smallMailbox.usageRate(), 0.001f);

        // 投递到满，usageRate 应为 1.0
        for (int i = 4; i < capacity; i++) {
            smallMailbox.offer(createTestTask("Event-" + i));
        }
        assertEquals("满队列 usageRate 应为 1.0", 1.0f, smallMailbox.usageRate(), 0.001f);

        // 消费一个，usageRate 应降低
        smallMailbox.poll();
        assertEquals("7/8 usageRate 应为 0.875", 0.875f, smallMailbox.usageRate(), 0.001f);
    }

    // ==================== 测试用例9：isEmpty/isFull 边界条件 ====================

    /**
     * 测试 isEmpty/isFull 边界条件。
     */
    @Test
    public void testIsEmptyAndIsFullBoundary() {
        int capacity = 4;
        Mailbox smallMailbox = new Mailbox(capacity);

        // 初始状态
        assertTrue("初始应为空", smallMailbox.isEmpty());
        assertFalse("初始不应满", smallMailbox.isFull());
        assertEquals("初始 size 应为 0", 0, smallMailbox.size());

        // 投递 1 个事件
        smallMailbox.offer(createTestTask("Event-0"));
        assertFalse("投递 1 个后不应为空", smallMailbox.isEmpty());
        assertFalse("投递 1 个后不应满", smallMailbox.isFull());
        assertEquals("size 应为 1", 1, smallMailbox.size());

        // 投递到满
        smallMailbox.offer(createTestTask("Event-1"));
        smallMailbox.offer(createTestTask("Event-2"));
        smallMailbox.offer(createTestTask("Event-3"));
        assertFalse("满时不应为空", smallMailbox.isEmpty());
        assertTrue("满时 isFull 应为 true", smallMailbox.isFull());
        assertEquals("size 应为容量", capacity, smallMailbox.size());

        // 消费 1 个
        smallMailbox.poll();
        assertFalse("消费 1 个后不应为空", smallMailbox.isEmpty());
        assertFalse("消费 1 个后不应满", smallMailbox.isFull());
        assertEquals("size 应为 3", 3, smallMailbox.size());

        // 消费全部
        for (int i = 0; i < 3; i++) {
            smallMailbox.poll();
        }
        assertTrue("消费全部后应为空", smallMailbox.isEmpty());
        assertFalse("消费全部后不应满", smallMailbox.isFull());
        assertEquals("消费全部后 size 应为 0", 0, smallMailbox.size());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用的空任务。
     */
    private Runnable createTestTask(final String name) {
        return () -> {};
    }
}