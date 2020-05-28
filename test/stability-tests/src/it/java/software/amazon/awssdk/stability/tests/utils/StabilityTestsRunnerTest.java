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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.utils.Logger;

/**
 * Tests of the tests
 */
public class StabilityTestsRunnerTest {
    private static final Logger LOGGER = Logger.loggerFor("RunnerTest");
    private static ExecutorService executors;

    @BeforeAll
    public static void setUp() {
        executors = Executors.newFixedThreadPool(10);
    }

    @AfterAll
    public static void tearDown() {
        executors.shutdown();
    }

    @Test
    public void testUsingFutureFactory() {
        StabilityTestRunner.newRunner()
                           .futureFactory(i -> CompletableFuture.runAsync(() -> LOGGER.debug(() ->
                                                                                                 "hello world " + i), executors))
                           .testName("test")
                           .requestCountPerRun(10)
                           .delaysBetweenEachRun(Duration.ofMillis(500))
                           .totalRuns(5)
                           .run();
    }

    @Test
    public void testUsingFutures() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            futures.add(CompletableFuture.runAsync(() -> LOGGER.debug(() -> "hello world " + finalI),
                                                   executors));
        }

        StabilityTestRunner.newRunner()
                           .futures(futures)
                           .testName("test")
                           .run();
    }

    @Test
    public void unexpectedExceptionThrown_shouldFailTests() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(CompletableFuture.runAsync(() -> {
            throw new RuntimeException("boom");
        }, executors));
        futures.add(CompletableFuture.runAsync(() -> LOGGER.debug(() -> "hello world "),
                                               executors));
        assertThatThrownBy(() ->
                               StabilityTestRunner.newRunner()
                                                  .futures(futures)
                                                  .testName("test")
                                                  .run()).hasMessageContaining("unknown exceptions were thrown");
    }

    @Test
    public void expectedExceptionThrownExceedsThreshold_shouldFailTests() {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            int finalI = i;

            if (i < 3) {
                futures.add(CompletableFuture.runAsync(() -> {
                    throw SdkServiceException.builder().message("boom").build();
                }, executors));
            } else {
                futures.add(CompletableFuture.runAsync(() -> LOGGER.debug(() -> "hello world " + finalI),
                                                       executors));
            }
        }

        assertThatThrownBy(() ->
                               StabilityTestRunner.newRunner()
                                                  .futures(futures)
                                                  .testName("test")
                                                  .run())
            .hasMessageContaining("failed of SdkServiceException or IOException");
    }

    @Test
    public void expectedExceptionThrownNotExceedsThreshold_shouldSucceed() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(CompletableFuture.runAsync(() -> {
            throw SdkServiceException.builder().message("boom").build();
        }, executors));

        for (int i = 1; i < 20; i++) {
            futures.add(CompletableFuture.runAsync(() -> LOGGER.debug(() -> "hello world "),
                                                   executors));
        }

        StabilityTestRunner.newRunner()
                           .futures(futures)
                           .testName("test")
                           .run();
    }

    @Test
    public void sdkClientExceptionsThrown_shouldFail() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(CompletableFuture.runAsync(() -> {
            throw SdkClientException.builder().message("boom").build();
        }, executors));
        futures.add(CompletableFuture.runAsync(() -> LOGGER.debug(() -> "hello world "),
                                               executors));
        assertThatThrownBy(() ->
                               StabilityTestRunner.newRunner()
                                                  .futures(futures)
                                                  .testName("test")
                                                  .run()).hasMessageContaining("SdkClientExceptions were thrown");
    }

}
