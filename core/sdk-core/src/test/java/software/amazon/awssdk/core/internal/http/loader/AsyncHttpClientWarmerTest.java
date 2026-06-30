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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

/**
 * Unit tests for {@link AsyncHttpClientWarmer}. Every test drives the real {@link AsyncHttpClientWarmer#warmAll()} with an
 * injected list of stub {@link SdkAsyncHttpService}s and a fixed endpoint. Stubbed clients drive the response handler so the
 * drain path runs and the bounded wait settles immediately.
 */
class AsyncHttpClientWarmerTest {

    private static final URI ENDPOINT = URI.create("https://sts.us-east-1.amazonaws.com/");

    @Test
    void warmAll_whenResponseHasBody_drainsAndClosesIt() {
        AtomicBoolean drained = new AtomicBoolean(false);
        Publisher<ByteBuffer> body = bodyPublisher(drained, ByteBuffer.wrap("<Error>denied</Error>".getBytes()));
        SdkAsyncHttpClient client = stubClient(body);

        warmer(serviceFor(client)).warmAll();

        assertThat(drained).isTrue();  // subscribed, drained to completion
        verify(client).close();
    }

    @Test
    void warmAll_whenInvoked_issuesGetToResolvedEndpoint() {
        SdkAsyncHttpClient client = stubClient(emptyBody());
        ArgumentCaptor<AsyncExecuteRequest> request = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

        warmer(serviceFor(client)).warmAll();

        verify(client).execute(request.capture());
        assertThat(request.getValue().request().method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(request.getValue().request().getUri()).isEqualTo(ENDPOINT);
    }

    @Test
    void warmAll_whenRequestFails_swallowsAndStillClosesClient() {
        SdkAsyncHttpClient client = mock(SdkAsyncHttpClient.class);
        when(client.execute(any(AsyncExecuteRequest.class))).thenThrow(new RuntimeException("offline"));

        assertThatCode(() -> warmer(serviceFor(client)).warmAll()).doesNotThrowAnyException();
        verify(client).close();
    }

    @Test
    void warmAll_whenNoResponseBody_stillClosesClient() {
        SdkAsyncHttpClient client = stubClient(emptyBody());

        assertThatCode(() -> warmer(serviceFor(client)).warmAll()).doesNotThrowAnyException();
        verify(client).close();
    }

    @Test
    void warmAll_whenMultipleServicesDiscovered_warmsEach() {
        SdkAsyncHttpClient first = stubClient(emptyBody());
        SdkAsyncHttpClient second = stubClient(emptyBody());

        warmer(serviceFor(first), serviceFor(second)).warmAll();

        verify(first).execute(any(AsyncExecuteRequest.class));
        verify(second).execute(any(AsyncExecuteRequest.class));
    }

    @Test
    void warmAll_whenOneServiceFailsToBuild_stillWarmsOthers() {
        SdkAsyncHttpService failing = mock(SdkAsyncHttpService.class);
        when(failing.createAsyncHttpClientFactory()).thenThrow(new RuntimeException("bad service"));
        SdkAsyncHttpClient healthy = stubClient(emptyBody());

        warmer(failing, serviceFor(healthy)).warmAll();

        verify(healthy).execute(any(AsyncExecuteRequest.class));
    }

    @Test
    void warmAll_whenNoServices_isNoOp() {
        assertThatCode(() -> warmer().warmAll()).doesNotThrowAnyException();
    }

    private static AsyncHttpClientWarmer warmer(SdkAsyncHttpService... services) {
        return new AsyncHttpClientWarmer(Arrays.asList(services), () -> ENDPOINT);
    }

    /** A service whose builder yields the given client. */
    private static SdkAsyncHttpService serviceFor(SdkAsyncHttpClient client) {
        SdkAsyncHttpClient.Builder<?> builder = mock(SdkAsyncHttpClient.Builder.class);
        when(builder.buildWithDefaults(any())).thenReturn(client);

        SdkAsyncHttpService service = mock(SdkAsyncHttpService.class);
        when(service.createAsyncHttpClientFactory()).thenReturn(builder);
        return service;
    }

    /**
     * A client whose {@code execute} drives the response handler with the given body and completes the returned future, so
     * the warmer's drain path runs and its bounded wait settles immediately.
     */
    private static SdkAsyncHttpClient stubClient(Publisher<ByteBuffer> body) {
        SdkAsyncHttpClient client = mock(SdkAsyncHttpClient.class);
        when(client.execute(any(AsyncExecuteRequest.class))).thenAnswer(invocation -> {
            AsyncExecuteRequest request = invocation.getArgument(0);
            SdkAsyncHttpResponseHandler handler = request.responseHandler();
            handler.onHeaders(SdkHttpResponse.builder().statusCode(403).build());
            handler.onStream(body);
            return CompletableFuture.completedFuture(null);
        });
        return client;
    }

    /** A publisher that delivers the given chunks then completes, flipping {@code drained} once the stream completes. */
    private static Publisher<ByteBuffer> bodyPublisher(AtomicBoolean drained, ByteBuffer... chunks) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                for (ByteBuffer chunk : chunks) {
                    subscriber.onNext(chunk);
                }
                drained.set(true);
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }

    private static Publisher<ByteBuffer> emptyBody() {
        return bodyPublisher(new AtomicBoolean(false));
    }
}
