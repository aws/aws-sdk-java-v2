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

package software.amazon.awssdk.retries.internal.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;

public class RateLimiterTokenBucketStoreTest {
    @Test
    void close_schedulerProvided_schedulerNotClosed() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        RateLimiterTokenBucketStore store = RateLimiterTokenBucketStore.builder()
                                                                       .clock(new SystemClock())
                                                                       .scheduler(scheduler)
                                                                       .build();

        store.close();

        verify(scheduler, never()).shutdownNow();
        verify(scheduler, never()).shutdown();
    }

    @Test
    void close_schedulerNotProvidedOnBuilder_schedulerClosed() {
        RateLimiterTokenBucketStore store = RateLimiterTokenBucketStore.builder()
                                                                       .clock(new SystemClock())
                                                                       .build();

        store.close();

        assertThat(store.scheduler().isShutdown()).isTrue();
    }

    @Test
    void close_closesAllCacheEntries() {
        int entries = 64;
        List<RateLimiterTokenBucket> buckets = new ArrayList<>(entries);

        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        RateLimiterTokenBucketStore store = RateLimiterTokenBucketStore.builder()
                                                                       .clock(new SystemClock())
                                                                       .scheduler(scheduler)
                                                                       .build();

        List<CompletableFuture<Void>> futures = new ArrayList<>(entries);

        for (int i = 0; i < entries; ++i) {
            RateLimiterTokenBucket bucket = store.tokenBucketForScope(Integer.toString(i));
            buckets.add(bucket);

            // enable throttling so futures from acquireAsync get queued
            bucket.updateRateAfterThrottling();
            futures.add(bucket.acquireAsync());
        }

        store.close();

        // New acquires from the closed bucket should fail
        assertThat(buckets).allSatisfy(b -> assertThatThrownBy(b.acquireAsync()::join)
            .hasMessageContaining("Rate limiter bucket is closed"));

        // All pending futures should be failed
        assertThat(futures).allSatisfy(cf -> assertThatThrownBy(cf::join)
            .hasMessageContaining("Rate limiter bucket is closed"));
    }
}
