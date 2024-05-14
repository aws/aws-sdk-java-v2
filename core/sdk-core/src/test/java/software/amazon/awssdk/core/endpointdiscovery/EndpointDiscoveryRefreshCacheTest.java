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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
        assertThrows(CancellationException.class, () -> future.get());

    }

}
