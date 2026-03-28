/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

/**
 * Formatting information.
 *
 * 格式化信息类。
 */
public class FormattingInfo {
    private final int minLength;
    private final int maxLength;
    private final boolean leftAlign;
    private final boolean truncateFromEnd; // true: 截断末尾保留开头 (Keep Left); false: 截断开头保留末尾 (Keep Right)

    /**
     * Creates a new FormattingInfo instance.
     *
     * 创建格式化信息实例。
     *
     * @param minLength the minimum length / 最小长度
     * @param maxLength the maximum length / 最大长度
     * @param leftAlign whether to left align / 是否左对齐
     */
    public FormattingInfo(int minLength, int maxLength, boolean leftAlign) {
        this(minLength, maxLength, leftAlign, false);
    }

    /**
     * Creates a new FormattingInfo instance with truncation direction control.
     *
     * 创建带截断方向控制的格式化信息实例。
     *
     * @param minLength the minimum length / 最小长度
     * @param maxLength the maximum length / 最大长度
     * @param leftAlign whether to left align / 是否左对齐
     * @param truncateFromEnd whether to truncate from end (keep left) / 是否截断末尾（保留开头）
     */
    public FormattingInfo(int minLength, int maxLength, boolean leftAlign, boolean truncateFromEnd) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.leftAlign = leftAlign;
        this.truncateFromEnd = truncateFromEnd;
    }

    /**
     * Formats the content and appends to buffer.
     *
     * 格式化内容并追加到缓冲区。
     *
     * @param buffer the target string builder / 目标缓冲区
     * @param content the original content / 原始内容
     */
    public void format(StringBuilder buffer, String content) {
        if (content == null) {
            content = "";
        }

        int len = content.length();
        String tempContent = content;

        // 处理最大长度（截断）
        if (maxLength != Integer.MAX_VALUE && len > maxLength) {
            if (truncateFromEnd) {
                // 截断末尾，保留开头 (例如 "INFO" -> "INF")
                tempContent = tempContent.substring(0, maxLength);
            } else {
                // 截断开头，保留末尾 (例如 "com.foo.Bar" -> "Bar")
                tempContent = tempContent.substring(len - maxLength);
            }
            len = maxLength;
        }

        // 处理最小长度（填充）
        if (len < minLength) {
            int padding = minLength - len;
            if (leftAlign) {
                // 左对齐：内容在左，空格在右
                buffer.append(tempContent);
                appendSpaces(buffer, padding);
            } else {
                // 右对齐：空格在左，内容在右
                appendSpaces(buffer, padding);
                buffer.append(tempContent);
            }
        } else {
            buffer.append(tempContent);
        }
    }

    public int getMinLength() {
        return minLength;
    }

    /**
     * Gets the maximum length.
     *
     * 获取最大长度。
     *
     * @return the maximum length / 最大长度
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Checks if left alignment is enabled.
     *
     * 检查是否左对齐。
     *
     * @return true if left align / 是否左对齐
     */
    public boolean isLeftAlign() {
        return leftAlign;
    }

    private void appendSpaces(StringBuilder buffer, int count) {
        for (int i = 0; i < count; i++) {
            buffer.append(' ');
        }
    }
}