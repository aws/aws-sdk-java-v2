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

package software.amazon.awssdk.core.internal.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;

class SdkDefaultRetryStrategyTest {
    private static final Duration SUGGESTED_DELAY = Duration.ofSeconds(60);

    @ParameterizedTest(name = "{0}")
    @MethodSource("retries21Strategies")
    void retries21Strategy_clampsSuggestedDelay(String description, Supplier<RetryStrategy> strategy) {
        RefreshRetryTokenResponse response = refreshWithSuggestedDelay(strategy.get());
        // 2.1: backoff (at most 50ms, jittered) <= delay <= backoff + 5s
        assertThat(response.delay()).isBetween(Duration.ofSeconds(5), Duration.ofSeconds(5).plusMillis(50));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("retries21Strategies")
    void retries21Strategy_afterToBuilder_clampsSuggestedDelay(String description, Supplier<RetryStrategy> strategy) {
        RetryStrategy rebuilt = strategy.get().toBuilder().maxAttempts(5).build();
        RefreshRetryTokenResponse response = refreshWithSuggestedDelay(rebuilt);
        assertThat(response.delay()).isBetween(Duration.ofSeconds(5), Duration.ofSeconds(5).plusMillis(50));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void forRetryMode_retries21Disabled_doesNotClampSuggestedDelay(RetryMode mode) {
        RefreshRetryTokenResponse response = refreshWithSuggestedDelay(SdkDefaultRetryStrategy.forRetryMode(mode, false));
        assertThat(response.delay()).isEqualTo(SUGGESTED_DELAY);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void forRetryMode_returnsExpectedStrategyType(RetryMode mode) {
        Class<?> expected = mode == RetryMode.STANDARD ? StandardRetryStrategy.class : AdaptiveRetryStrategy.class;
        assertThat(SdkDefaultRetryStrategy.forRetryMode(mode)).isInstanceOf(expected);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"LEGACY"})
    void forRetryMode_legacy_returnsLegacyStrategy(RetryMode mode) {
        RetryStrategy strategy = SdkDefaultRetryStrategy.forRetryMode(mode);
        assertThat(strategy).isInstanceOf(LegacyRetryStrategy.class);
        assertThat(strategy.maxAttempts()).isEqualTo(4);
        // Legacy keeps the larger of the suggested delay and the computed backoff
        assertThat(refreshWithSuggestedDelay(strategy).delay()).isEqualTo(SUGGESTED_DELAY);
    }

    private static Stream<Arguments> retries21Strategies() {
        return Stream.of(
            Arguments.of("forRetryMode(STANDARD)",
                         (Supplier<RetryStrategy>) () -> SdkDefaultRetryStrategy.forRetryMode(RetryMode.STANDARD)),
            Arguments.of("forRetryMode(ADAPTIVE_V2)",
                         (Supplier<RetryStrategy>) () -> SdkDefaultRetryStrategy.forRetryMode(RetryMode.ADAPTIVE_V2)),
            Arguments.of("standardRetryStrategy()",
                         (Supplier<RetryStrategy>) SdkDefaultRetryStrategy::standardRetryStrategy),
            Arguments.of("adaptiveRetryStrategy()",
                         (Supplier<RetryStrategy>) SdkDefaultRetryStrategy::adaptiveRetryStrategy),
            Arguments.of("standardRetryStrategyBuilder()",
                         (Supplier<RetryStrategy>) () -> SdkDefaultRetryStrategy.standardRetryStrategyBuilder().build()),
            Arguments.of("adaptiveRetryStrategyBuilder()",
                         (Supplier<RetryStrategy>) () -> SdkDefaultRetryStrategy.adaptiveRetryStrategyBuilder().build()),
            Arguments.of("defaultRetryStrategy()",
                         (Supplier<RetryStrategy>) SdkDefaultRetryStrategy::defaultRetryStrategy)
        );
    }

    private static RefreshRetryTokenResponse refreshWithSuggestedDelay(RetryStrategy strategy) {
        AcquireInitialTokenResponse initial = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test"));
        return strategy.refreshRetryToken(RefreshRetryTokenRequest.builder()
                                                                  .token(initial.token())
                                                                  .failure(SdkServiceException.builder()
                                                                                              .statusCode(500)
                                                                                              .build())
                                                                  .suggestedDelay(SUGGESTED_DELAY)
                                                                  .build());
    }
}
