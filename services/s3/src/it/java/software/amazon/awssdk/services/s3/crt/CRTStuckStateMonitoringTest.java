/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.crt;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.BlockingInputStreamAsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Advanced integration test that monitors system resources to detect the "stuck state" issue
 * in S3 CRT client after BlockingInputStreamAsyncRequestBody timeouts.
 * 
 * This test combines:
 * 1. Reproduction of the timeout issue
 * 2. Real-time system monitoring (CPU, memory, threads)
 * 3. Detection of stuck state indicators
 * 4. Validation of client recovery capabilities
 */
public class CRTStuckStateMonitoringTest extends S3IntegrationTestBase {

    private S3AsyncClient s3CrtClient;
    private ScheduledExecutorService monitoringExecutor;
    private SystemMonitor systemMonitor;
    protected String bucketName;

    // Monitoring thresholds
    private static final double HIGH_CPU_THRESHOLD = 80.0; // 80% CPU usage
    private static final long HIGH_MEMORY_THRESHOLD = 500 * 1024 * 1024; // 500MB
    private static final int HIGH_THREAD_THRESHOLD = 50; // 50+ threads
    private static final Duration MONITORING_INTERVAL = Duration.ofSeconds(2);
    private static final Duration TEST_DURATION = Duration.ofSeconds(60);

    @BeforeEach
    void setUpTest() {
        // Initialize base S3 client if needed
        if (s3 == null) {
            try {
                setUp();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize S3 client", e);
            }
        }
        
        // Create S3 CRT Async Client
        s3CrtClient = S3AsyncClient.crtBuilder()
                .region(DEFAULT_REGION)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .build();

        // Initialize monitoring
        monitoringExecutor = Executors.newScheduledThreadPool(2);
        systemMonitor = new SystemMonitor();
        
        // Create test bucket
        bucketName = "crt-stuck-state-monitor-" + System.currentTimeMillis();
        createBucket(bucketName);
        
        System.out.println("=== CRT Stuck State Monitoring Test Started ===");
        System.out.println("Bucket: " + bucketName);
        System.out.println("Monitoring thresholds - CPU: " + HIGH_CPU_THRESHOLD + "%, Memory: " + 
                          (HIGH_MEMORY_THRESHOLD / 1024 / 1024) + "MB, Threads: " + HIGH_THREAD_THRESHOLD);
    }

