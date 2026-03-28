/**
 * 默认行号获取提供者实现类，使用优化后的栈帧获取逻辑
 */
/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

import java.util.Arrays;

/**
 * Default location provider implementation.
 *
 * 默认位置信息提供者实现类。
 */
public class DefaultLocationProvider implements LocationProvider {

    /**
     * 默认要跳过的类名前缀列表
     */
    private static final String[] DEFAULT_SKIP = {
            "sun.",
            "jdk.",
            "org.slf4j.",
            "ch.qos.logback.",
            "org.apache.logging.",
            "java.util.logging.",
            "com.log4key.api.",
            "com.log4key.slf4j.",
            "com.log4key.location.",
            "com.log4key.util.",
            "com.log4key.config.",
            "com.log4key.formatter.",
            "com.log4key.appender.",
            "com.log4key.router.",
            "com.log4key.io."
    };

    public static final DefaultLocationProvider INSTANCE = new DefaultLocationProvider();

    /**
     * Throwable 栈轨迹捕获器
     */
    private final ThrowableStackCapture capture = new ThrowableStackCapture();

    /**
     * 帧筛选器
     */
    private final FrameFilter filter = new FrameFilter(Arrays.asList(DEFAULT_SKIP), DefaultLocationProvider.class);

    /**
     * 帧选择器
     */
    private final FrameSelector selector = new FrameSelector();

    /**
     * Gets the caller location.
     *
     * 获取调用者位置信息。
     *
     * @return the caller location / 调用者位置信息
     */
    @Override
    public CallerLocation getCallerLocation() {
        StackTraceElement[] stack = capture.capture();
        if (stack == null || stack.length == 0) {
            return CallerLocation.UNKNOWN;
        }
        return selector.select(stack, filter);
    }

}