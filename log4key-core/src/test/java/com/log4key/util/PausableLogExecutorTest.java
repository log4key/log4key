package com.log4key.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PausableLogExecutor测试类
 */
public class PausableLogExecutorTest {

    @Test
    public void testPauseAndResume() throws InterruptedException {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(2);
        
        // 用于等待任务完成的计数器
        CountDownLatch latch = new CountDownLatch(2);
        
        // 用于标记任务是否开始执行
        CountDownLatch taskStarted = new CountDownLatch(2);
        
        // 暂停执行器
        executor.pause();
        
        // 提交两个任务
        executor.execute(() -> {
            taskStarted.countDown();
            try {
                // 模拟任务执行
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        executor.execute(() -> {
            taskStarted.countDown();
            try {
                // 模拟任务执行
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        // 等待一段时间，确保任务已经提交但还未执行
        Thread.sleep(500);
        
        // 验证执行器处于暂停状态
        assertTrue(executor.isPaused());
        
        // 验证任务还未完成（计数器值仍为2）
        assertEquals(2, latch.getCount());
        
        // 恢复执行器
        executor.resume();
        
        // 验证执行器不再处于暂停状态
        assertFalse(executor.isPaused());
        
        // 等待所有任务完成
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        
        // 验证所有任务都已完成
        assertTrue(completed);
        assertEquals(0, latch.getCount());
        
        // 关闭执行器
        executor.shutdown();
    }
    
    @Test
    public void testIsPaused() {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(1);
        
        // 初始状态应该是未暂停
        assertFalse(executor.isPaused());
        
        // 暂停执行器
        executor.pause();
        
        // 验证状态变为暂停
        assertTrue(executor.isPaused());
        
        // 恢复执行器
        executor.resume();
        
        // 验证状态变为未暂停
        assertFalse(executor.isPaused());
        
        // 关闭执行器
        executor.shutdown();
    }
    
    @Test
    public void testMultiplePauseResume() throws InterruptedException {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(3);
        
        // 用于等待任务完成的计数器 - 第一次
        CountDownLatch latch1 = new CountDownLatch(3);
        
        // 第一次暂停
        executor.pause();
        assertTrue(executor.isPaused());
        
        // 提交任务
        for (int i = 0; i < 3; i++) {
            final CountDownLatch finalLatch = latch1;
            executor.execute(() -> {
                try {
                    // 模拟任务执行
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                finalLatch.countDown();
            });
        }
        
        // 等待一段时间，确保任务已经提交但还未执行
        Thread.sleep(300);
        
        // 验证任务还未完成
        assertEquals(3, latch1.getCount());
        
        // 第一次恢复
        executor.resume();
        assertFalse(executor.isPaused());
        
        // 等待所有任务完成
        boolean completed = latch1.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(0, latch1.getCount());
        
        // 第二次暂停和恢复
        executor.pause();
        assertTrue(executor.isPaused());
        
        // 第二次计数器
        CountDownLatch latch2 = new CountDownLatch(2);
        
        // 提交新任务
        executor.execute(() -> {
            latch2.countDown();
        });
        executor.execute(() -> {
            latch2.countDown();
        });
        
        // 等待一段时间
        Thread.sleep(200);
        
        // 验证任务还未完成
        assertEquals(2, latch2.getCount());
        
        // 第二次恢复
        executor.resume();
        assertFalse(executor.isPaused());
        
        // 等待任务完成
        completed = latch2.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(0, latch2.getCount());
        
        // 关闭执行器
        executor.shutdown();
    }
}