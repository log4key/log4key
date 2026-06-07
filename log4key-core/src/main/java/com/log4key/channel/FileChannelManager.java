/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.channel;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.log4key.internal.InternalLogger;
import com.log4key.metrics.IoMetrics;
import com.log4key.path.PathKey;

/**
 * FileChannel 生命周期管理器。
 *
 * 每个 Worker 持有一个实例，负责该 Worker 内所有 FileChannel 的创建、LRU 淘汰、Idle 扫描和关闭。
 * 使用 LinkedHashMap(accessOrder=true) 维护访问顺序，实现 LRU 淘汰策略。
 */
public class FileChannelManager {

    private static final InternalLogger logger = InternalLogger.getLogger(FileChannelManager.class);

    /** 系统 ulimit 的保留比例（0.2 = 20%） */
    private static final double ULIMIT_RESERVATION_RATIO = 0.2;

    /** LinkedHashMap 初始容量 */
    private static final int INITIAL_MAP_CAPACITY = 128;

    /** LinkedHashMap 负载因子 */
    private static final float LOAD_FACTOR = 0.75f;

    /** Channel 映射表，accessOrder=true 实现 LRU */
    private final LinkedHashMap<PathKey, FileChannel> channelMap;

    /** 单个 Worker 的 FD 上限 */
    private final int maxOpenChannels;

    /** 空闲超时（毫秒） */
    private final long idleTimeoutMs;

    /** Flush 字节阈值 */
    private final long batchSize;

    /** Flush 时间间隔（毫秒） */
    private final long flushIntervalMs;

    /** Buffer 扩容回收阈值 */
    private final long highWaterMark;

    /** 最大文件大小（字节），触发 rolling */
    private final long maxFileSize;

    /** 字符编码 */
    private final String charset;

    /** StringBuilder 初始容量 */
    private final int initialBufferSize;

