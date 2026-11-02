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

package software.amazon.awssdk.services.sts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.services.sts.model.ExpiredTokenException;
import software.amazon.awssdk.services.sts.model.StsException;

/**
 * Verifies that the generated STS {@code ExpiredTokenException} is treated as a modeled exception (no credential
 * invalidation, no auth-error retry) and that {@link StsException} with an auth error code is not.
 */
public class StsExpiredTokenClassificationTest {

    @Test
    void isAuthenticationError_modeledExpiredTokenException_returnsFalse() {
        assertThat(modeledExpiredTokenException().isAuthenticationError()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ExpiredToken", "ExpiredTokenException"})
    void isAuthenticationError_stsExceptionWithAuthErrorCode_returnsTrue(String errorCode) {
        assertThat(stsException(errorCode).isAuthenticationError()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RetryMode.class, names = {"LEGACY", "STANDARD"})
    void retryStrategy_modeledExpiredTokenException_isNotRetried(RetryMode mode) {
        RetryStrategy strategy = AwsRetryStrategy.forRetryMode(mode);

        assertThatThrownBy(() -> refreshRetryToken(strategy, modeledExpiredTokenException()))
            .isInstanceOf(TokenAcquisitionFailedException.class);
    }

    @ParameterizedTest
    @CsvSource({"LEGACY, ExpiredToken",
                "LEGACY, ExpiredTokenException",
                "STANDARD, ExpiredToken",
                "STANDARD, ExpiredTokenException"})
    void retryStrategy_stsExceptionWithAuthErrorCode_isRetried(RetryMode mode, String errorCode) {
        RetryStrategy strategy = AwsRetryStrategy.forRetryMode(mode);

        assertThat(refreshRetryToken(strategy, stsException(errorCode)).delay()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    private static RefreshRetryTokenResponse refreshRetryToken(RetryStrategy strategy, Throwable failure) {
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
        RefreshRetryTokenRequest refresh = RefreshRetryTokenRequest.builder()
                                                                   .token(token)
                                                                   .failure(failure)
                                                                   .build();
        return strategy.refreshRetryToken(refresh);
    }

    private static AwsServiceException modeledExpiredTokenException() {
        return ExpiredTokenException.builder()
                                    .awsErrorDetails(AwsErrorDetails.builder().errorCode("ExpiredTokenException").build())
                                    .statusCode(400)
                                    .build();
    }

    private static AwsServiceException stsException(String errorCode) {
        return StsException.builder()
                           .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
                           .statusCode(400)
                           .build();
    }
}
