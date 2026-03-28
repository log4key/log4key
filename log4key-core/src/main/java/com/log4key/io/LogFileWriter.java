/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;
import com.log4key.internal.InternalLogger;
import com.log4key.metrics.IoMetrics;

/**
 * Log file writer.
 *
 * 日志文件写入器。
 */
public class LogFileWriter implements AutoCloseable {

    /**
     * 内部日志记录器
     */
    private static final InternalLogger logger = InternalLogger.getLogger(LogFileWriter.class);

    /**
     * 默认缓冲区大小（8KB）
     */
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    /**
     * 最小缓冲区大小（1KB）
     */
    private static final int MIN_BUFFER_SIZE = 1024;

    /**
     * 最大缓冲区大小（1MB）
     */
    private static final int MAX_BUFFER_SIZE = 1024 * 1024;

    /**
     * 默认最大文件大小（50MB）
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 默认滚动策略
     */
    private static final RollingPolicy DEFAULT_ROLLING_POLICY = RollingPolicy.SIZE;

    /**
     * 默认滚动时间间隔（1天）
     */
    private static final long DEFAULT_ROLLING_INTERVAL = 24 * 60 * 60 * 1000;

    /**
     * 默认是否启用压缩
     */
    private static final boolean DEFAULT_COMPRESS_ENABLED = false;

    /**
     * 默认字符编码
     */
    private static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * 原始文件路径
     */
    private final String originalFilePath;

    /**
     * 规范文件路径（绝对路径，用于内部操作）
     */
    private final String canonicalFilePath;

    /**
     * 字符编码
     */
    private final String charset;

    /**
     * 写入器
     */
    private volatile Writer writer;

    /**
     * 写入锁，保证线程安全
     */
    private final ReentrantLock writeLock = new ReentrantLock();

    /**
     * 是否已关闭
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 缓冲区大小
     */
    private final int bufferSize;

    /**
     * 最大文件大小
     */
    private final long maxFileSize;

    /**
     * 滚动策略
     */
    private final RollingPolicy rollingPolicy;

    /**
     * 滚动时间间隔（毫秒）
     */
    private final long rollingInterval;

    /**
     * 是否启用压缩
     */
    private final boolean compressEnabled;

    /**
     * 文件当前大小
     */
    private volatile long currentFileSize = 0L;

    /**
     * 上次滚动时间
     */
    private volatile long lastRollTime = System.currentTimeMillis();

    /**
     * 写入次数统计
     */
    private final AtomicLong writeCount = new AtomicLong(0);

    /**
     * 批量写入次数统计
     */
    private final AtomicLong batchWriteCount = new AtomicLong(0);

    /**
     * 写入字节数统计
     */
    private final AtomicLong bytesWritten = new AtomicLong(0);

    /**
     * 滚动次数统计
     */
    private final AtomicLong rollCount = new AtomicLong(0);

    /**
     * 最后使用时间
     */
    private volatile long lastUsedTime = System.currentTimeMillis();

    /**
     * 滚动策略枚举
     */
    public enum RollingPolicy {
        /**
         * 基于文件大小的滚动策略
         */
        SIZE,

        /**
         * 基于时间的滚动策略
         */
        TIME,

        /**
         * 基于文件大小和时间的滚动策略
         */
        SIZE_AND_TIME
    }

    /**
     * Constructor with default buffer size and max file size.
     *
     * 使用默认缓冲区大小和最大文件大小的构造函数。
     *
     * @param filePath the log file path / 日志文件路径
     * @throws IOException if file creation fails / 如果创建或打开文件失败
     */
    public LogFileWriter(String filePath) throws IOException {
        this(filePath, DEFAULT_BUFFER_SIZE, DEFAULT_MAX_FILE_SIZE, DEFAULT_ROLLING_POLICY, DEFAULT_ROLLING_INTERVAL, DEFAULT_COMPRESS_ENABLED);
    }

    /**
     * Constructor with custom buffer size and max file size.
     *
     * 自定义缓冲区大小和最大文件大小的构造函数。
     *
     * @param filePath the log file path / 日志文件路径
     * @param bufferSize the buffer size in bytes / 缓冲区大小（字节）
     * @param maxFileSize the max file size in bytes / 最大文件大小（字节）
     * @throws IOException if file creation fails / 如果创建或打开文件失败
     */
    public LogFileWriter(String filePath, int bufferSize, long maxFileSize) throws IOException {
        this(filePath, bufferSize, maxFileSize, DEFAULT_ROLLING_POLICY, DEFAULT_ROLLING_INTERVAL, DEFAULT_COMPRESS_ENABLED);
    }

