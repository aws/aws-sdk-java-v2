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

package software.amazon.awssdk.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.noOpResponseHandler;
import static utils.HttpTestUtils.executionContext;
import static utils.HttpTestUtils.testAsyncClientBuilder;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import utils.ValidSdkObjects;

/**
 * Reusable suite that verifies sdk-core's async API-call-attempt timeout aborts a slow real response, exercised over a real
 * {@link SdkAsyncHttpClient}. Each concrete async HTTP client module subclasses this and provides its client via
 * {@link #createSdkAsyncHttpClient()}.
 */
public abstract class SdkAsyncHttpClientApiCallAttemptTimeoutTestSuite {

    private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(1);

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private AmazonAsyncHttpClient httpClient;

    protected abstract SdkAsyncHttpClient createSdkAsyncHttpClient();

    @Test
    public void slowApiAttempt_ThrowsApiCallAttemptTimeoutException() {
        httpClient = testAsyncClientBuilder()
            .asyncHttpClient(createSdkAsyncHttpClient())
            .apiCallTimeout(API_CALL_TIMEOUT)
            .apiCallAttemptTimeout(Duration.ofMillis(1))
            .build();

        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}").withFixedDelay(1_000)));
        CompletableFuture future = requestBuilder().execute(noOpResponseHandler());
        assertThatThrownBy(future::join).hasCauseInstanceOf(ApiCallAttemptTimeoutException.class);
    }

    private AmazonAsyncHttpClient.RequestExecutionBuilder requestBuilder() {
        return httpClient.requestExecutionBuilder()
                         .request(generateRequest())
                         .originalRequest(NoopTestRequest.builder().build())
                         .executionContext(executionContext(null));
    }

    private SdkHttpFullRequest generateRequest() {
        return ValidSdkObjects.sdkHttpFullRequest(wireMock.port())
                              .host("localhost")
                              .contentStreamProvider(() -> new ByteArrayInputStream("test".getBytes())).build();
    }
}
