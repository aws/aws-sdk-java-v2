/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.tests.timeout;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static software.amazon.awssdk.protocol.wiremock.WireMockUtils.verifyRequestCount;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesResponse;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Test apiCallAttemptTimeout feature for asynchronous operations.
 */
public class AsyncApiCallAttemptsTimeoutTest {
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
    public void nonstreamingOperation200_finishedWithinTime_shouldSucceed() throws InterruptedException {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
    }

    @Test
    public void nonstreamingOperation200_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void nonstreamingOperation500_finishedWithinTime_shouldNotTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500)
                                           .withHeader("x-amzn-ErrorType", "EmptyModeledException")
                                           .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ProtocolRestJsonException.class);
    }

    @Test
    public void nonstreamingOperation500_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = client.allTypes(SdkBuilder::build);

        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void streamingOperation_finishedWithinTime_shouldSucceed() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH))
                    .willReturn(aResponse().withStatus(200).withBody("test").withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));

        ResponseBytes<StreamingOutputOperationResponse> response =
                client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                                AsyncResponseTransformer.toBytes()).join();

        byte[] arrayCopy = response.asByteArray();
        assertThat(arrayCopy).containsExactly('t', 'e', 's', 't');
    }

    @Test
    public void streamingOperation_notFinishedWithinTime_shouldTimeout() {
        stubFor(post(urlPathEqualTo(STREAMING_OUTPUT_PATH))
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        assertThatThrownBy(() ->
            client.streamingOutputOperation(StreamingOutputOperationRequest.builder().build(),
                                            AsyncResponseTransformer.toBytes()).join())
            .hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void firstAttemptTimeout_retryFinishWithInTime_shouldNotTimeout() {
        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = clientWithRetry.allTypes(SdkBuilder::build);

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
        verifyRequestCount(2, wireMock);
    }

    @Test
    public void firstAttemptTimeout_retryFinishWithInTime500_shouldNotTimeout() {
        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("timed out in the first attempt")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withFixedDelay(DELAY_BEFORE_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = clientWithRetry.allTypes(SdkBuilder::build);
        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ProtocolRestJsonException.class);
        verifyRequestCount(2, wireMock);
    }

    @Test
    public void allAttemtsNotFinishedWithinTime_shouldTimeout() {
        stubFor(post(anyUrl())
                    .inScenario("timed out in both attempts")
                    .whenScenarioStateIs(Scenario.STARTED)
                    .willSetStateTo("first attempt")
                    .willReturn(aResponse()
                                    .withStatus(200).withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));

        stubFor(post(anyUrl())
                    .inScenario("timed out in both attempts")
                    .whenScenarioStateIs("first attempt")
                    .willSetStateTo("second attempt")
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withBody("{}")
                                    .withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture = clientWithRetry.allTypes(SdkBuilder::build);
        assertThatThrownBy(allTypesResponseCompletableFuture::join).hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    @Test
    public void increaseTimeoutInRequestOverrideConfig_shouldTakePrecedence() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT)));
        CompletableFuture<AllTypesResponse> allTypesResponseCompletableFuture =
            client.allTypes(b -> b.overrideConfiguration(c -> c.apiCallAttemptTimeout(
                Duration.ofMillis(DELAY_AFTER_API_CALL_ATTEMPT_TIMEOUT + 1000))));

        AllTypesResponse response = allTypesResponseCompletableFuture.join();
        assertThat(response).isNotNull();
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


    private static class SlowResponseTransformer<ResponseT>
        implements AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> {

        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AsyncResponseTransformer<ResponseT, ResponseBytes<ResponseT>> delegate;

        private SlowResponseTransformer() {
            this.delegate = AsyncResponseTransformer.toBytes();
        }

        public int currentCallCount() {
            return callCount.get();
        }

        @Override
        public void responseReceived(ResponseT response) {
            callCount.incrementAndGet();
            delegate.responseReceived(response);
        }

        @Override
        public void onStream(SdkPublisher<ByteBuffer> publisher) {
            delegate.onStream(publisher);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            delegate.exceptionOccurred(throwable);
        }

        @Override
        public ResponseBytes<ResponseT> complete() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return delegate.complete();
        }
    }
}
