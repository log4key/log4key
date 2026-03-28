/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * I/O metrics collector.
 *
 * I/O 性能指标采集器。
 */
public class IoMetrics {

    /**
     * 是否接受数据
     */
    private static volatile boolean accepting = true;
    
    /**
     * 总日志事件数
     */
    public static final AtomicLong TOTAL_EVENTS = new AtomicLong();
    
    /**
     * 写操作次数（真实触发底层 syscall 的次数）
     */
    public static final AtomicLong WRITE_CALLS = new AtomicLong();
    
    /**
     * 刷新操作次数
     */
    public static final AtomicLong FLUSH_CALLS = new AtomicLong();
    
    /**
     * 写入字节数
     */
    public static final AtomicLong BYTES_WRITTEN = new AtomicLong();

    /**
     * 写入文件数（同一文件只会计算一次）
     */
    public static final AtomicLong FILE_WRITTEN = new AtomicLong();
    
    /**
     * 文件切换次数（当前线程操作不同路由文件变更时）
     */
    public static final AtomicLong FILE_SWITCHES = new AtomicLong();
    
    /**
     * Records a log event.
     *
     * 记录日志事件。
     */
    public static void recordEvent() {
        if (accepting) {
            TOTAL_EVENTS.incrementAndGet();
        }
    }
    
    /**
     * Records a write operation.
     *
     * 记录写操作。
     *
     * @param bytes the number of bytes written / 写入的字节数
     */
    public static void recordWrite(long bytes) {
        if (accepting) {
            WRITE_CALLS.incrementAndGet();
            BYTES_WRITTEN.addAndGet(bytes);
        }
    }
    
    /**
     * Records a flush operation.
     *
     * 记录刷新操作。
     */
    public static void recordFlush() {
        if (accepting) {
            FLUSH_CALLS.incrementAndGet();
        }
    }


    /**
     * Records a file write.
     *
     * 记录文件数。
     */
    public static void recordFileWrite() {
        if (accepting) {
            FILE_WRITTEN.incrementAndGet();
        }
    }

    /**
     * Records a file switch.
     *
     * 记录文件切换。
     */
    public static void recordFileSwitch() {
        if (accepting) {
            FILE_SWITCHES.incrementAndGet();
        }
    }
    
    /**
     * Gets the total number of events.
     *
     * 获取总日志事件数。
     *
     * @return the total number of events / 总日志事件数
     */
    public static long getTotalEvents() {
        return TOTAL_EVENTS.get();
    }
    
    /**
     * Gets the number of write calls.
     *
     * 获取写操作次数。
     *
     * @return the number of write calls / 写操作次数
     */
    public static long getWriteCalls() {
        return WRITE_CALLS.get();
    }
    
    /**
     * Gets the number of flush calls.
     *
     * 获取刷新操作次数。
     *
     * @return the number of flush calls / 刷新操作次数
     */
    public static long getFlushCalls() {
        return FLUSH_CALLS.get();
    }
    
    /**
     * Gets the number of bytes written.
     *
     * 获取写入字节数。
     *
     * @return the number of bytes written / 写入字节数
     */
    public static long getBytesWritten() {
        return BYTES_WRITTEN.get();
    }


    /**
     * Gets the number of files written.
     *
     * 获取文件写入次数。
     *
     * @return the number of files written / 文件写入次数
     */
    public static long getFileWritten() { return FILE_WRITTEN.get(); }

    /**
     * Gets the number of file switches.
     *
     * 获取文件切换次数。
     *
     * @return the number of file switches / 文件切换次数
     */
    public static long getFileSwitches() {
        return FILE_SWITCHES.get();
    }

    /**
     * Disables metrics collection.
     *
     * 拒绝数据采集，用于测试或暂停采集。
     */
    public static void refuse() {
        accepting = false;
    }

    /**
     * Resets all counters.
     *
     * 重置所有计数器，用于测试或重新开始采集。
     */
    public static void reset() {
        accepting = true;
        TOTAL_EVENTS.set(0);
        WRITE_CALLS.set(0);
        FLUSH_CALLS.set(0);
        BYTES_WRITTEN.set(0);
        FILE_WRITTEN.set(0);
        FILE_SWITCHES.set(0);
    }

    /**
     * Returns a summary of all metrics.
     *
     * 显示所有计数指标。
     *
     * @return the metrics summary string / 所有计数指标字符串
     */
    public static String info() {
        return "TOTAL_EVENTS:" + TOTAL_EVENTS.get() + "\n" +
                "WRITE_CALLS:" + WRITE_CALLS.get() + "\n" +
                "FLUSH_CALLS:" + FLUSH_CALLS.get() + "\n" +
                "BYTES_WRITTEN:" + BYTES_WRITTEN.get() + "\n" +
                "FILE_WRITTEN:" + FILE_WRITTEN.get() + "\n" +
                "FILE_SWITCHES:" + FILE_SWITCHES.get();
    }
}