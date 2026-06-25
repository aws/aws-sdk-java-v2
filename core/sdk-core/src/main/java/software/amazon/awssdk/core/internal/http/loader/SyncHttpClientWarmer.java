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
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.internal.crac.RegionEndpointResolver;
import software.amazon.awssdk.core.internal.crac.WarmUpDiscovery;
import software.amazon.awssdk.core.internal.crac.WarmUpRequest;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Warms every sync {@link SdkHttpService} on the classpath for CRaC priming: builds each client and sends a best-effort
 * {@link WarmUpRequest} to the resolved STS endpoint, draining the response body, so the HTTP/DNS/TLS/cert-chain code is
 * JIT-compiled into the snapshot.
 *
 * <p>Lives in this package because the {@link SdkServiceLoader} it reuses is package-private.
 */
@SdkInternalApi
public final class SyncHttpClientWarmer implements HttpClientWarmer {

    private static final Logger log = Logger.loggerFor(SyncHttpClientWarmer.class);

    private final SdkServiceLoader serviceLoader;
    private final Supplier<URI> endpointProvider;
    private final WarmUpRequest warmUpRequest;

    @SdkTestInternalApi
    SyncHttpClientWarmer(SdkServiceLoader serviceLoader, Supplier<URI> endpointProvider, WarmUpRequest warmUpRequest) {
        this.serviceLoader = serviceLoader;
        this.endpointProvider = endpointProvider;
        this.warmUpRequest = warmUpRequest;
    }

    /**
     * Discovers sync HTTP clients from the classpath as usual, but warms them against {@code endpointProvider} instead of the
     * resolved STS endpoint. Lets a caller redirect the warm-up request (e.g. to a local mock server).
     */
    public SyncHttpClientWarmer(Supplier<URI> endpointProvider) {
        this(SdkServiceLoader.INSTANCE, endpointProvider, WarmUpRequest.get());
    }

    public static SyncHttpClientWarmer create() {
        return new SyncHttpClientWarmer(SdkServiceLoader.INSTANCE,
                                        () -> RegionEndpointResolver.create().stsEndpoint(),
                                        WarmUpRequest.get());
    }

    @Override
    public void warmAll() {
        URI endpoint = endpointProvider.get();
        WarmUpDiscovery.forEachDiscovered(serviceLoader.loadServices(SdkHttpService.class), service -> {
            SdkHttpClient client = service.createHttpClientBuilder().buildWithDefaults(AttributeMap.empty());
            warmClient(client, endpoint);
        });
    }

    /**
     * Sends the {@link WarmUpRequest} to {@code endpoint}, drains the response body (which warms the read and decode path),
     * and closes the client. Never throws; the goal is JIT compilation, not a successful request.
     */
    private void warmClient(SdkHttpClient client, URI endpoint) {
        try {
            HttpExecuteRequest request = HttpExecuteRequest.builder()
                                                           .request(warmUpRequest.toHttpRequest(endpoint))
                                                           .contentStreamProvider(warmUpRequest.contentStreamProvider()
                                                                                               .orElse(null))
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
