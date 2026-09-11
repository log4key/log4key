/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Directory browse result.
 *
 * 目录浏览结果：一级子目录名 + 文件列表。
 *
 * <p>目录名与文件名为纯名字数组（不含路径）。文件列表仅在请求携带 file 关键字时由
 * {@link LogQueryService#browse(String, String)} 填充；未请求时为空列表。
 * 排序：目录名升序；文件按最后修改时间降序（在 Service 内完成）。
 */
public final class DirectoryBrowse {

    /**
     * 一级子目录名列表（升序）
     */
    private final List<String> directories;

    /**
     * 文件名列表（按最后修改时间降序；仅在请求 file 关键字时非空）
     */
    private final List<String> files;

    /**
     * Creates a directory browse result.
     *
     * 构造目录浏览结果（内部做防御性拷贝并转为不可变列表）。
     *
     * @param directories sub-directory names / 子目录名列表
     * @param files file names / 文件名列表
     */
    public DirectoryBrowse(List<String> directories, List<String> files) {
        this.directories = Collections.unmodifiableList(new ArrayList<>(directories));
        this.files = Collections.unmodifiableList(new ArrayList<>(files));
    }

    /**
     * 获取一级子目录名列表（升序）。
     *
     * @return 子目录名列表
     */
    public List<String> getDirectories() {
        return directories;
    }

    /**
     * 获取文件列表（按最后修改时间降序；仅在请求 file 关键字时非空）。
     *
     * @return 文件名列表
     */
    public List<String> getFiles() {
        return files;
    }
}
