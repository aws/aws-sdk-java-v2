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

package software.amazon.awssdk.stability.tests.utils;


import static java.lang.management.MemoryType.HEAP;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.stability.tests.ExceptionCounter;
import software.amazon.awssdk.stability.tests.TestResult;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * Stability tests runner.
 *
 * There are two ways to run the tests:
 *
 * - providing futureFactories requestCountPerRun and totalRuns. eg:
 *
 * StabilityTestRunner.newRunner()
 *                    .futureFactory(i -> CompletableFuture.runAsync(() -> LOGGER.info(() ->
 *                      "hello world " + i), executors))
 *                    .testName("test")
 *                    .requestCountPerRun(10)
 *                    .delaysBetweenEachRun(Duration.ofMillis(1000))
 *                    .totalRuns(5)
 *                    .run();
 *
 * The tests runner will create futures from the factories and run the 10 requests per run for 5 runs with 1s delay between
 * each run.
 *
 * - providing futures, eg:
 *
 *         StabilityTestRunner.newRunner()
 *                            .futures(futures)
 *                            .testName("test")
 *                            .run();
 * The tests runner will run the given futures in one run.
 */
public class StabilityTestRunner {

    private static final Logger log = Logger.loggerFor(StabilityTestRunner.class);
    private static final double ALLOWED_FAILURE_RATIO = 0.05;
    private static final int TESTS_TIMEOUT_IN_MINUTES = 60;
    // The peak thread count might be different depending on the machine the tests are currently running on.
    // because of the internal thread pool used in AsynchronousFileChannel
    // Also, synchronous clients have their own thread pools so this measurement needs to be mutable
    // so that the async and synchronous paths can both use this runner.
    private int allowedPeakThreadCount = 90;

    private ThreadMXBean threadMXBean;
    private IntFunction<CompletableFuture<?>> futureFactory;
    private List<CompletableFuture<?>> futures;
    private String testName;
    private Duration delay = Duration.ZERO;
    private Integer requestCountPerRun;
    private Integer totalRuns = 1;


    private StabilityTestRunner() {
        threadMXBean = ManagementFactory.getThreadMXBean();
        // Reset peak thread count for every test
        threadMXBean.resetPeakThreadCount();
    }

    private StabilityTestRunner(int allowedPeakThreadCount) {
        this();
        this.allowedPeakThreadCount = allowedPeakThreadCount;
    }

    /**
     * Create a new test runner
     *
     * @return a new test runner
     */
    public static StabilityTestRunner newRunner() {
        return new StabilityTestRunner();
    }

    public static StabilityTestRunner newRunner(int allowedPeakThreadCount) {
        return new StabilityTestRunner(allowedPeakThreadCount);
    }

    public StabilityTestRunner futureFactory(IntFunction<CompletableFuture<?>> futureFactory) {
        this.futureFactory = futureFactory;
        return this;
    }

    public StabilityTestRunner futures(List<CompletableFuture<?>> futures) {
        this.futures = futures;
        return this;
    }

    public StabilityTestRunner futures(CompletableFuture<?>... futures) {
        this.futures = Arrays.asList(futures);
        return this;
    }

    /**
     * The test name to generate the test report.
     *
     * @param testName the name of the report
     * @return StabilityTestRunner
     */
    public StabilityTestRunner testName(String testName) {
        this.testName = testName;
        return this;
    }

    /**
     * @param delay delay between each run
     * @return StabilityTestRunner
     */
    public StabilityTestRunner delaysBetweenEachRun(Duration delay) {
        this.delay = delay;
        return this;
    }

    /**
     * @param runs total runs
     * @return StabilityTestRunner
     */
    public StabilityTestRunner totalRuns(Integer runs) {
        this.totalRuns = runs;
        return this;
    }

    /**
     * @param requestCountPerRun the number of request per run
     * @return StabilityTestRunner
     */
    public StabilityTestRunner requestCountPerRun(Integer requestCountPerRun) {
        this.requestCountPerRun = requestCountPerRun;
        return this;
    }

    /**
     * Run the tests based on the parameters provided
     */
    public void run() {
        validateParameters();
        TestResult result;

        if (futureFactory != null) {
            result = runTestsFromFutureFunction();
        } else {
            result = runTestsFromFutures();
        }

        processResult(result);
    }

    private TestResult runTestsFromFutureFunction() {
        Validate.notNull(requestCountPerRun, "requestCountPerRun cannot be null");
        Validate.notNull(totalRuns, "totalRuns cannot be null");

        ExceptionCounter exceptionCounter = new ExceptionCounter();
        int totalRequestNumber = requestCountPerRun * totalRuns;
        CompletableFuture[] completableFutures = new CompletableFuture[totalRequestNumber];
        int runNumber = 0;
        while (runNumber < totalRequestNumber) {

            for (int i = runNumber; i < runNumber + requestCountPerRun; i++) {
                CompletableFuture<?> future = futureFactory.apply(i);
                completableFutures[i] = handleException(future, exceptionCounter);
            }

            int finalRunNumber = runNumber;
            log.debug(() -> "Finishing one run " + finalRunNumber);
            runNumber += requestCountPerRun;
            addDelayIfNeeded();
        }
        return generateTestResult(totalRequestNumber, testName, exceptionCounter, completableFutures);
    }

