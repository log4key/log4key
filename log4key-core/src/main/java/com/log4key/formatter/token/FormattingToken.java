/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter.token;

import com.log4key.api.LogEvent;

/**
 * Formatting token wrapper.
 *
 * 格式化Token包装器。
 */
public class FormattingToken implements Token {
    private final Token delegate;
    private final FormattingInfo formattingInfo;

    /**
     * 构造函数
     *
     * @param delegate 被包装的Token
     * @param formattingInfo 格式化信息
     */
    public FormattingToken(Token delegate, FormattingInfo formattingInfo) {
        this.delegate = delegate;
        this.formattingInfo = formattingInfo;
    }

    @Override
    public void render(LogEvent event, StringBuilder out) {
        // 先将代理Token渲染到临时缓冲区
        StringBuilder temp = new StringBuilder();
        delegate.render(event, temp);
        
        // 应用格式化规则并追加到输出缓冲区
        formattingInfo.format(out, temp.toString());
    }

    public Token getDelegate() {
        return delegate;
    }
}
