/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Throwable token.
 *
 * 异常堆栈 Token。
 */
public class ThrowableToken implements Token {
    private final String option;

    public ThrowableToken() {
        this("full");
    }

    public ThrowableToken(String option) {
        this.option = option;
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        Throwable t = event.getThrowable();
        if (t == null) {
            return;
        }

        if ("none".equalsIgnoreCase(option)) {
            return;
        }

        if ("short".equalsIgnoreCase(option)) {
            out.append(t.toString());
            return;
        }

        // 获取完整堆栈字符串
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        String stackTrace = sw.toString();

        if ("full".equalsIgnoreCase(option)) {
            out.append(stackTrace);
        } else {
            // 尝试解析为行数限制
            try {
                int depth = Integer.parseInt(option);
                if (depth <= 0) {
                    return;
                }
                
                // 优化：直接查找截断位置，避免 split 产生大量临时对象
                String lineSeparator = System.lineSeparator();
                int separatorLength = lineSeparator.length();
                int endIndex = 0;
                int foundLines = 0;
                
                while (foundLines < depth) {
                    int nextSeparatorIndex = stackTrace.indexOf(lineSeparator, endIndex);
                    if (nextSeparatorIndex == -1) {
                        // 没找到换行符，说明已经到末尾
                        endIndex = stackTrace.length();
                        break;
                    }
                    // 指向换行符之后
                    endIndex = nextSeparatorIndex + separatorLength;
                    foundLines++;
                }
                
                // 截取需要的字符串
                if (endIndex > 0 && endIndex <= stackTrace.length()) {
                    // 如果截取位置是中间，endIndex 包含了最后一行的换行符
                    // 如果是最后一行且没有换行符，endIndex 就是 length
                    // 为了保持与 split 行为一致（不重复追加换行符），如果最后是换行符，且不是原始字符串结尾，可以考虑是否去除
                    // 这里简单处理：直接追加截取的子串
                    out.append(stackTrace, 0, endIndex);
                } else {
                    out.append(stackTrace);
                }
            } catch (NumberFormatException e) {
                // 无法识别的选项，默认输出完整堆栈
                out.append(stackTrace);
            }
        }
    }
}