    /**
     * 构造 FileChannelManager 实例。
     *
     * @param maxOpenChannels  单个 Worker 的 FD 上限
     * @param idleTimeoutMs    空闲超时（毫秒）
     * @param batchSize        Flush 字节阈值
     * @param flushIntervalMs  Flush 时间间隔（毫秒）
     * @param highWaterMark    Buffer 扩容回收阈值
     * @param maxFileSize      最大文件大小（字节）
     * @param charset          字符编码
     * @param initialBufferSize StringBuilder 初始容量
     */
    public FileChannelManager(int maxOpenChannels, long idleTimeoutMs, long batchSize,
                              long flushIntervalMs, long highWaterMark, long maxFileSize,
                              String charset, int initialBufferSize) {
        this.maxOpenChannels = maxOpenChannels;
        this.idleTimeoutMs = idleTimeoutMs;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.highWaterMark = highWaterMark;
        this.maxFileSize = maxFileSize;
        this.charset = charset;
        this.initialBufferSize = initialBufferSize;

        // accessOrder=true: 每次 get 命中时自动将 entry 移至链表末尾，实现 LRU
        this.channelMap = new LinkedHashMap<PathKey, FileChannel>(INITIAL_MAP_CAPACITY, LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<PathKey, FileChannel> eldest) {
                // 不使用 LinkedHashMap 内置淘汰，由 evictLRU() 显式控制
                return false;
            }
        };
    }

    /**
     * 获取或创建 FileChannel。
     *
     * 若 channelMap 中已存在对应 PathKey 的 Channel，直接返回（LinkedHashMap 自动更新访问顺序）。
     * 若不存在，且 channelMap 已满，则先淘汰最久未访问的 Channel，再创建新 Channel。
     *
     * @param pathKey 路径键
     * @return 对应的 FileChannel
     * @throws IOException 如果创建文件失败
     */
    public synchronized FileChannel getOrCreate(PathKey pathKey) throws IOException {
        // 1. 命中缓存：直接返回（LinkedHashMap accessOrder 自动更新 LRU 顺序）
        FileChannel channel = channelMap.get(pathKey);
        if (channel != null) {
            // 更新访问时间，确保 idleScan 不会误释放最近访问的 Channel
            channel.touch();
            return channel;
        }

        // 2. 未命中：检查容量，必要时淘汰
        if (channelMap.size() >= maxOpenChannels) {
            evictLRU();
        }

        // 3. 创建新 Channel
        Path dir = Paths.get(pathKey.getDir());
        String fileName = pathKey.getFile();
        channel = new FileChannel(pathKey, dir, fileName, charset, maxFileSize);

        // 4. 放入映射表
        channelMap.put(pathKey, channel);

        // 5. 统计：记录文件写入
        IoMetrics.recordFileWrite();

        logger.debug("FileChannel created: pathKey={}, total open channels={}", pathKey, channelMap.size());

        return channel;
    }

    /**
     * 淘汰最久未访问的 FileChannel（LRU）。
     *
     * 淘汰流程：flush → close → remove。
     * LinkedHashMap(accessOrder=true) 的第一个 entry 即为最久未访问的。
     */
    private void evictLRU() {
        Iterator<Map.Entry<PathKey, FileChannel>> it = channelMap.entrySet().iterator();
        if (!it.hasNext()) {
            return;
        }

        Map.Entry<PathKey, FileChannel> eldest = it.next();
        PathKey pathKey = eldest.getKey();
        FileChannel channel = eldest.getValue();

        try {
            channel.flush(batchSize, flushIntervalMs, highWaterMark, initialBufferSize);
            channel.close();
        } catch (IOException e) {
            logger.warn("Failed to evict FileChannel: pathKey={}, error={}", pathKey, e.getMessage());
        }

        it.remove();
        logger.debug("FileChannel evicted (LRU): pathKey={}, remaining={}", pathKey, channelMap.size());
    }

    /**
     * 扫描并释放空闲超时的 FileChannel。
     *
     * 遍历 channelMap，检查每个 Channel 的 lastAccessTime，
     * 若超过 idleTimeoutMs 未被访问，则 flush → close → remove。
     *
     * @return 被释放的 Channel 数量
     */
    public synchronized int idleScan() {
        int removedCount = 0;
        long now = System.currentTimeMillis();

        Iterator<Map.Entry<PathKey, FileChannel>> it = channelMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<PathKey, FileChannel> entry = it.next();
            FileChannel channel = entry.getValue();

            if (now - channel.getLastAccessTime() > idleTimeoutMs) {
                PathKey pathKey = entry.getKey();
                try {
                    // 先 flush 未写入的缓冲内容
                    if (channel.getEstimatedBytes() > 0) {
                        channel.flush(batchSize, flushIntervalMs, highWaterMark, initialBufferSize);
                    }
                    channel.close();
                } catch (IOException e) {
                    logger.warn("Failed to close idle FileChannel: pathKey={}, error={}", pathKey, e.getMessage());
                }

                it.remove();
                removedCount++;
                logger.debug("FileChannel idle closed: pathKey={}", pathKey);
            }
        }

        if (removedCount > 0) {
            logger.debug("Idle scan completed: removed={}, remaining={}", removedCount, channelMap.size());
        }

        return removedCount;
    }

    /**
     * 关闭所有 FileChannel。
     *
     * 遍历 channelMap，对每个 Channel 执行 flush → close，最后清空映射表。
     */
    public synchronized void closeAll() {
        for (Map.Entry<PathKey, FileChannel> entry : channelMap.entrySet()) {
            FileChannel channel = entry.getValue();
            try {
                if (channel.getEstimatedBytes() > 0) {
                    channel.flush(batchSize, flushIntervalMs, highWaterMark, initialBufferSize);
                }
                channel.close();
            } catch (IOException e) {
                logger.warn("Failed to close FileChannel: pathKey={}, error={}", entry.getKey(), e.getMessage());
            }
        }
        channelMap.clear();
        logger.debug("All FileChannels closed");
    }

    /**
     * 返回当前打开的 Channel 数量。
     *
     * @return 当前打开的 Channel 数量
     */
    public synchronized int size() {
        return channelMap.size();
    }

    /**
     * 返回 FD 上限。
     *
     * @return maxOpenChannels
     */
    public int getMaxOpenChannels() {
        return maxOpenChannels;
    }

    // ---- 静态方法：FD 动态计算 ----

    /**
     * 计算单个 Worker 的 FD 上限。
     *
     * 计算逻辑：
     * 1. 通过 OperatingSystemMXBean 获取系统 ulimit
     * 2. globalMaxOpenChannels = 系统 ulimit × 0.2（动态计算，非配置值）
     * 3. perWorkerLimit = min(globalMaxOpenChannels / workerCount, maxFileWriters)
     *
     * @param maxFileWriters 配置的最大文件写入器数（作为上限）
     * @param workerCount    Worker 数量
     * @return 单个 Worker 的 FD 上限
     */
    public static int calculatePerWorkerLimit(int maxFileWriters, int workerCount) {
        // 1. 获取系统 ulimit
        long systemUlimit = getSystemUlimit();

        // 2. 计算全局最大打开 Channel 数（动态计算，非配置值）
        long globalMaxOpenChannels = (long) (systemUlimit * ULIMIT_RESERVATION_RATIO);

        // 3. 计算每个 Worker 的上限
        long perWorkerLimit = globalMaxOpenChannels / workerCount;

        // 4. 取 min(perWorker, maxFileWriters)，且至少为 1
        int result = (int) Math.min(perWorkerLimit, maxFileWriters);
        if (result < 1) {
            result = 1;
        }

        logger.debug("FD 动态计算: systemUlimit={}, globalMaxOpenChannels={}, workerCount={}, "
                + "maxFileWriters={}, perWorkerLimit={}",
                systemUlimit, globalMaxOpenChannels, workerCount, maxFileWriters, result);

        return result;
    }

    /**
     * 获取系统 ulimit（最大文件描述符数）。
     *
     * 优先使用 com.sun.management.OperatingSystemMXBean.getMaxFileDescriptorCount()（JDK 14+），
     * 通过反射调用以避免 JDK 8 编译错误。
     * 回退使用保守默认值 1024。
     *
     * @return 系统 ulimit 值，若无法获取则返回保守默认值 1024
     */
    private static long getSystemUlimit() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        // 通过反射尝试调用 getMaxFileDescriptorCount()（JDK 14+ 可用）
        try {
            Method method = osBean.getClass().getMethod("getMaxFileDescriptorCount");
            Object result = method.invoke(osBean);
            if (result instanceof Long) {
                long maxFD = (Long) result;
                if (maxFD > 0) {
                    return maxFD;
                }
            }
        } catch (Exception e) {
            // 方法不存在或调用失败，使用默认值
            logger.debug("getMaxFileDescriptorCount() 不可用，使用默认 ulimit 值: {}", e.getMessage());
        }

        // 回退：无法获取时使用保守默认值
        logger.warn("无法获取系统 ulimit，使用默认值 1024");
        return 1024L;
    }
}