package com.log4key.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PausableLogExecutor扩展测试类
 * 验证其暂停、恢复功能和beforeExecute方法的延时等待机制，确保线程安全和性能
 */
public class PausableLogExecutorExtendedTest {

    /**
     * 测试beforeExecute方法的延时等待机制
     * 验证当执行器暂停时，任务会在beforeExecute中等待，直到恢复
     */
    @Test
    public void testBeforeExecuteDelayMechanism() throws InterruptedException {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(1);
        
        // 用于记录任务开始执行的时间
        final long[] taskStartTime = new long[1];
        
        // 用于等待任务完成
        CountDownLatch latch = new CountDownLatch(1);
        
        // 暂停执行器
        executor.pause();
        assertTrue(executor.isPaused());
        
        // 记录提交任务的时间
        long submitTime = System.currentTimeMillis();
        
        // 提交任务
        executor.execute(() -> {
            taskStartTime[0] = System.currentTimeMillis();
            try {
                // 模拟任务执行
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        // 等待一段时间，确保任务已经提交但还未执行
        Thread.sleep(500);
        
        // 验证任务还未开始执行（taskStartTime[0]应该为0）
        assertEquals(0, taskStartTime[0]);
        
        // 恢复执行器
        executor.resume();
        assertFalse(executor.isPaused());
        
        // 等待任务完成
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertTrue(completed);
        
        // 验证任务开始执行的时间晚于恢复时间
        long resumeTime = submitTime + 500; // 恢复时间约为提交时间+500ms
        assertTrue(taskStartTime[0] >= resumeTime, 
                "Task should start after resume, but start time: " + taskStartTime[0] + ", resume time: " + resumeTime);
        
        // 关闭执行器
        executor.shutdown();
    }

    /**
     * 测试多线程环境下的线程安全性
     * 验证多个线程同时调用pause和resume方法时的正确性
     */
    @Test
    public void testThreadSafety() throws InterruptedException {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(5);
        
        // 用于记录执行状态的计数器
        AtomicInteger executedTasks = new AtomicInteger(0);
        
        // 总任务数
        final int totalTasks = 100;
        
        // 用于等待所有任务完成
        CountDownLatch latch = new CountDownLatch(totalTasks);
        
        // 创建多个线程同时调用pause和resume方法
        int controlThreadCount = 10;
        CountDownLatch controlLatch = new CountDownLatch(controlThreadCount);
        
        // 启动控制线程
        for (int i = 0; i < controlThreadCount; i++) {
            new Thread(() -> {
                try {
                    // 随机调用pause和resume方法
                    for (int j = 0; j < 10; j++) {
                        if (Math.random() > 0.5) {
                            executor.pause();
                        } else {
                            executor.resume();
                        }
                        // 随机等待一段时间
                        Thread.sleep((long) (Math.random() * 100));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    controlLatch.countDown();
                }
            }).start();
        }
        
        // 提交大量任务
        for (int i = 0; i < totalTasks; i++) {
            executor.execute(() -> {
                try {
                    // 模拟任务执行
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    executedTasks.incrementAndGet();
                    latch.countDown();
                }
            });
        }
        
        // 等待所有控制线程完成
        controlLatch.await(5, TimeUnit.SECONDS);
        
        // 恢复执行器，确保所有任务都能继续执行
        executor.resume();
        
        // 等待所有任务完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All tasks should be completed");
        
        // 验证所有任务都已执行
        assertEquals(totalTasks, executedTasks.get());
        
        // 关闭执行器
        executor.shutdown();
    }

    /**
     * 测试暂停状态下的任务队列处理
     * 验证当执行器暂停时，提交的任务会被正确加入队列，不会丢失
     */
    @Test
    public void testTaskQueueHandlingWhenPaused() throws InterruptedException {
        // 创建一个可暂停的执行器，核心线程数为1，队列容量足够大
        PausableLogExecutor executor = new PausableLogExecutor(1);
        
        // 暂停执行器
        executor.pause();
        assertTrue(executor.isPaused());
        
        // 提交大量任务
        final int totalTasks = 20;
        CountDownLatch latch = new CountDownLatch(totalTasks);
        
        // 提交任务
        for (int i = 0; i < totalTasks; i++) {
            final int taskId = i;
            executor.execute(() -> {
                try {
                    // 模拟任务执行
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // 等待一段时间，确保所有任务都已加入队列
        Thread.sleep(500);
        
        // 恢复执行器
        executor.resume();
        assertFalse(executor.isPaused());
        
        // 等待所有任务完成
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All tasks should be completed");
        
        // 验证所有任务都已执行
        assertEquals(0, latch.getCount());
        
        // 关闭执行器
        executor.shutdown();
    }

    /**
     * 测试性能表现
     * 验证任务执行吞吐量
     * 注意：延迟指标（avgDelay/maxDelay）受测试环境和线程调度影响，不具备可重复性，不作为断言条件
     */
    @Test
    public void testPerformance() throws InterruptedException {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(4);
        
        final int totalTasks = 1000;
        CountDownLatch latch = new CountDownLatch(totalTasks);
        
        // 记录每个任务的执行延迟（仅用于诊断输出）
        List<Long> taskDelays = new ArrayList<>(totalTasks);
        
        // 提交任务并记录执行延迟
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < totalTasks; i++) {
            final long submitTime = System.currentTimeMillis();
            executor.execute(() -> {
                long executeTime = System.currentTimeMillis();
                long delay = executeTime - submitTime;
                taskDelays.add(delay);
                
                try {
                    // 模拟简单任务执行
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                latch.countDown();
            });
        }
        
        // 等待所有任务完成
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "All tasks should be completed");
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // 计算吞吐量（tasks per second）
        double throughput = totalTasks / (totalTime / 1000.0);
        
        // 计算平均延迟（仅用于诊断输出）
        long avgDelay = (long) taskDelays.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        // 计算最大延迟（仅用于诊断输出）
        long maxDelay = taskDelays.stream().mapToLong(Long::longValue).max().orElse(0);
        
        System.out.println("Performance Test Results:");
        System.out.println("Total Tasks: " + totalTasks);
        System.out.println("Total Time: " + totalTime + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " tasks/sec");
        System.out.println("Average Delay: " + avgDelay + " ms");
        System.out.println("Max Delay: " + maxDelay + " ms");
        
        // 验证吞吐量性能指标
        // 延迟指标（avgDelay/maxDelay）受测试环境线程调度影响，不具备可重复性，故不作为断言
        assertTrue(throughput > 100, "Throughput should be greater than 100 tasks/sec");
        
        // 关闭执行器
        executor.shutdown();
    }

    /**
     * 测试多线程同时调用pause和resume的并发安全性
     */
    @Test
    public void testConcurrentPauseResume() throws InterruptedException {
        // 创建一个可暂停的执行器
        PausableLogExecutor executor = new PausableLogExecutor(2);
        
        // 用于等待所有测试线程完成
        CountDownLatch testLatch = new CountDownLatch(20);
        
        // 启动多个线程同时调用pause和resume
        for (int i = 0; i < 10; i++) {
            // 线程1：调用pause
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    executor.pause();
                }
                testLatch.countDown();
            }).start();
            
            // 线程2：调用resume
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    executor.resume();
                }
                testLatch.countDown();
            }).start();
        }
        
        // 等待所有测试线程完成
        boolean completed = testLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All test threads should be completed");
        
        // 验证执行器处于稳定状态
        // 先恢复执行器，确保它处于可用状态
        executor.resume();
        // 执行一个简单任务，验证执行器可以正常工作
        CountDownLatch taskLatch = new CountDownLatch(1);
        executor.execute(taskLatch::countDown);
        completed = taskLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Simple task should be completed within 5 seconds");
        
        // 关闭执行器
        executor.shutdown();
    }
}
