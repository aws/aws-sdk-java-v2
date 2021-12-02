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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.retry.RateLimitingTokenBucket;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

public class RetryableStageAdaptiveModeTest {
    private RateLimitingTokenBucket tokenBucket;

    private RequestPipeline<SdkHttpFullRequest, Response<Object>> mockChildPipeline;

    private RetryableStage<Object> retryableStage;

    @Before
    public void setup() throws Exception {
        tokenBucket = spy(RateLimitingTokenBucket.class);
        mockChildPipeline = mock(RequestPipeline.class);
    }

    @Test
    public void execute_acquiresToken() throws Exception {
        retryableStage = createStage(false);
        mockChildResponse(createSuccessResponse());
        retryableStage.execute(createHttpRequest(), createExecutionContext());

        verify(tokenBucket).acquire(1.0, false);
    }

    @Test
    public void execute_fastFailEnabled_propagatesSettingToBucket() throws Exception {
        retryableStage = createStage(true);
        mockChildResponse(createSuccessResponse());
        retryableStage.execute(createHttpRequest(), createExecutionContext());

        verify(tokenBucket).acquire(1.0, true);
    }

    @Test
    public void execute_retryModeStandard_doesNotAcquireToken() throws Exception {
        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.STANDARD).build();
        mockChildResponse(createSuccessResponse());
        retryableStage = createStage(retryPolicy);
        retryableStage.execute(createHttpRequest(), createExecutionContext());

        verifyZeroInteractions(tokenBucket);
    }

    @Test
    public void execute_retryModeLegacy_doesNotAcquireToken() throws Exception {
        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.LEGACY).build();
        mockChildResponse(createSuccessResponse());
        retryableStage = createStage(retryPolicy);
        retryableStage.execute(createHttpRequest(), createExecutionContext());

        verifyZeroInteractions(tokenBucket);
    }

    @Test
    public void execute_acquireReturnsFalse_throws() {
        when(tokenBucket.acquire(anyDouble(), anyBoolean())).thenReturn(false);
        retryableStage = createStage(false);

        SdkHttpFullRequest httpRequest = createHttpRequest();
        RequestExecutionContext executionContext = createExecutionContext();
        assertThatThrownBy(() -> retryableStage.execute(httpRequest, executionContext))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to acquire a send token");
    }
    @Test
    public void execute_responseSuccessful_updatesWithThrottlingFalse() throws Exception {
        retryableStage = createStage(false);
        mockChildResponse(createSuccessResponse());
        retryableStage.execute(createHttpRequest(), createExecutionContext());

        verify(tokenBucket).updateClientSendingRate(false);
        verify(tokenBucket, never()).updateClientSendingRate(true);
    }

    @Test
    public void execute_nonThrottlingServiceException_doesNotUpdateRate() throws Exception {
        SdkServiceException exception = SdkServiceException.builder()
                                                           .statusCode(500)
                                                           .build();
        mockChildResponse(exception);
        retryableStage = createStage(false);

        SdkHttpFullRequest httpRequest = createHttpRequest();
        RequestExecutionContext executionContext = createExecutionContext();
        assertThatThrownBy(() -> retryableStage.execute(httpRequest, executionContext))
            .isInstanceOf(SdkServiceException.class);

        verify(tokenBucket, never()).updateClientSendingRate(anyBoolean());
    }

    @Test
    public void execute_throttlingServiceException_updatesRate() throws Exception {
        SdkServiceException exception = SdkServiceException.builder()
                                                           .statusCode(HttpStatusCode.THROTTLING)
                                                           .build();

        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.ADAPTIVE)
                                             .numRetries(0)
                                             .build();

        mockChildResponse(exception);
        retryableStage = createStage(retryPolicy);

        SdkHttpFullRequest httpRequest = createHttpRequest();
        RequestExecutionContext executionContext = createExecutionContext();
        assertThatThrownBy(() -> retryableStage.execute(httpRequest, executionContext))
            .isInstanceOf(SdkServiceException.class);

        verify(tokenBucket).updateClientSendingRate(true);
        verify(tokenBucket, never()).updateClientSendingRate(false);
    }

    @Test
    public void execute_unsuccessfulResponse_nonThrottlingError_doesNotUpdateRate() throws Exception {
        retryableStage = createStage(false);

        SdkServiceException error = SdkServiceException.builder()
                                                           .statusCode(500)
                                                           .build();

        mockChildResponse(createUnsuccessfulResponse(error));

        SdkHttpFullRequest httpRequest = createHttpRequest();
        RequestExecutionContext executionContext = createExecutionContext();
        assertThatThrownBy(() -> retryableStage.execute(httpRequest, executionContext))
        .isInstanceOf(SdkServiceException.class);

        verify(tokenBucket, never()).updateClientSendingRate(anyBoolean());
    }

    @Test
    public void execute_unsuccessfulResponse_throttlingError_updatesRate() throws Exception {
        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.ADAPTIVE)
                                             .numRetries(0)
                                             .build();

        retryableStage = createStage(retryPolicy);

        SdkServiceException error = SdkServiceException.builder()
                                                       .statusCode(HttpStatusCode.THROTTLING)
                                                       .build();

        mockChildResponse(createUnsuccessfulResponse(error));

        SdkHttpFullRequest httpRequest = createHttpRequest();
        RequestExecutionContext executionContext = createExecutionContext();
        assertThatThrownBy(() -> retryableStage.execute(httpRequest, executionContext))
            .isInstanceOf(SdkServiceException.class);

        verify(tokenBucket).updateClientSendingRate(true);
        verify(tokenBucket, never()).updateClientSendingRate(false);
    }

    private RetryableStage<Object> createStage(boolean failFast) {
        RetryPolicy retryPolicy = RetryPolicy.builder(RetryMode.ADAPTIVE)
                                             .fastFailRateLimiting(failFast)
                                             .build();

        return createStage(retryPolicy);
    }

    private RetryableStage<Object> createStage(RetryPolicy retryPolicy) {
        return new RetryableStage<>(clientDependencies(retryPolicy), mockChildPipeline, tokenBucket);
    }

    private Response<Object> createSuccessResponse() {
        return Response.builder()
                       .isSuccess(true)
                       .build();
    }

    public Response<Object> createUnsuccessfulResponse(SdkException exception) {
        return Response.builder()
                       .isSuccess(false)
                       .exception(exception)
                       .build();
    }

    private HttpClientDependencies clientDependencies(RetryPolicy retryPolicy) {

        SdkClientConfiguration clientConfiguration = SdkClientConfiguration.builder()
                .option(SdkClientOption.RETRY_POLICY, retryPolicy)
                .build();

        return HttpClientDependencies.builder()
                .clientConfiguration(clientConfiguration)
                .build();
    }

    private static RequestExecutionContext createExecutionContext() {
        return RequestExecutionContext.builder()
                                      .originalRequest(NoopTestRequest.builder().build())
                                      .executionContext(ExecutionContext.builder()
                                                                        .executionAttributes(new ExecutionAttributes())
                                                                        .metricCollector(NoOpMetricCollector.create())
                                                                        .build())
                                      .build();
    }

    private static SdkHttpFullRequest createHttpRequest() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .protocol("https")
                                 .host("amazon.com")
                                 .build();
    }

    private void mockChildResponse(Response<Object> response) throws Exception {
        when(mockChildPipeline.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class))).thenReturn(response);
    }

    private void mockChildResponse(Exception error) throws Exception {
        when(mockChildPipeline.execute(any(SdkHttpFullRequest.class), any(RequestExecutionContext.class))).thenThrow(error);
    }
}
