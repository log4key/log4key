/**
 * 行号获取提供者接口，定义行号获取的抽象方法
 */
/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

/**
 * Location provider interface.
 *
 * 位置信息提供者接口。
 */
public interface LocationProvider {
    /**
     * 获取调用者的栈帧信息
     * @return 调用者的栈帧元素，若无法获取则返回null
     */
    CallerLocation getCallerLocation();
}