    private double calculateHeapMemoryAfterGCUsage() {
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();

        long used = 0, max = 0;

        for (MemoryPoolMXBean memoryPoolMXBean : memoryPoolMXBeans) {
            String name = memoryPoolMXBean.getName();

            if (!name.contains("Eden")) {
                if (memoryPoolMXBean.getType().equals(HEAP)) {
                    MemoryUsage memoryUsage = memoryPoolMXBean.getCollectionUsage();
                    used += memoryUsage.getUsed();
                    max += memoryUsage.getMax() == -1 ? 0 : memoryUsage.getMax();
                }
            }
        }

        return used / (double) max;
    }

    private TestResult runTestsFromFutures() {
        ExceptionCounter exceptionCounter = new ExceptionCounter();
        CompletableFuture[] completableFutures =
            futures.stream().map(b -> handleException(b, exceptionCounter)).toArray(CompletableFuture[]::new);
        return generateTestResult(futures.size(), testName, exceptionCounter, completableFutures);
    }

    private void validateParameters() {
        Validate.notNull(testName, "testName cannot be null");
        Validate.isTrue(futureFactory == null || futures == null, "futureFactory and futures cannot be both configured");
    }

    private void addDelayIfNeeded() {
        log.debug(() -> "Sleeping for " + delay.toMillis());
        if (!delay.isZero()) {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                // Ignoring exception
            }
        }
    }

    /**
     * Handle the exceptions of executing the futures.
     *
     * @param future the future to be executed
     * @param exceptionCounter the exception counter
     * @return the completable future
     */
    private static CompletableFuture<?> handleException(CompletableFuture<?> future, ExceptionCounter exceptionCounter) {

        return future.exceptionally(t -> {
            Throwable cause = t.getCause();
            log.error(() -> "An exception was thrown ", t);
            if (cause instanceof SdkServiceException) {
                exceptionCounter.addServiceException();
            } else if (isIOExceptionOrHasIOCause(cause)) {
                exceptionCounter.addIoException();
            } else if (cause instanceof SdkClientException) {
                if (isIOExceptionOrHasIOCause(cause.getCause())) {
                    exceptionCounter.addIoException();
                } else {
                    exceptionCounter.addClientException();
                }
            } else {
                exceptionCounter.addUnknownException();
            }
            return null;
        });
    }

    private static boolean isIOExceptionOrHasIOCause(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        return throwable instanceof IOException || throwable.getCause() instanceof IOException;
    }

    private TestResult generateTestResult(int totalRequestNumber, String testName, ExceptionCounter exceptionCounter,
                                                 CompletableFuture[] completableFutures) {
        try {
            CompletableFuture.allOf(completableFutures).get(TESTS_TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error occurred running the tests: " + testName, e);
        } catch (TimeoutException e) {
            throw new RuntimeException(String.format("Tests (%s) did not finish within %s minutes", testName,
                                                     TESTS_TIMEOUT_IN_MINUTES));
        }

        return TestResult.builder()
                         .testName(testName)
                         .clientExceptionCount(exceptionCounter.clientExceptionCount())
                         .serviceExceptionCount(exceptionCounter.serviceExceptionCount())
                         .ioExceptionCount(exceptionCounter.ioExceptionCount())
                         .totalRequestCount(totalRequestNumber)
                         .unknownExceptionCount(exceptionCounter.unknownExceptionCount())
                         .peakThreadCount(threadMXBean.getPeakThreadCount())
                         .heapMemoryAfterGCUsage(calculateHeapMemoryAfterGCUsage())
                         .build();
    }

    /**
     * Process the result. Throws exception if SdkClientExceptions were thrown or 5% requests failed of
     * SdkServiceException or IOException.
     *
     * @param testResult the result to process.
     */
    private void processResult(TestResult testResult) {
        log.info(() -> "TestResult: " + testResult);

        int clientExceptionCount = testResult.clientExceptionCount();

        int expectedExceptionCount = testResult.ioExceptionCount() + testResult.serviceExceptionCount();

        int unknownExceptionCount = testResult.unknownExceptionCount();

        double ratio = expectedExceptionCount / (double) testResult.totalRequestCount();
        if (clientExceptionCount > 0) {
            String errorMessage = String.format("%s SdkClientExceptions were thrown, failing the tests",
                                          clientExceptionCount);
            log.error(() -> errorMessage);
            throw new AssertionError(errorMessage);
        }

        if (testResult.unknownExceptionCount() > 0) {
            String errorMessage = String.format("%s unknown exceptions were thrown, failing the tests",
                                          unknownExceptionCount);
            log.error(() -> errorMessage);
            throw new AssertionError(errorMessage);
        }

        if (ratio > ALLOWED_FAILURE_RATIO) {
            String errorMessage = String.format("More than %s percent requests (%s percent) failed of SdkServiceException "
                                          + "or IOException, failing the tests",
                                          ALLOWED_FAILURE_RATIO * 100, ratio * 100);
            throw new StabilityTestsRetryableException(errorMessage);
        }

        if (testResult.peakThreadCount() > allowedPeakThreadCount) {
            String errorMessage = String.format("The number of peak thread exceeds the allowed peakThread threshold %s",
                                                allowedPeakThreadCount);


            threadDump(testResult.testName());
            throw new AssertionError(errorMessage);
        }
    }

    private void threadDump(String testName) {
        StringBuilder threadDump = new StringBuilder("\n============").append(testName)
                                                                      .append(" Thread Dump:=============\n");
        ThreadInfo[] threadInfoList = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfoList) {
            threadDump.append('"');
            threadDump.append(threadInfo.getThreadName());
            threadDump.append("\":");
            threadDump.append(threadInfo.getThreadState());
            threadDump.append("\n");
        }
        threadDump.append("==================================");
        log.info(threadDump::toString);
    }
}
