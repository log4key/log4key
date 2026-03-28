/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.formatter;

import com.log4key.formatter.token.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pattern parser for log templates.
 *
 * 日志模板解析器。
 */
public class PatternParser {
    // 占位符到Token类型的映射
    private static final Map<String, Class<? extends Token>> PLACEHOLDER_MAP = new HashMap<>();
    
    static {
        // 初始化占位符映射，支持log4j2和logback格式
        // 日期相关占位符
        PLACEHOLDER_MAP.put("d", DateToken.class);
        PLACEHOLDER_MAP.put("date", DateToken.class);
        
        // 日志级别占位符
        PLACEHOLDER_MAP.put("p", LevelToken.class);
        PLACEHOLDER_MAP.put("level", LevelToken.class);
        PLACEHOLDER_MAP.put("priority", LevelToken.class);
        
        // 类名占位符
        PLACEHOLDER_MAP.put("c", ClassNameToken.class);
        PLACEHOLDER_MAP.put("logger", ClassNameToken.class);
        PLACEHOLDER_MAP.put("category", ClassNameToken.class);
        
        // Log4Key 为了保持高性能，特意将 %class 和 %C 映射到 Logger Name (ClassNameToken)。
        // 这样做避免了为了获取调用者类名而进行昂贵的堆栈分析。
        // 这是一种兼容性优化策略。
        PLACEHOLDER_MAP.put("class", ClassNameToken.class);
        PLACEHOLDER_MAP.put("C", ClassNameToken.class);
        
        // 日志消息占位符
        PLACEHOLDER_MAP.put("m", MessageToken.class);
        PLACEHOLDER_MAP.put("msg", MessageToken.class);
        PLACEHOLDER_MAP.put("message", MessageToken.class);
        
        // 行号占位符
        PLACEHOLDER_MAP.put("L", LineNumberToken.class);
        PLACEHOLDER_MAP.put("line", LineNumberToken.class);
        
        // 方法名占位符
        PLACEHOLDER_MAP.put("M", MethodNameToken.class);
        PLACEHOLDER_MAP.put("method", MethodNameToken.class);
        
        // 文件名占位符
        PLACEHOLDER_MAP.put("F", FileNameToken.class);
        PLACEHOLDER_MAP.put("file", FileNameToken.class);
        
        // 换行符占位符
        PLACEHOLDER_MAP.put("n", NewLineToken.class);
        
        // 线程名占位符
        PLACEHOLDER_MAP.put("t", ThreadNameToken.class);
        PLACEHOLDER_MAP.put("thread", ThreadNameToken.class);
        
        // 日志主键占位符
        PLACEHOLDER_MAP.put("key", KeyToken.class);
        
        // 节点ID占位符
        PLACEHOLDER_MAP.put("nodeId", NodeIdToken.class);
        
        // 相对时间占位符
        PLACEHOLDER_MAP.put("r", RelativeTimeToken.class);
        PLACEHOLDER_MAP.put("relative", RelativeTimeToken.class);

        // MDC占位符
        PLACEHOLDER_MAP.put("X", MdcToken.class);
        PLACEHOLDER_MAP.put("mdc", MdcToken.class);

        // Marker占位符
        PLACEHOLDER_MAP.put("marker", MarkerToken.class);

        // 异常堆栈占位符
        PLACEHOLDER_MAP.put("ex", ThrowableToken.class);
        PLACEHOLDER_MAP.put("exception", ThrowableToken.class);
        PLACEHOLDER_MAP.put("throwable", ThrowableToken.class);
    }
    
    /**
     * 解析日志模板，生成Token列表
     *
     * @param pattern 日志模板字符串
     * @return Token列表
     */
    public List<Token> parse(String pattern) {
        List<Token> tokens = new ArrayList<>();

        if (pattern == null || pattern.isEmpty()) {
            return tokens;
        }

        int length = pattern.length();
        int lastIndex = 0;

        for (int i = 0; i < length; i++) {
            // 查找占位符起始位置
            if (pattern.charAt(i) == '%') {
                // 添加前一个文本片段
                if (i > lastIndex) {
                    tokens.add(new TextToken(pattern.substring(lastIndex, i)));
                }

                // 解析占位符
                int currentPos = i + 1;

                // 解析修饰符（如 %-5level 中的 -5）
                StringBuilder modifierBuilder = new StringBuilder();
                while (currentPos < length) {
                    char c = pattern.charAt(currentPos);
                    if (c == '-' || c == '+' || Character.isDigit(c) || c == '.' || c == '#' || c == '0') {
                        modifierBuilder.append(c);
                        currentPos++;
                    } else {
                        break;
                    }
                }
                String modifier = modifierBuilder.toString();

                // 查找占位符名称结束位置
                int placeholderEnd = currentPos;
                while (placeholderEnd < length) {
                    char c = pattern.charAt(placeholderEnd);
                    // 占位符由字母和数字组成，不包括点号
                    if (Character.isLetterOrDigit(c) || c == '_') {
                        placeholderEnd++;
                    } else {
                        break;
                    }
                }

                // 提取占位符名称
                String placeholder = pattern.substring(currentPos, placeholderEnd);

                // 解析格式参数（如 %d{yyyy-MM-dd} 中的 {yyyy-MM-dd}）
                String formatParam = null;
                int formatParamEnd = placeholderEnd;
                if (formatParamEnd < length && pattern.charAt(formatParamEnd) == '{') {
                    // 查找格式参数的结束位置
                    int braceCount = 1;
                    formatParamEnd++;
                    int paramStart = formatParamEnd;
                    while (formatParamEnd < length && braceCount > 0) {
                        char c = pattern.charAt(formatParamEnd);
                        if (c == '{') {
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                        }
                        formatParamEnd++;
                    }
                    if (braceCount == 0) {
                        formatParam = pattern.substring(paramStart, formatParamEnd - 1);
                    }
                }

                // 创建对应的Token
                Token token = createToken(placeholder, formatParam, modifier);
                if (token != null) {
                    tokens.add(token);
                } else {
                    // 如果无法识别占位符，将其作为普通文本处理
                    tokens.add(new TextToken(pattern.substring(i, formatParamEnd)));
                }

                lastIndex = formatParamEnd;
                i = formatParamEnd - 1;
            }
        }

        // 添加最后一个文本片段
        if (lastIndex < length) {
            tokens.add(new TextToken(pattern.substring(lastIndex)));
        }

        return tokens;
    }

