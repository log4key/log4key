/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.web;

import com.log4key.spring.query.JsonWriter;
import com.log4key.spring.query.LogQueryService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * LogsQueryServlet 单元测试：目录浏览 / 文件列表 / 分页 / 错误码（fake request/response，零 MVC）。
 */
public class LogsQueryServletTest {

    /**
     * 测试用日志根目录（临时目录）
     */
    private Path root;

    /**
     * 被测 Servlet（内部使用真实 LogQueryService）
     */
    private LogsQueryServlet servlet;

    /**
     * 每个用例前创建临时根目录并构造 Servlet。
     *
     * @throws Exception 创建临时目录失败
     */
    @Before
    public void setUp() throws Exception {
        root = Files.createTempDirectory("log4key-servlet-test-");
        servlet = new LogsQueryServlet(new LogQueryService(root, StandardCharsets.UTF_8));
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
     * 根路径请求（pathInfo 为 null 或 "/"）返回一级子目录列表。
     */
    @Test
    public void rootListingWithoutParams() throws Exception {
        Files.createDirectory(root.resolve("info"));
        Files.createDirectory(root.resolve("error"));

        // /log4key（pathInfo 为 null，映射 /log4key 精确路径）
        CapturedResponse exact = get(null, null, null);
        assertEquals(200, exact.status.get());
        assertEquals("{\"type\":\"directory\",\"data\":[\"error\",\"info\"]}", exact.body());

        // /log4key/ （pathInfo = "/"）
        CapturedResponse slash = get("/", null, null);
        assertEquals("{\"type\":\"directory\",\"data\":[\"error\",\"info\"]}", slash.body());
    }

    /**
     * 多级目录请求返回该目录下的一级子目录（升序）。
     */
    @Test
    public void nestedDirectoryListing() throws Exception {
        mkdirs("info/20260908");
        mkdirs("info/20260907");
        CapturedResponse resp = get("/info", null, null);
        assertEquals(200, resp.status.get());
        assertEquals("{\"type\":\"directory\",\"data\":[\"20260907\",\"20260908\"]}", resp.body());
    }

    // ========================== 文件列表（file 参数） ==========================

    /**
     * file 关键字为空串时，返回子目录与全部文件（文件按修改时间降序）。
     */
    @Test
    public void emptyFileKeywordListsAllFilesWithDirs() throws Exception {
        mkdirs("day");
        Path older = Files.createFile(root.resolve("day/a.log"));
        Path newer = Files.createFile(root.resolve("day/b.log"));
        older.toFile().setLastModified(1_000L);
        newer.toFile().setLastModified(2_000L);

        CapturedResponse resp = get("/day", "", null);
        assertEquals(200, resp.status.get());
        assertEquals("{\"type\":\"directory\",\"data\":[],\"files\":[\"b.log\",\"a.log\"]}",
                resp.body());
    }

    /**
     * file 关键字按文件名模糊过滤（含滚动文件命中）。
     */
    @Test
    public void fuzzyFileKeywordFiltersFiles() throws Exception {
        mkdirs("player/20260908");
        Path base = write(root.resolve("player/20260908/player-10001.log"), "base\n");
        Path rolled = write(root.resolve("player/20260908/player-10001.20260908.103000.log"), "r\n");
        write(root.resolve("player/20260908/order-2.log"), "o\n");
        base.toFile().setLastModified(1_000L);
        rolled.toFile().setLastModified(2_000L);

        CapturedResponse resp = get("/player/20260908", "10001", null);
        assertEquals("{\"type\":\"directory\",\"data\":[],\"files\":["
                + "\"player-10001.20260908.103000.log\",\"player-10001.log\"]}", resp.body());
    }

    // ========================== 文件分页 ==========================

    /**
     * 显式 page 参数返回固定 1000 行的对应页。
     */
    @Test
    public void explicitPageReturnsFixedPage() throws Exception {
        Path big = root.resolve("big.log");
        Files.write(big, manyLines(2500).getBytes(StandardCharsets.UTF_8));

        CapturedResponse resp = get("/big.log", null, "2");
        assertEquals(200, resp.status.get());
        assertTrue(resp.body().startsWith("{\"type\":\"file\",\"page\":2,\"data\":[\"line-1000\","));
        assertTrue(resp.body().endsWith("\"line-1999\"]}"));
    }

    /**
     * 不带 page 参数时默认返回最后一页。
     */
    @Test
    public void defaultPageReadsLastPage() throws Exception {
        Path big = root.resolve("big.log");
        Files.write(big, manyLines(2500).getBytes(StandardCharsets.UTF_8));

        CapturedResponse resp = get("/big.log", null, null);
        assertEquals(200, resp.status.get());
        assertTrue(resp.body().contains("\"page\":3"));
        assertTrue(resp.body().endsWith("\"line-2499\"]}"));
    }

    /**
     * 行内容中的引号与反斜杠在 JSON 中被正确转义。
     */
    @Test
    public void lineContentIsJsonEscaped() throws Exception {
        Path f = root.resolve("esc.log");
        Files.write(f, "he said \"hi\" \\ ok\n".getBytes(StandardCharsets.UTF_8));

        CapturedResponse resp = get("/esc.log", null, null);
        assertTrue(resp.body().contains(JsonWriter.quote("he said \"hi\" \\ ok")));
    }

    // ========================== 错误路径与状态码 ==========================

    /**
     * 目标不存在时返回 404 与错误 JSON。
     */
    @Test
    public void missingPathReturns404() {
        CapturedResponse resp = get("/nope.log", null, null);
        assertEquals(404, resp.status.get());
        assertTrue(resp.body().contains("\"type\":\"error\""));
        assertTrue(resp.body().contains("\"code\":404"));
    }

    /**
     * 对目录指定 page 参数（类型不匹配）时返回 404。
     */
    @Test
    public void pageParamOnDirectoryReturns404() throws Exception {
        mkdirs("day");
        CapturedResponse resp = get("/day", null, "1");
        assertEquals(404, resp.status.get());
    }

    /**
     * 含 ".." 的路径按非法路径处理，返回 400。
     */
    @Test
    public void traversalReturns400() {
        CapturedResponse resp = get("/../x", null, null);
        assertEquals(400, resp.status.get());
        assertTrue(resp.body().contains("\"code\":400"));
    }

    /**
     * 未解码的编码穿越（%2e/%2f 字面量）只作为普通名字，返回 404 且不泄露根外内容。
     */
    @Test
    public void encodedTraversalDoesNotLeak() {
        // 容器未解码时（%2e/%2f 为字面量）只会得到 404，不会命中根目录之外的内容
        assertEquals(404, get("/%2e%2e/%2e%2e/secret", null, null).status.get());
        assertEquals(404, get("/..%2f..%2fsecret", null, null).status.get());
    }

    /**
     * 绝对路径形态（前导 '/'）被剥离后按根内相对路径处理，返回 404。
     */
    @Test
    public void absoluteLikePathDoesNotEscapeRoot() {
        assertEquals(404, get("/etc/passwd", null, null).status.get());
    }

    /**
     * page 参数非正整数（非数字 / 0 / 负数 / 空串）返回 400。
     */
    @Test
    public void invalidPageReturns400() {
        assertEquals(400, get("/x.log", null, "abc").status.get());
        assertEquals(400, get("/x.log", null, "0").status.get());
        assertEquals(400, get("/x.log", null, "-1").status.get());
        assertEquals(400, get("/x.log", null, "").status.get());
    }

    /**
     * page 参数超出 int 范围（含溢出）返回 400，而不是抛出异常。
     */
    @Test
    public void outOfIntRangePageReturns400() {
        assertEquals(400, get("/x.log", null, "2147483648").status.get());
        assertEquals(400, get("/x.log", null, "99999999999999").status.get());
    }

    /**
     * file 关键字带穿越片段时只做文件名匹配：返回 200、空文件列表，且不回显关键字。
     */
    @Test
    public void fileKeywordWithTraversalIsHarmless() throws Exception {
        mkdirs("info");
        write(root.resolve("info/user-1.log"), "x\n");

        CapturedResponse resp = get("/info", "../../etc/passwd", null);
        assertEquals(200, resp.status.get());
        assertTrue(resp.body().contains("\"files\":[]"));
        assertTrue("keyword must not be echoed or used as path: " + resp.body(),
                !resp.body().contains("etc") && !resp.body().contains("passwd"));
    }

    /**
     * 错误响应不得泄露绝对路径（根目录绝对路径或 Windows 盘符）。
     */
    @Test
    public void errorBodyDoesNotLeakAbsolutePath() {
        CapturedResponse resp = get("/nope.log", null, null);
        assertEquals(404, resp.status.get());
        String body = resp.body();
        assertTrue("must not leak absolute root: " + body, !body.contains(root.toString()));
        assertTrue("must not leak drive letter: " + body, !body.contains(":\\"));
    }

    /**
     * 成功响应固定为 application/json + UTF-8。
     */
    @Test
    public void contentTypeIsJsonUtf8() throws Exception {
        mkdirs("info");
        CapturedResponse resp = get("/info", null, null);
        assertEquals("application/json", resp.contentType);
        assertEquals("UTF-8", resp.characterEncoding);
    }

    // ========================== 辅助 ==========================

    /**
     * 以伪请求调用 Servlet 并捕获响应。
     *
     * @param pathInfo 伪 pathInfo（null 表示精确映射路径）
     * @param file file 参数（null 表示不携带）
     * @param page page 参数（null 表示不携带）
     * @return 捕获到的响应
     */
    private CapturedResponse get(String pathInfo, String file, String page) {
        HttpServletRequest request = fakeRequest(pathInfo, file, page);
        CapturedResponse captured = new CapturedResponse();
        HttpServletResponse response = fakeResponse(captured);
        try {
            servlet.service(request, response);
            response.flushBuffer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return captured;
    }

    /**
     * 构造伪 HttpServletRequest（动态代理，只实现被测代码用到的取值方法）。
     *
     * @param pathInfo pathInfo 返回值
     * @param file file 参数值
     * @param page page 参数值
     * @return 伪请求对象
     */
    private HttpServletRequest fakeRequest(final String pathInfo, final String file,
                                           final String page) {
        final Map<String, String> params = new HashMap<>();
        if (file != null) {
            params.put("file", file);
        }
        if (page != null) {
            params.put("page", page);
        }
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new InvocationHandler() {
                    /**
                     * 返回 pathInfo / 参数 / 请求方法，其余按返回类型给默认值。
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getPathInfo".equals(name)) {
                            return pathInfo;
                        }
                        if ("getParameter".equals(name)) {
                            return params.get((String) args[0]);
                        }
                        if ("getMethod".equals(name)) {
                            return "GET";
                        }
                        return defaultValue(method);
                    }
                });
    }

    /**
     * 构造伪 HttpServletResponse（动态代理）：记录状态码、内容类型与字符集，写出的内容进入缓冲。
     *
     * @param captured 捕获容器
     * @return 伪响应对象
     */
    private HttpServletResponse fakeResponse(final CapturedResponse captured) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                new InvocationHandler() {

                    /**
                     * 响应 writer（首次 getWriter 时创建，UTF-8）
                     */
                    private PrintWriter writer;

                    /**
                     * 记录状态码 / 内容类型 / 字符集；flushBuffer 时刷新 writer。
                     */
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getWriter".equals(name)) {
                            if (writer == null) {
                                writer = new PrintWriter(new OutputStreamWriter(
                                        captured.out, StandardCharsets.UTF_8), false);
                            }
                            return writer;
                        }
                        if ("setStatus".equals(name)) {
                            if (args != null && args.length > 0) {
                                captured.status.set((Integer) args[0]);
                            }
                            return null;
                        }
                        if ("sendError".equals(name)) {
                            if (args != null && args.length > 0) {
                                captured.status.set((Integer) args[0]);
                            }
                            return null;
                        }
                        if ("setContentType".equals(name)) {
                            captured.contentType = (String) args[0];
                            return null;
                        }
                        if ("setCharacterEncoding".equals(name)) {
                            captured.characterEncoding = (String) args[0];
                            return null;
                        }
                        if ("flushBuffer".equals(name)) {
                            if (writer != null) {
                                writer.flush();
                            }
                            return null;
                        }
                        if ("getStatus".equals(name)) {
                            return captured.status.get();
                        }
                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(name)) {
                                return "fakeResponse";
                            }
                            return null;
                        }
                        return defaultValue(method);
                    }
                });
    }

    /**
     * 按方法返回类型给出默认值（未实现的 Servlet 方法返回该值即可，被测代码不会用到）。
     *
     * @param method 被调用方法
     * @return 该返回类型的默认值
     */
    private static Object defaultValue(Method method) {
        Class<?> type = method.getReturnType();
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    /**
     * 捕获到的伪响应内容。
     */
    private static final class CapturedResponse {

        /**
         * 响应体缓冲
         */
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        /**
         * 响应状态码（默认 200）
         */
        private final AtomicInteger status = new AtomicInteger(200);

        /**
         * 记录的内容类型
         */
        private String contentType;

        /**
         * 记录的字符集
         */
        private String characterEncoding;

        /**
         * 以 UTF-8 解码响应体。
         *
         * @return 响应体文本
         */
        private String body() {
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 在根目录下按相对路径逐级创建目录。
     *
     * @param relative 相对路径（以 '/' 分隔）
     * @return 创建后的目录
     * @throws Exception 创建失败
     */
    private Path mkdirs(String relative) throws Exception {
        Path dir = root;
        for (String segment : relative.split("/")) {
            dir = dir.resolve(segment);
        }
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * 写入文本文件（UTF-8）。
     *
     * @param file 目标文件
     * @param content 文件内容
     * @return 写入后的文件
     * @throws Exception 写入失败
     */
    private static Path write(Path file, String content) throws Exception {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * 生成指定行数的文本（每行以 '\n' 结尾）。
     *
     * @param count 行数
     * @return 文本内容
     */
    private static String manyLines(int count) {
        StringBuilder sb = new StringBuilder(count * 12);
        for (int i = 0; i < count; i++) {
            sb.append(String.format("line-%04d\n", i));
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
