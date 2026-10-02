/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.api;

/**
 * Directory template provider capability for log keys.
 *
 * 日志主键的目录模板提供能力接口。
 * 由应用侧按业务类型实现，固定返回该类型主键的落盘目录模板。
 * 目录模板与 {@code appender.directory} 语义一致：是 rootDirectory 下的子目录模板，
 * 支持 {@code {date}}、{@code {level}}、{@code {key}} 占位符，由路由在运行时展开。
 *
 * <p>当一个日志事件的主键实现了本接口且返回非空模板时，路由将用该模板
 * 完全取代对应的 {@code appender.directory}；否则回落 {@code appender.directory}。
 *
 * <p>注意：实例化成本与模板字符串创建解耦的关键在于 —— 模板是业务类的编译期常量，
 * 实例只携带 key，因此目录模板字符串不会随主键高频实例化而重复分配。
 */
public interface DirectoryTemplateProvider {

    /**
     * Returns the fixed directory template for this key type.
     *
     * 返回该主键类型固定使用的目录模板值，支持 {@code {date}/{level}/{key}} 占位符。
     * 返回 null 或空串表示不覆盖，路由回落 {@code appender.directory}。
     *
     * @return directory template under rootDirectory / rootDirectory 下的目录模板
     */
    String getDirectoryTemplate();
}