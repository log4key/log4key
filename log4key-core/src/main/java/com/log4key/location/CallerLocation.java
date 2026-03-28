/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

/**
 * Caller location information.
 *
 * 调用者位置信息类。
 */
public final class CallerLocation {

    /**
     * 未知调用者位置常量
     */
    public static final CallerLocation UNKNOWN =
        new CallerLocation("?", "?", "?", -1);


    /**
     * 调用者类名
     */
    public final String className;
    /**
     * 调用者方法名
     */
    public final String methodName;
    /**
     * 调用者文件名
     */
    public final String fileName;
    /**
     * 调用者行号
     */
    public final int lineNumber;

    /**
     * Creates a new CallerLocation.
     *
     * 创建调用者位置信息。
     *
     * @param className the caller's class name / 调用者类名
     * @param methodName the caller's method name / 调用者方法名
     * @param fileName the caller's file name / 调用者文件名
     * @param lineNumber the caller's line number / 调用者行号
     */
    public CallerLocation(String className, String methodName, String fileName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    /**
     * Checks if this is an unknown caller location.
     *
     * 检查是否未知调用者位置。
     *
     * @return true if the location is unknown / 如果行号小于等于0，则返回true
     */
    public boolean isUnknown() {
        return lineNumber <= 0;
    }
    
}
