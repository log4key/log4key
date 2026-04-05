package com.log4key.util;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * 直接运行PausableLogExecutor测试的主类
 * 用于避免Gradle构建系统的文件占用问题
 */
public class RunPausableLogExecutorTest {
    public static void main(String[] args) {
        // 运行PausableLogExecutorTest
        System.out.println("Running PausableLogExecutorTest...");
        Result result1 = JUnitCore.runClasses(com.log4key.util.PausableLogExecutorTest.class);
        printResult("PausableLogExecutorTest", result1);
        
        // 运行PausableLogExecutorExtendedTest
        System.out.println("\nRunning PausableLogExecutorExtendedTest...");
        Result result2 = JUnitCore.runClasses(com.log4key.util.PausableLogExecutorExtendedTest.class);
        printResult("PausableLogExecutorExtendedTest", result2);
    }
    
    private static void printResult(String testName, Result result) {
        System.out.println("Test: " + testName);
        System.out.println("Tests run: " + result.getRunCount());
        System.out.println("Failures: " + result.getFailureCount());
        System.out.println("Errors: " + result.getIgnoreCount());
        System.out.println("Execution time: " + result.getRunTime() + " ms");
        
        if (result.getFailureCount() > 0) {
            System.out.println("\nFailures:");
            for (Failure failure : result.getFailures()) {
                System.out.println("- " + failure.toString());
                System.out.println("  " + failure.getTrace().substring(0, Math.min(500, failure.getTrace().length())) + "...");
            }
        } else {
            System.out.println("\nAll tests passed!");
        }
    }
}
