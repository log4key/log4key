/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.worker;

import com.log4key.internal.InternalLogger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高性能固定容量环形数组 Mailbox，多生产者-单消费者模型。
 *
 * 使用 Slot + volatile ready 标志实现精确的发布控制，解决 tail 提前发布问题。
 * head 由单消费者独占更新（volatile），tail 由多生产者通过 CAS 竞争。
 * 存储类型为 Runnable，用于 Worker 线程模型中的任务投递。
 *
 * 并发安全保证：
 * - 生产者：slot.task = task（plain write）→ slot.ready = true（volatile write）
 * - 消费者：slot.ready == true（volatile read）→ slot.task（plain read，happens-before 保证可见）
 */
public class Mailbox {

    /** 内部日志记录器 */
    private static final InternalLogger logger = InternalLogger.getLogger(Mailbox.class);

    /** 默认容量 */
    private static final int DEFAULT_CAPACITY = 8192;

    /** 背压检测采样间隔：每 N 次 offer 检测一次 */
    private static final int BACKPRESSURE_SAMPLE_INTERVAL = 256;

    // ==================== head cursor padding（避免 false sharing） ====================
    /** 填充字段 p1-p7，确保 head 独占一个缓存行 */
    @SuppressWarnings("unused")
    private long p1, p2, p3, p4, p5, p6, p7;

    /**
     * 消费 cursor，仅由单消费者更新。
     * 改为 volatile long（非 AtomicLong）：写入仅由单消费者执行，生产者仅读取，volatile 保证可见性。
     */
    private volatile long head;

    /** 填充字段 p8-p14，确保 head 与 tail 不在同一缓存行 */
    @SuppressWarnings("unused")
    private long p8, p9, p10, p11, p12, p13, p14;

    // ==================== tail cursor padding（避免 false sharing） ====================
    /** 填充字段 p15-p21，确保 tail 独占一个缓存行 */
    @SuppressWarnings("unused")
    private long p15, p16, p17, p18, p19, p20, p21;

    /**
     * 生产 cursor，多生产者通过 CAS 竞争更新。
     * 保持 AtomicLong：多生产者需要 CAS 操作。
     */
    private final AtomicLong tail = new AtomicLong(0);

    /** 填充字段 p22-p28，确保 tail 不与后续字段共享缓存行 */
    @SuppressWarnings("unused")
    private long p22, p23, p24, p25, p26, p27, p28;

    // ==================== 核心数据区 ====================

    /** 环形数组，每个槽位包含 ready 标志和任务引用 */
    private final Slot[] buffer;

    /** 容量 */
    private final int capacity;

    /** 容量掩码，用于快速取模（capacity - 1） */
    private final int mask;

    /** 累计拒绝次数 */
    private final AtomicLong rejectedCount = new AtomicLong(0);

    /**
     * 80% 背压告警标记位。
     * 改为 AtomicBoolean：解决多生产者并发时的重复告警问题。
     */
    private final AtomicBoolean warned80Percent = new AtomicBoolean(false);

    /** offer 采样计数器，用于降低背压检测频率（volatile，非原子递增，允许少量偏差） */
    private volatile int offerSampleCounter;

    /**
     * 环形数组槽位。
     *
     * ready 为 volatile：生产者写入 task 后设置 ready=true，
     * 消费者读取 ready=true 后通过 happens-before 保证看到 task 内容。
     */
    static final class Slot {
        /** 发布标志，volatile 保证 happens-before */
        volatile boolean ready;

        /** 待执行的任务 */
        Runnable task;
    }

    /**
     * 默认构造方法，使用默认容量 8192。
     */
    public Mailbox() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * 带容量参数的构造方法。
     * 容量必须是 2 的幂，若不是则自动向上取整到最近的 2 的幂。
     *
     * @param requestedCapacity 请求的容量
     */
    public Mailbox(int requestedCapacity) {
        if (requestedCapacity < 1) {
            requestedCapacity = 1;
        }
        this.capacity = roundUpToPowerOfTwo(requestedCapacity);
        this.mask = this.capacity - 1;
        this.buffer = new Slot[this.capacity];
        for (int i = 0; i < this.capacity; i++) {
            this.buffer[i] = new Slot();
        }
    }

    /**
     * 多生产者投递任务。
     *
     * 发布时序（保证消费者不会读到未完成写入的数据）：
     * 1. CAS 竞争 tail，获取槽位索引
     * 2. 写入 slot.task = task（plain write）
     * 3. 设置 slot.ready = true（volatile write，发布数据）
     *
     * 消费者读取 slot.ready（volatile read）后，通过 JMM happens-before 保证
     * 能看到 slot.task 的写入。
     *
     * @param task 待执行的任务
     * @return true 表示投递成功，false 表示被拒绝（容量已满）
     */
    public boolean offer(Runnable task) {
        if (task == null) {
            return false;
        }

        while (true) {
            long currentTail = tail.get();
            long currentHead = head;

            // 检查容量是否已满
            if (currentTail - currentHead >= capacity) {
                rejectedCount.incrementAndGet();
                return false;
            }

            // CAS 推进 tail cursor
            if (tail.compareAndSet(currentTail, currentTail + 1)) {
                int index = (int) (currentTail & mask);
                Slot slot = buffer[index];

                // 先写入数据，再设置 ready 标志
                // 消费者通过 volatile read ready 保证看到 task 的写入
                slot.task = task;
                slot.ready = true;

                // 背压检查：采样降低频率，每 256 次 offer 检测一次
                //noinspection NonAtomicOperationOnVolatileField
                if (++offerSampleCounter % BACKPRESSURE_SAMPLE_INTERVAL == 0) {
                    checkBackpressure();
                }

                return true;
            }
            // CAS 失败，自旋重试
        }
    }

