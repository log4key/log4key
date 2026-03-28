/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.location;

import java.util.List;

/**
 * Frame filter for call stack.
 *
 * 帧筛选器。
 */
public final class FrameFilter {

    /**
     * 要跳过的类名前缀列表
     */
    private final List<String> skipPrefixes;

    /**
     * 位置信息提供者类的完全限定名
     */
    private final String providerClass;

    /**
     * Checks if the stack trace element is meaningful.
     *
     * 检查当前帧是否有意义（包含有效行号和文件名）。
     *
     * @param e the stack trace element / 栈轨迹元素
     * @return true if meaningful / 如果有意义则返回 true
     */
    public boolean isMeaningful(StackTraceElement e) {
        if (e.getLineNumber() <= 0) {
            return false;
        }
        if (e.getFileName() == null) {
            return false;
        }
        return true;
    }

    /**
     * 构造函数
     *
     * @param skipPrefixes  要跳过的类名前缀列表
     * @param providerClass 位置信息提供者类
     */
    FrameFilter(List<String> skipPrefixes, Class<?> providerClass) {
        this.skipPrefixes = skipPrefixes;
        this.providerClass = providerClass.getName();
    }

    /**
     * Checks if the frame should be skipped.
     *
     * 检查是否应该跳过当前帧。
     *
     * @param e the stack trace element / 栈轨迹元素
     * @return true if should skip / 如果应该跳过则返回true
     */
    boolean shouldSkip(StackTraceElement e) {
        String cn = e.getClassName();

        // provider 自身
        if (cn.equals(providerClass) || cn.startsWith(providerClass + "$")) {
            return true;
        }

        // 是否已知框架
        if (isFrameworkOrSystemClass(cn)) {
            return true;
        }

        // synthetic lambda（只过滤这一种）
        if (isSyntheticClass(e)) {
            return true;
        }

        return false;
    }


    /**
     * 检查类名是否为已知框架或系统类
     *
     * @param className 类名
     * @return 如果是已知框架或系统类则返回 true，否则返回 false
     */
    private boolean isFrameworkOrSystemClass(String className) {
        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return true;
        }
        for (String prefix : skipPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if the class is a synthetic class (Lambda or CGLIB/Proxy enhanced class).
     *
     * 检查类名是否为合成类（Lambda 表达式或 CGLIB/Proxy 增强类）。
     *
     * @param frame the stack trace element / 栈轨迹元素
     * @return true if synthetic class / 如果是合成类则返回true
     */
    private boolean isSyntheticClass(StackTraceElement frame) {
        String className = frame.getClassName();
        // Lambda: MyClass$$Lambda$1/...
        // CGLIB / Proxy: MyClass$$EnhancerByCGLIB$$..., Proxy$...
        if (className.contains("$Lambda$")
                || className.startsWith("java.lang.invoke.LambdaForm$")) {
            return true;
        }

        // synthetic lambda 类，且行号和文件名无效
        if (className.contains("$$") && !isMeaningful(frame)) {
            return true;
        }

        return false;
    }

}