    @AfterEach
    void tearDownTest() {
        // Stop monitoring first
        if (monitoringExecutor != null) {
            monitoringExecutor.shutdown();
            try {
                monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Close CRT client
        if (s3CrtClient != null) {
            try {
                s3CrtClient.close();
            } catch (Exception e) {
                System.err.println("Failed to close S3 CRT client: " + e.getMessage());
            }
        }
        
        // Clean up bucket
        if (bucketName != null) {
            try {
                deleteBucketAndAllContents(bucketName);
            } catch (Exception e) {
                System.err.println("Failed to delete test bucket: " + e.getMessage());
            }
        }
        
        // Print final monitoring report
        if (systemMonitor != null) {
            systemMonitor.printFinalReport();
        }
    }

    /**
     * Test that reproduces the stuck state issue and monitors system resources
     * to detect the characteristic patterns of the bug.
     */
    @Test
    @Timeout(120)
    void testStuckStateWithSystemMonitoring() throws Exception {
        System.out.println("--- Starting Stuck State Detection Test ---");
        
        // Start system monitoring
        systemMonitor.startMonitoring(monitoringExecutor);
        
        // Phase 1: Trigger the timeout issue
        System.out.println("Phase 1: Triggering timeout to cause stuck state...");
        boolean timeoutOccurred = triggerTimeoutIssue();
        
        assertThat(timeoutOccurred).isTrue();
        System.out.println("✓ Timeout successfully triggered");
        
        // Phase 2: Monitor for stuck state indicators
        System.out.println("Phase 2: Monitoring for stuck state indicators...");
        Thread.sleep(10000); // Wait 10 seconds for stuck state to manifest
        
        StuckStateIndicators indicators = systemMonitor.analyzeStuckState();
        
        // Phase 3: Test client recovery
        System.out.println("Phase 3: Testing client recovery...");
        boolean recoverySuccessful = testClientRecovery();
        
        // Phase 4: Analysis and reporting
        System.out.println("--- Analysis Results ---");
        
        if (indicators.isStuckStateDetected()) {
            System.out.println("✗ STUCK STATE DETECTED!");
            System.out.println("  - High CPU: " + indicators.hasHighCpu());
            System.out.println("  - Memory Issues: " + indicators.hasMemoryIssues());
            System.out.println("  - Thread Problems: " + indicators.hasThreadIssues());
            System.out.println("  - CRT Threads Stuck: " + indicators.hasCrtThreadsStuck());
            
            if (!recoverySuccessful) {
                fail("S3 CRT Client entered stuck state after timeout. " +
                     "Client recovery failed. Indicators: " + indicators);
            }
        } else {
            System.out.println("✓ No stuck state detected - client appears healthy");
        }
        
        if (recoverySuccessful) {
            System.out.println("✓ Client recovery successful");
        } else {
            System.out.println("✗ Client recovery failed");
        }
    }

    /**
     * Test that reproduces the exact user pattern that causes the issue
     */
    @Test
    @Timeout(90)
    void testUserPatternReproduction() throws Exception {
        System.out.println("--- Testing User Pattern Reproduction ---");
        
        systemMonitor.startMonitoring(monitoringExecutor);
        
        // Reproduce the exact pattern from user code analysis
        byte[] testData = "User pattern test data - this simulates the CloudWatch Elephant service pattern".getBytes();
        
        try {
            // Step 1: Create request body with null content length (user's pattern)
            BlockingInputStreamAsyncRequestBody requestBody = 
                BlockingInputStreamAsyncRequestBody.builder()
                    .contentLength(null) // User passes null
                    .subscribeTimeout(Duration.ofSeconds(5)) // Shorter timeout to trigger issue
                    .build();

            // Step 2: Start S3 operation (user's pattern)
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("user-pattern-test-key")
                    .build();

            CompletableFuture<PutObjectResponse> putFuture = s3CrtClient.putObject(putRequest, requestBody);

            // Step 3: Immediately try to write input stream (user's pattern)
            // This creates the race condition that leads to timeout
            InputStream inputStream = new ByteArrayInputStream(testData);
            
            long startTime = System.currentTimeMillis();
            requestBody.writeInputStream(inputStream);
            
            // If we get here without timeout, wait for completion
            putFuture.get(30, TimeUnit.SECONDS);
            
            System.out.println("✓ User pattern succeeded (no timeout occurred)");
            
        } catch (IllegalStateException e) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("✓ User pattern reproduced timeout after " + duration + "ms");
            System.out.println("  Exception: " + e.getMessage());
            
            // Now monitor for stuck state
            Thread.sleep(5000);
            StuckStateIndicators indicators = systemMonitor.analyzeStuckState();
            
            if (indicators.isStuckStateDetected()) {
                System.out.println("✗ CRITICAL: User pattern caused stuck state!");
                System.out.println("  This confirms the production issue reported in the ticket");
            }
        }
    }

    /**
     * Test concurrent operations to simulate production load
     */
    @Test
    @Timeout(150)
    void testConcurrentOperationsStuckState() throws Exception {
        System.out.println("--- Testing Concurrent Operations for Stuck State ---");
        
        systemMonitor.startMonitoring(monitoringExecutor);
        
        int concurrentOperations = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentOperations);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        // Start concurrent operations
        for (int i = 0; i < concurrentOperations; i++) {
            final int operationId = i;
            
            CompletableFuture.runAsync(() -> {
                try {
                    startLatch.await();
                    
                    byte[] data = ("Concurrent operation " + operationId + " data").getBytes();
                    
                    BlockingInputStreamAsyncRequestBody requestBody = 
                        BlockingInputStreamAsyncRequestBody.builder()
                            .contentLength((long) data.length)
                            .subscribeTimeout(Duration.ofSeconds(3)) // Short timeout
                            .build();

                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key("concurrent-key-" + operationId)
                            .build();

                    CompletableFuture<PutObjectResponse> putFuture = s3CrtClient.putObject(putRequest, requestBody);
                    requestBody.writeInputStream(new ByteArrayInputStream(data));
                    putFuture.get(30, TimeUnit.SECONDS);
                    
                    successCount.incrementAndGet();
                    System.out.println("✓ Concurrent operation " + operationId + " succeeded");
                    
                } catch (IllegalStateException e) {
                    timeoutCount.incrementAndGet();
                    System.out.println("⚠ Concurrent operation " + operationId + " timed out");
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.out.println("✗ Concurrent operation " + operationId + " failed: " + e.getMessage());
                    
                } finally {
                    completionLatch.countDown();
                }
            });
        }
        
        // Start all operations simultaneously
        startLatch.countDown();
        
        // Wait for completion
        completionLatch.await(60, TimeUnit.SECONDS);
        
        System.out.println("--- Concurrent Operations Results ---");
        System.out.println("Successes: " + successCount.get());
        System.out.println("Timeouts: " + timeoutCount.get());
        System.out.println("Errors: " + errorCount.get());
        
        // Analyze stuck state after concurrent operations
        Thread.sleep(5000);
        StuckStateIndicators indicators = systemMonitor.analyzeStuckState();
        
        if (indicators.isStuckStateDetected()) {
            System.out.println("✗ CRITICAL: Concurrent operations caused stuck state!");
            System.out.println("  Success rate: " + successCount.get() + "/" + concurrentOperations);
            
            if (successCount.get() == 0 && timeoutCount.get() > 0) {
                fail("All concurrent operations failed after initial timeout - indicates stuck state");
            }
        }
    }

