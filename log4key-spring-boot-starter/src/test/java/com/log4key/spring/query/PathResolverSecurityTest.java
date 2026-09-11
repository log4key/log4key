/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * PathResolver 安全测试：路径穿越 / 分隔符 / 控制字符 / 越界（符号链接）/ 不存在路径。
 */
public class PathResolverSecurityTest {

    /**
     * 测试基目录（临时目录；root 为其下的 logs 子目录）
     */
    private Path base;

    /**
     * 日志根目录（安全边界）
     */
    private Path root;

    /**
     * 被测路径解析器
     */
    private PathResolver resolver;

    /**
     * 每个用例前创建临时基目录与根目录，并构造解析器。
     *
     * @throws Exception 创建临时目录失败
     */
    @Before
    public void setUp() throws Exception {
        base = Files.createTempDirectory("log4key-security-");
        root = Files.createDirectory(base.resolve("logs"));
        resolver = new PathResolver(root);
    }

    /**
     * 每个用例后递归清理临时基目录。
     *
     * @throws Exception 清理失败
     */
    @org.junit.After
    public void tearDown() throws Exception {
        deleteRecursively(base.toFile());
    }

    /**
     * 穿越段（..、..\、空段、"." 段）一律按 INVALID_PATH 拒绝。
     */
    @Test
    public void traversalSegmentsAreRejected() {
        assertInvalid("..");
        assertInvalid("../x");
        assertInvalid("a/../../b");
        assertInvalid("..\\x");
        assertInvalid("a\\b");
        assertInvalid("a//b");
        assertInvalid("a/./b");
    }

    /**
     * 控制字符（NUL、Tab、换行、DEL）一律按 INVALID_PATH 拒绝。
     */
    @Test
    public void controlCharactersAreRejected() {
        assertInvalid("a\u0000b");
        assertInvalid("a\tb");
        assertInvalid("a\nb");
        assertInvalid("a\u007fb");
    }

    /**
     * 绝对路径形态（前导 '/'、Windows 盘符、UNC）不得逃出根目录。
     */
    @Test
    public void absoluteLikePathsCannotEscapeRoot() {
        // 前导 '/' 被剥离后按根内相对路径解析；
        // Windows 盘符形式（C:/...）会解析到真实系统路径，此时由 canonical 边界判定为越界并拒绝。
        assertRejected("etc/passwd");
        assertRejected("/etc/passwd");
        assertRejected("C:/Windows/win.ini");
        assertRejected("//server/share");
    }

    /**
     * 未解码的编码形态（%2e/%2f）只作为普通名字，得到 404 而不会越界。
     */
    @Test
    public void encodedTraversalIsTreatedAsPlainNameAndNotFound() {
        // 容器未解码时（%2e/%2f 保持字面量），它们是普通字符，只会得到 404，不会越界
        assertCode("%2e%2e/secret", QueryException.ErrorCode.NOT_FOUND);
        assertCode("..%2fsecret", QueryException.ErrorCode.NOT_FOUND);
        assertCode("%2e%2e%2f%2e%2e%2fsecret", QueryException.ErrorCode.NOT_FOUND);
    }

    /**
     * 不存在的路径返回 NOT_FOUND。
     */
    @Test
    public void missingPathIsNotFound() {
        try {
            resolver.resolveDirectory("no/such");
            fail("expected NOT_FOUND");
        } catch (QueryException e) {
            assertEquals(QueryException.ErrorCode.NOT_FOUND, e.getCode());
        }
    }

    /**
     * 指向根目录之外的目录符号链接被判定为 OUTSIDE_ROOT。
     */
    @Test
    public void symlinkEscapingRootIsRejected() throws Exception {
        Path outside = Files.createDirectory(base.resolve("outside"));
        Files.write(outside.resolve("secret.log"),
                "secret".getBytes(StandardCharsets.UTF_8));
        Path link = root.resolve("escape-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (Exception e) {
            // Windows 无权限 / 文件系统不支持符号链接时跳过该用例
            Assume.assumeTrue("symbolic link not supported, skipped", false);
            return;
        }
        try {
            resolver.resolveFile("escape-link/secret.log");
            fail("expected OUTSIDE_ROOT");
        } catch (QueryException e) {
            assertEquals(QueryException.ErrorCode.OUTSIDE_ROOT, e.getCode());
        }
    }

    /**
     * 指向根目录之外的文件符号链接被判定为 OUTSIDE_ROOT。
     */
    @Test
    public void symlinkEscapingRootAsFileIsRejected() throws Exception {
        Path outsideFile = Files.createFile(base.resolve("outside-file.log"));
        Files.write(outsideFile, "x".getBytes(StandardCharsets.UTF_8));
        Path link = root.resolve("escape-file.log");
        try {
            Files.createSymbolicLink(link, outsideFile);
        } catch (Exception e) {
            Assume.assumeTrue("symbolic link not supported, skipped", false);
            return;
        }
        try {
            resolver.resolveFile("escape-file.log");
            fail("expected OUTSIDE_ROOT");
        } catch (QueryException e) {
            assertEquals(QueryException.ErrorCode.OUTSIDE_ROOT, e.getCode());
        }
    }

    /**
     * 指向根目录内部的符号链接属于合法访问（边界校验不应误杀）。
     */
    @Test
    public void symlinkStayingInsideRootIsAllowed() throws Exception {
        Path realDir = Files.createDirectories(root.resolve("info").resolve("20260908"));
        Files.write(realDir.resolve("a.log"), "x".getBytes(StandardCharsets.UTF_8));
        Path link = root.resolve("inside-link");
        try {
            Files.createSymbolicLink(link, realDir);
        } catch (Exception e) {
            Assume.assumeTrue("symbolic link not supported, skipped", false);
            return;
        }
        Path file = resolver.resolveFile("inside-link/a.log");
        assertTrue(file.startsWith(root.toRealPath()));
    }

