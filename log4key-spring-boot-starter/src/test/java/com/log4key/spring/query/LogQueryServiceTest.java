/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * LogQueryService 单元测试：目录浏览 / 模糊文件列表 / 分页读取 / 默认最后一页 / 边界行为。
 */
public class LogQueryServiceTest {

    /**
     * 测试用日志根目录（临时目录）
     */
    private Path root;

    /**
     * 被测查询服务
     */
    private LogQueryService service;

    /**
     * 每个用例前创建临时根目录并构造查询服务。
     *
     * @throws Exception 创建临时目录失败
     */
    @Before
    public void setUp() throws Exception {
        root = Files.createTempDirectory("log4key-query-test-");
        service = new LogQueryService(root, StandardCharsets.UTF_8);
    }

    /**
     * 每个用例后递归清理临时根目录。
     *
     * @throws Exception 清理失败
     */
    @After
    public void tearDown() throws Exception {
        deleteRecursively(root.toFile());
    }

    // ========================== 目录浏览 ==========================

    /**
     * 无 file 关键字时，目录浏览只返回一级子目录（升序），不返回文件。
     */
    @Test
    public void browseRootWithoutKeywordReturnsOnlyDirectories() throws Exception {
        mkdir(root, "info");
        mkdir(root, "error");
        Files.write(root.resolve("readme.txt"), "x".getBytes(StandardCharsets.UTF_8));

        DirectoryBrowse browse = service.browse("", null);
        assertEquals(Arrays.asList("error", "info"), browse.getDirectories());
        assertTrue("file list must be empty without keyword", browse.getFiles().isEmpty());
    }

