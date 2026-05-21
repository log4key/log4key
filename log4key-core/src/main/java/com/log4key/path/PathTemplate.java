/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import com.log4key.api.LogEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Path template engine.
 *
 * 路径模板引擎，将含占位符的模板字符串编译为可执行的路径生成链。
 */
public class PathTemplate {

    /**
     * 编译后的片段列表。
     */
    final List<Segment> segments;

    /**
     * ThreadLocal StringBuilder，复用实例避免频繁创建。
     */
    private static final ThreadLocal<StringBuilder> SB_HOLDER =
            ThreadLocal.withInitial(() -> new StringBuilder(256));

    /**
     * 私有构造函数，通过静态工厂方法创建实例。
     *
     * @param segments 编译后的片段列表
     */
    private PathTemplate(List<Segment> segments) {
        this.segments = segments;
    }

    /**
     * 编译模板字符串为 PathTemplate 实例。
     *
     * 支持的占位符：{date}、{level}、{key}。
     * 遇到其他 {xxx} 格式将抛出 IllegalArgumentException。
     *
     * @param template 模板字符串
     * @return 编译后的 PathTemplate
     * @throws IllegalArgumentException 如果模板中包含非法的占位符
     */
    public static PathTemplate compile(String template) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }

        List<Segment> segments = new ArrayList<>();
        int len = template.length();
        int pos = 0;

        while (pos < len) {
            int openBrace = template.indexOf('{', pos);

            if (openBrace < 0) {
                segments.add(new LiteralSegment(template.substring(pos)));
                break;
            }

            if (openBrace > pos) {
                segments.add(new LiteralSegment(template.substring(pos, openBrace)));
            }

            int closeBrace = template.indexOf('}', openBrace);
            if (closeBrace < 0) {
                segments.add(new LiteralSegment(template.substring(pos)));
                break;
            }

            String placeholder = template.substring(openBrace + 1, closeBrace);

            switch (placeholder) {
                case "date":
                    segments.add(new DateSegment());
                    break;
                case "level":
                    segments.add(new LevelSegment());
                    break;
                case "key":
                    segments.add(new KeySegment());
                    break;
                default:
                    throw new IllegalArgumentException("非法的占位符: {" + placeholder + "}，仅允许 {date}、{level}、{key}");
            }

            pos = closeBrace + 1;
        }

        return new PathTemplate(segments);
    }

    /**
     * 将模板应用到日志事件，生成最终路径字符串。
     *
     * @param event 日志事件
     * @return 生成的路径字符串
     */
    public String apply(LogEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        StringBuilder sb = SB_HOLDER.get();
        sb.setLength(0);

        for (Segment segment : segments) {
            segment.append(sb, event);
        }

        return sb.toString();
    }

    /**
     * 将模板应用到日志事件，支持覆盖日志级别，生成最终路径字符串。
     *
     * @param event 日志事件
     * @param overrideLevel 覆盖的日志级别（为null时使用event中的级别）
     * @return 生成的路径字符串
     */
    public String apply(LogEvent event, String overrideLevel) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }

        StringBuilder sb = SB_HOLDER.get();
        sb.setLength(0);

        for (Segment segment : segments) {
            segment.append(sb, event, overrideLevel);
        }

        return sb.toString();
    }

    /**
     * 返回编译后的片段列表（package-private，供测试用）。
     *
     * @return 片段列表
     */
    List<Segment> getSegments() {
        return segments;
    }
}