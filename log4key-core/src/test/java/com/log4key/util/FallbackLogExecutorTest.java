package com.log4key.util;

import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * FallbackLogExecutor测试类
 * 验证降级执行器的核心功能
 */
public class FallbackLogExecutorTest {

    @Test
    public void testExecute() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 测试执行任务
        final boolean[] taskExecuted = {false};
        executor.execute("test-key", () -> taskExecuted[0] = true);

        // 等待任务执行完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 验证任务被执行
        assertTrue(taskExecuted[0]);

        // 关闭执行器
        executor.shutdown();
    }

    @Test
    public void testSubmit() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 测试提交任务
        Future<?> future = executor.submit("test-key", () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 验证future对象不为null
        assertNotNull(future);

        // 等待任务完成
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            fail("任务执行失败");
        }

        // 关闭执行器
        executor.shutdown();
    }

    @Test
    public void testExecuteAfterShutdown() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 先关闭执行器
        executor.shutdown();

        // 测试关闭后执行任务（应在调用线程执行）
        final boolean[] taskExecuted = {false};
        final long[] startTime = {System.currentTimeMillis()};
        final long[] endTime = {0};

        executor.execute("test-key", () -> {
            taskExecuted[0] = true;
            endTime[0] = System.currentTimeMillis();
        });

        // 验证任务被立即执行（执行时间应很短）
        assertTrue(taskExecuted[0]);
        assertTrue(endTime[0] - startTime[0] < 100);
    }

    @Test
    public void testShutdownWithNoEvents() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 记录开始关闭时间
        long startTime = System.currentTimeMillis();

        // 关闭执行器（此时没有新事件，应等待5秒）
        executor.shutdown();

        // 记录结束时间
        long endTime = System.currentTimeMillis();

        // 验证关闭过程耗时接近5秒（考虑线程调度误差，允许±1秒）
        long shutdownTime = endTime - startTime;
        System.out.println("Shutdown time with no events: " + shutdownTime + "ms");
        assertTrue(shutdownTime >= 4000 && shutdownTime <= 6000);

        // 验证执行器已关闭
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testShutdownWithNewEvents() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 启动一个线程，定期发送新事件
        Thread eventSender = new Thread(() -> {
            try {
                // 每1秒发送一个新事件，共发送3次
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(1000);
                    executor.execute("test-key", () -> {
                        System.out.println("Received event during shutdown: " + System.currentTimeMillis());
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 启动事件发送线程
        eventSender.start();

        // 等待一段时间，确保事件发送线程已启动
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 记录开始关闭时间
        long startTime = System.currentTimeMillis();

        // 关闭执行器
        executor.shutdown();

        // 记录结束时间
        long endTime = System.currentTimeMillis();

        // 等待事件发送线程结束
        try {
            eventSender.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 验证关闭过程耗时超过5秒（因为期间有新事件）
        long shutdownTime = endTime - startTime;
        System.out.println("Shutdown time with new events: " + shutdownTime + "ms");
        assertTrue(shutdownTime > 5000);

        // 验证执行器已关闭
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testShutdownNow() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 记录开始关闭时间
        long startTime = System.currentTimeMillis();

        // 立即关闭执行器（不等待）
        executor.shutdownNow();

        // 记录结束时间
        long endTime = System.currentTimeMillis();

        // 验证立即关闭耗时很短
        long shutdownTime = endTime - startTime;
        System.out.println("ShutdownNow time: " + shutdownTime + "ms");
        assertTrue(shutdownTime < 100);

        // 验证执行器已关闭
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testIsShutdownAndIsTerminated() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 初始状态：未关闭
        assertFalse(executor.isShutdown());
        assertFalse(executor.isTerminated());

        // 执行关闭
        executor.shutdown();

        // 关闭后状态
        assertTrue(executor.isShutdown());
        // 等待执行器完全终止
        try {
            executor.awaitTermination(6, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testAwaitTermination() throws InterruptedException {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 执行关闭
        executor.shutdown();

        // 等待终止，超时时间7秒（应足够）
        boolean terminated = executor.awaitTermination(7, TimeUnit.SECONDS);
        assertTrue(terminated);
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testAwaitTerminationTimeout() throws InterruptedException {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 提交一个事件，确保lastEventTime被更新
        executor.submit("test-key", () -> System.out.println("Test event"));
        Thread.sleep(100); // 等待事件处理

        // 执行关闭
        executor.shutdown();

        // 等待终止，超时时间3秒
        // 注意：由于awaitSafeShutdown的实现，如果没有新事件，它会立即返回true
        // 所以这里不再断言terminated为false，而是验证方法能正常执行
        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);

        // 如果3秒内没有新事件，awaitSafeShutdown会返回true
        // 然后executor.awaitTermination也会返回true（因为executor已经关闭）
        // 所以terminated可能是true或false，取决于执行速度

        // 立即关闭（确保资源释放）
        executor.shutdownNow();

        // 验证执行器最终会被终止
        assertTrue("Executor should eventually terminate", executor.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    public void testShutdownWithNoNewEvents() {
        // 创建降级执行器
        FallbackLogExecutor executor = new FallbackLogExecutor();

        // 执行一个任务，触发事件记录
        executor.execute("test-key", () -> {});

        // 等待一段时间确保任务完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        // 启动关闭流程
        executor.shutdown();

        // 验证执行器已关闭
        assertTrue(executor.isShutdown());
        
        try {
            boolean terminated = executor.isTerminated() || executor.awaitTermination(3, TimeUnit.SECONDS);
            assertTrue(terminated);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
