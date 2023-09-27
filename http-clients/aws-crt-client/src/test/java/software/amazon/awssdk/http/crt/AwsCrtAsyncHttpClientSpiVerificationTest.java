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

package software.amazon.awssdk.http.crt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.binaryEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.emptyMap;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.HttpTestUtils.createProvider;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;

public class AwsCrtAsyncHttpClientSpiVerificationTest {
    private static final int TEST_BODY_LEN = 1024;

    @Rule
    public WireMockRule mockServer = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort());

    private static SdkAsyncHttpClient client;

    @BeforeClass
    public static void setup() throws Exception {
        client = AwsCrtAsyncHttpClient.builder()
                                      .connectionHealthConfiguration(b -> b.minimumThroughputInBps(4068L)
                                                                           .minimumThroughputTimeout(Duration.ofSeconds(3)))
                                      .build();
    }

    @AfterClass
    public static void tearDown() {
        client.close();
        CrtResource.waitForNoResources();
    }

    private byte[] generateRandomBody(int size) {
        byte[] randomData = new byte[size];
        new Random().nextBytes(randomData);
        return randomData;
    }

    @Test
    public void signalsErrorViaOnErrorAndFuture() throws Exception {
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
        assertThatThrownBy(executeFuture::join).hasCauseInstanceOf(IOException.class).hasRootCauseInstanceOf(HttpException.class);
    }

    @Test
    public void requestFailed_connectionTimeout_shouldWrapException() {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().connectionTimeout(Duration.ofNanos(1)).build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
            SdkHttpRequest request = createRequest(uri);
            RecordingResponseHandler recorder = new RecordingResponseHandler();
            client.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider("")).responseHandler(recorder).build());
            assertThatThrownBy(() -> recorder.completeFuture().get(5, TimeUnit.SECONDS)).hasCauseInstanceOf(IOException.class)
                                                                                        .hasRootCauseInstanceOf(HttpException.class);
        }
    }

    @Test
    public void requestFailed_notRetryable_shouldNotWrapException() {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().build()) {
            URI uri = URI.create("http://localhost:" + mockServer.port());
            // make it invalid by doing a non-zero content length with no request body...
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("host", Collections.singletonList(uri.getHost()));

            List<String> contentLengthValues = new LinkedList<>();
            contentLengthValues.add("1");
            headers.put("content-length", contentLengthValues);

            SdkHttpRequest request = createRequest(uri).toBuilder().headers(headers).build();

            RecordingResponseHandler recorder = new RecordingResponseHandler();
            client.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(new EmptyPublisher()).responseHandler(recorder).build());
            // invalid request should have returned an HttpException and not an IOException.
            assertThatThrownBy(() -> recorder.completeFuture().get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(HttpException.class).hasMessageContaining("does not match the previously declared length");
        }
    }

    @Test
    public void callsOnStreamForEmptyResponseContent() throws Exception {
        stubFor(any(urlEqualTo("/")).willReturn(aResponse().withStatus(204).withHeader("foo", "bar")));

        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();
        AtomicReference<SdkHttpResponse> response = new AtomicReference<>(null);

        SdkAsyncHttpResponseHandler handler = new TestResponseHandler() {
            @Override
            public void onHeaders(SdkHttpResponse headers) {
                response.compareAndSet(null, headers);
            }
            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                super.onStream(stream);
                streamReceived.complete(true);
            }
        };

        SdkHttpRequest request = createRequest(URI.create("http://localhost:" + mockServer.port()));

        CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder()
                .request(request)
                .responseHandler(handler)
                .requestContentPublisher(new EmptyPublisher())
                .build());

        future.get(60, TimeUnit.SECONDS);
        assertThat(streamReceived.get(1, TimeUnit.SECONDS)).isTrue();
        assertThat(response.get() != null).isTrue();
        assertThat(response.get().statusCode() == 204).isTrue();
        assertThat(response.get().headers().get("foo").isEmpty()).isFalse();
    }

    @Test
    public void testGetRequest() throws Exception {
        String path = "/testGetRequest";
        byte[] body = generateRandomBody(TEST_BODY_LEN);
        String expectedBodyHash = sha256Hex(body).toUpperCase();
        stubFor(any(urlEqualTo(path)).willReturn(aResponse().withStatus(200)
                                                           .withHeader("Content-Length", Integer.toString(TEST_BODY_LEN))
                                                           .withHeader("foo", "bar")
                                                           .withBody(body)));

        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();
        AtomicReference<SdkHttpResponse> response = new AtomicReference<>(null);
        Sha256BodySubscriber bodySha256Subscriber = new Sha256BodySubscriber();
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        SdkAsyncHttpResponseHandler handler = new SdkAsyncHttpResponseHandler() {
            @Override
            public void onHeaders(SdkHttpResponse headers) {
                response.compareAndSet(null, headers);
            }
            @Override
            public void onStream(Publisher<ByteBuffer> stream) {
                stream.subscribe(bodySha256Subscriber);
                streamReceived.complete(true);
            }

            @Override
            public void onError(Throwable t) {
                error.compareAndSet(null, t);
            }
        };

        URI uri = URI.create("http://localhost:" + mockServer.port());
        SdkHttpRequest request = createRequest(uri, path, null, SdkHttpMethod.GET, emptyMap());

        CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder()
                                                                           .request(request)
                                                                           .responseHandler(handler)
                                                                           .requestContentPublisher(new EmptyPublisher())
                                                                           .build());

        future.get(60, TimeUnit.SECONDS);
        assertThat(error.get()).isNull();
        assertThat(streamReceived.get(1, TimeUnit.SECONDS)).isTrue();
        assertThat(bodySha256Subscriber.getFuture().get(60, TimeUnit.SECONDS)).isEqualTo(expectedBodyHash);
        assertThat(response.get().statusCode()).isEqualTo(200);
        assertThat(response.get().headers().get("foo").isEmpty()).isFalse();
    }


    private void makePutRequest(String path, byte[] reqBody, int expectedStatus) throws Exception {
        CompletableFuture<Boolean> streamReceived = new CompletableFuture<>();
        AtomicReference<SdkHttpResponse> response = new AtomicReference<>(null);
        AtomicReference<Throwable> error = new AtomicReference<>(null);

        Subscriber<ByteBuffer> subscriber = CrtHttpClientTestUtils.createDummySubscriber();

        SdkAsyncHttpResponseHandler handler = CrtHttpClientTestUtils.createTestResponseHandler(response,
                streamReceived, error, subscriber);

        URI uri = URI.create("http://localhost:" + mockServer.port());
        SdkHttpRequest request = createRequest(uri, path, reqBody, SdkHttpMethod.PUT, emptyMap());

        CompletableFuture<Void> future = client.execute(AsyncExecuteRequest.builder()
                                                                           .request(request)
                                                                           .responseHandler(handler)
                                                                           .requestContentPublisher(new SdkTestHttpContentPublisher(reqBody))
                                                                           .build());
        future.get(60, TimeUnit.SECONDS);
        assertThat(error.get()).isNull();
        assertThat(streamReceived.get(60, TimeUnit.SECONDS)).isTrue();
        assertThat(response.get().statusCode()).isEqualTo(expectedStatus);
    }


    @Test
    public void testPutRequest() throws Exception {
        String pathExpect200 = "/testPutRequest/return_200_on_exact_match";
        byte[] expectedBody = generateRandomBody(TEST_BODY_LEN);
        stubFor(any(urlEqualTo(pathExpect200)).withRequestBody(binaryEqualTo(expectedBody)).willReturn(aResponse().withStatus(200)));
        makePutRequest(pathExpect200, expectedBody, 200);

        String pathExpect404 = "/testPutRequest/return_404_always";
        byte[] randomBody = generateRandomBody(TEST_BODY_LEN);
        stubFor(any(urlEqualTo(pathExpect404)).willReturn(aResponse().withStatus(404)));
        makePutRequest(pathExpect404, randomBody, 404);
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
