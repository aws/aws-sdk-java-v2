/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class EndpointDiscoveryRefreshCache {

    private static final Logger log = Logger.loggerFor(EndpointDiscoveryRefreshCache.class);

    private final Map<String, EndpointDiscoveryEndpoint> cache = new ConcurrentHashMap<>();

    private final EndpointDiscoveryCacheLoader client;

    private EndpointDiscoveryRefreshCache(EndpointDiscoveryCacheLoader client) {
        this.client = client;
    }

    public static EndpointDiscoveryRefreshCache create(EndpointDiscoveryCacheLoader client) {
        return new EndpointDiscoveryRefreshCache(client);
    }

    /**
     * Abstract method to be implemented by each service to handle retrieving
     * endpoints from a cache. Each service must handle converting a request
     * object into the relevant cache key.
     *
     * @return The endpoint to use for this request
     */
    public URI get(String accessKey, EndpointDiscoveryRequest request) {
        String key = accessKey;
        if (request.cacheKey().isPresent()) {
            key = key + ":" + request.cacheKey().get();
        }

        EndpointDiscoveryEndpoint endpoint = cache.get(key);

        if (endpoint == null) {
            if (request.required()) {
                return cache.computeIfAbsent(key, k -> discoverEndpoint(request).join()).endpoint();
            } else {
                EndpointDiscoveryEndpoint tempEndpoint = EndpointDiscoveryEndpoint.builder()
                                                                                  .endpoint(request.defaultEndpoint())
                                                                                  .expirationTime(Instant.now().plusSeconds(60))
                                                                                  .build();
                return cache.computeIfAbsent(key, k -> tempEndpoint).endpoint();
            }
        }

        if (endpoint.expirationTime().isBefore(Instant.now())) {
            cache.put(key, endpoint.toBuilder().expirationTime(Instant.now().plusSeconds(60)).build());
            String finalKey = key;
            discoverEndpoint(request).thenApply(v -> cache.put(finalKey, v));
        }

        return endpoint.endpoint();
    }

    public CompletableFuture<EndpointDiscoveryEndpoint> discoverEndpoint(EndpointDiscoveryRequest request) {
        return client.discoverEndpoint(request);
    }

    public void evict(String key) {
        cache.remove(key);
    }
}
