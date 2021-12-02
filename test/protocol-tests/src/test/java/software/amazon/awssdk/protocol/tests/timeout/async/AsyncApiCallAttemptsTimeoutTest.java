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

package software.amazon.awssdk.protocol.tests.timeout.async;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.protocol.tests.timeout.BaseApiCallAttemptTimeoutTest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;

/**
 * Test apiCallAttemptTimeout feature for asynchronous operations.
 */
public class AsyncApiCallAttemptsTimeoutTest extends BaseApiCallAttemptTimeoutTest {
    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private static final String STREAMING_OUTPUT_PATH = "/2016-03-11/streamingOutputOperation";
    private ProtocolRestJsonAsyncClient client;
    private ProtocolRestJsonAsyncClient clientWithRetry;
    private static final int API_CALL_ATTEMPT_TIMEOUT = 800;
    private static final int DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT = 100;
    private static final int DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT = 1000;

    @Before
    public void setup() {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_1)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                            .overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
                                                                         .retryPolicy(RetryPolicy.none()))
                                            .build();

        clientWithRetry = ProtocolRestJsonAsyncClient.builder()
                                                     .region(Region.US_WEST_1)
                                                     .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                     .credentialsProvider(() -> AwsBasicCredentials.create("akid", "skid"))
                                                     .overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMillis(API_CALL_ATTEMPT_TIMEOUT))
                                                                                  .retryPolicy(RetryPolicy.builder()
                                                                                                          .numRetries(1)
                                                                                                          .build()))
                                                     .build();

    }

    @Test
    public void streamingOperation_slowTransformer_shouldThrowApiCallAttemptTimeoutException() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));

        CompletableFuture<ResponseBytes<StreamingOutputOperationResponse>> future = client
            .streamingOutputOperation(
                StreamingOutputOperationRequest.builder().build(), new SlowResponseTransformer<>());

        assertThatThrownBy(future::join)
            .hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Override
    protected Consumer<ThrowableAssert.ThrowingCallable> timeoutExceptionAssertion() {
        return c -> assertThatThrownBy(c).hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Override
    protected Consumer<ThrowableAssert.ThrowingCallable> serviceExceptionAssertion() {
        return c -> assertThatThrownBy(c).hasCauseInstanceOf(ProtocolRestJsonException.class);
    }

    @Override
    protected Callable callable() {
        return () -> client.allTypes().join();
    }

    @Override
    protected Callable retryableCallable() {
        return () -> clientWithRetry.allTypes().join();
    }

    @Override
    protected Callable streamingCallable() {
        return () -> client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                     AsyncResponseTransformer.toBytes()).join();
    }

    @Override
    protected WireMockRule wireMock() {
        return wireMock;
    }

    private static class SlowResponseTransformer<ResponseT>
        implements AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> {

        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> delegate;

        private SlowResponseTransformer() {
            this.delegate = AsyncResponseTransformer.toBytes();
        }

        @Override
        public CompletableFuture<ResponseBytes<ResponseT>> prepare() {
            return delegate.prepare()
                           .thenApply(r -> {
                               try {
                                   Thread.sleep(1_000);
                               } catch (InterruptedException e) {
                                   e.printStackTrace();
                               }
                               return r;
                           });
        }

        public int currentCallCount() {
            return callCount.get();
        }

        @Override
        public void onResponse(ResponseT response) {
            callCount.incrementAndGet();
            delegate.onResponse(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            delegate.exceptionOccurred(throwable);
        }
    }
}