    /**
     * Constructor with custom all parameters (without specifying charset).
     *
     * 自定义所有参数的构造函数（不指定charset，使用默认值）。
     *
     * @param filePath the log file path / 日志文件路径
     * @param bufferSize the buffer size in bytes / 缓冲区大小（字节）
     * @param maxFileSize the max file size in bytes / 最大文件大小（字节）
     * @param rollingPolicy the rolling policy / 滚动策略
     * @param rollingInterval the rolling interval in milliseconds / 滚动时间间隔（毫秒）
     * @param compressEnabled whether compression is enabled / 是否启用压缩
     * @throws IOException if file creation fails / 如果创建或打开文件失败
     */
    public LogFileWriter(String filePath, int bufferSize, long maxFileSize, RollingPolicy rollingPolicy, long rollingInterval, boolean compressEnabled) throws IOException {
        this(filePath, bufferSize, maxFileSize, rollingPolicy, rollingInterval, compressEnabled, DEFAULT_CHARSET);
    }

    /**
     * Constructor with custom parameters including charset.
     *
     * 自定义所有参数的构造函数（包括字符编码）。
     *
     * @param filePath the log file path / 日志文件路径
     * @param bufferSize the buffer size in bytes / 缓冲区大小（字节）
     * @param maxFileSize the max file size in bytes / 最大文件大小（字节）
     * @param rollingPolicy the rolling policy / 滚动策略
     * @param rollingInterval the rolling interval in milliseconds / 滚动时间间隔（毫秒）
     * @param compressEnabled whether compression is enabled / 是否启用压缩
     * @param charset the character encoding / 字符编码
     * @throws IOException if file creation fails / 如果创建或打开文件失败
     */
    public LogFileWriter(String filePath, int bufferSize, long maxFileSize, RollingPolicy rollingPolicy, long rollingInterval, boolean compressEnabled, String charset) throws IOException {
        // 验证文件路径合法性
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        // 保存原始路径
        this.originalFilePath = filePath;

        // 清理路径，防止路径注入（获取规范路径）
        String canonicalPath = new File(filePath).getCanonicalPath();
        this.canonicalFilePath = canonicalPath;

        // 验证缓冲区大小
        if (bufferSize < MIN_BUFFER_SIZE || bufferSize > MAX_BUFFER_SIZE) {
            throw new IllegalArgumentException("Buffer size must be between " + MIN_BUFFER_SIZE + " and " + MAX_BUFFER_SIZE + " bytes");
        }

        // 验证最大文件大小
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Max file size must be positive");
        }

        // 验证滚动策略
        if (rollingPolicy == null) {
            throw new IllegalArgumentException("Rolling policy cannot be null");
        }

        // 验证滚动时间间隔
        if (rollingInterval <= 0) {
            throw new IllegalArgumentException("Rolling interval must be positive");
        }

        // 验证字符编码
        if (charset == null || charset.trim().isEmpty()) {
            charset = DEFAULT_CHARSET;
        }

        this.bufferSize = bufferSize;
        this.maxFileSize = maxFileSize;
        this.rollingPolicy = rollingPolicy;
        this.rollingInterval = rollingInterval;
        this.compressEnabled = compressEnabled;
        this.charset = charset;

        // 初始化最后滚动时间为当前时间
        this.lastRollTime = System.currentTimeMillis();

