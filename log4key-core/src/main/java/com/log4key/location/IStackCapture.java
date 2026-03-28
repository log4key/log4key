/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

/**
 * Stack capture interface.
 *
 * 栈捕获接口。
 */
public interface IStackCapture {
    /**
     * 捕获栈轨迹
     * 
     * @return 栈轨迹元素数组
     */
    StackTraceElement[] capture();
}
