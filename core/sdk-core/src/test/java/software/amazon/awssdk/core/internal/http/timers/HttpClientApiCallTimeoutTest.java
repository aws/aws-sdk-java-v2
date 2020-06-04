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
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.noOpSyncResponseHandler;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.superSlowResponseHandler;
import static utils.HttpTestUtils.testClientBuilder;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.request.SlowExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import utils.ValidSdkObjects;


public class HttpClientApiCallTimeoutTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    private AmazonSyncHttpClient httpClient;

    @Before
    public void setup() {
        httpClient = testClientBuilder()
            .retryPolicy(RetryPolicy.none())
            .apiCallTimeout(API_CALL_TIMEOUT)
            .build();
    }

    @Test
    public void successfulResponse_SlowResponseHandler_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));

        assertThatThrownBy(() -> requestBuilder().execute(combinedSyncResponseHandler(
            superSlowResponseHandler(API_CALL_TIMEOUT.toMillis()), null)))
            .isInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void errorResponse_SlowErrorResponseHandler_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(500).withBody("{}")));

        ExecutionContext executionContext = ClientExecutionAndRequestTimerTestUtils.executionContext(null);

        assertThatThrownBy(() -> httpClient.requestExecutionBuilder()
                                           .originalRequest(NoopTestRequest.builder().build())
                                           .executionContext(executionContext)
                                           .request(generateRequest())
                                           .execute(combinedSyncResponseHandler(noOpSyncResponseHandler(),
                                                                                superSlowResponseHandler(API_CALL_TIMEOUT.toMillis()))))
            .isInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void successfulResponse_SlowBeforeTransmissionExecutionInterceptor_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));
        ExecutionInterceptor interceptor =
            new SlowExecutionInterceptor().beforeTransmissionWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT);

        assertThatThrownBy(() -> requestBuilder().executionContext(withInterceptors(interceptor))
                                                 .execute(noOpSyncResponseHandler()))
            .isInstanceOf(ApiCallTimeoutException.class);
    }

    @Test
    public void successfulResponse_SlowAfterResponseExecutionInterceptor_ThrowsApiCallTimeoutException() {
        stubFor(get(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));
        ExecutionInterceptor interceptor =
            new SlowExecutionInterceptor().afterTransmissionWaitInSeconds(SLOW_REQUEST_HANDLER_TIMEOUT);
        assertThatThrownBy(() -> requestBuilder().executionContext(withInterceptors(interceptor))
                                                 .execute(noOpSyncResponseHandler()))
            .isInstanceOf(ApiCallTimeoutException.class);
    }

    private AmazonSyncHttpClient.RequestExecutionBuilder requestBuilder() {
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
