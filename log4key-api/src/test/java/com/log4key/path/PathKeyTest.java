/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.path;

import java.nio.file.Paths;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * PathKey 单元测试。
 *
 * 验证 PathKey 的 equals/hashCode 语义、getter 方法以及路径拼接逻辑。
 */
public class PathKeyTest {

    /**
     * 测试相同 dir 和 file 的 PathKey 相等性。
     */
    @Test
    public void testEqualsSameValues() {
        PathKey key1 = new PathKey("dir1", "file1.log");
        PathKey key2 = new PathKey("dir1", "file1.log");
        assertEquals(key1, key2);
    }

    /**
     * 测试不同 dir 的 PathKey 不相等。
     */
    @Test
    public void testEqualsDifferentDir() {
        PathKey key1 = new PathKey("dir1", "file1.log");
        PathKey key2 = new PathKey("dir2", "file1.log");
        assertNotEquals(key1, key2);
    }

    /**
     * 测试不同 file 的 PathKey 不相等。
     */
    @Test
    public void testEqualsDifferentFile() {
        PathKey key1 = new PathKey("dir1", "file1.log");
        PathKey key2 = new PathKey("dir1", "file2.log");
        assertNotEquals(key1, key2);
    }

    /**
     * 测试相同 dir 和 file 的 PathKey 的 hashCode 一致性。
     */
    @Test
    public void testHashCodeConsistency() {
        PathKey key1 = new PathKey("dir1", "file1.log");
        PathKey key2 = new PathKey("dir1", "file1.log");
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    /**
     * 测试路径拼接逻辑：Paths.get(dir, file).toString()。
     */
    @Test
    public void testToAbsolutePath() {
        PathKey key = new PathKey("./logs/info/20260519", "order-1.log");
        String expected = Paths.get("./logs/info/20260519", "order-1.log").toString();
        assertEquals(expected, Paths.get(key.getDir(), key.getFile()).toString());
    }

    /**
     * 测试 getDir 方法。
     */
    @Test
    public void testGetDir() {
        PathKey key = new PathKey("mydir", "myfile.log");
        assertEquals("mydir", key.getDir());
    }

    /**
     * 测试 getFile 方法。
     */
    @Test
    public void testGetFile() {
        PathKey key = new PathKey("mydir", "myfile.log");
        assertEquals("myfile.log", key.getFile());
    }
}