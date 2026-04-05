package com.log4key.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * ExecutorController测试类
 * 验证执行器控制器的核心功能
 */
public class ExecutorControllerTest {

    @Test
    public void testExecuteWithHealthyMainExecutor() {
        // 创建一个健康的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                task.run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {
                command.run();
            }

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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 测试健康状态下执行任务
        final boolean[] taskExecuted = {false};
        controller.execute("test-key", () -> taskExecuted[0] = true);

        // 验证任务被执行
        assertTrue(taskExecuted[0]);
        // 验证健康状态
        assertEquals(ExecutorHealthStatus.HEALTHY, controller.getHealthStatus());
    }

    @Test
    public void testExecuteWithFailedMainExecutor() throws InterruptedException {
        // 创建一个失败的主执行器
        LogExecutor mockFailedExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                throw new RejectedExecutionException("Test exception");
            }

            @Override
            public void execute(String key, Runnable command) {
                throw new RejectedExecutionException("Test exception");
            }

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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockFailedExecutor);

        // 测试主执行器失败时的降级行为
        final boolean[] taskExecuted = {false};
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        controller.execute("test-key", () -> {
            taskExecuted[0] = true;
            latch.countDown();
        });

        // 等待任务执行完毕，最多等待1秒
        latch.await(1, TimeUnit.SECONDS);
        
        // 验证任务被执行
        assertTrue(taskExecuted[0]);
        // 验证状态变为DEGRADED
        assertEquals(ExecutorHealthStatus.DEGRADED, controller.getHealthStatus());
    }

    @Test
    public void testCheckMainExecutorHealth() {
        // 创建一个健康的主执行器
        LogExecutor mockHealthyExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return null;
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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockHealthyExecutor);

        // 验证健康检查返回HEALTHY
        assertEquals(ExecutorHealthStatus.HEALTHY, controller.checkMainExecutorHealth());

        // 创建一个已关闭的主执行器
        LogExecutor mockShutdownExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return null;
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
                return true;
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

        // 创建执行器控制器
        ExecutorController controller2 = new ExecutorController(mockShutdownExecutor);

        // 验证健康检查返回ERROR
        assertEquals(ExecutorHealthStatus.ERROR, controller2.checkMainExecutorHealth());
    }

    @Test
    public void testShutdown() throws InterruptedException {
        // 创建一个可关闭的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            private volatile boolean shutdown = false;
            private volatile boolean terminated = false;

            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return null;
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {
                shutdown = true;
                terminated = true;
            }

            @Override
            public List<Runnable> shutdownNow() {
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
                return true;
            }
        };

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 执行关闭
        controller.shutdown();

        // 验证控制器已关闭
        assertTrue(controller.isShutdown());
        // 验证已终止
        assertTrue(controller.isTerminated());
    }

    @Test
    public void testExecuteAfterShutdown() {
        // 创建主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return null;
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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 先关闭控制器
        controller.shutdown();

        // 测试关闭后执行任务
        final boolean[] taskExecuted = {false};
        controller.execute("test-key", () -> taskExecuted[0] = true);

        // 验证任务被立即执行（在调用线程）
        assertTrue(taskExecuted[0]);
    }

    @Test
    public void testExecuteWithErrorStatus() {
        // 创建一个会抛出RuntimeException的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                throw new RuntimeException("Test runtime exception");
            }

            @Override
            public void execute(String key, Runnable command) {
                throw new RuntimeException("Test runtime exception");
            }

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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 第一次执行任务，应该会进入ERROR状态
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        controller.execute("test-key", latch::countDown);

        // 验证健康状态变为ERROR（无需等待任务执行完成，因为状态是立即设置的）
        assertEquals(ExecutorHealthStatus.ERROR, controller.getHealthStatus());
        
        // 等待任务执行完成，确保降级执行器正常工作
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void testFallbackExecutorFailure() {
        // 创建一个会失败的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                throw new RejectedExecutionException("Test exception");
            }

            @Override
            public void execute(String key, Runnable command) {
                throw new RejectedExecutionException("Test exception");
            }

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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 获取降级执行器并使其失效（通过反射修改内部状态）
        FallbackLogExecutor fallbackExecutor = controller.getFallbackExecutor();
        
        // 关闭降级执行器，使其无法接受任务
        fallbackExecutor.shutdownNow();

        // 测试主执行器和降级执行器都失败时的行为
        final boolean[] taskExecuted = {false};
        controller.execute("test-key", () -> taskExecuted[0] = true);

        // 验证任务被直接执行（在调用线程）
        assertTrue(taskExecuted[0]);
    }

    @Test
    public void testShutdownNow() throws InterruptedException {
        // 创建一个可关闭的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            private volatile boolean shutdown = false;
            private volatile boolean terminated = false;

            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return null;
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
                return true;
            }
        };

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 执行shutdownNow
        controller.shutdownNow();

        // 验证控制器已关闭
        assertTrue(controller.isShutdown());
        // 验证已终止
        assertTrue(controller.isTerminated());
    }

    @Test
    public void testAwaitTermination() throws InterruptedException {
        // 创建一个需要时间关闭的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            private volatile boolean shutdown = false;
            private volatile boolean terminated = false;

            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                return null;
            }

            @Override
            public void execute(String key, Runnable command) {}

            @Override
            public void shutdown() {
                shutdown = true;
                // 模拟需要时间关闭
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        terminated = true;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
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
                long startTime = System.currentTimeMillis();
                long timeoutMillis = unit.toMillis(timeout);
                
                while (!terminated && (System.currentTimeMillis() - startTime) < timeoutMillis) {
                    Thread.sleep(10);
                }
                return terminated;
            }
        };

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 执行关闭
        controller.shutdown();

        // 等待终止，应该会成功
        boolean terminated = controller.awaitTermination(500, TimeUnit.MILLISECONDS);
        assertTrue(terminated);
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        // 创建一个健康的主执行器
        LogExecutor mockMainExecutor = new LogExecutor() {
            @Override
            public java.util.concurrent.Future<?> submit(String key, Runnable task) {
                task.run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }

            @Override
            public void execute(String key, Runnable command) {
                command.run();
            }

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

        // 创建执行器控制器
        ExecutorController controller = new ExecutorController(mockMainExecutor);

        // 并发执行多个任务
        final int taskCount = 1000;
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(taskCount);
        final java.util.concurrent.atomic.AtomicInteger executedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // 创建多个线程并发执行任务
        for (int i = 0; i < taskCount; i++) {
            new Thread(() -> {
                controller.execute("test-key", () -> {
                    executedCount.incrementAndGet();
                    latch.countDown();
                });
            }).start();
        }

        // 等待所有任务完成
        latch.await(5, TimeUnit.SECONDS);

        // 验证所有任务都被执行
        assertEquals(taskCount, executedCount.get());
        // 验证健康状态仍然是HEALTHY
        assertEquals(ExecutorHealthStatus.HEALTHY, controller.getHealthStatus());
    }
}
