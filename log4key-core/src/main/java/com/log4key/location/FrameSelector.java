/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

/**
 * Frame selector for call stack.
 *
 * 帧选择器。
 */
public final class FrameSelector {

    /**
     * 选择有意义的调用者位置
     * 
     * @param stack  栈轨迹元素数组
     * @param filter 帧筛选器
     * @return 有意义的调用者位置
     */
    CallerLocation select(StackTraceElement[] stack, FrameFilter filter) {
        // 从栈顶开始遍历，第一个非框架帧(保底作用)
        StackTraceElement fallback = null;

        for (int i = 0; i < stack.length && i < 32; i++) {
            StackTraceElement e = stack[i];

            // 跳过框架类和合成类
            if (filter.shouldSkip(e)){
                continue;
            }

            // 第一个非框架帧
            if (fallback == null) {
                fallback = e; 
            }

            // 如果当前帧有意义，返回它
            if (filter.isMeaningful(e)) {
                return toLocation(e);
            }
        }

        // 如果没有有意义的帧，返回第一个非框架帧（如果有）
        return fallback != null
                ? toLocation(fallback)
                : CallerLocation.UNKNOWN;
    }

    /**
     * 将栈轨迹元素转换为调用者位置
     * 
     * @param e 栈轨迹元素
     * @return 调用者位置
     */
    private CallerLocation toLocation(StackTraceElement e) {
        return new CallerLocation(
                e.getClassName(),
                e.getMethodName(),
                e.getFileName(),
                e.getLineNumber());
    }

}
