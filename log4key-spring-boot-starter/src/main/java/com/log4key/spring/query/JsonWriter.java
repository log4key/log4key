/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

/**
 * Minimal JSON string encoder.
 *
 * 极简 JSON 工具：仅提供字符串转义（无第三方依赖，供查询 Servlet 输出 JSON 使用）。
 * 行内容可能含引号 / 反斜杠 / 控制字符，输出前必须转义。
 */
public final class JsonWriter {

    /**
     * 私有构造函数：工具类不允许实例化
     */
    private JsonWriter() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 转义 JSON 字符串内容（不含首尾引号），随后由调用方包裹引号。
     * 处理：双引号、反斜杠、退格、换页、换行、回车、制表符，
     * 以及 U+0000..U+001F 控制符（按四位十六进制转义输出）。
     *
     * @param value 原始字符串
     * @return JSON 转义后的内容
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        appendHex4(sb, c);
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 输出带引号的 JSON 字符串。
     *
     * @param value 原始字符串
     * @return 形如 "..." 的 JSON 字符串字面量
     */
    public static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    /**
     * 将控制字符按四位十六进制追加到输出（\\uXXXX 形式）。
     *
     * @param sb 输出缓冲
     * @param c 待转义的控制字符
     */
    private static void appendHex4(StringBuilder sb, char c) {
        String hex = Integer.toHexString(c);
        for (int i = hex.length(); i < 4; i++) {
            sb.append('0');
        }
        sb.append(hex);
    }
}
