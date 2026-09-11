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
 * Paged file content result.
 *
 * 文件分页读取结果：页码 + 行内容。每页固定 {@link LogQueryService#PAGE_SIZE} 行（最后一页可能少于该值）。
 */
public final class FilePage {

    /**
     * 页码（从 1 开始）
     */
    private final int page;

    /**
     * 该页行内容
     */
    private final List<String> lines;

    /**
     * Creates a paged file content result.
     *
     * 构造文件分页结果（内部做防御性拷贝并转为不可变列表）。
     *
     * @param page page number (1-based) / 页码（从 1 开始）
     * @param lines lines of this page / 该页行内容
     */
    public FilePage(int page, List<String> lines) {
        this.page = page;
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    /**
     * 获取页码（从 1 开始；默认读最后一页时回显实际最后一页页码）。
     *
     * @return 页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 获取该页行内容。
     *
     * @return 行列表（可能为空，如空文件或越界页）
     */
    public List<String> getLines() {
        return lines;
    }
}
