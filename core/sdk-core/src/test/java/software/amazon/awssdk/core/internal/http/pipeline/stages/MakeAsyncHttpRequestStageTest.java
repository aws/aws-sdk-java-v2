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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_ATTEMPT_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ASYNC_HTTP_CLIENT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SCHEDULED_EXECUTOR_SERVICE;
import static software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils.combinedAsyncResponseHandler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.core.internal.util.AsyncResponseHandlerTestUtils;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import utils.ValidSdkObjects;

@RunWith(MockitoJUnitRunner.class)
public class MakeAsyncHttpRequestStageTest {

    @Mock
    private SdkAsyncHttpClient sdkAsyncHttpClient;

    @Mock
    private ScheduledExecutorService timeoutExecutor;

    private CompletableFuture<Void> clientExecuteFuture = CompletableFuture.completedFuture(null);

    @Mock
    private ScheduledFuture future;

    private MakeAsyncHttpRequestStage stage;

    @Before
    public void setup() {
        when(sdkAsyncHttpClient.execute(any())).thenReturn(clientExecuteFuture);
        when(timeoutExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(future);
    }

    @Test
    public void apiCallAttemptTimeoutEnabled_shouldInvokeExecutor() throws Exception {
        stage = new MakeAsyncHttpRequestStage<>(
            combinedAsyncResponseHandler(AsyncResponseHandlerTestUtils.noOpResponseHandler(),
                                         AsyncResponseHandlerTestUtils.noOpResponseHandler()),
            clientDependencies(Duration.ofMillis(1000)));

        CompletableFuture<SdkHttpFullRequest> requestFuture = CompletableFuture.completedFuture(
                ValidSdkObjects.sdkHttpFullRequest().build());
        stage.execute(requestFuture, requestContext());

        verify(timeoutExecutor, times(1)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void apiCallAttemptTimeoutNotEnabled_shouldNotInvokeExecutor() throws Exception {
        stage = new MakeAsyncHttpRequestStage<>(
            combinedAsyncResponseHandler(AsyncResponseHandlerTestUtils.noOpResponseHandler(),
                                         AsyncResponseHandlerTestUtils.noOpResponseHandler()),
            clientDependencies(null));

        CompletableFuture<SdkHttpFullRequest> requestFuture = CompletableFuture.completedFuture(
                ValidSdkObjects.sdkHttpFullRequest().build());

        stage.execute(requestFuture, requestContext());

        verify(timeoutExecutor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testExecute_contextContainsMetricCollector_addsChildToExecuteRequest() {
        stage = new MakeAsyncHttpRequestStage<>(
                combinedAsyncResponseHandler(AsyncResponseHandlerTestUtils.noOpResponseHandler(),
                        AsyncResponseHandlerTestUtils.noOpResponseHandler()),
                clientDependencies(null));

        SdkHttpFullRequest sdkHttpRequest = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .host("mybucket.s3.us-west-2.amazonaws.com")
                .protocol("https")
                .build();

        MetricCollector mockCollector = mock(MetricCollector.class);
        MetricCollector childCollector = mock(MetricCollector.class);

        when(mockCollector.createChild(any(String.class))).thenReturn(childCollector);

        ExecutionContext executionContext = ExecutionContext.builder()
                .executionAttributes(new ExecutionAttributes())
                .build();

        RequestExecutionContext context = RequestExecutionContext.builder()
                .originalRequest(ValidSdkObjects.sdkRequest())
                .executionContext(executionContext)
                .build();

        context.attemptMetricCollector(mockCollector);

        CompletableFuture<SdkHttpFullRequest> requestFuture = CompletableFuture.completedFuture(sdkHttpRequest);

        try {
            stage.execute(requestFuture, context);
        } catch (Exception e) {
            e.printStackTrace();
            // ignored, don't really care about successful execution of the stage in this case
        } finally {
            ArgumentCaptor<AsyncExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(AsyncExecuteRequest.class);

            verify(mockCollector).createChild(eq("HttpClient"));
            verify(sdkAsyncHttpClient).execute(httpRequestCaptor.capture());
            assertThat(httpRequestCaptor.getValue().metricCollector()).contains(childCollector);
        }
    }

    @Test
    public void execute_handlerFutureCompletedNormally_futureCompletionExecutorRejectsWhenCompleteAsync_futureCompletedSynchronously() {
        ExecutorService mockExecutor = mock(ExecutorService.class);
        doThrow(new RejectedExecutionException("Busy")).when(mockExecutor).execute(any(Runnable.class));

        SdkClientConfiguration config =
            SdkClientConfiguration.builder()
                .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, mockExecutor)
                .option(ASYNC_HTTP_CLIENT, sdkAsyncHttpClient)
                .build();
        HttpClientDependencies dependencies = HttpClientDependencies.builder().clientConfiguration(config).build();

        TransformingAsyncResponseHandler mockHandler = mock(TransformingAsyncResponseHandler.class);
        CompletableFuture prepareFuture = new CompletableFuture();
        when(mockHandler.prepare()).thenReturn(prepareFuture);

        stage = new MakeAsyncHttpRequestStage<>(mockHandler, dependencies);

        CompletableFuture<SdkHttpFullRequest> requestFuture = CompletableFuture.completedFuture(
            ValidSdkObjects.sdkHttpFullRequest().build());

        CompletableFuture executeFuture = stage.execute(requestFuture, requestContext());

        long testThreadId = Thread.currentThread().getId();
        CompletableFuture afterWhenComplete =
            executeFuture.whenComplete((r, t) -> assertThat(Thread.currentThread().getId()).isEqualTo(testThreadId));

        prepareFuture.complete(null);

        afterWhenComplete.join();

        verify(mockExecutor).execute(any(Runnable.class));
    }

    @Test
    public void execute_handlerFutureCompletedExceptionally_doesNotAttemptSynchronousComplete() {
        String threadNamePrefix = "async-handle-test";
        ExecutorService mockExecutor = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().threadNamePrefix(threadNamePrefix).build());

        SdkClientConfiguration config =
            SdkClientConfiguration.builder()
                                  .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, mockExecutor)
                                  .option(ASYNC_HTTP_CLIENT, sdkAsyncHttpClient)
                                  .build();
        HttpClientDependencies dependencies = HttpClientDependencies.builder().clientConfiguration(config).build();

        TransformingAsyncResponseHandler mockHandler = mock(TransformingAsyncResponseHandler.class);
        CompletableFuture prepareFuture = spy(new CompletableFuture());
        when(mockHandler.prepare()).thenReturn(prepareFuture);

        stage = new MakeAsyncHttpRequestStage<>(mockHandler, dependencies);

        CompletableFuture<SdkHttpFullRequest> requestFuture = CompletableFuture.completedFuture(
            ValidSdkObjects.sdkHttpFullRequest().build());

        CompletableFuture executeFuture = stage.execute(requestFuture, requestContext());

        try {
            CompletableFuture afterHandle =
                executeFuture.handle((r, t) -> assertThat(Thread.currentThread().getName()).startsWith(threadNamePrefix));

            prepareFuture.completeExceptionally(new RuntimeException("parse error"));

            afterHandle.join();

            assertThatThrownBy(executeFuture::join)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("parse error");

            verify(prepareFuture, times(0)).whenComplete(any());
        } finally {
            mockExecutor.shutdown();
        }
    }

    private HttpClientDependencies clientDependencies(Duration timeout) {
        SdkClientConfiguration configuration = SdkClientConfiguration.builder()
                                                                     .option(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, Runnable::run)
                                                                     .option(ASYNC_HTTP_CLIENT, sdkAsyncHttpClient)
                                                                     .option(SCHEDULED_EXECUTOR_SERVICE, timeoutExecutor)
                                                                     .option(API_CALL_ATTEMPT_TIMEOUT, timeout)
                                                                     .build();


        return HttpClientDependencies.builder()
                                     .clientConfiguration(configuration)
                                     .build();
    }

    private RequestExecutionContext requestContext() {
        ExecutionContext executionContext = ClientExecutionAndRequestTimerTestUtils.executionContext(ValidSdkObjects.sdkHttpFullRequest().build());
        return RequestExecutionContext.builder()
                                      .executionContext(executionContext)
                                      .originalRequest(NoopTestRequest.builder().build())
                                      .build();
    }
}
