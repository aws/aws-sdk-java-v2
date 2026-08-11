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

package software.amazon.awssdk.core.internal.http.pipeline.stages.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Either;

public class RetryableStageHelperTest {
    private RetryStrategy mockRetryStrategy;

    @BeforeEach
    void setup() {
        mockRetryStrategy = mock(RetryStrategy.class);
    }

    @ParameterizedTest(name = "IS_LONG_POLLING = {0}, expected = {1}")
    @MethodSource("longPollingValueTestParams")
    void tryRefreshToken_forwardsLongPollingAttrValue(Boolean attribute, boolean expected) {
        ExecutionAttributes.Builder attributes = ExecutionAttributes.builder();
        if (attribute != null) {
            attributes.put(SdkInternalExecutionAttribute.IS_LONG_POLLING, attribute);
        }

        RetryableStageHelper helper = makeTestHelper(attributes.build());

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
        when(mockRefreshResponse.delay()).thenReturn(Duration.ZERO);
        ArgumentCaptor<RefreshRetryTokenRequest> refreshRequestCaptor = ArgumentCaptor.forClass(RefreshRetryTokenRequest.class);

        when(mockRetryStrategy.refreshRetryToken(any())).thenReturn(mockRefreshResponse);

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());
        helper.tryRefreshToken(Duration.ZERO);

        verify(mockRetryStrategy).refreshRetryToken(refreshRequestCaptor.capture());

        assertThat(refreshRequestCaptor.getValue().isLongPolling()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "delay on successful refresh = {0}, delay on failed refresh = {1}")
    @MethodSource("refreshBackoffTestParams")
    void tryRefreshToken_returnsCorrectBackoff(Duration successDelay, Duration failureDelay) {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build());

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        if (successDelay != null) {
            RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
            when(mockRefreshResponse.delay()).thenReturn(successDelay);
            when(mockRetryStrategy.refreshRetryToken(any())).thenReturn(mockRefreshResponse);
        } else {
            when(mockRetryStrategy.refreshRetryToken(any())).thenThrow(
                new TokenAcquisitionFailedException("failed", token, null, failureDelay)
            );
        }

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());
        Either<Duration, Duration> backoff = helper.tryRefreshToken(Duration.ZERO);

        if (successDelay != null) {
            assertThat(backoff.left().get()).isEqualTo(successDelay);
        } else {
            assertThat(backoff.right().get()).isEqualTo(failureDelay);
        }
    }

    @Test
    void tryRefreshTokenAsync_refreshThrowsAcquireFailure_wrappedInCompletionException_acquireFailureDurationReturned() {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build());

        Duration failureAcquireDuration = Duration.ofSeconds(1);
        TokenAcquisitionFailedException acquireException = new TokenAcquisitionFailedException("could not acquire",
                                                                                               mock(RetryToken.class), null,
                                                                                               failureAcquireDuration);

        // This chaining is important to make sure whenComplete sees a CompletionException instead of the
        // TokenAcquisitionFailedException directly
        CompletableFuture<RefreshRetryTokenResponse> future1 = CompletableFutureUtils.failedFuture(acquireException);
        CompletableFuture<RefreshRetryTokenResponse> toReturn = future1.thenApply(Function.identity());

        when(mockRetryStrategy.refreshRetryTokenAsync(any(RefreshRetryTokenRequest.class)))
            .thenReturn(toReturn);

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());

        assertThat(helper.tryRefreshTokenAsync(Duration.ZERO).join().right()).hasValue(failureAcquireDuration);
    }

    @Test
    void tryRefreshTokenAsync_refreshThrowsAcquireFailure__acquireFailureDurationReturned() {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build());

        Duration failureAcquireDuration = Duration.ofSeconds(1);
        TokenAcquisitionFailedException acquireException = new TokenAcquisitionFailedException("could not acquire",
                                                                                               mock(RetryToken.class), null,
                                                                                               failureAcquireDuration);

        CompletableFuture<RefreshRetryTokenResponse> future = CompletableFutureUtils.failedFuture(acquireException);

        when(mockRetryStrategy.refreshRetryTokenAsync(any(RefreshRetryTokenRequest.class)))
            .thenReturn(future);

        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        RetryToken token = mock(RetryToken.class);
        when(mockAcquireResponse.token()).thenReturn(token);
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        helper.acquireInitialToken();

        helper.setLastException(new RuntimeException());

        assertThat(helper.tryRefreshTokenAsync(Duration.ZERO).join().right()).hasValue(failureAcquireDuration);
    }

    RetryableStageHelper makeTestHelper(ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder()
                                                           .method(SdkHttpMethod.GET)
                                                           .uri(URI.create("https://my-service.amazonaws.com"))
                                                           .build();

        ExecutionContext executionContext = ExecutionContext.builder()
                                                            .executionAttributes(executionAttributes)
                                                            .build();

        RequestExecutionContext requestExecutionContext = RequestExecutionContext.builder()
                                                                                 .originalRequest(mock(SdkRequest.class))
                                                                                 .executionContext(executionContext)
                                                                                 .build();

        RetryStrategy retryStrategy = mockRetryStrategy;

        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(SdkClientOption.RETRY_STRATEGY, retryStrategy)
                                                                    .build();

        HttpClientDependencies dependencies = HttpClientDependencies.builder()
                                                                    .clientConfiguration(clientConfig)
                                                                    .build();

        return new RetryableStageHelper(httpRequest, requestExecutionContext, dependencies);
    }

    private static Stream<Arguments> longPollingValueTestParams() {
        return Stream.of(
            // Absent should default to false
            Arguments.of(null, false),
            Arguments.of(true, true),
            Arguments.of(false, false)
        );
    }

    private static Stream<Arguments> refreshBackoffTestParams() {
        return Stream.of(
            Arguments.of(null, Duration.ofSeconds(1)),
            Arguments.of(Duration.ofSeconds(1), null)
        );
    }
}
