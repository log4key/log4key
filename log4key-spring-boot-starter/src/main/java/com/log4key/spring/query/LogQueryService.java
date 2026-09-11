/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Local log file query service.
 *
 * 本地日志查询服务（纯文件系统实现，不依赖任何 servlet / HTTP 概念）。
 *
 * <p>能力范围（严格限定）：
 * <ul>
 *   <li>{@link #browse(String, String)}：目录浏览 —— 返回一级子目录（升序）；携带 file 关键字时同时返回
 *       文件列表（关键字为空=全部文件，否则文件名模糊匹配，均按最后修改时间降序）；</li>
 *   <li>{@link #readPage(String, Integer)}：文件分页读取，每页固定 {@link #PAGE_SIZE} 行；
 *       page 为空时默认读取最后一页（page 回显实际页码）；越界页返回空行数组。</li>
 * </ul>
 *
 * <p>读取实现说明：分页定位基于“换行符计数 + 目标窗口解码”，始终只解码请求窗口（单页字节护栏
 * {@link #MAX_PAGE_BYTES}），内存占用有界，不会把整个文件读入内存；默认最后一页需要一次全文件
 * 原始字节换行计数以确定总页数/页码（不解码内容），换取 page 回显与越界语义的确定性。
 * 超大文件的免全扫优化（尾部反向扫描）已评估但暂不实现，避免过度设计与双实现维护。
 */
public final class LogQueryService {

    /**
     * 每页固定行数
     */
    public static final int PAGE_SIZE = 1000;

    /**
     * 单页原始字节护栏（超过视为异常，不做超长页解码）
     */
    public static final long MAX_PAGE_BYTES = 8L * 1024 * 1024;

    /**
     * 原始扫描块大小（字节）
     */
    private static final int SCAN_BLOCK = 64 * 1024;

    /**
     * 路径解析与安全校验器
     */
    private final PathResolver pathResolver;

    /**
     * 读取日志使用的字符集
     */
    private final Charset charset;

    /**
     * 构造查询服务。
     *
     * @param rootDirectory 日志根目录（与 Log4Key 配置 rootDirectory 一致）
     * @param charset       读取日志使用的字符集（与日志写入字符集一致，默认 UTF-8）
     */
    public LogQueryService(Path rootDirectory, Charset charset) {
        this.pathResolver = new PathResolver(rootDirectory);
        this.charset = charset != null ? charset : Charset.forName("UTF-8");
    }

    /**
     * 获取规范化后的日志根目录。
     *
     * @return 根目录
     */
    public Path getRootDirectory() {
        return pathResolver.getRoot();
    }

    /**
     * 目录浏览。
     *
     * @param relativePath 相对路径（空串 / "/" 表示根目录）
     * @param fileKeyword  文件关键字；null 表示不请求文件列表，空串表示列出全部文件
     * @return 目录浏览结果（子目录名升序；文件按最后修改时间降序）
     * @throws QueryException 路径非法 / 不存在 / 越界 / IO 错误
     */
    public DirectoryBrowse browse(String relativePath, String fileKeyword) {
        Path dir = pathResolver.resolveDirectory(relativePath);

        List<String> dirs = new ArrayList<>();
        List<TimedFile> timedFiles = new ArrayList<>();
        boolean includeFiles = fileKeyword != null;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    dirs.add(child.getFileName().toString());
                } else if (includeFiles && Files.isRegularFile(child)) {
                    String name = child.getFileName().toString();
                    if (fileKeyword.isEmpty() || name.contains(fileKeyword)) {
                        long mtime = Files.getLastModifiedTime(child).toMillis();
                        timedFiles.add(new TimedFile(name, mtime));
                    }
                }
            }
        } catch (IOException e) {
            throw new QueryException(QueryException.ErrorCode.IO_ERROR,
                    "Failed to list directory: " + display(relativePath), e);
        }

        Collections.sort(dirs);

        List<String> files = new ArrayList<>(timedFiles.size());
        if (includeFiles) {
            Collections.sort(timedFiles, new Comparator<TimedFile>() {
                /**
                 * 按最后修改时间降序；时间相同时按文件名升序，保证结果稳定。
                 */
                @Override
                public int compare(TimedFile a, TimedFile b) {
                    int byTime = Long.compare(b.lastModified, a.lastModified);
                    return byTime != 0 ? byTime : a.name.compareTo(b.name);
                }
            });
            for (TimedFile timedFile : timedFiles) {
                files.add(timedFile.name);
            }
        }
        return new DirectoryBrowse(dirs, files);
    }

    /**
     * 文件分页读取。
     *
     * @param relativePath 相对文件路径
     * @param page         页码（从 1 开始）；null 表示读取最后一页（page 回显实际页码）；越界返回空行数组
     * @return 分页结果
     * @throws QueryException 路径非法 / 不存在 / 非文件 / 参数非法 / 页超限 / IO 错误
     */
    public FilePage readPage(String relativePath, Integer page) {
        Path file = pathResolver.resolveFile(relativePath);
        if (page != null && page <= 0) {
            throw new QueryException(QueryException.ErrorCode.INVALID_ARGUMENT,
                    "Page must be >= 1, but was: " + page);
        }
        return doReadPage(file, display(relativePath), page);
    }

    // ========================== 分页读取实现 ==========================

    /**
     * 分页核心：换行计数 + 窗口定位 + 窗口解码。
     *
     * @param file 已校验的目标文件
     * @param displayPath 展示用相对路径（用于错误消息）
     * @param requestedPage 请求页码；null 表示最后一页
     * @return 分页结果
     * @throws QueryException 页超限 / IO 错误
     */
    private FilePage doReadPage(Path file, String displayPath, Integer requestedPage) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long fileLength = raf.length();
            long[] scan = countNewlines(raf, fileLength);
            long totalNewlines = scan[0];
            boolean endsWithNewline = scan[1] == 1L;

            long totalLines = fileLength == 0L ? 0L : totalNewlines + (endsWithNewline ? 0L : 1L);
            long totalPages = totalLines == 0L ? 1L : (totalLines + PAGE_SIZE - 1L) / PAGE_SIZE;

            long startLine;
            int resultPage;
            if (requestedPage == null) {
                resultPage = (int) totalPages;
                startLine = (totalPages - 1L) * PAGE_SIZE;
            } else {
                resultPage = requestedPage;
                startLine = ((long) requestedPage - 1L) * PAGE_SIZE;
            }

            // 越界页：返回空行数组（page 原样回显）
            if (startLine >= totalLines) {
                return new FilePage(resultPage, Collections.<String>emptyList());
            }

            // 定位窗口起始字节：跳过前 startLine 个换行符结尾的记录
            long skipNewlines = Math.min(startLine, totalNewlines);
            long byteStart = offsetAfterNthNewline(raf, fileLength, skipNewlines);

            // 定位窗口结束字节：窗口覆盖 PAGE_SIZE 行；含未换行结尾的尾部行时延伸到 EOF
            long byteEnd;
            if (startLine + PAGE_SIZE <= totalNewlines) {
                byteEnd = offsetAfterNthNewline(raf, fileLength, startLine + PAGE_SIZE);
            } else {
                byteEnd = fileLength;
            }

            long windowBytes = byteEnd - byteStart;
            if (windowBytes > MAX_PAGE_BYTES) {
                throw new QueryException(QueryException.ErrorCode.PAGE_TOO_LARGE,
                        "Page content exceeds " + MAX_PAGE_BYTES + " bytes: " + displayPath);
            }

            List<String> lines = decodeWindow(raf, byteStart, (int) windowBytes);
            return new FilePage(resultPage, lines);
        } catch (QueryException e) {
            throw e;
        } catch (IOException e) {
            throw new QueryException(QueryException.ErrorCode.IO_ERROR,
                    "Failed to read file: " + displayPath, e);
        }
    }

    /**
     * 原始扫描：统计换行符数量并判断文件是否以换行符结尾。
     * 返回 long[]{换行数, 是否以换行结尾(1/0)}。
     *
     * @param raf 已打开的文件句柄
     * @param fileLength 文件长度（字节）
     * @return long[]{换行数, 是否以换行结尾(1/0)}
     * @throws IOException 读取失败
     */
    private static long[] countNewlines(RandomAccessFile raf, long fileLength) throws IOException {
        if (fileLength == 0L) {
            return new long[]{0L, 0L};
        }
        byte[] buffer = new byte[SCAN_BLOCK];
        long total = 0L;
        long newlines = 0L;
        byte last = 0;
        raf.seek(0L);
        while (total < fileLength) {
            int read = raf.read(buffer, 0, (int) Math.min(SCAN_BLOCK, fileLength - total));
            if (read <= 0) {
                break;
            }
            for (int i = 0; i < read; i++) {
                if (buffer[i] == '\n') {
                    newlines++;
                }
            }
            last = buffer[read - 1];
            total += read;
        }
        return new long[]{newlines, last == '\n' ? 1L : 0L};
    }

    /**
     * 返回跳过 n 个换行符之后（第 n 个 '\n' 之后一个字节）的文件偏移。
     * n == 0 返回 0；文件中换行符不足 n 个时返回文件长度。
     *
     * @param raf 已打开的文件句柄
     * @param fileLength 文件长度（字节）
     * @param n 需要跳过的换行符个数
     * @return 偏移量（字节）
     * @throws IOException 读取失败
     */
    private static long offsetAfterNthNewline(RandomAccessFile raf, long fileLength, long n)
            throws IOException {
        if (n <= 0L) {
            return 0L;
        }
        byte[] buffer = new byte[SCAN_BLOCK];
        long total = 0L;
        long seen = 0L;
        raf.seek(0L);
        while (total < fileLength) {
            int read = raf.read(buffer, 0, (int) Math.min(SCAN_BLOCK, fileLength - total));
            if (read <= 0) {
                break;
            }
            for (int i = 0; i < read; i++) {
                if (buffer[i] == '\n') {
                    seen++;
                    if (seen == n) {
                        return total + i + 1L;
                    }
                }
            }
            total += read;
        }
        return fileLength;
    }

    /**
     * 解码 [byteStart, byteStart+length) 窗口并按行切分。
     * 兼容 '\n' 与 '\r\n'（行尾 '\r' 会被去除）；窗口以换行符边界或 EOF 结束，多字节字符不会跨窗口截断。
     *
     * @param raf 已打开的文件句柄
     * @param byteStart 窗口起始字节偏移
     * @param length 窗口字节长度
     * @return 该窗口内的行列表
     * @throws IOException 读取失败
     */
    private List<String> decodeWindow(RandomAccessFile raf, long byteStart, int length)
            throws IOException {
        List<String> lines = new ArrayList<>();
        if (length <= 0) {
            return lines;
        }
        byte[] bytes = new byte[length];
        raf.seek(byteStart);
        int total = 0;
        while (total < length) {
            int read = raf.read(bytes, total, length - total);
            if (read <= 0) {
                break;
            }
            total += read;
        }
        String content = new String(bytes, 0, length, charset);
        // 按 '\n' 切分；若内容以 '\n' 结尾则丢弃 split 产生的末尾空串
        String[] parts = content.split("\n", -1);
        int count = parts.length;
        if (content.endsWith("\n")) {
            count--;
        }
        for (int i = 0; i < count; i++) {
            String line = parts[i];
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            lines.add(line);
        }
        return lines;
    }

    // ========================== 辅助 ==========================

    /**
     * 展示用相对路径（避免把绝对路径暴露给外部）。
     *
     * @param relative 原始相对路径
     * @return 展示用路径（空路径显示为 "/"）
     */
    private static String display(String relative) {
        return (relative == null || relative.isEmpty()) ? "/" : relative;
    }

    /**
     * 内部排序载体：文件名 + 最后修改时间
     */
    private static final class TimedFile {

        /**
         * 文件名
         */
        private final String name;

        /**
         * 最后修改时间（毫秒）
         */
        private final long lastModified;

        /**
         * 构造排序载体。
         *
         * @param name 文件名
         * @param lastModified 最后修改时间（毫秒）
         */
        private TimedFile(String name, long lastModified) {
            this.name = name;
            this.lastModified = lastModified;
        }
    }
}
