/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

/**
 * MurmurHash3 algorithm implementation.
 *
 * MurmurHash3算法实现。
 */
public class MurmurHash3 {
    
    /**
     * Computes a 32-bit MurmurHash3 hash.
     *
     * 32位MurmurHash3算法实现。
     *
     * @param data the input data / 输入数据
     * @return the 32-bit hash value / 32位哈希值
     */
    public static int hash32(String data) {
        if (data == null) {
            return 0;
        }
        
        byte[] bytes = data.getBytes();
        return hash32(bytes, 0, bytes.length, 0x12345678);
    }
    
    /**
     * Computes a 32-bit MurmurHash3 hash with parameters.
     *
     * 带参数的32位MurmurHash3算法实现。
     *
     * @param data the input byte array / 输入字节数组
     * @param offset the offset / 偏移量
     * @param length the length / 长度
     * @param seed the seed value / 种子值
     * @return the 32-bit hash value / 32位哈希值
     */
    public static int hash32(byte[] data, int offset, int length, int seed) {
        int h1 = seed;
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        
        int roundedEnd = offset + (length & 0xfffffffc);
        
        for (int i = offset; i < roundedEnd; i += 4) {
            int k1 = (data[i] & 0xff) |
                    ((data[i + 1] & 0xff) << 8) |
                    ((data[i + 2] & 0xff) << 16) |
                    ((data[i + 3] & 0xff) << 24);
            
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;
            
            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }
        
        int k1 = 0;
        
        switch (length & 0x03) {
            case 3:
                k1 = (data[roundedEnd + 2] & 0xff) << 16;
                // fall through
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
                // fall through
            case 1:
                k1 |= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= c2;
                h1 ^= k1;
        }
        
        h1 ^= length;
        
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        
        return h1;
    }
    
    /**
     * Computes a 64-bit MurmurHash3 hash.
     *
     * 64位MurmurHash3算法实现。
     *
     * @param data the input data / 输入数据
     * @return the 64-bit hash value / 64位哈希值
     */
    public static long hash64(String data) {
        if (data == null) {
            return 0;
        }
        
        byte[] bytes = data.getBytes();
        return hash64(bytes, 0, bytes.length, 0x123456789abcdefL);
    }
    
    /**
     * Computes a 64-bit MurmurHash3 hash with parameters.
     *
     * 带参数的64位MurmurHash3算法实现。
     *
     * @param data the input byte array / 输入字节数组
     * @param offset the offset / 偏移量
     * @param length the length / 长度
     * @param seed the seed value / 种子值
     * @return the 64-bit hash value / 64位哈希值
     */
    public static long hash64(byte[] data, int offset, int length, long seed) {
        long h1 = seed;
        long h2 = seed;
        
        long c1 = 0x87c37b91114253d5L;
        long c2 = 0x4cf5ad432745937fL;
        
        int roundedEnd = offset + (length & 0xfffffff0);
        
        for (int i = offset; i < roundedEnd; i += 16) {
            long k1 = ((long) data[i] & 0xff) |
                    (((long) data[i + 1] & 0xff) << 8) |
                    (((long) data[i + 2] & 0xff) << 16) |
                    (((long) data[i + 3] & 0xff) << 24) |
                    (((long) data[i + 4] & 0xff) << 32) |
                    (((long) data[i + 5] & 0xff) << 40) |
                    (((long) data[i + 6] & 0xff) << 48) |
                    (((long) data[i + 7] & 0xff) << 56);
            
            long k2 = ((long) data[i + 8] & 0xff) |
                    (((long) data[i + 9] & 0xff) << 8) |
                    (((long) data[i + 10] & 0xff) << 16) |
                    (((long) data[i + 11] & 0xff) << 24) |
                    (((long) data[i + 12] & 0xff) << 32) |
                    (((long) data[i + 13] & 0xff) << 40) |
                    (((long) data[i + 14] & 0xff) << 48) |
                    (((long) data[i + 15] & 0xff) << 56);
            
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;
            
            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;
            
            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h2 ^= k2;
            
            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }
        
        long k1 = 0;
        long k2 = 0;
        
        switch (length & 0xf) {
            case 15:
                k2 = (data[roundedEnd + 14] & 0xff) << 48;
                // fall through
            case 14:
                k2 |= (data[roundedEnd + 13] & 0xff) << 40;
                // fall through
            case 13:
                k2 |= (data[roundedEnd + 12] & 0xff) << 32;
                // fall through
            case 12:
                k2 |= (data[roundedEnd + 11] & 0xff) << 24;
                // fall through
            case 11:
                k2 |= (data[roundedEnd + 10] & 0xff) << 16;
                // fall through
            case 10:
                k2 |= (data[roundedEnd + 9] & 0xff) << 8;
                // fall through
            case 9:
                k2 |= (data[roundedEnd + 8] & 0xff);
                k2 *= c2;
                k2 = Long.rotateLeft(k2, 33);
                k2 *= c1;
                h2 ^= k2;
                // fall through
            case 8:
                k1 = ((long) data[roundedEnd + 7] & 0xff) << 56;
                // fall through
            case 7:
                k1 |= ((long) data[roundedEnd + 6] & 0xff) << 48;
                // fall through
            case 6:
                k1 |= ((long) data[roundedEnd + 5] & 0xff) << 40;
                // fall through
            case 5:
                k1 |= ((long) data[roundedEnd + 4] & 0xff) << 32;
                // fall through
            case 4:
                k1 |= (data[roundedEnd + 3] & 0xff) << 24;
                // fall through
            case 3:
                k1 |= (data[roundedEnd + 2] & 0xff) << 16;
                // fall through
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
                // fall through
            case 1:
                k1 |= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
        }
        
        h1 ^= length;
        h2 ^= length;
        
        h1 += h2;
        h2 += h1;
        
        h1 ^= h1 >>> 33;
        h1 *= 0xff51afd7ed558ccdL;
        h1 ^= h1 >>> 33;
        h1 *= 0xc4ceb9fe1a85ec53L;
        h1 ^= h1 >>> 33;
        
        h2 ^= h2 >>> 33;
        h2 *= 0xff51afd7ed558ccdL;
        h2 ^= h2 >>> 33;
        h2 *= 0xc4ceb9fe1a85ec53L;
        h2 ^= h2 >>> 33;
        
        h1 += h2;
        h2 += h1;
        
        return h1;
    }
}