/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves and validates query paths against the log root directory.
 *
 * 日志查询路径解析与安全校验（唯一可信校验点）。
 *
 * <p>安全模型：所有查询路径必须位于日志根目录（rootDirectory）以内。两层防护：
 * <ol>
 *   <li>段级预校验：相对路径按 '/' 分段，逐段拒绝空段、"."/".."、反斜杠、控制字符等，
 *       使 "../"、"..\" 等在解析阶段即被拒绝；</li>
 *   <li>canonical 边界校验：对真实存在的目标求 real path（跟随符号链接后的规范路径），
 *       断言其位于根目录 real path 之内，防止符号链接 / 大小写 / 归一化差异导致的越界。</li>
 * </ol>
 *
 * <p>目标不存在一律按 NOT_FOUND 处理，不做任何“未来路径”推理；本类只读，不创建任何文件/目录。
 */
public final class PathResolver {

    /**
     * 根目录（real path；若创建时不存在则保留绝对规范化路径）
     */
    private final Path root;

    /**
     * 根目录的 real path（仅当创建时根目录已存在时有效，用于边界比较）
     */
    private final Path realRoot;

    /**
     * 构造路径解析器。
     *
     * @param rootDirectory 日志根目录
     */
    public PathResolver(Path rootDirectory) {
        Path absolute = rootDirectory.toAbsolutePath().normalize();
        Path real = null;
        if (Files.exists(absolute)) {
            try {
                real = absolute.toRealPath();
            } catch (IOException e) {
                throw new QueryException(QueryException.ErrorCode.IO_ERROR,
                        "Failed to resolve log root directory: " + absolute, e);
            }
        }
        this.root = real != null ? real : absolute;
        this.realRoot = real;
    }

    /**
     * 获取规范化后的日志根目录。
     *
     * @return 根目录
     */
    public Path getRoot() {
        return root;
    }

    /**
     * 解析并校验一个目录目标（必须存在且为目录）。
     *
     * @param relative 相对路径（空串或 "/" 表示根目录本身）
     * @return 校验后的目标目录
     * @throws QueryException 路径非法 / 不存在 / 越界 / IO 错误
     */
    public Path resolveDirectory(String relative) {
        Path target = resolve(relative);
        if (!Files.isDirectory(target)) {
            throw new QueryException(QueryException.ErrorCode.NOT_DIRECTORY,
                    "Target is not a directory: " + display(relative));
        }
        return target;
    }

    /**
     * 解析并校验一个文件目标（必须存在且为普通文件）。
     *
     * @param relative 相对路径
     * @return 校验后的目标文件
     * @throws QueryException 路径非法 / 不存在 / 越界 / IO 错误
     */
    public Path resolveFile(String relative) {
        Path target = resolve(relative);
        if (!Files.isRegularFile(target)) {
            throw new QueryException(QueryException.ErrorCode.NOT_FILE,
                    "Target is not a regular file: " + display(relative));
        }
        return target;
    }

    /**
     * 核心解析：段级预校验 + 拼接 + canonical 边界校验。
     *
     * @param relative 相对路径（空串 / "/" 表示根目录）
     * @return 校验后的真实路径
     * @throws QueryException 路径非法 / 不存在 / 越界 / IO 错误
     */
    private Path resolve(String relative) {
        List<String> segments = splitSegments(relative);

        try {
            Path candidate = root;
            for (String segment : segments) {
                candidate = candidate.resolve(segment);
            }

            // 目标必须真实存在
            if (!Files.exists(candidate)) {
                throw new QueryException(QueryException.ErrorCode.NOT_FOUND,
                        "Path does not exist: " + display(relative));
            }

            // canonical 边界校验：跟随符号链接后的规范路径必须位于根目录内
            Path realTarget = candidate.toRealPath();
            if (realRoot != null && !realTarget.startsWith(realRoot)) {
                throw new QueryException(QueryException.ErrorCode.OUTSIDE_ROOT,
                        "Path escapes log root directory: " + display(relative));
            }
            return realTarget;
        } catch (InvalidPathException e) {
            // 平台非法路径（如 Windows 上的超长路径 / 非法字符）统一归一为 INVALID_PATH，
            // 避免以未受控的 RuntimeException 形式外泄为 500
            throw new QueryException(QueryException.ErrorCode.INVALID_PATH,
                    "Invalid path: " + display(relative), e);
        } catch (IOException e) {
            throw new QueryException(QueryException.ErrorCode.IO_ERROR,
                    "Failed to resolve real path: " + display(relative), e);
        }
    }

    /**
     * 相对路径分段与段级预校验。
     *
     * @param relative 相对路径
     * @return 合法路径段列表（根路径返回空列表）
     * @throws QueryException 存在非法路径段
     */
    private static List<String> splitSegments(String relative) {
        List<String> segments = new ArrayList<>();
        if (relative == null || relative.isEmpty() || "/".equals(relative)) {
            return segments;
        }
        String trimmed = relative;
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return segments;
        }
        String[] parts = trimmed.split("/", -1);
        for (String part : parts) {
            if (!isValidSegment(part)) {
                throw new QueryException(QueryException.ErrorCode.INVALID_PATH,
                        "Invalid path segment: " + part);
            }
            segments.add(part);
        }
        return segments;
    }

    /**
     * 单个路径段合法性：非空、非 . / ..、不含分隔符与控制字符。
     *
     * @param segment 单个路径段
     * @return true 表示合法
     */
    private static boolean isValidSegment(String segment) {
        if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c == '/' || c == '\\' || c < 0x20 || c == 0x7f) {
                return false;
            }
        }
        return true;
    }

    /**
     * 展示用相对路径（避免把绝对路径暴露给外部）。
     *
     * @param relative 原始相对路径
     * @return 展示用路径（空路径显示为 "/"）
     */
    private static String display(String relative) {
        return (relative == null || relative.isEmpty()) ? "/" : relative;
    }
}
