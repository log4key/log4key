/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Canonical paths utility.
 *
 * 规范路径工具类。
 */
public class CanonicalPaths {

    /**
     * 获取系统默认的日志目录
     * @return 系统默认的日志目录路径
     */
    public static String getDefaultLogDirectory() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "log4key" + File.separator + "logs";
    }

    /**
     * 判断路径是否为绝对路径
     * @param path 路径
     * @return 如果是绝对路径返回true，否则返回false
     */
    public static boolean isAbsolutePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        // 处理Windows路径
        if (path.length() >= 2 && path.charAt(1) == ':') {
            return true;
        }
        // 处理Unix/Linux路径
        return path.startsWith("/");
    }

    /**
     * 规范化路径
     * @param path 原始路径
     * @return 规范化后的路径
     */
    public static String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // 替换反斜杠为正斜杠
        String normalized = path.replace('\\', '/');

        // 移除多余的斜杠
        normalized = normalized.replaceAll("/+", "/");

        // 移除末尾的斜杠（除非是根路径）
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    /**
     * 解析相对路径为绝对路径
     * @param basePath 基础路径
     * @param relativePath 相对路径
     * @return 绝对路径
     */
    public static String resolve(String basePath, String relativePath) {
        if (basePath == null || basePath.isEmpty()) {
            return relativePath;
        }
        if (relativePath == null || relativePath.isEmpty()) {
            return basePath;
        }

        // 如果相对路径已经是绝对路径，直接返回
        if (isAbsolutePath(relativePath)) {
            return normalize(relativePath);
        }

        // 合并路径
        String separator = basePath.endsWith("/") ? "" : "/";
        return normalize(basePath + separator + relativePath);
    }

    /**
     * 获取路径的父目录
     * @param path 路径
     * @return 父目录路径，如果不存在父目录则返回null
     */
    public static String getParent(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String normalized = normalize(path);
        int lastSlashIndex = normalized.lastIndexOf('/');

        if (lastSlashIndex <= 0) {
            return null;
        }

        return normalized.substring(0, lastSlashIndex);
    }

    /**
     * 获取路径的文件名
     * @param path 路径
     * @return 文件名，如果路径为空或没有文件名则返回null
     */
    public static String getFileName(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String normalized = normalize(path);
        int lastSlashIndex = normalized.lastIndexOf('/');

        if (lastSlashIndex < 0) {
            return normalized;
        }

        if (lastSlashIndex >= normalized.length() - 1) {
            return null;
        }

        return normalized.substring(lastSlashIndex + 1);
    }

    /**
     * 判断路径是否存在
     * @param path 路径
     * @return 如果存在返回true，否则返回false
     */
    public static boolean exists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return new File(path).exists();
    }

    /**
     * 判断路径是否为目录
     * @param path 路径
     * @return 如果是目录返回true，否则返回false
     */
    public static boolean isDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }

    /**
     * 判断路径是否为文件
     * @param path 路径
     * @return 如果是文件返回true，否则返回false
     */
    public static boolean isFile(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    /**
     * 创建目录（如果不存在）
     * @param path 目录路径
     * @return 如果创建成功或目录已存在返回true，否则返回false
     */
    public static boolean createDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        if (file.exists()) {
            return file.isDirectory();
        }
        return file.mkdirs();
    }

    /**
     * 获取Path对象
     * @param path 路径字符串
     * @return Path对象
     */
    public static Path toPath(String path) {
        if (path == null) {
            return null;
        }
        return Paths.get(path);
    }

    /**
     * 将Path对象转换为字符串路径
     * @param path Path对象
     * @return 字符串路径
     */
    public static String fromPath(Path path) {
        if (path == null) {
            return null;
        }
        return path.toString();
    }
}
