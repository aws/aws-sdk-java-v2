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
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.internal.http.AmazonAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import utils.ValidSdkObjects;

/**
 * Tests to verify that exceptions thrown by the RetryStrategy are reported through the returned future.
 * {@link java.util.concurrent.CompletableFuture}.
 *
 * @see AsyncClientHandlerExceptionTest
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncClientRetryStrategyExceptionTest {

    public static final String MESSAGE = "test exception";

    @Mock
    private RetryStrategy retryStrategy;

    @Test
    public void exceptionInInitialTokenReportedInFuture() {
        Exception exception = new RuntimeException(MESSAGE);
        when(retryStrategy.acquireInitialToken(any())).thenThrow(exception);

        CompletableFuture<SdkResponse> responseFuture = makeRequest();

        assertThatThrownBy(() -> responseFuture.get(1, TimeUnit.SECONDS)).hasRootCause(exception);
    }

    @Test
    public void exceptionInRefreshTokenReportedInFuture() {
        when(retryStrategy.acquireInitialToken(any())).thenReturn(
            AcquireInitialTokenResponse.create(new RetryToken() {
            }, Duration.ZERO)
        );
        Exception exception = new RuntimeException(MESSAGE);
        when(retryStrategy.refreshRetryToken(any())).thenThrow(exception);

        CompletableFuture<SdkResponse> responseFuture = makeRequest();

        assertThatThrownBy(() -> responseFuture.get(1, TimeUnit.SECONDS)).hasRootCause(exception);
    }

    private CompletableFuture<SdkResponse> makeRequest() {
        AmazonAsyncHttpClient asyncClient = testAsyncClientBuilder().retryStrategy(retryStrategy).build();

        SdkHttpFullRequest httpFullRequest = ValidSdkObjects.sdkHttpFullRequest().build();
        NoopTestRequest sdkRequest = NoopTestRequest.builder().build();
        InterceptorContext interceptorContext = InterceptorContext
            .builder()
            .request(sdkRequest)
            .httpRequest(httpFullRequest)
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
                    .metricCollector(MetricCollector.create("test"))
                    .interceptorChain(new ExecutionInterceptorChain(Collections.emptyList()))
                    .build()
            )
            .execute(noOpResponseHandler());
    }
}
