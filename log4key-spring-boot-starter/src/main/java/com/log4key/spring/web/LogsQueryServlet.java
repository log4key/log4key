/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.web;

import com.log4key.spring.query.DirectoryBrowse;
import com.log4key.spring.query.FilePage;
import com.log4key.spring.query.JsonWriter;
import com.log4key.spring.query.LogQueryService;
import com.log4key.spring.query.QueryException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Local log query servlet (no Spring MVC involved).
 *
 * 本地日志查询 Servlet（纯 Servlet，不依赖任何 Spring MVC 组件）。
 *
 * <p>注册于 {@code /log4key} 与 {@code /log4key/*}（由自动配置的 ServletRegistrationBean 完成）。
 * 职责仅限：HTTP 参数读取、调用 {@link LogQueryService}、JSON 输出与 HTTP 错误码；
 * 目录/文件/安全逻辑全部在 Service 层。
 *
 * <p>URL 语义：
 * <ul>
 *   <li>无 page 且目标是目录：无 file 参数仅返回子目录；带 file 参数时子目录与文件一起返回
 *       （file 为空=全部文件，非空=文件名模糊匹配）；</li>
 *   <li>目标是文件：文件分页内容（page 为空默认最后一页）；</li>
 *   <li>带 page 参数时仅接受文件目标；错误按语义错误码映射 HTTP 状态。</li>
 * </ul>
 */
public class LogsQueryServlet extends HttpServlet {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 1L;

    /**
     * 响应内容类型（JSON）
     */
    private static final String JSON_CONTENT_TYPE = "application/json";

    /**
     * 响应字符集
     */
    private static final String RESPONSE_CHARSET = "UTF-8";

    /**
     * 日志查询服务（目录/文件逻辑与安全校验均在其内部）
     */
    private final LogQueryService queryService;

    /**
     * Creates the log query servlet.
     *
     * 构造日志查询 Servlet。
     *
     * @param queryService query service / 日志查询服务
     */
    public LogsQueryServlet(LogQueryService queryService) {
        if (queryService == null) {
            throw new IllegalArgumentException("queryService must not be null");
        }
        this.queryService = queryService;
    }

    /**
     * 处理 GET 请求：分发到目录浏览或文件分页，并把语义错误码映射为 HTTP 状态。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @throws ServletException Servlet 处理异常
     * @throws IOException 写出响应失败
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            handle(request, response);
        } catch (QueryException e) {
            writeError(response, statusOf(e.getCode()), e.getMessage());
        } catch (Exception e) {
            if (getServletContext() != null) {
                log("Failed to handle /log4key request", e);
            }
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal server error");
        }
    }

    /**
     * 请求分发：目录浏览 vs 文件分页（遵循 §6 规则）。
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @throws IOException 写出响应失败
     */
    private void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String relative = normalizePathInfo(request.getPathInfo());
        String fileKeyword = request.getParameter("file");
        Integer page = parsePage(request.getParameter("page"));

        if (page != null) {
            // 显式指定 page：只接受文件目标
            writeFilePage(response, queryService.readPage(relative, page));
            return;
        }

        // 未指定 page：优先按“文件 → 最后一页”处理；目标是目录时转入目录浏览
        try {
            FilePage lastPage = queryService.readPage(relative, null);
            writeFilePage(response, lastPage);
        } catch (QueryException e) {
            if (e.getCode() != QueryException.ErrorCode.NOT_FILE) {
                throw e;
            }
            DirectoryBrowse browse = queryService.browse(relative, fileKeyword);
            writeDirectory(response, browse, fileKeyword != null);
        }
    }

    // ========================== JSON 输出 ==========================

    /**
     * 输出目录浏览 JSON：无 file 关键字时仅 data（子目录）；有关键字时追加 files 字段。
     *
     * @param response HTTP 响应
     * @param browse 目录浏览结果
     * @param includeFiles 是否输出文件列表
     * @throws IOException 写出响应失败
     */
    private void writeDirectory(HttpServletResponse response, DirectoryBrowse browse,
                                boolean includeFiles) throws IOException {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"type\":\"directory\",\"data\":[");
        appendStringList(sb, browse.getDirectories());
        if (includeFiles) {
            sb.append("],\"files\":[");
            appendStringList(sb, browse.getFiles());
        }
        sb.append("]}");
        writeOk(response, sb.toString());
    }

    /**
     * 输出文件分页 JSON：{@code {"type":"file","page":N,"data":[...]}}。
     *
     * @param response HTTP 响应
     * @param page 分页结果
     * @throws IOException 写出响应失败
     */
    private void writeFilePage(HttpServletResponse response, FilePage page) throws IOException {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\"type\":\"file\",\"page\":").append(page.getPage()).append(",\"data\":[");
        appendStringList(sb, page.getLines());
        sb.append("]}");
        writeOk(response, sb.toString());
    }

    /**
     * 输出错误 JSON（{@code {"type":"error","code":N,"message":"..."}}），并设置对应 HTTP 状态。
     *
     * @param response HTTP 响应
     * @param status HTTP 状态码
     * @param message 错误消息（相对路径，不含绝对路径）
     * @throws IOException 写出响应失败
     */
    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        StringBuilder sb = new StringBuilder(128);
        sb.append("{\"type\":\"error\",\"code\":").append(status)
                .append(",\"message\":").append(JsonWriter.quote(message)).append('}');
        writeJson(response, status, sb.toString());
    }

    /**
     * 输出成功响应（HTTP 200）。
     *
     * @param response HTTP 响应
     * @param json JSON 文本
     * @throws IOException 写出响应失败
     */
    private void writeOk(HttpServletResponse response, String json) throws IOException {
        writeJson(response, HttpServletResponse.SC_OK, json);
    }

    /**
     * 统一的 JSON 响应写出：设置状态码、内容类型与字符集。
     *
     * @param response HTTP 响应
     * @param status HTTP 状态码
     * @param json JSON 文本
     * @throws IOException 写出响应失败
     */
    private void writeJson(HttpServletResponse response, int status, String json)
            throws IOException {
        response.setStatus(status);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(RESPONSE_CHARSET);
        response.getWriter().write(json);
    }

    /**
     * 把字符串列表按 JSON 数组元素形式追加到缓冲（逐项转义并加引号，逗号分隔）。
     *
     * @param sb 输出缓冲
     * @param items 字符串列表
     */
    private static void appendStringList(StringBuilder sb, List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(JsonWriter.quote(items.get(i)));
        }
    }

    // ========================== 参数解析与工具 ==========================

    /**
     * 规范化 pathInfo：去首尾 '/'，根路径返回空串。
     *
     * @param pathInfo 原始 pathInfo（可能为 null）
     * @return 去首尾 '/' 后的相对路径（根路径为空串）
     */
    private static String normalizePathInfo(String pathInfo) {
        if (pathInfo == null) {
            return "";
        }
        String p = pathInfo;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /**
     * 解析 page 参数：null=默认最后一页；非正整数/非数字=INVALID_ARGUMENT。
     *
     * @param rawPage 原始 page 参数（可能为 null）
     * @return 合法页码；null 表示未指定
     * @throws QueryException page 参数非法
     */
    private static Integer parsePage(String rawPage) {
        if (rawPage == null) {
            return null;
        }
        String trimmed = rawPage.trim();
        if (trimmed.isEmpty()) {
            throw invalidPage(rawPage);
        }
        try {
            int page = Integer.parseInt(trimmed);
            if (page <= 0) {
                throw invalidPage(rawPage);
            }
            return page;
        } catch (NumberFormatException e) {
            throw invalidPage(rawPage);
        }
    }

    /**
     * 构造 page 参数非法异常。
     *
     * @param raw 原始 page 参数值
     * @return 语义为 INVALID_ARGUMENT 的查询异常
     */
    private static QueryException invalidPage(String raw) {
        return new QueryException(QueryException.ErrorCode.INVALID_ARGUMENT,
                "page must be a positive integer, but was: " + raw);
    }

    /**
     * 语义错误码 → HTTP 状态。
     *
     * @param code 语义错误码
     * @return 对应的 HTTP 状态码
     */
    private static int statusOf(QueryException.ErrorCode code) {
        switch (code) {
            case NOT_FOUND:
            case NOT_DIRECTORY:
            case NOT_FILE:
                return HttpServletResponse.SC_NOT_FOUND;
            case INVALID_PATH:
            case INVALID_ARGUMENT:
            case OUTSIDE_ROOT:
                return HttpServletResponse.SC_BAD_REQUEST;
            case PAGE_TOO_LARGE:
                return HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
            case IO_ERROR:
            default:
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
    }
}