    private boolean triggerTimeoutIssue() {
        try {
            byte[] testData = "Timeout trigger test data".getBytes();
            
            BlockingInputStreamAsyncRequestBody requestBody = 
                BlockingInputStreamAsyncRequestBody.builder()
                    .contentLength((long) testData.length)
                    .subscribeTimeout(Duration.ofSeconds(1)) // Very short timeout
                    .build();

            // Call writeInputStream BEFORE putObject to guarantee timeout
            InputStream inputStream = new ByteArrayInputStream(testData);
            requestBody.writeInputStream(inputStream);
            
            return false; // If we get here, no timeout occurred
            
        } catch (IllegalStateException e) {
            return e.getMessage().contains("service request was not made within");
        } catch (Exception e) {
            System.err.println("Unexpected exception during timeout trigger: " + e.getMessage());
            return false;
        }
    }

    private boolean testClientRecovery() {
        try {
            byte[] testData = "Recovery test data".getBytes();
            
            BlockingInputStreamAsyncRequestBody requestBody = 
                AsyncRequestBody.forBlockingInputStream((long) testData.length);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key("recovery-test-key")
                    .build();

            CompletableFuture<PutObjectResponse> putFuture = s3CrtClient.putObject(putRequest, requestBody);
            requestBody.writeInputStream(new ByteArrayInputStream(testData));
            putFuture.get(30, TimeUnit.SECONDS);
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Client recovery failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * System monitor that tracks CPU, memory, threads, and CRT-specific indicators
     */
    private static class SystemMonitor {
        private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        
        private final AtomicLong maxCpuUsage = new AtomicLong(0);
        private final AtomicLong maxMemoryUsage = new AtomicLong(0);
        private final AtomicInteger maxThreadCount = new AtomicInteger(0);
        private final AtomicInteger highCpuCount = new AtomicInteger(0);
        private final AtomicBoolean crtThreadsStuck = new AtomicBoolean(false);
        
        private final List<String> stuckThreads = new ArrayList<>();
        
        void startMonitoring(ScheduledExecutorService executor) {
            executor.scheduleAtFixedRate(this::collectMetrics, 0, 
                                       MONITORING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
        }
        
        private void collectMetrics() {
            // CPU monitoring
            double cpuUsage = osBean.getProcessCpuLoad() * 100;
            if (cpuUsage > 0) { // -1 indicates not available
                maxCpuUsage.updateAndGet(current -> Math.max(current, (long) cpuUsage));
                
                if (cpuUsage > HIGH_CPU_THRESHOLD) {
                    highCpuCount.incrementAndGet();
                    checkForStuckThreads();
                }
            }
            
            // Memory monitoring
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            maxMemoryUsage.updateAndGet(current -> Math.max(current, usedMemory));
            
            // Thread monitoring
            int threadCount = threadBean.getThreadCount();
            maxThreadCount.updateAndGet(current -> Math.max(current, threadCount));
            
            // Log current state
            System.out.printf("[MONITOR] CPU: %.1f%%, Memory: %dMB, Threads: %d%n", 
                             cpuUsage, usedMemory / (1024 * 1024), threadCount);
        }
        
        private void checkForStuckThreads() {
            ThreadInfo[] threads = threadBean.dumpAllThreads(false, false);
            
            for (ThreadInfo thread : threads) {
                String threadName = thread.getThreadName();
                
                // Look for CRT-related threads
                if (threadName.contains("aws-crt") || 
                    threadName.contains("crt") ||
                    threadName.contains("s3-meta-request")) {
                    
                    Thread.State state = thread.getThreadState();
                    
                    // Check for problematic states
                    if (state == Thread.State.RUNNABLE || state == Thread.State.BLOCKED) {
                        String threadInfo = threadName + " (" + state + ")";
                        
                        if (!stuckThreads.contains(threadInfo)) {
                            stuckThreads.add(threadInfo);
                            crtThreadsStuck.set(true);
                            System.out.println("[MONITOR] Potentially stuck CRT thread: " + threadInfo);
                        }
                    }
                }
            }
        }
        
        StuckStateIndicators analyzeStuckState() {
            return new StuckStateIndicators(
                maxCpuUsage.get() > HIGH_CPU_THRESHOLD,
                maxMemoryUsage.get() > HIGH_MEMORY_THRESHOLD,
                maxThreadCount.get() > HIGH_THREAD_THRESHOLD,
                crtThreadsStuck.get(),
                highCpuCount.get() > 3 // High CPU for multiple monitoring cycles
            );
        }
        
        void printFinalReport() {
            System.out.println("=== Final System Monitoring Report ===");
            System.out.println("Max CPU Usage: " + maxCpuUsage.get() + "%");
            System.out.println("Max Memory Usage: " + (maxMemoryUsage.get() / 1024 / 1024) + "MB");
            System.out.println("Max Thread Count: " + maxThreadCount.get());
            System.out.println("High CPU Cycles: " + highCpuCount.get());
            System.out.println("CRT Threads Stuck: " + crtThreadsStuck.get());
            
            if (!stuckThreads.isEmpty()) {
                System.out.println("Stuck Threads Detected:");
                stuckThreads.forEach(thread -> System.out.println("  - " + thread));
            }
        }
    }

    /**
     * Data class to hold stuck state analysis results
     */
    private static class StuckStateIndicators {
        private final boolean highCpu;
        private final boolean memoryIssues;
        private final boolean threadIssues;
        private final boolean crtThreadsStuck;
        private final boolean persistentHighCpu;
        
        StuckStateIndicators(boolean highCpu, boolean memoryIssues, boolean threadIssues, 
                           boolean crtThreadsStuck, boolean persistentHighCpu) {
            this.highCpu = highCpu;
            this.memoryIssues = memoryIssues;
            this.threadIssues = threadIssues;
            this.crtThreadsStuck = crtThreadsStuck;
            this.persistentHighCpu = persistentHighCpu;
        }
        
        boolean isStuckStateDetected() {
            // Stuck state is indicated by multiple symptoms
            return (highCpu && crtThreadsStuck) || 
                   (persistentHighCpu && threadIssues) ||
                   (crtThreadsStuck && memoryIssues);
        }
        
        boolean hasHighCpu() { return highCpu; }
        boolean hasMemoryIssues() { return memoryIssues; }
        boolean hasThreadIssues() { return threadIssues; }
        boolean hasCrtThreadsStuck() { return crtThreadsStuck; }
        
        @Override
        public String toString() {
            return String.format("StuckStateIndicators{highCpu=%s, memoryIssues=%s, threadIssues=%s, " +
                               "crtThreadsStuck=%s, persistentHighCpu=%s}", 
                               highCpu, memoryIssues, threadIssues, crtThreadsStuck, persistentHighCpu);
        }
    }
}