        // 初始化文件写入器
        initializeWriter();
    }

    /**
     * Writes a log message.
     *
     * 写入日志消息。
     *
     * @param message the message to write / 要写入的消息
     * @throws IOException if write fails / 如果写入失败
     */
    public void write(String message) throws IOException {
        if (closed.get()) {
            throw new IOException("LogFileWriter is already closed: " + originalFilePath);
        }
        if (message == null) {
            return; // 静默忽略null消息
        }

        // 记录日志事件
        IoMetrics.recordEvent();

        // 更新最后使用时间
        lastUsedTime = System.currentTimeMillis();

        // 计算消息大小
        byte[] messageBytes = message.getBytes(java.nio.charset.Charset.forName(this.charset));
        long messageSize = messageBytes.length;

        // 检查是否需要滚动（优化锁粒度）
        if (needsRolling(messageSize)) {
            writeLock.lock();
            try {
                // 双重检查，防止并发滚动
                if (needsRolling(messageSize)) {
                    rollFile();
                }
            } finally {
                writeLock.unlock();
            }
        }

        writeLock.lock();
        try {
            // 确保writer有效
            if (writer == null) {
                initializeWriter();
            }

            // 写入消息
            writer.write(message);

            // 记录写操作
            IoMetrics.recordWrite(messageSize);

            // 更新文件大小
            currentFileSize += messageSize;
            writeCount.incrementAndGet();
            bytesWritten.addAndGet(messageSize);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 批量写入多个日志消息
     * 通过一次获取锁完成多条消息的写入，减少锁竞争
     * @param messages 消息列表
     * @throws IOException 如果写入失败
     */
    public void writeBatch(List<String> messages) throws IOException {
        if (closed.get()) {
            throw new IOException("LogFileWriter is already closed: " + originalFilePath);
        }
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 更新最后使用时间
        lastUsedTime = System.currentTimeMillis();

        writeLock.lock();
        try {
            // 确保writer有效
            if (writer == null) {
                initializeWriter();
            }

            for (String message : messages) {
                if (message != null) {
                    // 记录日志事件
                    IoMetrics.recordEvent();

                    // 计算消息大小
                    byte[] messageBytes = message.getBytes(java.nio.charset.Charset.forName(this.charset));
                    long messageSize = messageBytes.length;

                    if (needsRolling(messageSize)) {
                        rollFile();
                    }

                    // 写入消息
                    writer.write(message);

                    // 记录写操作
                    IoMetrics.recordWrite(messageSize);

                    // 更新文件大小
                    currentFileSize += messageSize;
                    writeCount.incrementAndGet();
                    bytesWritten.addAndGet(messageSize);
                }
            }
            batchWriteCount.incrementAndGet();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Flushes the buffer to disk.
     *
     * 刷新缓冲区到磁盘。
     */
    public void flush() {
        if (closed.get()) {
            return;
        }

        // 更新最后使用时间
        lastUsedTime = System.currentTimeMillis();

        writeLock.lock();
        try {
            if (writer != null) {
                writer.flush();
                // 记录刷新操作
                IoMetrics.recordFlush();
            }
        } catch (IOException e) {
            logger.warn("Error flushing log file writer for " + originalFilePath + ": " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Forces shutdown of the file writer (with timeout control).
     *
     * 强制关闭文件写入器（带超时控制）。
     */
    public void shutdown() {
        if (closed.getAndSet(true)) {
            return; // 已经关闭
        }

        writeLock.lock();
        try {
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e) {
                    logger.warn("Error flushing log file writer for " + originalFilePath + ": " + e.getMessage());
                }

                try {
                    writer.close();
                } catch (IOException e) {
                    logger.warn("Error closing log file writer for " + originalFilePath + ": " + e.getMessage());
                }
                writer = null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Closes the file writer and releases resources.
     *
     * 关闭文件写入器并释放资源。
     */
    @Override
    public void close() {
        if (closed.getAndSet(true)) {
            return; // 已经关闭
        }

        writeLock.lock();
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        } catch (IOException e) {
            logger.warn("Error closing log file writer for " + originalFilePath + ": " + e.getMessage());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Checks if the writer is idle.
     *
     * 检查写入器是否空闲。
     *
     * @param currentTimeMillis the current time in milliseconds / 当前时间（毫秒）
     * @param idleThreshold the idle threshold in milliseconds / 空闲阈值（毫秒）
     * @return true if idle / 如果超过空闲阈值返回true
     */
    public boolean isIdle(long currentTimeMillis, long idleThreshold) {
        return currentTimeMillis - lastUsedTime > idleThreshold;
    }

    /**
     * Checks if the writer is closed.
     *
     * 检查写入器是否已关闭。
     *
     * @return true if closed / 如果已关闭返回true
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Gets the current file size.
     *
     * 获取当前文件大小。
     *
     * @return the current file size in bytes / 当前文件大小（字节）
     */
    public long getCurrentFileSize() {
        return currentFileSize;
    }

    /**
     * Gets the file path.
     *
     * 获取文件路径。
     *
     * @return the file path / 文件路径
     */
    public String getFilePath() {
        return originalFilePath;
    }

    /**
     * Gets the write count.
     *
     * 获取写入次数。
     *
     * @return the write count / 写入次数
     */
    public long getWriteCount() {
        return writeCount.get();
    }

    /**
     * Gets the batch write count.
     *
     * 获取批量写入次数。
     *
     * @return the batch write count / 批量写入次数
     */
    public long getBatchWriteCount() {
        return batchWriteCount.get();
    }

    /**
     * Gets the number of bytes written.
     *
     * 获取写入字节数。
     *
     * @return the number of bytes written / 写入字节数
     */
    public long getBytesWritten() {
        return bytesWritten.get();
    }

    /**
     * Gets the roll count.
     *
     * 获取滚动次数。
     *
     * @return the roll count / 滚动次数
     */
    public long getRollCount() {
        return rollCount.get();
    }

    /**
     * 初始化文件写入器
     * @throws IOException 如果初始化失败
     */
    private void initializeWriter() throws IOException {
        // 确保目录存在
        ensureDirectoryExists();

        // 检查文件是否存在，并获取当前大小
        File file = new File(canonicalFilePath);
        if (file.exists()) {
            currentFileSize = file.length();
        } else {
            // 创建新文件
            boolean created = file.createNewFile();
            if (!created && !file.exists()) {
                throw new IOException("Failed to create log file: " + canonicalFilePath);
            }

            // 设置文件权限
            file.setReadable(true, false);  // 所有者可读
            file.setWritable(true, false); // 所有者可写
            file.setExecutable(false);     // 不可执行
        }

        // 创建带缓冲的写入器
        writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(file, true), // 追加模式
                        java.nio.charset.Charset.forName(this.charset)),
                bufferSize
        );
    }

    /**
     * 确保目录存在
     * @throws IOException 如果创建目录失败
     */
    private void ensureDirectoryExists() throws IOException {
        File file = new File(canonicalFilePath);
        File directory = file.getParentFile();

        if (directory != null && !directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created && !directory.exists()) {
                throw new IOException("Failed to create directory: " + directory.getPath());
            }
        }
    }

    /**
     * 检查文件是否需要滚动
     * @param messageSize 待写入消息的大小（字节）
     * @return 是否需要滚动文件
     */
    private boolean needsRolling(long messageSize) {
        // 使用当前对象的状态，避免多线程竞态条件
        long currentFileSizeSnapshot = currentFileSize;
        long lastRollTimeSnapshot = lastRollTime;
        long currentTime = System.currentTimeMillis();

        boolean sizeCondition = currentFileSizeSnapshot + messageSize >= maxFileSize;
        boolean timeCondition = currentTime - lastRollTimeSnapshot >= rollingInterval;

        switch (rollingPolicy) {
            case SIZE:
                return sizeCondition;
            case TIME:
                return timeCondition;
            case SIZE_AND_TIME:
                return sizeCondition || timeCondition;
            default:
                return sizeCondition; // 默认基于大小
        }
    }

    /**
     * 滚动日志文件（创建新文件，重命名旧文件）
     * @throws IOException 如果滚动失败
     */
    private void rollFile() throws IOException {
        // 关闭当前writer
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }

        // 生成滚动后的文件名
        String rolledFileName = generateRolledFileName();

        // 重命名当前文件
        File currentFile = new File(canonicalFilePath);
        File rolledFile = new File(rolledFileName);

        // 如果滚动后的文件已存在，则删除它
        if (rolledFile.exists() && !rolledFile.delete()) {
            throw new IOException("Failed to delete existing rolled file: " + rolledFileName);
        }

        // 执行文件滚动
        boolean renamed = currentFile.renameTo(rolledFile);
        if (!renamed) {
            // 尝试复制文件内容，作为重命名失败的备选方案
            try {
                Files.copy(Paths.get(canonicalFilePath), Paths.get(rolledFileName));
                // 清除当前文件内容
                Files.write(Paths.get(canonicalFilePath), new byte[0]);
            } catch (IOException e) {
                throw new IOException("Failed to roll log file: " + canonicalFilePath, e);
            }
        }

        // 实现压缩功能
        if (compressEnabled) {
            compressFile(rolledFile);
        }

        // 重置文件大小和滚动时间
        currentFileSize = 0L;
        lastRollTime = System.currentTimeMillis();
        rollCount.incrementAndGet();

        // 重新初始化writer
        initializeWriter();
    }

    /**
     * 生成滚动后的文件名
     * @return 滚动后的文件名
     */
    private String generateRolledFileName() {
        // 使用时间戳作为后缀
        long timestamp = System.currentTimeMillis();
        int lastDotIndex = canonicalFilePath.lastIndexOf('.');

        if (lastDotIndex > 0) {
            String baseName = canonicalFilePath.substring(0, lastDotIndex);
            String extension = canonicalFilePath.substring(lastDotIndex);
            // 格式为 base.timestamp.extension
            return baseName + "." + timestamp + extension;
        } else {
            return canonicalFilePath + "." + timestamp;
        }
    }

    /**
     * 压缩文件
     * @param file 要压缩的文件
     * @throws IOException 如果压缩失败
     */
    private void compressFile(File file) throws IOException {
        String compressedFileName = file.getAbsolutePath() + ".gz";
        File compressedFile = new File(compressedFileName);

        try (GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(compressedFile));
             OutputStreamWriter osw = new OutputStreamWriter(gzipOut, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             BufferedReader reader =  Files.newBufferedReader(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8)){

            String line;
            while ((line = reader.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }

            // 压缩完成后删除原文件
            if (!file.delete()) {
                logger.warn("Failed to delete original file after compression: " + file.getAbsolutePath());
            }
        }
    }
}
