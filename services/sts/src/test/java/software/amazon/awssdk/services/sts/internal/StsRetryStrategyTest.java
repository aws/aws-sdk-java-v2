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

package software.amazon.awssdk.services.sts.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.services.sts.model.IdpCommunicationErrorException;

public class StsRetryStrategyTest {
    private static final String OLD_GATE_PROPERTY = "aws.newRetries2026";

    @Test
    void resolveRetryStrategy_preexistingStrategy_returnsPreexisting() {
        RetryStrategy strategy = mock(RetryStrategy.class);
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.RETRY_STRATEGY, strategy)
                                                              .build();

        assertThat(StsRetryStrategy.resolveRetryStrategy(config)).isSameAs(strategy);
    }

    @Test
    void resolveRetryStrategy_retryPolicyConfigured_returnsNull() {
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.RETRY_POLICY, RetryPolicy.defaultRetryPolicy())
                                                              .build();

        assertThat(StsRetryStrategy.resolveRetryStrategy(config)).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    void resolveRetryStrategy_defaultNewRetries2026False_isIgnored() {
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.DEFAULT_NEW_RETRIES_2026, false)
                                                              .build();

        RetryStrategy resolved = StsRetryStrategy.resolveRetryStrategy(config);
        assertThat(resolved).isInstanceOf(StandardRetryStrategy.class);
        assertRetriesOnIdpCommunicationException(resolved);
    }

    @Test
    void resolveRetryStrategy_oldGateSystemPropertyFalse_isIgnored() {
        System.setProperty(OLD_GATE_PROPERTY, "false");
        try {
            SdkClientConfiguration config = SdkClientConfiguration.builder().build();
            RetryStrategy resolved = StsRetryStrategy.resolveRetryStrategy(config);
            assertThat(resolved).isInstanceOf(StandardRetryStrategy.class);
            assertRetriesOnIdpCommunicationException(resolved);
        } finally {
            System.clearProperty(OLD_GATE_PROPERTY);
        }
    }

    @Test
    void resolveRetryStrategy_default_usesRetries21() {
        RetryStrategy resolved = StsRetryStrategy.resolveRetryStrategy(SdkClientConfiguration.builder().build());
        assertThat(resolved).isInstanceOf(StandardRetryStrategy.class);
        assertThat(resolved.maxAttempts()).isEqualTo(3);

        RetryToken token = resolved.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .token(token)
                                                                   .failure(idpCommunicationError())
                                                                   .suggestedDelay(Duration.ofSeconds(60))
                                                                   .build();
        // 2.1 clamps the suggested delay to at most the backoff (at most 50ms) plus 5 seconds
        assertThat(resolved.refreshRetryToken(refresh).delay())
            .isBetween(Duration.ofSeconds(5), Duration.ofSeconds(5).plusMillis(50));
    }

    @ParameterizedTest
    @MethodSource("retryModeResolutionCases")
    void resolveRetryStrategy_returnsCorrectStrategyBasedOnMode(String mode, Class<?> expected) {
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), mode);
        try {
            SdkClientConfiguration config = SdkClientConfiguration.builder().build();
            RetryStrategy resolved = StsRetryStrategy.resolveRetryStrategy(config);
            assertThat(resolved).isInstanceOf(expected);
            assertRetriesOnIdpCommunicationException(resolved);
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
        }
    }

    @Test
    void resolveRetryStrategy_legacyMode_returnsNull() {
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), "legacy");
        try {
            assertThat(StsRetryStrategy.resolveRetryStrategy(SdkClientConfiguration.builder().build())).isNull();
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
        }
    }

    @ParameterizedTest
    @EnumSource(value = RetryMode.class, names = {"LEGACY", "ADAPTIVE"})
    @SuppressWarnings("deprecation")
    void resolveRetryStrategy_legacyOrAdaptiveDefaultMode_returnsNull(RetryMode mode) {
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.DEFAULT_RETRY_MODE, mode)
                                                              .build();
        assertThat(StsRetryStrategy.resolveRetryStrategy(config)).isNull();
    }

    private static IdpCommunicationErrorException idpCommunicationError() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                                                      .errorCode("IDPCommunicationError")
                                                      .build();

        return IdpCommunicationErrorException.builder()
                                             .awsErrorDetails(errorDetails)
                                             .build();
    }

    void assertRetriesOnIdpCommunicationException(RetryStrategy strategy) {
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();

        IdpCommunicationErrorException failure = idpCommunicationError();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .token(token)
                                                                   .failure(failure)
                                                                   .build();
        // Should not throw TokenAcquisitionFailedException
        assertThat(strategy.refreshRetryToken(refresh)).isNotNull();
    }

    private static Stream<Arguments> retryModeResolutionCases() {
        return Stream.of(
            Arguments.of("standard", StandardRetryStrategy.class),
            Arguments.of("adaptive", AdaptiveRetryStrategy.class)
        );
    }
}