    /**
     * 正常的多级相对路径解析为根目录内的真实路径。
     */
    @Test
    public void legitNestedPathResolvesInsideRoot() throws Exception {
        Files.createDirectories(root.resolve("info").resolve("20260908"));
        Files.write(root.resolve("info").resolve("20260908").resolve("a.log"),
                "x".getBytes(StandardCharsets.UTF_8));

        Path file = resolver.resolveFile("info/20260908/a.log");
        assertTrue(file.startsWith(resolver.getRoot()));
        Path dir = resolver.resolveDirectory("info/20260908");
        assertEquals("20260908", dir.getFileName().toString());
    }

    // ========================== 平台相关与畸形输入（只保证不越界） ==========================

    /**
     * Windows 保留设备名 / 尾部点与空格 / NTFS 备用数据流 / 全角点等平台特例均不得越界。
     */
    @Test
    public void windowsReservedNamesTrailingDotsAndAdsNeverEscapeRoot() throws Exception {
        Files.write(root.resolve("demo.log"), "x".getBytes(StandardCharsets.UTF_8));
        // Windows 保留设备名
        assertNoEscape("CON");
        assertNoEscape("NUL");
        assertNoEscape("COM1");
        // 尾部点 / 空格：Windows 会归一化，可能命中根内同名目录，但绝不能越界
        Files.createDirectories(root.resolve("info"));
        assertNoEscape("info.");
        assertNoEscape("info ");
        // NTFS 备用数据流（ADS）：允许命中根内文件，或直接被拒，但绝不能解析到根外
        assertNoEscape("demo.log:secret");
        // 全角点（U+FF0E）不是 "."，只作为普通名字
        assertNoEscape("info\uFF0E\uFF0E");
    }

    /**
     * 超长路径段、双重编码、反斜杠编码与重复点等畸形输入均不得越界。
     */
    @Test
    public void overlongAndDoubleEncodedSegmentsNeverEscapeRoot() {
        assertNoEscape(repeat('x', 400) + "/" + repeat('y', 400));
        assertNoEscape("%252e%252e/secret");
        assertNoEscape("..%5c..%5csecret");
        assertNoEscape("....//....//secret");
        assertNoEscape(repeat('\u4e2d', 300));
    }

    /**
     * 根目录本身是符号链接时，仍以真实路径为边界约束其子路径。
     */
    @Test
    public void rootAsSymlinkStillBoundsChildren() throws Exception {
        Path realLogs = Files.createDirectory(base.resolve("real-logs"));
        Files.createDirectories(realLogs.resolve("info"));
        Files.write(realLogs.resolve("info").resolve("a.log"), "x".getBytes(StandardCharsets.UTF_8));
        Path linkRoot = base.resolve("logs-link");
        try {
            Files.createSymbolicLink(linkRoot, realLogs);
        } catch (Exception e) {
            Assume.assumeTrue("symbolic link not supported, skipped", false);
            return;
        }
        PathResolver linkResolver = new PathResolver(linkRoot);
        Path file = linkResolver.resolveFile("info/a.log");
        assertTrue(file.startsWith(realLogs.toRealPath()));
    }

    /**
     * 断言该相对路径在任何情况下都不会解析到根目录之外：
     * 允许解析成功（此时必须在根内），也允许以任意语义错误码被拒绝。
     *
     * @param relative 待校验的相对路径
     */
    private void assertNoEscape(String relative) {
        try {
            Path resolved = resolver.resolveDirectory(relative);
            assertTrue("resolved outside root: " + resolved, resolved.startsWith(resolver.getRoot()));
        } catch (QueryException e) {
            // 被拒绝即为安全
        }
    }

    /**
     * 生成重复字符组成的字符串（用于构造超长路径段）。
     *
     * @param c 重复字符
     * @param count 重复次数
     * @return 重复后的字符串
     */
    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 断言该相对路径以 INVALID_PATH 被拒绝。
     *
     * @param relative 待校验的相对路径
     */
    private void assertInvalid(String relative) {
        try {
            resolver.resolveDirectory(relative);
            fail("expected INVALID_PATH for: " + relative);
        } catch (QueryException e) {
            assertEquals("unexpected code for: " + relative,
                    QueryException.ErrorCode.INVALID_PATH, e.getCode());
        }
    }

    /**
     * 断言该相对路径以指定语义错误码被拒绝。
     *
     * @param relative 待校验的相对路径
     * @param expected 期望的错误码
     */
    private void assertCode(String relative, QueryException.ErrorCode expected) {
        try {
            resolver.resolveDirectory(relative);
            fail("expected " + expected + " for: " + relative);
        } catch (QueryException e) {
            assertEquals("unexpected code for: " + relative, expected, e.getCode());
        }
    }

    /**
     * 断言该相对路径被拒绝（404 / 越界 / 非法路径均可），关键是不能解析到根目录之外的内容。
     *
     * @param relative 待校验的相对路径
     */
    private void assertRejected(String relative) {
        try {
            Path resolved = resolver.resolveDirectory(relative);
            fail("expected rejection for: " + relative + " but resolved to: " + resolved);
        } catch (QueryException e) {
            QueryException.ErrorCode code = e.getCode();
            assertTrue("unexpected code " + code + " for: " + relative,
                    code == QueryException.ErrorCode.NOT_FOUND
                            || code == QueryException.ErrorCode.OUTSIDE_ROOT
                            || code == QueryException.ErrorCode.INVALID_PATH);
        }
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
