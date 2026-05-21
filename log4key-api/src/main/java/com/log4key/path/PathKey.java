/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import java.nio.file.Paths;

/**
 * 日志文件路径标识键。
 *
 * 不可变类，用于唯一标识一个日志文件的目录和文件名组合。
 * 由两个核心字段组成：{@code dir}（完整目录路径，已包含 rootDirectory）和 {@code file}（文件名）。
 * 提供基于 {@code dir} 和 {@code file} 的 hashCode/equals 实现，适合作为 HashMap 的 key 使用。
 */
public class PathKey {

    /** 完整目录路径，已包含 rootDirectory */
    private final String dir;

    /** 文件名 */
    private final String file;

    /**
     * 构造一个 PathKey 实例。
     *
     * @param dir  完整目录路径（已包含 rootDirectory）
     * @param file 文件名
     */
    public PathKey(String dir, String file) {
        this.dir = dir;
        this.file = file;
    }

    /**
     * 返回完整目录路径。
     *
     * @return 完整目录路径（已包含 rootDirectory）
     */
    public String getDir() {
        return dir;
    }

    /**
     * 返回文件名。
     *
     * @return 文件名
     */
    public String getFile() {
        return file;
    }

    /**
     * 返回内部路径表示（dir + File.separator + file），供日志输出使用。
     *
     * @return 完整的路径字符串
     */
    public String toPath() {
        return Paths.get(dir, file).toString();
    }

    @Override
    public int hashCode() {
        return dir.hashCode() * 31 + file.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathKey)) return false;
        PathKey other = (PathKey) o;
        return dir.equals(other.dir) && file.equals(other.file);
    }

    @Override
    public String toString() {
        return "PathKey{dir='" + dir + "', file='" + file + "'}";
    }
}