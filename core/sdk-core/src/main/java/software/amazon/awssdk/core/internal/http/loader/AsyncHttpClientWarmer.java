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
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.internal.crac.RegionEndpointResolver;
import software.amazon.awssdk.core.internal.crac.WarmUpDiscovery;
import software.amazon.awssdk.core.internal.http.async.SimpleHttpContentPublisher;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.async.SimpleSubscriber;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Warms every async {@link SdkAsyncHttpService} on the classpath for CRaC priming: builds each client and sends a best-effort
 * {@code GET} to the resolved STS endpoint, draining the reactive response body, so the HTTP/DNS/TLS/cert-chain code is
 * JIT-compiled into the snapshot.
 */
@SdkInternalApi
public final class AsyncHttpClientWarmer implements HttpClientWarmer {

    private static final Logger log = Logger.loggerFor(AsyncHttpClientWarmer.class);

    private static final long WARM_UP_TIMEOUT_SECONDS = 5;

    private final Iterable<SdkAsyncHttpService> services;
    private final Supplier<URI> endpointProvider;

    @SdkTestInternalApi
    AsyncHttpClientWarmer(Iterable<SdkAsyncHttpService> services, Supplier<URI> endpointProvider) {
        this.services = services;
        this.endpointProvider = endpointProvider;
    }

    /**
     * Warms a single {@code service} against {@code endpointProvider}.
     */
    @SdkTestInternalApi
    public static AsyncHttpClientWarmer forService(Supplier<URI> endpointProvider, SdkAsyncHttpService service) {
        return new AsyncHttpClientWarmer(Collections.singletonList(service), endpointProvider);
    }

    public static AsyncHttpClientWarmer create() {
        return new AsyncHttpClientWarmer(discoverServices(), () -> RegionEndpointResolver.create().endpoint());
    }

    /**
     * Like {@link #create()}, but warms against {@code endpointProvider}.
     */
    @SdkTestInternalApi
    public static AsyncHttpClientWarmer create(Supplier<URI> endpointProvider) {
        return new AsyncHttpClientWarmer(discoverServices(), endpointProvider);
    }

    private static Iterable<SdkAsyncHttpService> discoverServices() {
        return () -> SdkServiceLoader.INSTANCE.loadServices(SdkAsyncHttpService.class);
    }

    @Override
    public void warmAll() {
        URI endpoint = endpointProvider.get();
        WarmUpDiscovery.forEachDiscovered(services.iterator(), service -> {
            SdkAsyncHttpClient client = service.createAsyncHttpClientFactory().buildWithDefaults(AttributeMap.empty());
            warmClient(client, endpoint);
        });
    }

    /**
     * Sends the warm-up {@code GET} to {@code endpoint}, drains the response body, and closes the client. Best-effort: any
     * failure or timeout is logged and swallowed. We block on the execute future (bounded) because the bundled async
     * clients complete it only after the body is drained, so its completion implies the full path was exercised.
     */
    private void warmClient(SdkAsyncHttpClient client, URI endpoint) {
        try {
            SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                               .method(SdkHttpMethod.GET)
                                                               .uri(endpoint)
                                                               .build();
            AsyncExecuteRequest request = AsyncExecuteRequest.builder()
                                                             .request(httpRequest)
                                                             .requestContentPublisher(new SimpleHttpContentPublisher(httpRequest))
                                                             .responseHandler(new WarmUpResponseHandler())
                                                             .build();
            client.execute(request).get(WARM_UP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug(() -> "Async HTTP client warm-up call was interrupted (ignored).", e);
        } catch (Exception e) {
            // Includes ExecutionException for a failed warm-up request; best-effort, so swallow.
            log.debug(() -> "Async HTTP client warm-up call failed (ignored).", e);
        } finally {
            IoUtils.closeQuietlyV2(client, log);
        }
    }

    /**
     * Subscribes a {@link SimpleSubscriber} to drain and discard the response body, exercising the body-read/TLS path.
     * The subscription is required: some clients complete the execute future only once the body is consumed.
     */
    private static final class WarmUpResponseHandler implements SdkAsyncHttpResponseHandler {

        @Override
        public void onHeaders(SdkHttpResponse headers) {
            // No-op: warm-up only needs the body drained, not the headers.
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new SimpleSubscriber(byteBuffer -> {
                // Discard the bytes; warm-up only needs the path exercised.
            }));
        }

        @Override
        public void onError(Throwable error) {
            // No-op: failure is surfaced via the execute future, which the caller blocks on.
        }
    }
}
