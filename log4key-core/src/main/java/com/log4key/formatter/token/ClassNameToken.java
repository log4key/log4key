/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Class name token.
 *
 * 类名Token。
 */
public class ClassNameToken implements Token {

    private final int targetLength;

    public ClassNameToken() {
        this.targetLength = -1;
    }

    public ClassNameToken(String lengthStr) {
        int parsedLength = -1;
        if (lengthStr != null && !lengthStr.isEmpty()) {
            try {
                parsedLength = Integer.parseInt(lengthStr);
            } catch (NumberFormatException e) {
                // ignore, use default -1
            }
        }
        this.targetLength = parsedLength;
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        String loggerName = event.getLoggerName() != null ? event.getLoggerName() : "unknown";
        if (targetLength > 0) {
            out.append(abbreviate(loggerName, targetLength));
        } else {
            out.append(loggerName);
        }
    }

    // -------------------- private --------------------

    /**
     * 缩写类名以适应目标长度
     * 策略：保留最后一个部分（类名），从左向右将包名缩写为首字母，直到总长度满足要求
     * 优化：避免使用 String.split 正则分割，使用索引查找以提升性能
     */
    private String abbreviate(String className, int targetLength) {
        if (className == null || className.length() <= targetLength) {
            return className;
        }

        // 预计算所有点的位置
        int[] dotIndexes = new int[16]; // 初始容量，一般够用
        int dotCount = 0;
        int pos = 0;
        while ((pos = className.indexOf('.', pos)) != -1) {
            if (dotCount >= dotIndexes.length) {
                // 扩容
                int[] newIndexes = new int[dotIndexes.length * 2];
                System.arraycopy(dotIndexes, 0, newIndexes, 0, dotIndexes.length);
                dotIndexes = newIndexes;
            }
            dotIndexes[dotCount++] = pos;
            pos++;
        }

        // 如果没有包名，直接返回
        if (dotCount == 0) {
            return className;
        }

        // 动态计算当前长度
        int currentLength = className.length();

        StringBuilder buf = new StringBuilder(currentLength);

        int lastDotEnd = -1;

        // 遍历包名部分 (i < dotCount 意味着只处理到最后一个点之前，即所有包名)
        for (int i = 0; i < dotCount; i++) {
            int dotPos = dotIndexes[i];
            int segmentStart = lastDotEnd + 1;
            int segmentLen = dotPos - segmentStart;

            // 检查是否需要缩写当前段
            // 逻辑：如果当前总长度 > 目标长度，且当前段长度 > 1 (可以缩写)，则进行缩写
            // 注意：一旦开始缩写，是从左向右贪婪进行的，直到长度满足要求
            boolean shouldAbbreviate = (currentLength > targetLength) && (segmentLen > 1);

            if (shouldAbbreviate) {
                // 缩写：只取第一个字符
                buf.append(className.charAt(segmentStart));
                // 长度变化：原长度 - 缩写后长度(1)
                int reduction = segmentLen - 1;
                currentLength -= reduction;
            } else {
                // 不缩写：追加完整片段
                buf.append(className, segmentStart, dotPos);
            }

            buf.append('.');
            lastDotEnd = dotPos;
        }

        // 追加类名部分（最后一个点之后的所有内容）
        // 类名永远不缩写
        buf.append(className.substring(lastDotEnd + 1));

        return buf.toString();
    }
}
