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

package software.amazon.awssdk.awscore.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.base.Supplier;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.internal.DefaultRetryToken;

public class AwsRetryStrategyTest {

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void standardRetryStrategy_limitExceededException_retryBehaviorCorrect(boolean newRetries2026Enabled) {
        StandardRetryStrategy strategy = AwsRetryStrategy.standardRetryStrategy(newRetries2026Enabled);
        verifyLimitExceededBehavior(strategy, newRetries2026Enabled);
    }

    @ParameterizedTest
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void forRetryMode_limitExceededException_isRetried(RetryMode mode) {
        verifyLimitExceededBehavior(AwsRetryStrategy.forRetryMode(mode), true);
    }

    @Test
    void noArgFactories_limitExceededException_isRetried() {
        verifyLimitExceededBehavior(AwsRetryStrategy.standardRetryStrategy(), true);
        verifyLimitExceededBehavior(AwsRetryStrategy.adaptiveRetryStrategy(), true);
    }

    @Test
    void configure_nonLegacyBuilder_limitExceededException_isRetried() {
        verifyLimitExceededBehavior(AwsRetryStrategy.configure(StandardRetryStrategy.builder()).build(), true);
        verifyLimitExceededBehavior(AwsRetryStrategy.configure(AdaptiveRetryStrategy.builder()).build(), true);
    }

    @Test
    void configure_legacyBuilder_limitExceededException_isNotRetried() {
        verifyLimitExceededBehavior(AwsRetryStrategy.configure(LegacyRetryStrategy.builder()).build(), false);
    }

    @Test
    void configureStrategy_nonLegacyBuilder_limitExceededException_isRetried() {
        RetryStrategy.Builder<?, ?> builder = StandardRetryStrategy.builder();
        verifyLimitExceededBehavior(AwsRetryStrategy.configureStrategy(builder).build(), true);
    }

    @Test
    void configureStrategy_legacyBuilder_limitExceededException_isNotRetried() {
        RetryStrategy.Builder<?, ?> builder = LegacyRetryStrategy.builder();
        verifyLimitExceededBehavior(AwsRetryStrategy.configureStrategy(builder).build(), false);
    }

    @Test
    void legacyRetryStrategy_limitExceededException_isNotRetried() {
        verifyLimitExceededBehavior(AwsRetryStrategy.forRetryMode(RetryMode.LEGACY), false);
    }

    @ParameterizedTest
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void forRetryMode_clampsSuggestedDelay(RetryMode mode) {
        RetryStrategy strategy = AwsRetryStrategy.forRetryMode(mode);
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .failure(createTestException("Throttling"))
                                                                   .suggestedDelay(Duration.ofSeconds(60))
                                                                   .token(token)
                                                                   .build();
        // throttling backoff is at most 1s, and the suggested delay is clamped to the backoff + 5s
        assertThat(strategy.refreshRetryToken(refresh).delay()).isBetween(Duration.ofSeconds(5), Duration.ofSeconds(6));
    }

    private static void verifyLimitExceededBehavior(RetryStrategy strategy, boolean expectRetry) {

        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .failure(createTestException("LimitExceededException"))
                                                                   .token(token)
                                                                   .build();

        if (expectRetry) {
            assertThat(strategy.refreshRetryToken(refresh).delay()).isGreaterThanOrEqualTo(Duration.ZERO);
        } else {
            assertThatThrownBy(() -> strategy.refreshRetryToken(refresh))
                .isInstanceOf(TokenAcquisitionFailedException.class)
                .matches(e -> {
                    TokenAcquisitionFailedException acquireException = (TokenAcquisitionFailedException) e;
                    DefaultRetryToken exceptionToken = (DefaultRetryToken) acquireException.token();
                    return exceptionToken.state() == DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION;
                });
        }
    }

    @ParameterizedTest
    @CsvSource({"Throttling",
                "ThrottlingException",
                "ThrottledException",
                "RequestThrottledException",
                "TooManyRequestsException",
                "ProvisionedThroughputExceededException",
                "TransactionInProgressException",
                "RequestLimitExceeded",
                "BandwidthLimitExceeded",
                "LimitExceededException",
                "RequestThrottled",
                "SlowDown",
                "PriorRequestNotComplete",
                "EC2ThrottledException"})
    void standardRetryStrategy_retry21_throttlingBehaviorCorrect(String errorCode) {
        AwsServiceException exception = createTestException(errorCode);

        for (int i = 0; i < 128; ++i) {
            StandardRetryStrategy strategy = AwsRetryStrategy.standardRetryStrategy(true);

            RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
            RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                       .token(token)
                                                                       .failure(exception)
                                                                       .build();
            Duration delay = strategy.refreshRetryToken(refresh).delay();

            assertThat(delay).isBetween(Duration.ZERO, Duration.ofMillis(1000));
        }
    }

    private static AwsServiceException createTestException(String errorCode) {
        AwsErrorDetails details = AwsErrorDetails.builder()
                                                 .errorCode(errorCode)
                                                 .build();
        return AwsServiceException.builder().awsErrorDetails(details).build();
    }
}
