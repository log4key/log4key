/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.spring.query;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JsonWriter 转义测试。
 */
public class JsonWriterTest {

    /**
     * 特殊字符（引号、反斜杠、退格、换页、换行、回车、制表符）转义为短转义序列。
     */
    @Test
    public void escapesSpecialCharacters() {
        assertEquals("\\\"", JsonWriter.escape("\""));
        assertEquals("\\\\", JsonWriter.escape("\\"));
        assertEquals("\\b", JsonWriter.escape("\b"));
        assertEquals("\\f", JsonWriter.escape("\f"));
        assertEquals("\\n", JsonWriter.escape("\n"));
        assertEquals("\\r", JsonWriter.escape("\r"));
        assertEquals("\\t", JsonWriter.escape("\t"));
    }

    /**
     * 其余控制字符（U+0000..U+001F）按四位十六进制转义输出。
     */
    @Test
    public void escapesControlCharactersAsUnicode() {
        assertEquals("\\u0000", JsonWriter.escape("\u0000"));
        assertEquals("\\u0001", JsonWriter.escape("\u0001"));
        assertEquals("\\u001f", JsonWriter.escape("\u001f"));
    }

    /**
     * 普通字符与非 ASCII（中文）字符原样保留。
     */
    @Test
    public void keepsNormalAndNonAsciiCharacters() {
        assertEquals("abc", JsonWriter.escape("abc"));
        assertEquals("中-文 log", JsonWriter.escape("中-文 log"));
    }

    /**
     * quote 在转义内容外包裹双引号。
     */
    @Test
    public void quoteWrapsEscapedContent() {
        assertEquals("\"a\\\"b\"", JsonWriter.quote("a\"b"));
        assertEquals("\"line1\\nline2\"", JsonWriter.quote("line1\nline2"));
    }

    /**
     * null 转义为空字符串。
     */
    @Test
    public void nullEscapesToEmpty() {
        assertEquals("", JsonWriter.escape(null));
    }
}
