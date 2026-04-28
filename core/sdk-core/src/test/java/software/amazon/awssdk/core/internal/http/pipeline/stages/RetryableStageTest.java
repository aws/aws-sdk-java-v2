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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;

public class RetryableStageTest {
    private RetryStrategy mockRetryStrategy;
    private AcquireInitialTokenResponse mockAcquireInitialTokenResponse;
    private RetryToken mockRetryToken;

    private RequestPipeline<SdkHttpFullRequest, Response<SdkResponse>> mockDelegatePipeline;

    @BeforeEach
    void setup() {
        mockRetryStrategy = mock(RetryStrategy.class);
        mockAcquireInitialTokenResponse = mock(AcquireInitialTokenResponse.class);
        mockRetryToken = mock(RetryToken.class);

        when(mockAcquireInitialTokenResponse.token()).thenReturn(mockRetryToken);
        when(mockAcquireInitialTokenResponse.delay()).thenReturn(Duration.ZERO);

        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireInitialTokenResponse);

        mockDelegatePipeline = mock(RequestPipeline.class);
    }

    @ParameterizedTest
    @MethodSource("acquireDelayTestCases")
    void execute_acquireDelay_behavesCorrectly(AcquireDelayTestCase testCase) throws Exception {
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(SdkClientOption.RETRY_STRATEGY, mockRetryStrategy)
                                                                    .build();

        HttpClientDependencies deps = HttpClientDependencies.builder()
                                                            .clientConfiguration(clientConfig)
                                                            .build();

        RetryableStage<SdkResponse> retryableStage = new RetryableStage<>(deps, mockDelegatePipeline);

        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        ExecutionAttributes execAttrs = ExecutionAttributes.builder()
                                                           .build();

        ExecutionContext execCtx = ExecutionContext.builder()
                                                   .executionAttributes(execAttrs)
                                                   .build();

        RequestExecutionContext ctx = RequestExecutionContext.builder()
                                                             .originalRequest(mock(SdkRequest.class))
                                                             .executionContext(execCtx)
                                                             .build();

        SdkHttpFullResponse.Builder httpResponse = SdkHttpFullResponse.builder()
                                                                      .statusCode(502);

        Response<SdkResponse> response = Response.<SdkResponse>builder()
                                                 .httpResponse(httpResponse.build())
                                                 .isSuccess(false)
                                                 .exception(SdkException.builder().build())
                                                 .build();

        when(mockDelegatePipeline.execute(any(), any())).thenReturn(response);

        if (testCase.failure) {
            when(mockRetryStrategy.refreshRetryToken(any())).thenThrow(
                new TokenAcquisitionFailedException("Acquire failed", mockRetryToken, null, testCase.failureDelay));
        } else {
            // only retry once, otherwise we'll get into an infinite loop
            AtomicBoolean first = new AtomicBoolean();
            when(mockRetryStrategy.refreshRetryToken(any())).thenAnswer(i -> {
                if (first.compareAndSet(false, true)) {
                    return RefreshRetryTokenResponse.create(mockRetryToken, testCase.successDelay);
                }
                throw new TokenAcquisitionFailedException("Acquire failed", mockRetryToken, null, Duration.ZERO);
            });
        }

        long start = System.nanoTime();
        // exception thrown doesn't matter, just results in exception because we mock just enough...
        assertThatThrownBy(() -> retryableStage.execute(httpRequest, ctx));
        long end = System.nanoTime();

        Duration lowerBound = testCase.expectedDelay();
        assertThat(Duration.ofNanos(end - start)).isBetween(lowerBound, lowerBound.plusMillis(250));
    }

    private static Stream<AcquireDelayTestCase> acquireDelayTestCases() {
        return Stream.of(
            new AcquireDelayTestCase(true, Duration.ofDays(1), Duration.ZERO),
            new AcquireDelayTestCase(true, Duration.ofDays(1), Duration.ofMillis(100)),


            new AcquireDelayTestCase(false, Duration.ZERO, Duration.ofDays(1)),
            new AcquireDelayTestCase(false, Duration.ofMillis(100), Duration.ofDays(1))
        );
    }

    private static class AcquireDelayTestCase {
        private boolean failure;
        private Duration successDelay;
        private Duration failureDelay;

        public AcquireDelayTestCase(boolean failure, Duration successDelay, Duration failureDelay) {
            this.failure = failure;
            this.successDelay = successDelay;
            this.failureDelay = failureDelay;
        }

        public Duration expectedDelay() {
            if (failure) {
                return failureDelay;
            }
            return successDelay;
        }

        @Override
        public String toString() {
            return (failure ? "Failure" : "Success") + " with delay " + expectedDelay();
        }
    }
}
