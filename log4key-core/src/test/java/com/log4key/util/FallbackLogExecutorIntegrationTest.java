package com.log4key.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * FallbackLogExecutor整合测试类
 * 验证FallbackLogExecutor与ExecutorController整合PausableLogExecutor的功能
 * 包括暂停/恢复机制、shutdown流程和状态通知
 */
public class FallbackLogExecutorIntegrationTest {

    /**
     * 测试Fallback执行器的暂停和恢复功能
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testFallbackExecutorPauseAndResume() throws InterruptedException {
        // 创建执行器控制器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {}

            @Override
            public List<Runnable> shutdownNow() {
                return new ArrayList<>();
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return true;
            }
        };
        ExecutorController controller = new ExecutorController(mockMainExecutor);
        FallbackLogExecutor fallbackExecutor = controller.getFallbackExecutor();

        // 创建测试任务和计数器
        final AtomicInteger executedCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(3);

        // 暂停执行器
        fallbackExecutor.pause();
        assertTrue(fallbackExecutor.getExecutor().isPaused());

        // 提交多个任务
        for (int i = 0; i < 3; i++) {
            fallbackExecutor.submit("test-key", () -> {
                executedCount.incrementAndGet();
                latch.countDown();
            });
        }

        // 等待一段时间，验证任务未执行（因为执行器已暂停）
        Thread.sleep(100);
        assertEquals(0, executedCount.get());

        // 恢复执行器
        fallbackExecutor.resume();
        assertFalse(fallbackExecutor.getExecutor().isPaused());

        // 等待所有任务执行完成
        latch.await(1, TimeUnit.SECONDS);
        assertEquals(3, executedCount.get());
    }

    /**
     * 测试ExecutorController的shutdown流程
     * 验证分阶段关闭：准备阶段（暂停Fallback执行器）和正式阶段（恢复并关闭）
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testExecutorControllerShutdownProcess() throws InterruptedException {
        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {
                // 立即关闭
            }

            @Override
            public List<Runnable> shutdownNow() {
                return new ArrayList<>();
            }

            @Override
            public boolean isShutdown() {
                return true;
            }

            @Override
            public boolean isTerminated() {
                return true;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return true;
            }
        });
        FallbackLogExecutor fallbackExecutor = controller.getFallbackExecutor();

        // 提交一些任务到Fallback执行器
        final AtomicInteger fallbackTaskCount = new AtomicInteger(0);
        final CountDownLatch fallbackLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            fallbackExecutor.submit("test-key", () -> {
                try {
                    // 模拟任务执行时间
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                fallbackTaskCount.incrementAndGet();
                fallbackLatch.countDown();
            });
        }

        // 暂停Fallback执行器
        fallbackExecutor.pause();
        assertTrue(fallbackExecutor.getExecutor().isPaused());

        // 恢复Fallback执行器
        fallbackExecutor.resume();
        assertFalse(fallbackExecutor.getExecutor().isPaused());

        // 等待所有Fallback任务执行完成
        fallbackLatch.await(2, TimeUnit.SECONDS);
        assertEquals(5, fallbackTaskCount.get());

        // 关闭控制器
        controller.shutdown();
        assertTrue(controller.isShutdown());
        controller.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(controller.isTerminated());
    }

    /**
     * 测试shutdownNow流程
     * 验证立即关闭时，Fallback执行器是否被正确处理
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testExecutorControllerShutdownNow() throws InterruptedException {
        // 创建主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            private volatile boolean shutdown = false;
            private volatile boolean terminated = false;

            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {
                shutdown = true;
            }

            @Override
            public List<Runnable> shutdownNow() {
                shutdown = true;
                terminated = true;
                return new ArrayList<>();
            }

            @Override
            public boolean isShutdown() {
                return shutdown;
            }

            @Override
            public boolean isTerminated() {
                return terminated;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return terminated;
            }
        };

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);
        FallbackLogExecutor fallbackExecutor = controller.getFallbackExecutor();

        // 提交一些任务到Fallback执行器
        final AtomicInteger fallbackTaskCount = new AtomicInteger(0);
        for (int i = 0; i < 5; i++) {
            fallbackExecutor.submit("test-key", () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                fallbackTaskCount.incrementAndGet();
            });
        }

        // 立即关闭控制器
        controller.shutdownNow();

        // 验证健康状态变为SHUTDOWN
        assertEquals(ExecutorHealthStatus.SHUTDOWN, controller.getHealthStatus());
        assertTrue(controller.isShutdown());

        // 等待一段时间，验证shutdownNow不会等待所有任务完成
        Thread.sleep(150);
        // 由于使用了shutdownNow，部分任务可能不会执行完成
        assertTrue(fallbackTaskCount.get() < 5);
    }

    /**
     * 测试在暂停状态下的任务提交
     * 验证暂停状态下提交的任务会在恢复后执行
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testTaskSubmissionWhilePaused() throws InterruptedException {
        // 创建执行器控制器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {}

            @Override
            public List<Runnable> shutdownNow() {
                return new ArrayList<>();
            }
            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return true;
            }
        };
        ExecutorController controller = new ExecutorController(mockMainExecutor);
        FallbackLogExecutor fallbackExecutor = controller.getFallbackExecutor();

        // 暂停Fallback执行器
        fallbackExecutor.pause();
        assertTrue(fallbackExecutor.getExecutor().isPaused());

        // 提交任务
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        fallbackExecutor.submit("test-key", () -> {
            taskExecuted.set(true);
            latch.countDown();
        });

        // 等待一段时间，验证任务未执行
        Thread.sleep(100);
        assertFalse(taskExecuted.get());

        // 恢复执行器
        fallbackExecutor.resume();
        assertFalse(fallbackExecutor.getExecutor().isPaused());

        // 等待任务执行完成
        latch.await(1, TimeUnit.SECONDS);
        assertTrue(taskExecuted.get());
    }

    /**
     * 测试shutdown流程
     * 验证shutdown流程是否正确执行
     * @throws InterruptedException 中断异常
     */
    @Test
    public void testShutdownProcess() throws InterruptedException {
        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {
                // 立即关闭
            }

            @Override
            public List<Runnable> shutdownNow() {
                // 立即关闭
                return new ArrayList<>();
            }

            @Override
            public boolean isShutdown() {
                return true;
            }

            @Override
            public boolean isTerminated() {
                return true;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                return true;
            }
        });

        // 初始状态应该是HEALTHY
        assertEquals(ExecutorHealthStatus.HEALTHY, controller.getHealthStatus());

        // 启动shutdown流程
        controller.shutdown();

        // 等待控制器完全关闭
        controller.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(controller.isTerminated());
    }
}
