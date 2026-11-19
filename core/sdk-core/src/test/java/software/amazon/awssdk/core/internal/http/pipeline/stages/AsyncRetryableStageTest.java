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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.TransformingAsyncResponseHandler;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.internal.DefaultAwareRetryStrategy;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class AsyncRetryableStageTest extends BaseRetryableStageTest {
    private RetryStrategy mockRetryStrategy;
    private AcquireInitialTokenResponse mockAcquireInitialTokenResponse;
    private RetryToken mockRetryToken;

    private RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<SdkResponse>>> mockDelegatePipeline;

    private static ScheduledExecutorService executorService;

    @BeforeAll
    static void setup() {
        executorService = Executors.newScheduledThreadPool(1);
    }

    @AfterAll
    static void teardown() {
        executorService.shutdownNow();
    }

    @BeforeEach
    void methodSetup() {
        mockAcquireInitialTokenResponse = mock(AcquireInitialTokenResponse.class);
        mockRetryToken = mock(RetryToken.class);

        when(mockAcquireInitialTokenResponse.token()).thenReturn(mockRetryToken);
        when(mockAcquireInitialTokenResponse.delay()).thenReturn(Duration.ZERO);

        mockRetryStrategy = newMockRetryStrategy(RetryStrategy.class);

        mockDelegatePipeline = mock(RequestPipeline.class);
    }

    private RetryStrategy newMockRetryStrategy(Class<? extends RetryStrategy> strategyClass) {
        RetryStrategy strategy = mock(strategyClass, withSettings().extraInterfaces(DefaultAwareRetryStrategy.class));
        when(((DefaultAwareRetryStrategy) strategy).usesRetryBehavior21()).thenReturn(strategyClass != LegacyRetryStrategy.class);
        when(strategy.acquireInitialTokenAsync(any()))
            .thenReturn(CompletableFuture.completedFuture(mockAcquireInitialTokenResponse));
        return strategy;
    }

    @ParameterizedTest
    @MethodSource("acquireDelayTestCases")
    void execute_acquireDelay_behavesCorrectly(AcquireDelayTestCase testCase) throws Exception {
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, executorService);

        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder()
                                                                      .statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        if (testCase.isFailure()) {
            when(mockRetryStrategy.refreshRetryTokenAsync(any())).thenReturn(
                CompletableFutureUtils.failedFuture(new TokenAcquisitionFailedException("Acquire failed", mockRetryToken, null,
                                                                           testCase.failureDelay()))
            );
        } else {
            // only retry once, otherwise we'll get into an infinite loop
            AtomicBoolean first = new AtomicBoolean();
            when(mockRetryStrategy.refreshRetryTokenAsync(any())).thenAnswer(i -> {
                if (first.compareAndSet(false, true)) {
                    return CompletableFuture.completedFuture(RefreshRetryTokenResponse.create(mockRetryToken,
                                                                                              testCase.successDelay()));
                }
                return CompletableFutureUtils.failedFuture(new TokenAcquisitionFailedException("Acquire failed",
                                                                                               mockRetryToken,
                                                                                               null,
                                                                                               Duration.ZERO));
            });
        }

        long start = System.nanoTime();
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();
        CompletableFuture<Response<SdkResponse>> execute = retryableStage.execute(httpRequest, ctx);
        // exception thrown doesn't matter, just results in exception because we mock just enough...
        assertThatThrownBy(execute::join);
        long end = System.nanoTime();

        Duration lowerBound = testCase.expectedDelay();
        assertThat(Duration.ofNanos(end - start)).isBetween(lowerBound, lowerBound.plusMillis(250));
    }


    @ParameterizedTest
    @MethodSource("retryAfterTestCases")
    void execute_retryableException_treatsRetryAfterCorrectly(RetryAfterTestCase testCase) throws Exception {
        mockRetryStrategy = newMockRetryStrategy(testCase.isLegacyRetryStrategy() ? LegacyRetryStrategy.class
                                                                                  : StandardRetryStrategy.class);

        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, executorService);

        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder()
                                                                      .statusCode(502);

        if (testCase.retryAfter() != null) {
            httpResponse.putHeader(RETRY_AFTER_HEADER, testCase.retryAfter());
        }

        if (testCase.xAmzRetryAfter() != null) {
            httpResponse.putHeader(X_AMZ_RETRY_AFTER_HEADER, testCase.xAmzRetryAfter());
        }

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        CompletableFuture<Response<SdkResponse>> execute = retryableStage.execute(httpRequest, ctx);
        // exception thrown doesn't matter, just results in exception because we mock just enough...
        assertThatThrownBy(execute::join);

        ArgumentCaptor<RefreshRetryTokenRequest> refreshRequestCaptor = ArgumentCaptor.forClass(RefreshRetryTokenRequest.class);

        verify(mockRetryStrategy).refreshRetryTokenAsync(refreshRequestCaptor.capture());

        RefreshRetryTokenRequest refreshRequest = refreshRequestCaptor.getValue();

        // Unlike the sync stage, the async stage never passes a suggested delay to a legacy strategy.
        Duration expectedDelay = testCase.isLegacyRetryStrategy() ? Duration.ZERO : testCase.expectedDelay();
        assertThat(refreshRequest.suggestedDelay().get()).isEqualTo(expectedDelay);
    }

    @ParameterizedTest(name = "Legacy retry strategy = {0}")
    @CsvSource({"true", "false"})
    void execute_delegateThrows_noHttpResponse_uses0SuggestedDelay(boolean legacyRetryStrategy) throws Exception {
        mockRetryStrategy = newMockRetryStrategy(legacyRetryStrategy ? LegacyRetryStrategy.class : StandardRetryStrategy.class);
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, executorService);

        RequestExecutionContext ctx = createRequestExecutionContext();

        CompletableFuture<Response<SdkResponse>> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("connection"));
        when(mockDelegatePipeline.execute(any(), any())).thenReturn(future);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        CompletableFuture<Response<SdkResponse>> execute = retryableStage.execute(httpRequest, ctx);
        // exception thrown doesn't matter, just results in exception because we mock just enough...
        assertThatThrownBy(execute::join);

        ArgumentCaptor<RefreshRetryTokenRequest> refreshRequestCaptor = ArgumentCaptor.forClass(RefreshRetryTokenRequest.class);

        verify(mockRetryStrategy).refreshRetryTokenAsync(refreshRequestCaptor.capture());

        RefreshRetryTokenRequest refreshRequest = refreshRequestCaptor.getValue();

        assertThat(refreshRequest.suggestedDelay().get()).isEqualTo(Duration.ZERO);
    }

    @Test
    void execute_isFirstAttempt_acquireInitialTokenCompleteSameThread_doesNotSchedule() throws Exception {
        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder().statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        ScheduledExecutorService exec = mock(ScheduledExecutorService.class);

        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, exec);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        CompletableFuture<Response<SdkResponse>> executeFuture = retryableStage.execute(httpRequest, ctx);

        assertThatThrownBy(executeFuture::join);

        verifyNoInteractions(exec);
    }

    @Test
    void execute_isFirstAttempt_acquireInitialTokenCompleteSameDurationNonZero_schedules() throws Exception {
        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder().statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));


        Duration delay = Duration.ofMillis(1);

        AcquireInitialTokenResponse acquireTokenResponse = AcquireInitialTokenResponse.create(mock(RetryToken.class), delay);
        when(mockRetryStrategy.acquireInitialTokenAsync(any(AcquireInitialTokenRequest.class))).thenReturn(
            CompletableFuture.completedFuture(acquireTokenResponse));

        ScheduledExecutorService mockExec = mock(ScheduledExecutorService.class);
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, mockExec);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();
        Phaser phaser = new Phaser(2);

        when(mockExec.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.MILLISECONDS))).thenAnswer(i -> {
            Runnable r = i.getArgument(0);
            r.run();
            phaser.arrive();
            return null;
        });

        retryableStage.execute(httpRequest, ctx);

        phaser.arriveAndAwaitAdvance();

        verify(mockExec).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void execute_isFirstAttempt_acquireInitialTokenCompleteSameThreadDurationNonZero_schedules() throws Exception {
        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder().statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));


        Duration delay = Duration.ofMillis(1);

        AcquireInitialTokenResponse acquireTokenResponse = AcquireInitialTokenResponse.create(mock(RetryToken.class), delay);
        when(mockRetryStrategy.acquireInitialTokenAsync(any(AcquireInitialTokenRequest.class))).thenReturn(
            CompletableFuture.completedFuture(acquireTokenResponse));

        ScheduledExecutorService mockExec = mock(ScheduledExecutorService.class);
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, mockExec);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();
        Phaser phaser = new Phaser(2);

        when(mockExec.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.MILLISECONDS))).thenAnswer(i -> {
            Runnable r = i.getArgument(0);
            r.run();
            phaser.arrive();
            return null;
        });

        retryableStage.execute(httpRequest, ctx);

        phaser.arriveAndAwaitAdvance();

        verify(mockExec).schedule(any(Runnable.class), eq(1L), eq(TimeUnit.MILLISECONDS));
    }


    @Test
    void execute_isFirstAttempt_scheduleFails_completesExceptionally() throws Exception {
        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder().statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));


        Duration delay = Duration.ofMillis(1);

        AcquireInitialTokenResponse acquireTokenResponse = AcquireInitialTokenResponse.create(mock(RetryToken.class), delay);
        when(mockRetryStrategy.acquireInitialTokenAsync(any(AcquireInitialTokenRequest.class))).thenReturn(
            CompletableFuture.completedFuture(acquireTokenResponse));

        ScheduledExecutorService mockExec = mock(ScheduledExecutorService.class);
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, mockExec);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        when(mockExec.schedule(any(Runnable.class), eq(1L), eq(TimeUnit.MILLISECONDS)))
            .thenThrow(new RejectedExecutionException("closed"));

        CompletableFuture<Response<SdkResponse>> future = retryableStage.execute(httpRequest, ctx);

        assertThatThrownBy(future::join).hasRootCauseInstanceOf(RejectedExecutionException.class)
                                        .hasRootCauseMessage("closed");
    }

    @Test
    void execute_isRetryAttempt_scheduleFails_completesExceptionally() throws Exception {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

        when(mockExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenThrow(new RejectedExecutionException("closed"));
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, mockExecutor);

        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder()
                                                                      .statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

            // only retry once, otherwise we'll get into an infinite loop
            AtomicBoolean first = new AtomicBoolean();
            when(mockRetryStrategy.refreshRetryTokenAsync(any())).thenAnswer(i -> {
                if (first.compareAndSet(false, true)) {
                    return CompletableFuture.completedFuture(RefreshRetryTokenResponse.create(mockRetryToken, Duration.ZERO));
                }
                return CompletableFutureUtils.failedFuture(new TokenAcquisitionFailedException("Acquire failed",
                                                                                               mockRetryToken,
                                                                                               null,
                                                                                               Duration.ZERO));
            });

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();
        CompletableFuture<Response<SdkResponse>> execute = retryableStage.execute(httpRequest, ctx);
        assertThatThrownBy(execute::join).hasRootCauseInstanceOf(RejectedExecutionException.class).hasRootCauseMessage("closed");
    }

    @Test
    void execute_retryDisallowed_delayNonZero_scheduleFails_completesExceptionally() throws Exception {
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);

        when(mockExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenThrow(new RejectedExecutionException("closed"));
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, mockExecutor);

        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder()
                                                                      .statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        // only retry once, otherwise we'll get into an infinite loop
        AtomicBoolean first = new AtomicBoolean();
        when(mockRetryStrategy.refreshRetryTokenAsync(any()))
            .thenAnswer(i ->
                            CompletableFutureUtils.failedFuture(new TokenAcquisitionFailedException("Acquire failed",
                                                                                                    mockRetryToken,
                                                                                                    null,
                                                                                                    Duration.ofSeconds(1))));

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();
        CompletableFuture<Response<SdkResponse>> execute = retryableStage.execute(httpRequest, ctx);
        assertThatThrownBy(execute::join).hasRootCauseInstanceOf(RejectedExecutionException.class).hasRootCauseMessage("closed");
    }

    @Test
    void execute_isFirstAttempt_acquireInitialTokenCompleteDifferentThread_schedules() throws Exception {
        RequestExecutionContext ctx = createRequestExecutionContext();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder().statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        AcquireInitialTokenResponse acquireTokenResponse = AcquireInitialTokenResponse.create(mock(RetryToken.class),
                                                                                              Duration.ZERO);

        ScheduledExecutorService mockExec = mock(ScheduledExecutorService.class);
        AsyncRetryableStage<SdkResponse> retryableStage = createRetryableStage(mockRetryStrategy, mockExec);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        // wait for execution to "start" before completing the future
        Phaser phaser = new Phaser(2);

        when(mockRetryStrategy.acquireInitialTokenAsync(any(AcquireInitialTokenRequest.class)))
            .thenAnswer(i -> {
                CompletableFuture<AcquireInitialTokenResponse> future = new CompletableFuture<>();
                executorService.submit(() -> {
                    phaser.arriveAndAwaitAdvance();
                    future.complete(acquireTokenResponse);
                });
                return future;
            });

        when(mockExec.schedule(any(Runnable.class), eq(0L), eq(TimeUnit.MILLISECONDS))).thenAnswer(i -> {
            Runnable r = i.getArgument(0);
            r.run();
            return null;
        });

        CompletableFuture<Response<SdkResponse>> executeFuture = retryableStage.execute(httpRequest, ctx);

        phaser.arrive();
        assertThatThrownBy(executeFuture::join);

        verify(mockExec).schedule(any(Runnable.class), eq(0L), eq(TimeUnit.MILLISECONDS));
    }

    private AsyncRetryableStage<SdkResponse> createRetryableStage(RetryStrategy retryStrategy,
                                                                  ScheduledExecutorService scheduler) {
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(SdkClientOption.RETRY_STRATEGY, retryStrategy)
                                                                    .option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE,
                                                                            scheduler)
                                                                    .build();

        HttpClientDependencies deps = HttpClientDependencies.builder()
                                                            .clientConfiguration(clientConfig)
                                                            .build();

        return new AsyncRetryableStage<>(mock(TransformingAsyncResponseHandler.class),
                                         deps, mockDelegatePipeline);
    }

    private RequestExecutionContext createRequestExecutionContext() {
        ExecutionAttributes execAttrs = ExecutionAttributes.builder().build();

        ExecutionContext execCtx = ExecutionContext.builder()
                                                   .metricCollector(NoOpMetricCollector.create())
                                                   .executionAttributes(execAttrs)
                                                   .build();

        return RequestExecutionContext.builder()
                                                             .originalRequest(mock(SdkRequest.class))
                                                             .executionContext(execCtx)
                                                             .build();
    }
}
