/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.channel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.log4key.metrics.IoMetrics;
import com.log4key.path.PathKey;

/**
 * 日志文件写入执行单元。
 *
 * 负责单一日志文件的缓冲写入、刷新策略、文件滚动和 IoMetrics 统计。
 */
public class FileChannel {

    /** 路径键 */
    private final PathKey pathKey;

    /** 写入缓冲区，初始容量 4096 */
    private StringBuilder buffer;

    /** 近似字节数（禁止 getBytes()，以 length * 2 近似统计） */
    private long estimatedBytes;

    /** 最后访问时间 */
    private long lastAccessTime;

    /** 最后刷新时间 */
    private long lastFlushTime;

    /** 字符编码 */
    private final String charset;

    /** 文件写入器 */
    private Writer writer;

    /** 最大文件大小（字节） */
    private final long maxFileSize;

    /** 当前写入的文件对象 */
    private final File currentFile;

    /**
     * 构造 FileChannel 实例。
     *
     * @param pathKey     路径键，用于 rolling 后重新创建 writer
     * @param directory   文件所在目录
     * @param fileName    文件名
     * @param charset     字符编码
     * @param maxFileSize 最大文件大小（字节）
     * @throws IOException 如果创建目录或文件失败
     */
    public FileChannel(PathKey pathKey, Path directory, String fileName, String charset, long maxFileSize) throws IOException {
        this.pathKey = pathKey;
        this.charset = charset;
        this.maxFileSize = maxFileSize;
        this.buffer = new StringBuilder(4096);
        this.estimatedBytes = 0L;
        this.lastAccessTime = System.currentTimeMillis();
        this.lastFlushTime = System.currentTimeMillis();
        this.currentFile = directory.resolve(fileName).toFile();

        // 确保父目录存在
        File parentDir = currentFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 创建追加模式的写入器（使用 NIO Files API 以确保 Windows 文件句柄正确释放）
        this.writer = Files.newBufferedWriter(
                currentFile.toPath(),
                Charset.forName(charset),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    /**
     * 追加日志消息到缓冲区（不直接写入文件）。
     *
     * @param message 日志消息
     */
    public void append(String message) {
        buffer.append(message);
        // 近似统计：每个字符约 2 字节（禁止调用 getBytes()）
        estimatedBytes += (long) message.length() << 1;
        lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 更新最后访问时间，用于 idleScan 判断。
     *
     * 不修改缓冲区内容和统计计数器。
     */
    public void touch() {
        lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 判断是否需要执行刷新。
     *
     * @param batchSize       批量大小阈值（字节）
     * @param flushIntervalMs 刷新间隔阈值（毫秒）
     * @param highWaterMark   高水位阈值（字节）
     * @return true 如果需要刷新
     */
    public boolean shouldFlush(long batchSize, long flushIntervalMs, long highWaterMark) {
        long now = System.currentTimeMillis();
        return estimatedBytes >= batchSize
                || (now - lastFlushTime) >= flushIntervalMs
                || estimatedBytes >= highWaterMark;
    }

    /**
     * 执行缓冲区刷新：检查滚动、写入文件、更新 IoMetrics、清空缓冲区。
     *
     * @param batchSize        批量大小阈值（字节）
     * @param flushIntervalMs  刷新间隔阈值（毫秒）
     * @param highWaterMark    高水位阈值（字节）
     * @param initialBufferSize 初始缓冲区大小（用于重建 buffer）
     * @throws IOException 如果写入或滚动失败
     */
    public void flush(long batchSize, long flushIntervalMs, long highWaterMark, int initialBufferSize) throws IOException {
        long now = System.currentTimeMillis();

        // 1. 检查是否需要滚动
        if (needsRolling()) {
            rollFile();
        }

        // 2. 将 buffer 内容编码为字节数组（用于统计精确字节数）
        String content = buffer.toString();
        byte[] bytes = content.getBytes(Charset.forName(charset));

        // 3. 写入文件
        writer.write(content);

        // 4. 记录写操作（使用实际字节数）
        IoMetrics.recordWrite(bytes.length);

        // 5. 刷新 writer
        writer.flush();

        // 6. 记录刷新操作
        IoMetrics.recordFlush();

        // 7. 清空缓冲区
        if (buffer.capacity() > highWaterMark) {
            // 缓冲区容量过高，创建新实例以释放内存
            buffer = new StringBuilder(initialBufferSize);
        } else {
            // 缓冲区容量在允许范围内，复用
            buffer.setLength(0);
        }

        // 8. 重置统计
        estimatedBytes = 0L;
        lastFlushTime = now;
    }

    /**
     * 判断当前文件是否需要滚动。
     *
     * @return true 如果 currentFile 实际大小 + 近似字节数 >= maxFileSize
     */
    public boolean needsRolling() {
        return currentFile.length() + estimatedBytes >= maxFileSize;
    }

    /**
     * 执行文件滚动：关闭当前 writer → 重命名当前文件 → 创建新 writer。
     *
     * 注意：单条日志不能跨文件 —— 调用方必须在 flush 前检查并执行滚动。
     *
     * @throws IOException 如果文件重命名或创建新 writer 失败
     */
    public void rollFile() throws IOException {
        // 1. 关闭当前 writer
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }

        // 2. 重命名当前文件：原名.yyyyMMdd.HHmmss.扩展名
        String timestamp = new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date());
        String currentPath = currentFile.getAbsolutePath();
        int lastDot = currentPath.lastIndexOf('.');

        String rolledPath;
        if (lastDot > 0) {
            // 例如: app.log → app.20260606.143012.log
            rolledPath = currentPath.substring(0, lastDot) + "." + timestamp + currentPath.substring(lastDot);
        } else {
            rolledPath = currentPath + "." + timestamp;
        }

        Files.move(currentFile.toPath(), new File(rolledPath).toPath(), StandardCopyOption.REPLACE_EXISTING);

        // 4. 创建新 writer（非追加模式，因为文件已重命名）
        writer = Files.newBufferedWriter(
                currentFile.toPath(),
                Charset.forName(charset),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * 关闭 FileChannel：flush 并关闭 writer。
     */
    public void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }

    // ---- 以下为测试/监控辅助方法 ----

    /**
     * 返回当前缓冲区内容，供测试验证。
     *
     * @return 缓冲区内容
     */
    public String getBufferContent() {
        return buffer.toString();
    }

    /**
     * 返回近似字节数，供测试验证。
     *
     * @return estimatedBytes
     */
    public long getEstimatedBytes() {
        return estimatedBytes;
    }

    /**
     * 返回最后访问时间，供空闲检测使用。
     *
     * @return lastAccessTime
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * 返回最后刷新时间，供刷新策略判断。
     *
     * @return lastFlushTime
     */
    public long getLastFlushTime() {
        return lastFlushTime;
    }

    /**
     * 返回当前文件对象，供测试验证。
     *
     * @return currentFile
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * 返回缓冲区当前容量，供测试验证 highWaterMark 逻辑。
     *
     * @return buffer.capacity()
     */
    public int getBufferCapacity() {
        return buffer.capacity();
    }

    /**
     * 返回写入器是否已关闭（null 视为已关闭）。
     *
     * @return true 如果 writer 为 null
     */
    public boolean isWriterClosed() {
        return writer == null;
    }
}