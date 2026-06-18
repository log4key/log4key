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
     * 每次 batch write 到 BufferedWriter 的次数（batchSize ≈4KB 触发 write()）
     */
    public static final AtomicLong WRITE_CALLS = new AtomicLong();
    
    /**
     * 每次 BufferedWriter.flush() 的次数。触发条件：flushInterval(1s) / highWaterMark(32KB) / idle / rolling / shutdown
     */
    public static final AtomicLong FLUSH_CALLS = new AtomicLong();
    
    /**
     * 写入字节数
     */
    public static final AtomicLong BYTES_WRITTEN = new AtomicLong();

    /**
     * File Touched，触达的文件数（同一文件只会计算一次）
     */
    public static final AtomicLong FILE_TOUCHED = new AtomicLong();
    
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
     * Records a file touch.
     *
     * 记录触达文件。
     */
    public static void recordFileTouched() {
        if (accepting) {
            FILE_TOUCHED.incrementAndGet();
        }
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
     * Gets the number of files touched.
     *
     * 获取触达文件数。
     *
     * @return the number of files touched / 触达文件数
     */
    public static long getFileTouched() { return FILE_TOUCHED.get(); }

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
        WRITE_CALLS.set(0);
        FLUSH_CALLS.set(0);
        BYTES_WRITTEN.set(0);
        FILE_TOUCHED.set(0);
    }

    /**
     * Returns a summary of all metrics.
     *
     * 显示所有计数指标。
     *
     * @return the metrics summary string / 所有计数指标字符串
     */
    public static String info() {
        return "WRITE_CALLS:" + WRITE_CALLS.get() + "\n" +
                "FLUSH_CALLS:" + FLUSH_CALLS.get() + "\n" +
                "BYTES_WRITTEN:" + BYTES_WRITTEN.get() + "\n" +
                "FILE_TOUCHED:" + FILE_TOUCHED.get();
    }
}