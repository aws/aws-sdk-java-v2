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

package software.amazon.awssdk.core.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Tests the static {@link SdkWarmUp#prime()} entry point end to end through {@link java.util.ServiceLoader},
 * using a test-scoped {@code META-INF/services} registration of {@link RegisteredWarmUpProvider}. {@code prime()}
 * runs at most once per JVM, so many concurrent calls must invoke the provider exactly once in total.
 */
class SdkWarmUpTest {

    private String savedRegionProperty;

    @BeforeEach
    void setup() {
        // Dummy region so prime()'s HTTP warm-up resolves a non-existent STS host and fails DNS immediately, keeping the test offline.
        savedRegionProperty = System.getProperty("aws.region");
        System.setProperty("aws.region", "warmup-unit-test");
        RegisteredWarmUpProvider.INVOCATIONS.set(0);
        RegisteredWarmUpProvider.WARMED_CLIENTS.clear();
    }

    @AfterEach
    void teardown() {
        if (savedRegionProperty != null) {
            System.setProperty("aws.region", savedRegionProperty);
        } else {
            System.clearProperty("aws.region");
        }
    }

    @Test
    void prime_concurrentCalls_invokeRegisteredProviderExactlyOnce() throws InterruptedException {
        int threadCount = 16;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                    SdkWarmUp.prime();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        start.countDown();
        done.await();
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(RegisteredWarmUpProvider.INVOCATIONS.get()).isEqualTo(1);
    }

    @Test
    void prime_withMatchingSyncClient_warmsSyncClientTypeThroughWarmUpClient() {
        SdkWarmUp.prime(RegisteredSyncClient.class);

        // Targeted prime warms via warmUpClient() only; the full warmUp() path (counted by INVOCATIONS) must not run.
        assertThat(RegisteredWarmUpProvider.INVOCATIONS.get()).isEqualTo(0);
        assertThat(RegisteredWarmUpProvider.WARMED_CLIENTS).containsExactly(ClientType.SYNC);
    }

    @Test
    void prime_withMatchingAsyncClient_warmsAsyncClientTypeThroughWarmUpClient() {
        SdkWarmUp.prime(RegisteredAsyncClient.class);

        assertThat(RegisteredWarmUpProvider.INVOCATIONS.get()).isEqualTo(0);
        assertThat(RegisteredWarmUpProvider.WARMED_CLIENTS).containsExactly(ClientType.ASYNC);
    }

    @Test
    void prime_withUnmatchedClient_doesNotThrow() {
        assertThatCode(() -> SdkWarmUp.prime(UnmatchedClient.class)).doesNotThrowAnyException();
    }

    @Test
    void prime_withUnmatchedClient_warnsOnEveryCallAndIsRetried() {
        // An unmatched client is not recorded as primed, so every call warns and retries.
        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            SdkWarmUp.prime(RetriedUnmatchedClient.class);
            SdkWarmUp.prime(RetriedUnmatchedClient.class);

            long unmatchedWarns = logCaptor.loggedEvents().stream()
                .filter(event -> event.getLevel() == Level.WARN
                                 && event.getMessage().getFormattedMessage()
                                         .contains(RetriedUnmatchedClient.class.getName()))
                .count();
            assertThat(unmatchedWarns).isEqualTo(2);
        }
    }

    @Test
    void prime_withEmptyArray_isNoOp() {
        assertThatCode(() -> SdkWarmUp.prime(new Class[0])).doesNotThrowAnyException();
    }

    interface UnmatchedClient extends SdkClient {
    }

    interface RetriedUnmatchedClient extends SdkClient {
    }
}
