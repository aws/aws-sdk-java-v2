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

package software.amazon.awssdk.http.nio.netty;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.EmptyPublisher;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Verify the behavior of {@link NettyNioAsyncHttpClient} is consistent with the SPI.
 */
public class NettyNioAsyncHttpClientSpiVerificationTest {
    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort());

    private static SdkAsyncHttpClient client = NettyNioAsyncHttpClient.builder().buildWithDefaults(mapWithTrustAllCerts());

    @AfterClass
    public static void tearDown() throws Exception {
        client.close();
    }

    // CONNECTION_RESET_BY_PEER does not work on JDK 11. See https://github.com/tomakehurst/wiremock/issues/1009
    @Test
    public void signalsErrorViaOnErrorAndFuture() throws InterruptedException, ExecutionException, TimeoutException {
        stubFor(any(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        CompletableFuture<Boolean> errorSignaled = new CompletableFuture<>();

        SdkAsyncHttpResponseHandler handler = new TestResponseHandler() {
            @Override
            public void onError(Throwable error) {
                errorSignaled.complete(true);
            }
        };

        SdkHttpRequest request = createRequest(URI.create("http://localhost:" + mockServer.port()));

        CompletableFuture<Void> executeFuture = client.execute(AsyncExecuteRequest.builder()
                .request(request)
                .responseHandler(handler)
                .requestContentPublisher(new EmptyPublisher())
                .build());

        assertThat(errorSignaled.get(1, TimeUnit.SECONDS)).isTrue();
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void callsOnStreamForEmptyResponseContent() throws InterruptedException, ExecutionException, TimeoutException {
        stubFor(any(urlEqualTo("/")).willReturn(aResponse().withStatus(204).withHeader("foo", "bar")));

        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();

        SdkAsyncHttpResponseHandler handler = new TestResponseHandler() {
            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                super.onStream(stream);
                streamReceived.complete(true);
            }
        };

        SdkHttpRequest request = createRequest(URI.create("http://localhost:" + mockServer.port()));

        client.execute(AsyncExecuteRequest.builder()
                .request(request)
                .responseHandler(handler)
                .requestContentPublisher(new EmptyPublisher())
                .build());

        assertThat(streamReceived.get(1, TimeUnit.SECONDS)).isTrue();
    }

    private static AttributeMap mapWithTrustAllCerts() {
        return AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                .build();
    }

    private SdkHttpFullRequest createRequest(URI endpoint) {
        return createRequest(endpoint, "/", null, SdkHttpMethod.GET, emptyMap());
    }

    private SdkHttpFullRequest createRequest(URI endpoint,
                                             String resourcePath,
                                             String body,
                                             SdkHttpMethod method,
                                             Map<String, String> params) {

        String contentLength = body == null ? null : String.valueOf(body.getBytes(UTF_8).length);
        return SdkHttpFullRequest.builder()
                                 .uri(endpoint)
                                 .method(method)
                                 .encodedPath(resourcePath)
                                 .applyMutation(b -> params.forEach(b::putRawQueryParameter))
                                 .applyMutation(b -> {
                                     b.putHeader("Host", endpoint.getHost());
                                     if (contentLength != null) {
                                         b.putHeader("Content-Length", contentLength);
                                     }
                                 }).build();
    }

    private static class TestResponseHandler implements SdkAsyncHttpResponseHandler {
        @Override
        public void onHeaders(SdkHttpResponse headers) {
        }

        @Override
        public void onStream(Publisher<ByteBuffer> stream) {
            stream.subscribe(new DrainingSubscriber<>());
        }

        @Override
        public void onError(Throwable error) {
        }
    }

    private static class DrainingSubscriber<T> implements Subscriber<T> {
        private Subscription subscription;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
            this.subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
