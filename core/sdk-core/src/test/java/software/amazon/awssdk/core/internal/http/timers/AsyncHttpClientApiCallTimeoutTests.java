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

package software.amazon.awssdk.core.internal.http.timers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.API_CALL_TIMEOUT;
import static software.amazon.awssdk.core.internal.http.timers.TimeoutTestConstants.SLOW_REQUEST_HANDLER_TIMEOUT;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.combinedAsyncResponseHandler;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.noOpResponseHandler;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.superSlowResponseHandler;
import static utils.HttpTestUtils.testAsyncClientBuilder;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.internal.http.request.SlowExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import utils.ValidSdkObjects;

public class AsyncHttpClientApiCallTimeoutTests {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private AmazonAsyncHttpClient httpClient;

    @Before
    public void setup() {
        httpClient = testAsyncClientBuilder()
            .retryPolicy(RetryPolicy.none())
            .apiCallTimeout(API_CALL_TIMEOUT)
            .build();
    }

    @Test
    public void errorResponse_SlowErrorResponseHandler_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(500).withBody("{}")));

        ExecutionContext executionContext = ClientExecutionAndRequestTimerTestUtils.executionContext(null);

        CompletableFuture future = httpClient.requestExecutionBuilder()
                                             .originalRequest(NoopTestRequest.builder().build())
                                             .executionContext(executionContext)
                                             .request(generateRequest())
                                             .execute(combinedAsyncResponseHandler(noOpResponseHandler(),
                                                                                   superSlowResponseHandler(API_CALL_TIMEOUT.toMillis())));

        assertThatThrownBy(future::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void errorResponse_SlowAfterErrorRequestHandler_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(500).withBody("{}")));
        ExecutionInterceptorChain interceptors =
            new ExecutionInterceptorChain(
                Collections.singletonList(new SlowExecutionInterceptor().onExecutionFailureWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT)));

        SdkHttpFullRequest request = generateRequest();
        InterceptorContext incerceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(request)
                              .build();

        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .signer(new NoOpSigner())
                                                            .interceptorChain(interceptors)
                                                            .executionAttributes(new ExecutionAttributes())
                                                            .interceptorContext(incerceptorContext)
                                                            .metricCollector(MetricCollector.create("ApiCall"))
                                                            .build();

        CompletableFuture future =
            httpClient.requestExecutionBuilder()
                      .originalRequest(NoopTestRequest.builder().build())
                      .request(request)
                      .executionContext(executionContext)
                      .execute(combinedAsyncResponseHandler(noOpResponseHandler(),
                                                            noOpResponseHandler(SdkServiceException.builder().build())));

        assertThatThrownBy(future::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void successfulResponse_SlowBeforeRequestRequestHandler_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));
        ExecutionInterceptor interceptor =
            new SlowExecutionInterceptor().beforeTransmissionWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT);

        CompletableFuture future = requestBuilder().executionContext(withInterceptors(interceptor))
                                                   .execute(noOpResponseHandler());
        assertThatThrownBy(future::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void successfulResponse_SlowResponseHandler_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));
        CompletableFuture future = requestBuilder().execute(superSlowResponseHandler(API_CALL_TIMEOUT.toMillis()));
        assertThatThrownBy(future::join).hasCauseInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void slowApiAttempt_ThrowsApiCallAttemptTimeoutException() {
        httpClient = testAsyncClientBuilder()
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
                         .executionContext(ClientExecutionAndRequestTimerTestUtils.executionContext(null));
    }

    private SdkHttpFullRequest generateRequest() {
        return ValidSdkObjects.sdkHttpFullRequest(wireMock.port())
                              .host("localhost")
                              .contentStreamProvider(() -> new ByteArrayInputStream("test".getBytes())).build();
    }

    private ExecutionContext withInterceptors(ExecutionInterceptor... requestHandlers) {

        ExecutionInterceptorChain interceptors =
            new ExecutionInterceptorChain(Arrays.asList(requestHandlers));

        InterceptorContext incerceptorContext =
            InterceptorContext.builder()
                              .request(NoopTestRequest.builder().build())
                              .httpRequest(generateRequest())
                              .build();
        return ExecutionContext.builder()
                               .signer(new NoOpSigner())
                               .interceptorChain(interceptors)
                               .executionAttributes(new ExecutionAttributes())
                               .interceptorContext(incerceptorContext)
                               .metricCollector(MetricCollector.create("ApiCall"))
                               .build();
    }
}
