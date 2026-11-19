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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createRequest;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * Verifies that a connection returned to the pool does not keep the last request's body reachable.
 */
public class NettyNioAsyncHttpClientRequestBodyRetentionTest {
    private static final int CONCURRENT_REQUESTS = 4;
    private static final int BODY_SIZE = 4 * 1024 * 1024;

    private WireMockServer mockServer;
    private SdkAsyncHttpClient client;

    @BeforeEach
    void setup() {
        mockServer = new WireMockServer(wireMockConfig().dynamicPort().disableRequestJournal());
        mockServer.start();
        client = NettyNioAsyncHttpClient.builder()
                                        .useIdleConnectionReaper(false)
                                        .connectionMaxIdleTime(Duration.ofMinutes(5))
                                        .build();
    }

    @AfterEach
    void tearDown() {
        client.close();
        mockServer.stop();
    }

    @Test
    void execute_connectionIdleInPool_shouldNotRetainRequestBody() throws Exception {
        mockServer.stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200).withBody("ok")));

        List<WeakReference<byte[]>> bodies = executeConcurrentRequests();

        assertThat(countRetainedAfterGc(bodies, Duration.ofSeconds(10)))
            .as("request bodies still reachable after all responses completed and the connections went idle")
            .isZero();
    }

    /**
     * Runs the requests in a separate frame so the test method holds no strong reference to the bodies.
     */
    private List<WeakReference<byte[]>> executeConcurrentRequests() throws Exception {
        URI uri = URI.create("http://localhost:" + mockServer.port());
        List<WeakReference<byte[]>> bodies = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            byte[] body = new byte[BODY_SIZE];
            bodies.add(new WeakReference<>(body));
            RecordingResponseHandler handler = new RecordingResponseHandler();
            AsyncExecuteRequest request =
                AsyncExecuteRequest.builder()
                                   .request(createRequest(uri, "/", null, SdkHttpMethod.PUT, emptyMap())
                                                .toBuilder()
                                                .putHeader("Content-Length", String.valueOf(BODY_SIZE))
                                                .build())
                                   .requestContentPublisher(new ByteArrayContentPublisher(body))
                                   .responseHandler(handler)
                                   .build();
            futures.add(client.execute(request));
            futures.add(handler.completeFuture);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
        return bodies;
    }

    private static long countRetainedAfterGc(List<WeakReference<byte[]>> refs, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        long retained;
        do {
            System.gc();
            retained = refs.stream().filter(ref -> ref.get() != null).count();
            if (retained == 0) {
                return 0;
            }
            Thread.sleep(100);
        } while (System.nanoTime() < deadline);
        return retained;
    }

    /**
     * Emits a single buffer that wraps the given array, like {@code AsyncRequestBody.fromBytesUnsafe}.
     */
    private static final class ByteArrayContentPublisher implements SdkHttpContentPublisher {
        private final ByteBuffer buffer;

        private ByteArrayContentPublisher(byte[] bytes) {
            this.buffer = ByteBuffer.wrap(bytes);
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of((long) buffer.capacity());
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
            AtomicBoolean done = new AtomicBoolean();
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (n > 0 && done.compareAndSet(false, true)) {
                        subscriber.onNext(buffer.asReadOnlyBuffer());
                        subscriber.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    done.set(true);
                }
            });
        }
    }
}
