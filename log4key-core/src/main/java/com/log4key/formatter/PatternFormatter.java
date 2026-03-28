/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter;

import com.log4key.api.LogEvent;
import com.log4key.api.spi.LogFormatter;
import com.log4key.formatter.token.FormattingToken;
import com.log4key.formatter.token.ThrowableToken;
import com.log4key.formatter.token.Token;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pattern-based log formatter implementation.
 *
 * 模式格式化器。
 * 
 * 基于Token列表实现高效渲染，支持log4j2和logback占位符兼容
 * 支持的占位符：
 * %d/%date - 日期时间
 * %p/%level/%priority - 日志级别
 * %c/%logger/%category - 类名
 * %m/%msg/%message - 日志消息
 * %L/%line - 行号
 * %M/%method - 方法名
 * %F/%file - 文件名
 * %n - 换行符
 * %t/%thread - 线程名
 * %key - 日志主键
 * %nodeId - 节点ID
 * %r/%relative - 相对时间
 * %X/%mdc - MDC
 * %marker - Marker
 * %ex/%throwable - 异常堆栈
 */
public class PatternFormatter implements LogFormatter {
    
    // 默认日志格式
    private static final String DEFAULT_PATTERN = "%d [%p] [%c] %m%n";
    
    // 默认异常Token
    private static final ThrowableToken DEFAULT_THROWABLE_TOKEN = new ThrowableToken();

    // 模板解析结果缓存，使用线程安全的ConcurrentHashMap
    private static final ConcurrentHashMap<String, List<Token>> PATTERN_CACHE = new ConcurrentHashMap<>();
    
    // Pattern解析器
    private static final PatternParser PATTERN_PARSER = new PatternParser();
    
    /**
     * 日志格式
     */
    private String pattern;

    /**
     * 解析后的Token列表
     */
    private List<Token> tokens;

    /**
     * 是否包含异常Token
     */
    private boolean hasThrowableToken;
    
    /**
     * 默认构造函数，使用默认日志格式
     */
    public PatternFormatter() {
        this.pattern = DEFAULT_PATTERN;
        updateTokens(DEFAULT_PATTERN);
    }

    /**
     * Creates a new pattern formatter with the specified pattern.
     *
     * 使用指定模式创建格式化器。
     *
     * @param pattern the log pattern / 日志格式
     */
    public PatternFormatter(String pattern) {
        this.pattern = pattern != null ? pattern : DEFAULT_PATTERN;
        updateTokens(this.pattern);
    }

    /**
     * Sets the log pattern.
     *
     * 设置日志格式。
     *
     * @param pattern the log pattern / 日志格式
     */
    public void setPattern(String pattern) {
        String newPattern = pattern != null ? pattern : DEFAULT_PATTERN;
        if (!this.pattern.equals(newPattern)) {
            this.pattern = newPattern;
            updateTokens(this.pattern);
        }
    }

    @Override
    public String getName() {
        return "pattern";
    }

    @Override
    public String getType() {
        return "pattern";
    }

    @Override
    public String format(LogEvent event, Map<String, Object> context) {
        if (event == null) {
            throw new IllegalArgumentException("Log event cannot be null");
        }

        // 优化StringBuilder使用，预计算容量，减少扩容次数
        // 根据日志格式化经验，典型日志行长度约为200-500字符，设置初始容量为256
        StringBuilder sb = new StringBuilder(256);

        // 基于Token列表进行高效渲染
        for (Token token : tokens) {
            token.render(event, sb);
        }

        // 如果Pattern中没有显式包含异常Token，且Event中有异常，则自动追加
        if (event.getThrowable() != null && !hasThrowableToken) {
            // 确保异常堆栈另起一行
            if (sb.length() > 0) {
                char lastChar = sb.charAt(sb.length() - 1);
                if (lastChar != '\n' && lastChar != '\r') {
                    sb.append(System.lineSeparator());
                }
            }
            DEFAULT_THROWABLE_TOKEN.render(event, sb);
        }

        return sb.toString();
    }

    /**
     * Gets the current log pattern.
     *
     * 获取当前日志格式。
     *
     * @return the log pattern / 日志格式
     */
    public String getPattern() {
        return pattern;
    }

    // -------------------- private --------------------

    /**
     * 更新Tokens列表和状态
     */
    private void updateTokens(String pattern) {
        this.tokens = parsePattern(pattern);
        this.hasThrowableToken = checkThrowableToken(this.tokens);
    }

    /**
     * 检查是否包含ThrowableToken
     */
    private boolean checkThrowableToken(List<Token> tokens) {
        if (tokens == null) return false;
        for (Token token : tokens) {
            if (isThrowableToken(token)) {
                return true;
            }
        }
        return false;
    }

    private boolean isThrowableToken(Token token) {
        if (token instanceof ThrowableToken) {
            return true;
        }
        if (token instanceof FormattingToken) {
            return isThrowableToken(((FormattingToken) token).getDelegate());
        }
        return false;
    }

    /**
     * 解析日志模板，优先从缓存中获取结果
     *
     * @param pattern 日志模板字符串
     * @return Token列表
     */
    private List<Token> parsePattern(String pattern) {
        // 优先从缓存中获取解析结果
        return PATTERN_CACHE.computeIfAbsent(pattern, PATTERN_PARSER::parse);
    }

}
