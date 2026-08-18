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

package software.amazon.awssdk.services.s3.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.internal.multipart.MultipartDownloadResumeContext;

/**
 * Tests that verify thread-safety of {@link MultipartDownloadResumeContext} when accessed concurrently,
 * as happens in the parallel multipart download path.
 */
class MultipartDownloadResumeContextConcurrencyTest {

    private static final int NUM_THREADS = 32;
    private static final int TOTAL_PARTS = 1000;

    @Test
    void addCompletedPart_concurrentAccess_doesNotCorruptState() throws Exception {
        MultipartDownloadResumeContext context = new MultipartDownloadResumeContext();
        context.totalParts(TOTAL_PARTS);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= TOTAL_PARTS; i++) {
            int partNumber = i;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                context.addCompletedPart(partNumber);
            }));
        }

        // Release all threads simultaneously to maximize contention
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertThat(context.completedParts()).hasSize(TOTAL_PARTS);
        assertThat(context.isComplete()).isTrue();
        assertThat(context.highestSequentialCompletedPart()).isEqualTo(TOTAL_PARTS);
    }

    @Test
    void addToBytesToLastCompletedParts_concurrentAccess_correctSum() throws Exception {
        MultipartDownloadResumeContext context = new MultipartDownloadResumeContext();
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        long bytesPerPart = 8 * 1024 * 1024; // 8 MiB
        for (int i = 0; i < TOTAL_PARTS; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                context.addToBytesToLastCompletedParts(bytesPerPart);
            }));
        }

        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertThat(context.bytesToLastCompletedParts()).isEqualTo(bytesPerPart * TOTAL_PARTS);
    }
}
