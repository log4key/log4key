/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

/**
 * Throwable stack capture utility.
 *
 * Throwable栈捕获工具类。
 */
public final class ThrowableStackCapture implements IStackCapture {

    /**
     * 捕获当前线程的栈轨迹
     * 
     * @return 栈轨迹元素数组
     */
    @Override
    public StackTraceElement[] capture() {
        return new Throwable().getStackTrace();
    }
    
}
