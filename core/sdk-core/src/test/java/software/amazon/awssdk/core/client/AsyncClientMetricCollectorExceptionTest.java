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

package software.amazon.awssdk.core.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.noOpResponseHandler;
import static utils.HttpTestUtils.testAsyncClientBuilder;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.core.protocol.VoidSdkResponse;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.ValidSdkObjects;

/**
 * Tests to verify that exceptions thrown by the MetricCollector are reported through the returned future.
 * {@link java.util.concurrent.CompletableFuture}.
 *
 * @see AsyncClientHandlerExceptionTest
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncClientMetricCollectorExceptionTest {

    public static final String MESSAGE = "test exception";

    @Mock
    private MetricCollector metricCollector;

    @Mock
    private SdkAsyncHttpClient asyncHttpClient;

    @Test
    public void exceptionInReportMetricReportedInFuture() {
        when(metricCollector.createChild(any())).thenReturn(metricCollector);
        Exception exception = new RuntimeException(MESSAGE);
        doThrow(exception).when(metricCollector).reportMetric(eq(CoreMetric.API_CALL_DURATION), any(Duration.class));

        CompletableFuture<SdkResponse> responseFuture = makeRequest();

        assertThatThrownBy(() -> responseFuture.get(1, TimeUnit.SECONDS)).hasRootCause(exception);
    }

    private CompletableFuture<SdkResponse> makeRequest() {
        when(asyncHttpClient.execute(any(AsyncExecuteRequest.class))).thenAnswer((Answer<CompletableFuture<Void>>) invocationOnMock -> {
            SdkAsyncHttpResponseHandler handler = invocationOnMock.getArgument(0, AsyncExecuteRequest.class).responseHandler();
            handler.onHeaders(SdkHttpFullResponse.builder()
                                                 .statusCode(200)
                                                 .build());
            handler.onStream(new EmptyPublisher<>());
            return CompletableFuture.completedFuture(null);
        });

        AmazonAsyncHttpClient asyncClient = testAsyncClientBuilder()
            .retryStrategy(DefaultRetryStrategy.doNotRetry())
            .asyncHttpClient(asyncHttpClient)
            .build();

        SdkHttpFullRequest httpFullRequest = ValidSdkObjects.sdkHttpFullRequest().build();
        NoopTestRequest sdkRequest = NoopTestRequest.builder().build();
        InterceptorContext interceptorContext = InterceptorContext
            .builder()
            .request(sdkRequest)
            .httpRequest(httpFullRequest)
            .build();

        Response<SdkResponse> response =
            Response.<SdkResponse>builder()
                    .isSuccess(true)
                    .response(VoidSdkResponse.builder().build())
                    .httpResponse(SdkHttpResponse.builder().statusCode(200).build())
                    .build();

        return asyncClient
            .requestExecutionBuilder()
            .originalRequest(sdkRequest)
            .request(httpFullRequest)
            .executionContext(
                ExecutionContext
                    .builder()
                    .executionAttributes(new ExecutionAttributes())
                    .interceptorContext(interceptorContext)
                    .metricCollector(metricCollector)
                    .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                    .build()
            )
            .execute(noOpResponseHandler(response));
    }
}
