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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

/**
 * Tests the static {@link SdkWarmUp#prime()} entry point end to end through {@link java.util.ServiceLoader},
 * using a test-scoped {@code META-INF/services} registration of {@link RegisteredWarmUpProvider}. {@code prime()}
 * runs at most once per JVM, so many concurrent calls must invoke the provider exactly once in total.
 */
class SdkWarmUpTest {

    @Test
    void prime_concurrentCalls_invokeRegisteredProviderExactlyOnce() throws InterruptedException {
        RegisteredWarmUpProvider.INVOCATIONS.set(0);
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
}
