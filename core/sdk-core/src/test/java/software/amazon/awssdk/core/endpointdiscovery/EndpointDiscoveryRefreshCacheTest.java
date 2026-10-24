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

package software.amazon.awssdk.core.endpointdiscovery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EndpointDiscoveryRefreshCacheTest {

    private EndpointDiscoveryRefreshCache endpointDiscoveryRefreshCache;
    private EndpointDiscoveryCacheLoader mockClient;
    private static final URI testURI = URI.create("test_endpoint");
    private static final String requestCacheKey = "request_cache_key";
    private static final String accessKey = "access_cache_key";

    @BeforeEach
    public void setup() {
         this.mockClient= mock(EndpointDiscoveryCacheLoader.class);
         this.endpointDiscoveryRefreshCache = EndpointDiscoveryRefreshCache.create(mockClient);
    }

    @Test
    public void getAsync_notRequired_returns_CompletedFuture() throws ExecutionException, InterruptedException {
        when(mockClient.discoverEndpoint(any())).thenReturn(new CompletableFuture<>());
        EndpointDiscoveryRequest request = EndpointDiscoveryRequest.builder()
                                                                   .required(false)
                                                                   .defaultEndpoint(testURI)
                                                                   .build();
        assertThat(endpointDiscoveryRefreshCache.getAsync("key", request).isDone()).isEqualTo(true);
        assertThat(endpointDiscoveryRefreshCache.getAsync("key", request).get()).isEqualTo(testURI);

    }

    @Test
    public void getAsync_returns_CompletedFuture() throws ExecutionException, InterruptedException {

        when(mockClient.discoverEndpoint(any())).thenReturn(new CompletableFuture<>());
        EndpointDiscoveryRequest request = EndpointDiscoveryRequest.builder()
                                                                   .required(true)
                                                                   .defaultEndpoint(testURI)
                                                                   .build();
        CompletableFuture<URI> future = endpointDiscoveryRefreshCache.getAsync("key", request);
        assertThat(future.isDone()).isEqualTo(false);

        future.complete(testURI);

        assertThat(future.isDone()).isEqualTo(true);
        assertThat(future.get()).isEqualTo(testURI);
    }

    @Test
    public void getAsync_future_cancelled() {

        when(mockClient.discoverEndpoint(any())).thenReturn(new CompletableFuture<>());
        EndpointDiscoveryRequest request = EndpointDiscoveryRequest.builder()
                                                                   .required(true)
                                                                   .defaultEndpoint(testURI)
                                                                   .build();
        CompletableFuture<URI> future = endpointDiscoveryRefreshCache.getAsync("key", request);
        assertThat(future.isDone()).isEqualTo(false);

        future.cancel(true);
        assertThat(future.isCancelled()).isEqualTo(true);
        assertThatThrownBy(future::get).isInstanceOf(CancellationException.class);

    }

    @Test
    public void get_concurrentCallsOnExpiredEntry_onlyRefreshesOnce() throws Exception {
        // Regression test: returnCachedOrDefaultEndpoint used to check-then-act on an expired
        // cache entry (isBefore(now) followed by an unguarded cache.put) with no synchronization,
        // unlike the sibling "no cached entry yet" branch a few lines above it, which correctly
        // uses cache.putIfAbsent as a compare-and-swap. Every thread that read the same expired
        // entry before any of them wrote back independently decided to kick off a background
        // refresh, causing duplicate discoverEndpoint calls under concurrent load right when an
        // entry expires. There should only ever be exactly one refresh call per expiration.
        AtomicInteger discoveryCalls = new AtomicInteger(0);
        when(mockClient.discoverEndpoint(any())).thenAnswer(invocation -> {
            discoveryCalls.incrementAndGet();
            return CompletableFuture.completedFuture(
                EndpointDiscoveryEndpoint.builder()
                                          .endpoint(testURI)
                                          .expirationTime(Instant.now().plusSeconds(60))
                                          .build());
        });

        EndpointDiscoveryRequest request = EndpointDiscoveryRequest.builder()
                                                                     .required(false)
                                                                     .cacheKey(requestCacheKey)
                                                                     .defaultEndpoint(testURI)
                                                                     .build();

        // Prime the cache with an already-expired entry, simulating the moment right after a
        // real cache entry expires under concurrent load.
        Field cacheField = EndpointDiscoveryRefreshCache.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, EndpointDiscoveryEndpoint> internalCache =
            (Map<String, EndpointDiscoveryEndpoint>) cacheField.get(endpointDiscoveryRefreshCache);
        internalCache.put(accessKey + ":" + requestCacheKey,
                           EndpointDiscoveryEndpoint.builder()
                                                     .endpoint(URI.create("stale_endpoint"))
                                                     .expirationTime(Instant.now().minusSeconds(1))
                                                     .build());

        int threadCount = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                endpointDiscoveryRefreshCache.get(accessKey, request);
            });
        }
        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(discoveryCalls.get()).isEqualTo(1);
    }

}
