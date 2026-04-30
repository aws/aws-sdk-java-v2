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
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.services.sts.model.IdpCommunicationErrorException;

public class StsRetryStrategyTest {
    @Test
    void resolveRetryStrategy_preexistingStrategy_returnsPreexisting() {
        RetryStrategy strategy = mock(RetryStrategy.class);
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.RETRY_STRATEGY, strategy)
                                                              .build();

        assertThat(StsRetryStrategy.resolveRetryStrategy(config)).isSameAs(strategy);
    }

    @Test
    void resolveRetryStrategy_defaultNewRetries2026False_returnsNull() {
        SdkClientConfiguration config = SdkClientConfiguration.builder()
                                                              .option(SdkClientOption.DEFAULT_NEW_RETRIES_2026, false)
                                                              .build();

        assertThat(StsRetryStrategy.resolveRetryStrategy(config)).isNull();
    }

    @Test
    void resolveRetryStrategy_newRetries2026False_returnsNull() {
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "false");
        try {
            SdkClientConfiguration config = SdkClientConfiguration.builder().build();
            assertThat(StsRetryStrategy.resolveRetryStrategy(config)).isNull();
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        }
    }

    @ParameterizedTest
    @MethodSource("retryModeResolutionCases")
    void resolveRetryStrategy_returnsCorrectStrategyBasedOnMode(String mode, Class<?> expected) {
        System.setProperty(SdkSystemSetting.AWS_RETRY_MODE.property(), mode);
        System.setProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property(), "true");
        try {
            SdkClientConfiguration config = SdkClientConfiguration.builder().build();
            RetryStrategy resolved = StsRetryStrategy.resolveRetryStrategy(config);
            assertThat(resolved).isInstanceOf(expected);
            assertRetriesOnIdpCommunicationException(resolved);
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_RETRY_MODE.property());
            System.clearProperty(SdkSystemSetting.AWS_NEW_RETRIES_2026.property());
        }
    }

    void assertRetriesOnIdpCommunicationException(RetryStrategy strategy) {
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                                                      .errorCode("IDPCommunicationError")
                                                      .build();

        IdpCommunicationErrorException failure = IdpCommunicationErrorException.builder()
                                                                               .awsErrorDetails(errorDetails)
                                                                               .build();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .token(token)
                                                                   .failure(failure)
                                                                   .build();
        assertThat(strategy.refreshRetryToken(refresh).delay()).isGreaterThan(Duration.ZERO);
    }

    private static Stream<Arguments> retryModeResolutionCases() {
        return Stream.of(
            Arguments.of("standard", StandardRetryStrategy.class),
            Arguments.of("legacy", LegacyRetryStrategy.class),
            Arguments.of("adaptive", AdaptiveRetryStrategy.class)
        );
    }
}
