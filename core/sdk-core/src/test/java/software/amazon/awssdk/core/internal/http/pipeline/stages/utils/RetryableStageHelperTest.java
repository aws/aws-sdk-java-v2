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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
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

    @ParameterizedTest(name = "OPERATION_NAME = {0}")
    @NullSource
    @ValueSource(strings = "SubscribeToShard")
    void tryRefreshToken_forwardsOperationName(String operationName) {
        ExecutionAttributes.Builder attributes = ExecutionAttributes.builder();
        if (operationName != null) {
            attributes.put(SdkExecutionAttribute.OPERATION_NAME, operationName);
        }

        ArgumentCaptor<RefreshRetryTokenRequest> captor = refreshOnce(makeTestHelper(attributes.build()), false);

        assertThat(captor.getValue().operationName()).isEqualTo(Optional.ofNullable(operationName));
    }

    @Test
    void tryRefreshTokenAsync_forwardsOperationName() {
        ExecutionAttributes attributes = ExecutionAttributes.builder()
                                                            .put(SdkExecutionAttribute.OPERATION_NAME, "SubscribeToShard")
                                                            .build();

        ArgumentCaptor<RefreshRetryTokenRequest> captor = refreshOnce(makeTestHelper(attributes), true);

        assertThat(captor.getValue().operationName()).hasValue("SubscribeToShard");
    }

    private ArgumentCaptor<RefreshRetryTokenRequest> refreshOnce(RetryableStageHelper helper, boolean async) {
        AcquireInitialTokenResponse mockAcquireResponse = mock(AcquireInitialTokenResponse.class);
        when(mockAcquireResponse.token()).thenReturn(mock(RetryToken.class));
        when(mockRetryStrategy.acquireInitialToken(any())).thenReturn(mockAcquireResponse);

        RefreshRetryTokenResponse mockRefreshResponse = mock(RefreshRetryTokenResponse.class);
        when(mockRefreshResponse.delay()).thenReturn(Duration.ZERO);
        when(mockRetryStrategy.refreshRetryToken(any())).thenReturn(mockRefreshResponse);
        when(mockRetryStrategy.refreshRetryTokenAsync(any())).thenReturn(CompletableFuture.completedFuture(mockRefreshResponse));

        helper.acquireInitialToken();
        helper.setLastException(new RuntimeException());

        ArgumentCaptor<RefreshRetryTokenRequest> captor = ArgumentCaptor.forClass(RefreshRetryTokenRequest.class);
        if (async) {
            helper.tryRefreshTokenAsync(Duration.ZERO).join();
            verify(mockRetryStrategy).refreshRetryTokenAsync(captor.capture());
        } else {
            helper.tryRefreshToken(Duration.ZERO);
            verify(mockRetryStrategy).refreshRetryToken(captor.capture());
        }
        return captor;
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("legacyRetryAfterHeaderTestParams")
    void usesLegacyRetryAfterHeaders_dependsOnRetryStrategyInUse(String description,
                                                                 SdkClientConfiguration clientConfig,
                                                                 boolean expected) {
        RetryableStageHelper helper = makeTestHelper(ExecutionAttributes.builder().build(), clientConfig);
        assertThat(helper.usesLegacyRetryAfterHeaders()).isEqualTo(expected);
    }

    @SuppressWarnings("deprecation")
    private static Stream<Arguments> legacyRetryAfterHeaderTestParams() {
        return Stream.of(
            Arguments.of("STANDARD strategy", strategyConfig(SdkDefaultRetryStrategy.forRetryMode(RetryMode.STANDARD)), false),
            Arguments.of("ADAPTIVE_V2 strategy", strategyConfig(SdkDefaultRetryStrategy.forRetryMode(RetryMode.ADAPTIVE_V2)),
                         false),
            Arguments.of("default strategy", strategyConfig(SdkDefaultRetryStrategy.defaultRetryStrategy()), false),
            Arguments.of("custom strategy", strategyConfig(mock(RetryStrategy.class)), true),
            Arguments.of("STANDARD strategy without retry behavior 2.1",
                         strategyConfig(SdkDefaultRetryStrategy.forRetryMode(RetryMode.STANDARD, false)), true),
            Arguments.of("ADAPTIVE_V2 strategy without retry behavior 2.1",
                         strategyConfig(SdkDefaultRetryStrategy.forRetryMode(RetryMode.ADAPTIVE_V2, false)), true),
            Arguments.of("LEGACY strategy", strategyConfig(SdkDefaultRetryStrategy.forRetryMode(RetryMode.LEGACY)), true),
            Arguments.of("ADAPTIVE strategy (RetryPolicyAdapter)",
                         strategyConfig(SdkDefaultRetryStrategy.forRetryMode(RetryMode.ADAPTIVE)), true),
            Arguments.of("RetryPolicy",
                         SdkClientConfiguration.builder()
                                               .option(SdkClientOption.RETRY_POLICY, RetryPolicy.forRetryMode(RetryMode.STANDARD))
                                               .option(SdkClientOption.RETRY_STRATEGY,
                                                       SdkDefaultRetryStrategy.forRetryMode(RetryMode.STANDARD))
                                               .build(),
                         true)
        );
    }

    private static SdkClientConfiguration strategyConfig(RetryStrategy strategy) {
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.RETRY_STRATEGY, strategy)
                                     .build();
    }

    RetryableStageHelper makeTestHelper(ExecutionAttributes executionAttributes) {
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(SdkClientOption.RETRY_STRATEGY, mockRetryStrategy)
                                                                    .build();
        return makeTestHelper(executionAttributes, clientConfig);
    }

    RetryableStageHelper makeTestHelper(ExecutionAttributes executionAttributes, SdkClientConfiguration clientConfig) {
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