    /**
     * file 关键字为空串时返回全部文件，并按最后修改时间降序排列。
     */
    @Test
    public void browseWithEmptyKeywordReturnsAllFilesSortedByMtimeDesc() throws Exception {
        mkdir(root, "info");
        Path older = Files.createFile(root.resolve("a.log"));
        Path newer = Files.createFile(root.resolve("b.log"));
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000L));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000L));

        DirectoryBrowse browse = service.browse("", "");
        assertEquals(Arrays.asList("info"), browse.getDirectories());
        assertEquals(Arrays.asList("b.log", "a.log"), browse.getFiles());
    }

    /**
     * file 关键字按文件名模糊匹配，且仅匹配当前目录、不递归子目录（含滚动文件命中）。
     */
    @Test
    public void browseWithKeywordFuzzyMatchesFilesInCurrentDirectoryOnly() throws Exception {
        Path dayDir = mkdir(root, "player", "20260908");
        Path base = writeLineFile(dayDir.resolve("player-10001.log"), "base");
        Path rolled = writeLineFile(dayDir.resolve("player-10001.20260908.103000.log"), "rolled");
        writeLineFile(dayDir.resolve("order-2.log"), "order");
        Files.setLastModifiedTime(base, FileTime.fromMillis(1_000L));
        Files.setLastModifiedTime(rolled, FileTime.fromMillis(2_000L));

        DirectoryBrowse browse = service.browse("player/20260908", "10001");
        assertEquals(Arrays.asList("player-10001.20260908.103000.log", "player-10001.log"),
                browse.getFiles());

        DirectoryBrowse byOrder = service.browse("player/20260908", "order");
        assertEquals(Arrays.asList("order-2.log"), byOrder.getFiles());
    }

    /**
     * 目标目录不存在时抛出 NOT_FOUND。
     */
    @Test(expected = QueryException.class)
    public void browseMissingDirectoryThrowsNotFound() {
        service.browse("no/such/dir", null);
    }

    /**
     * 对文件执行目录浏览时抛出 NOT_DIRECTORY（类型不匹配）。
     */
    @Test(expected = QueryException.class)
    public void browseFileTargetThrowsNotDirectory() throws Exception {
        Path file = writeLineFile(root.resolve("a.log"), "x");
        service.browse(file.getFileName().toString(), null);
    }

    // ========================== 分页读取 ==========================

    /**
     * 显式指定页码：每页固定 1000 行，最后一页按实际行数返回。
     */
    @Test
    public void readExplicitPagesWithFixedSize() throws Exception {
        Path file = root.resolve("big.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            sb.append(String.format("line-%04d%n", i));
        }
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));

        FilePage page1 = service.readPage("big.log", 1);
        assertEquals(1, page1.getPage());
        assertEquals(1000, page1.getLines().size());
        assertEquals("line-0000", page1.getLines().get(0));
        assertEquals("line-0999", page1.getLines().get(999));

        FilePage page2 = service.readPage("big.log", 2);
        assertEquals("line-1000", page2.getLines().get(0));
        assertEquals(1000, page2.getLines().size());

        FilePage page3 = service.readPage("big.log", 3);
        assertEquals(500, page3.getLines().size());
        assertEquals("line-2000", page3.getLines().get(0));
        assertEquals("line-2499", page3.getLines().get(499));
    }

    /**
     * 不传 page 时默认读取最后一页，并回显实际页码。
     */
    @Test
    public void defaultPageReadsLastPageAndEchoesPageNumber() throws Exception {
        Path file = root.resolve("big.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            sb.append(String.format("line-%04d%n", i));
        }
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));

        FilePage last = service.readPage("big.log", null);
        assertEquals(3, last.getPage());
        assertEquals(500, last.getLines().size());
        assertEquals("line-2000", last.getLines().get(0));
    }

    /**
     * 页码范围越界时返回空行数组，且原样回显请求的页码。
     */
    @Test
    public void pageBeyondRangeReturnsEmptyPage() throws Exception {
        Path file = writeLines(root.resolve("small.log"), 50);
        FilePage page = service.readPage("small.log", 9);
        assertEquals(9, page.getPage());
        assertTrue(page.getLines().isEmpty());
    }

    /**
     * 空文件默认返回第 1 页的空行数组。
     */
    @Test
    public void emptyFileReturnsEmptyPage() throws Exception {
        Files.createFile(root.resolve("empty.log"));
        FilePage last = service.readPage("empty.log", null);
        assertEquals(1, last.getPage());
        assertTrue(last.getLines().isEmpty());
    }

    /**
     * 文件末尾无换行符时，最后一行（未换行结尾）仍应在最后一页返回。
     */
    @Test
    public void fileWithoutTrailingNewlineKeepsFinalLineOnLastPage() throws Exception {
        Path file = root.resolve("partial.log");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            sb.append(String.format("line-%04d%n", i));
        }
        sb.append("final-no-newline"); // 不带换行结尾
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));

        FilePage last = service.readPage("partial.log", null);
        assertEquals(3, last.getPage());
        assertEquals(501, last.getLines().size());
        assertEquals("line-2000", last.getLines().get(0));
        assertEquals("final-no-newline", last.getLines().get(500));
    }

    /**
     * UTF-8 多字节内容（中文）解码正确，不出现乱码或截断。
     */
    @Test
    public void utf8ContentIsDecodedCorrectly() throws Exception {
        Path file = writeLines(root.resolve("utf8.log"), 3);
        List<String> raw = Arrays.asList("日志中文-0", "日志中文-1", "日志中文-2");
        Files.write(file, join(raw).getBytes(StandardCharsets.UTF_8));

        FilePage page = service.readPage("utf8.log", 1);
        assertEquals(3, page.getLines().size());
        assertEquals("日志中文-0", page.getLines().get(0));
        assertEquals("日志中文-2", page.getLines().get(2));
    }

    /**
     * 单页内容超过 8MB 字节护栏时抛出 PAGE_TOO_LARGE，避免超大页解码。
     */
    @Test
    public void oversizedPageIsRejectedByGuard() throws Exception {
        // 1000 行 × 9000 字节 ≈ 9MB，超过单页 8MB 护栏
        StringBuilder sb = new StringBuilder(9 * 1024 * 1024);
        StringBuilder line = new StringBuilder(9000);
        for (int i = 0; i < 9000; i++) {
            line.append('x');
        }
        for (int i = 0; i < 1000; i++) {
            sb.append(line).append('\n');
        }
        Files.write(root.resolve("huge.log"), sb.toString().getBytes(StandardCharsets.UTF_8));

        try {
            service.readPage("huge.log", 1);
            fail("expected PAGE_TOO_LARGE");
        } catch (QueryException e) {
            assertEquals(QueryException.ErrorCode.PAGE_TOO_LARGE, e.getCode());
        }
    }

    /**
     * file 关键字只做文件名包含匹配，不参与路径解析（穿越关键字既越界也不报错）。
     */
    @Test
    public void fileKeywordIsNotTreatedAsPath() throws Exception {
        Path dir = mkdir(root, "info");
        writeLineFile(dir.resolve("user-1.log"), "x");

        // 关键字只参与文件名包含匹配，不参与路径解析：带分隔符/穿越的关键字不应越界、也不应报错
        DirectoryBrowse browse = service.browse("info", "../../etc/passwd");
        assertTrue(browse.getDirectories().isEmpty());
        assertTrue(browse.getFiles().isEmpty());

        DirectoryBrowse matched = service.browse("info", "user-1");
        assertEquals(Arrays.asList("user-1.log"), matched.getFiles());
    }

    /**
     * 读取不存在的文件时抛出 NOT_FOUND。
     */
    @Test
    public void readMissingFileThrowsNotFound() {
        try {
            service.readPage("nope.log", 1);
            fail("expected NOT_FOUND");
        } catch (QueryException e) {
            assertEquals(QueryException.ErrorCode.NOT_FOUND, e.getCode());
        }
    }

    /**
     * 对目录执行文件读取时抛出 NOT_FILE（类型不匹配）。
     */
    @Test(expected = QueryException.class)
    public void readDirectoryTargetThrowsNotFile() throws Exception {
        mkdir(root, "info");
        service.readPage("info", 1);
    }

    /**
     * page 参数非正数时抛出 INVALID_ARGUMENT。
     */
    @Test(expected = QueryException.class)
    public void nonPositivePageThrowsInvalidArgument() throws Exception {
        Path file = writeLines(root.resolve("x.log"), 5);
        service.readPage("x.log", 0);
    }

    // ========================== 辅助 ==========================

    /**
     * 按给定路径片段逐级创建目录。
     *
     * @param base 起始目录
     * @param segments 依次拼接的目录片段
     * @return 创建后的目录
     * @throws Exception 创建失败
     */
    private static Path mkdir(Path base, String... segments) throws Exception {
        Path p = base;
        for (String segment : segments) {
            p = p.resolve(segment);
        }
        Files.createDirectories(p);
        return p;
    }

    /**
     * 写入指定行数的文本文件（每行以换行符结尾）。
     *
     * @param file 目标文件
     * @param count 行数
     * @return 写入后的文件
     * @throws Exception 写入失败
     */
    private static Path writeLines(Path file, int count) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(String.format("line-%04d%n", i));
        }
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * 写入单行内容（自动补换行符）。
     *
     * @param file 目标文件
     * @param content 行内容
     * @return 写入后的文件
     * @throws Exception 写入失败
     */
    private static Path writeLineFile(Path file, String content) throws Exception {
        Files.write(file, (content + "\n").getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * 将行列表拼成以换行符结尾的文本。
     *
     * @param lines 行列表
     * @return 拼接后的文本
     */
    private static String join(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    /**
     * 递归删除文件或目录。
     *
     * @param file 待删除的文件或目录
     */
    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