    /**
     * 背压检测：检查当前使用率是否达到告警阈值。
     *
     * 使用 AtomicBoolean.compareAndSet 防止多生产者重复告警。
     * 使用率回落到 80% 以下时重置告警标记。
     */
    private void checkBackpressure() {
        long size = tail.get() - head;
        float usageRate = (float) size / capacity;

        if (usageRate < 0.8f) {
            // 回落到 80% 以下，重置告警标记位
            warned80Percent.set(false);
        } else if (warned80Percent.compareAndSet(false, true)) {
            // 首次跨越 80% 阈值，触发告警
            logger.warn("Mailbox usage {} >= 80%", usageRate);
        }
    }

    /**
     * 单消费者取出任务。
     *
     * 通过 slot.ready 判断数据是否已发布，而非依赖 tail cursor。
     * slot.ready == true 时，通过 volatile happens-before 保证 slot.task 非空。
     *
     * @return 待执行的任务，若当前槽位未就绪则返回 null
     */
    public Runnable poll() {
        long currentHead = head;
        Slot slot = buffer[(int) (currentHead & mask)];

        // 检查槽位是否已发布
        if (!slot.ready) {
            return null;
        }

        // slot.ready == true → slot.task 通过 happens-before 保证已写入，必定非空
        Runnable task = slot.task;

        // 清空槽位，帮助 GC
        slot.task = null;
        slot.ready = false;

        // 推进 head cursor
        head = currentHead + 1;

        return task;
    }

    /**
     * 获取当前队列中的元素数量（近似值，仅用于监控，不得参与逻辑处理判断）。
     *
     * 基于 tail - head 计算，是乐观值：
     * 包含 CAS 已声明但尚未 ready 的槽位。
     * 与 poll() 以 ready 为准不一致——可能出现 size() > 0 但 poll() 返回 null 的情况。
     *
     * @return 当前元素数量（近似值）
     */
    public int size() {
        long t = tail.get();
        long h = head;
        long diff = t - h;
        return diff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) diff;
    }

    /**
     * 获取信箱容量。
     *
     * @return 容量
     */
    public int capacity() {
        return capacity;
    }

    /**
     * 判断队列是否为空（近似值，仅用于监控，不得参与逻辑处理判断）。
     *
     * 基于 tail == head 判断，是乐观值：
     * CAS 已声明但尚未 ready 的槽位也视为非空。
     * 与 poll() 以 ready 为准不一致——可能出现 isEmpty() 为 false 但 poll() 返回 null 的情况。
     *
     * @return true 表示队列为空（所有已声明槽位均已消费）
     */
    public boolean isEmpty() {
        return tail.get() == head;
    }

    /**
     * 判断队列是否已满（近似值，仅用于监控，不得参与逻辑处理判断）。
     *
     * 基于 tail - head >= capacity 判断，是乐观值：
     * CAS 已声明但尚未 ready 的槽位也计入已用量。
     * 内在与 offer() 容量守卫（tail - head >= capacity）一致，
     * 但用于外部调用时，读到的 head/tail 不保证原子一致性。
     *
     * @return true 表示队列已满（近似值）
     */
    public boolean isFull() {
        return tail.get() - head >= capacity;
    }

    /**
     * 获取当前队列使用率（近似值，仅用于监控，不得参与逻辑处理判断）。
     *
     * 基于 (tail - head) / capacity 计算，是乐观值：
     * CAS 已声明但尚未 ready 的槽位也计入使用量。
     *
     * @return 使用率，范围 [0.0, 1.0]（近似值）
     */
    public float usageRate() {
        long t = tail.get();
        long h = head;
        if (t == h) {
            return 0.0f;
        }
        return (float) (t - h) / capacity;
    }

    /**
     * 获取累计拒绝次数。
     *
     * @return 累计拒绝次数
     */
    public long getRejectedCount() {
        return rejectedCount.get();
    }

    /**
     * 将数值向上取整到最近的 2 的幂。
     *
     * @param n 输入值
     * @return 不小于 n 的最小 2 的幂
     */
    @SuppressWarnings("DuplicatedCode")
    static int roundUpToPowerOfTwo(int n) {
        if (n <= 1) {
            return 1;
        }
        // 防止溢出，上限为 2^30
        if (n > (1 << 30)) {
            return 1 << 30;
        }
        n--;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }
}