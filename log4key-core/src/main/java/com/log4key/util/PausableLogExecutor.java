/*
 * Copyright 2026 Log4Key
 *
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.log4key.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pausable log executor.
 *
 * 可暂停的日志执行器。
 */
public class PausableLogExecutor extends ThreadPoolExecutor {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition unpaused = lock.newCondition();
    private volatile boolean isPaused = false;
    private static final AtomicInteger threadCounter = new AtomicInteger(1);

    /**
     * 暂停执行器
     * 调用后，所有新任务将在beforeExecute方法中等待，直到调用resume()
     */
    public void pause() {
        lock.lock();
        try {
            isPaused = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 恢复执行器
     * 唤醒所有等待的任务，允许它们继续执行
     */
    public void resume() {
        lock.lock();
        try {
            isPaused = false;
            unpaused.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查执行器是否处于暂停状态
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * 构造函数
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime 线程存活时间
     * @param unit 时间单位
     * @param workQueue 工作队列
     * @param threadFactory 线程工厂
     * @param handler 拒绝策略
     */
    public PausableLogExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 在任务执行前检查是否需要暂停
     * 如果执行器处于暂停状态，当前线程将等待直到被唤醒
     * @param t 执行任务的线程
     * @param r 要执行的任务
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        lock.lock();
        try {
            while (isPaused) {
                unpaused.await();
            }
        } catch (InterruptedException e) {
            t.interrupt();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 构造函数 - 使用默认配置
     * @param corePoolSize 核心线程数
     */
    public PausableLogExecutor(int corePoolSize) {
        super(corePoolSize, corePoolSize * 2, 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<>(), // 使用无界队列，与 FallbackLogExecutor 保持一致
              r -> {
                  Thread thread = new Thread(r, "log4key-pausable-executor-" + threadCounter.getAndIncrement());
                  thread.setDaemon(false); // 非守护线程
                  return thread;
              },
              new ThreadPoolExecutor.CallerRunsPolicy()); // 使用 CallerRunsPolicy，与 FallbackLogExecutor 保持一致
    }
}