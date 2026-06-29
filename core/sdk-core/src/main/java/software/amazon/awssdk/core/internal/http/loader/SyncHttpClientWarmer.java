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

package software.amazon.awssdk.core.internal.http.loader;

import java.net.URI;
import java.util.Collections;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.internal.crac.RegionEndpointResolver;
import software.amazon.awssdk.core.internal.crac.WarmUpDiscovery;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Warms every sync {@link SdkHttpService} on the classpath for CRaC priming: builds each client and sends a best-effort
 * {@code GET} to the resolved STS endpoint, draining the response body, so the HTTP/DNS/TLS/cert-chain code is JIT-compiled
 * into the snapshot.
 */
@SdkInternalApi
public final class SyncHttpClientWarmer implements HttpClientWarmer {

    private static final Logger log = Logger.loggerFor(SyncHttpClientWarmer.class);

    private final Iterable<SdkHttpService> services;
    private final Supplier<URI> endpointProvider;

    @SdkTestInternalApi
    SyncHttpClientWarmer(Iterable<SdkHttpService> services, Supplier<URI> endpointProvider) {
        this.services = services;
        this.endpointProvider = endpointProvider;
    }

    /**
     * Warms a single {@code service} against {@code endpointProvider}.
     */
    @SdkTestInternalApi
    public static SyncHttpClientWarmer forService(Supplier<URI> endpointProvider, SdkHttpService service) {
        return new SyncHttpClientWarmer(Collections.singletonList(service), endpointProvider);
    }

    public static SyncHttpClientWarmer create() {
        return new SyncHttpClientWarmer(discoverServices(), () -> RegionEndpointResolver.create().endpoint());
    }

    /**
     * Like {@link #create()}, but warms against {@code endpointProvider}.
     */
    @SdkTestInternalApi
    public static SyncHttpClientWarmer create(Supplier<URI> endpointProvider) {
        return new SyncHttpClientWarmer(discoverServices(), endpointProvider);
    }

    private static Iterable<SdkHttpService> discoverServices() {
        return () -> SdkServiceLoader.INSTANCE.loadServices(SdkHttpService.class);
    }

    @Override
    public void warmAll() {
        URI endpoint = endpointProvider.get();
        WarmUpDiscovery.forEachDiscovered(services.iterator(), service -> {
            SdkHttpClient client = service.createHttpClientBuilder().buildWithDefaults(AttributeMap.empty());
            warmClient(client, endpoint);
        });
    }

    /**
     * Sends the warm-up {@code GET} to {@code endpoint}, drains the response body, and closes the client. Best-effort: the
     * goal is JIT compilation, not a successful request, so any failure is logged and swallowed.
     */
    private void warmClient(SdkHttpClient client, URI endpoint) {
        try {
            SdkHttpRequest httpRequest = SdkHttpRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(endpoint)
                                                       .build();
            HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                           .request(httpRequest)
                                                           .build();
            ExecutableHttpRequest executableRequest = client.prepareRequest(request);
            HttpExecuteResponse response = executableRequest.call();
            response.responseBody().ifPresent(body -> {
                try {
                    IoUtils.drainInputStream(body);
                } finally {
                    IoUtils.closeQuietlyV2(body, log);
                }
            });
        } catch (Exception e) {
            log.debug(() -> "Sync HTTP client warm-up call failed (ignored).", e);
        } finally {
            IoUtils.closeQuietlyV2(client, log);
        }
    }
}