    // -------------------- private --------------------

    /**
     * 根据占位符名称、格式参数和修饰符创建对应的Token实例
     *
     * @param placeholder 占位符名称
     * @param formatParam 格式参数
     * @param modifier 修饰符
     * @return Token实例，如果无法识别则返回null
     */
    private Token createToken(String placeholder, String formatParam, String modifier) {
        Token token = null;
        try {
            Class<? extends Token> tokenClass = PLACEHOLDER_MAP.get(placeholder);
            if (tokenClass != null) {
                // 尝试使用带格式参数的构造函数创建Token
                try {
                    Constructor<? extends Token> constructor = tokenClass.getConstructor(String.class);
                    token = constructor.newInstance(formatParam);
                } catch (NoSuchMethodException e1) {
                    // 如果没有带格式参数的构造函数，尝试使用默认构造函数
                    try {
                        Constructor<? extends Token> constructor = tokenClass.getConstructor();
                        token = constructor.newInstance();
                    } catch (NoSuchMethodException e2) {
                        // 如果没有默认构造函数，返回null
                        return null;
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            // 如果创建Token失败，返回null，将占位符作为普通文本处理
        }

        // 如果成功创建了Token，且存在修饰符，则应用格式化
        if (token != null && modifier != null && !modifier.isEmpty()) {
            FormattingInfo info = parseFormattingInfo(modifier, placeholder);
            if (info != null) {
                token = new FormattingToken(token, info);
            }
        }

        return token;
    }

    /**
     * 解析格式化修饰符
     * 格式示例: "-5", "20", ".30", "20.30", "-20.30"
     */
    private FormattingInfo parseFormattingInfo(String modifier, String placeholder) {
        if (modifier == null || modifier.isEmpty()) {
            return null;
        }

        boolean leftAlign = false;
        int minLength = 0;
        int maxLength = Integer.MAX_VALUE;

        int pos = 0;
        int len = modifier.length();

        // 1. 检查对齐方式 (左对齐)
        if (pos < len && modifier.charAt(pos) == '-') {
            leftAlign = true;
            pos++;
        }

        // 2. 检查最小长度
        if (pos < len && Character.isDigit(modifier.charAt(pos))) {
            int start = pos;
            while (pos < len && Character.isDigit(modifier.charAt(pos))) {
                pos++;
            }
            try {
                minLength = Integer.parseInt(modifier.substring(start, pos));
            } catch (NumberFormatException e) {
                // 忽略无效数字
            }
        }

        // 3. 检查最大长度 (精度)
        if (pos < len && modifier.charAt(pos) == '.') {
            pos++;
            if (pos < len && Character.isDigit(modifier.charAt(pos))) {
                int start = pos;
                while (pos < len && Character.isDigit(modifier.charAt(pos))) {
                    pos++;
                }
                try {
                    maxLength = Integer.parseInt(modifier.substring(start, pos));
                } catch (NumberFormatException e) {
                    // 忽略无效数字
                }
            }
        }

        if (minLength == 0 && maxLength == Integer.MAX_VALUE && !leftAlign) {
            return null;
        }

        // 根据占位符类型决定截断方向
        boolean truncateFromEnd = false; // 默认 Keep Right (适合 Logger)
        if (placeholder != null) {
            String p = placeholder.toLowerCase();
            // Level 和 Message 通常保留左侧 (Keep Left)
            if (p.equals("level") || p.equals("p") || p.equals("priority") ||
                p.equals("m") || p.equals("msg") || p.equals("message")) {
                truncateFromEnd = true;
            }
        }

        return new FormattingInfo(minLength, maxLength, leftAlign, truncateFromEnd);
    }
}
