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

package software.amazon.awssdk.services.sts.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Unit tests for {@link StsCredentialsProvider#isNonRecoverableError(RuntimeException)}.
 */
class StsNonRecoverableErrorTest {

    static Stream<String> nonRecoverableErrorCodes() {
        return StsCredentialsProvider.NON_RECOVERABLE_ERROR_CODES.stream();
    }

    @ParameterizedTest
    @MethodSource("nonRecoverableErrorCodes")
    void isNonRecoverableError_matchingErrorCode_returnsTrue(String errorCode) {
        AwsServiceException exception = AwsServiceException.builder()
            .message("test")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode(errorCode)
                                            .errorMessage("test")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();

        assertThat(StsCredentialsProvider.isNonRecoverableError(exception)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("nonRecoverableErrorCodes")
    void isNonRecoverableError_matchingErrorCodeAsCause_returnsTrue(String errorCode) {
        AwsServiceException serviceException = AwsServiceException.builder()
            .message("test")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode(errorCode)
                                            .errorMessage("test")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();
        SdkClientException wrapper = SdkClientException.create("wrapped", serviceException);

        assertThat(StsCredentialsProvider.isNonRecoverableError(wrapper)).isTrue();
    }

    @Test
    void isNonRecoverableError_recoverableErrorCode_returnsFalse() {
        AwsServiceException exception = AwsServiceException.builder()
            .message("Throttling")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("Throttling")
                                            .errorMessage("Rate exceeded")
                                            .serviceName("STS")
                                            .build())
            .statusCode(400)
            .build();

        assertThat(StsCredentialsProvider.isNonRecoverableError(exception)).isFalse();
    }

    @Test
    void isNonRecoverableError_expiredTokenErrorCode_returnsFalse() {
        AwsServiceException exception = AwsServiceException.builder()
            .message("Token expired")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("ExpiredTokenException")
                                            .errorMessage("The security token included in the request is expired")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();

        assertThat(StsCredentialsProvider.isNonRecoverableError(exception)).isFalse();
    }

    @Test
    void isNonRecoverableError_sdkClientExceptionWithoutCause_returnsFalse() {
        SdkClientException exception = SdkClientException.create("network timeout");

        assertThat(StsCredentialsProvider.isNonRecoverableError(exception)).isFalse();
    }

    @Test
    void isNonRecoverableError_nullErrorDetails_returnsFalse() {
        AwsServiceException exception = AwsServiceException.builder()
            .message("no details")
            .statusCode(500)
            .build();

        assertThat(StsCredentialsProvider.isNonRecoverableError(exception)).isFalse();
    }

    @Test
    void isNonRecoverableError_nullErrorCode_returnsFalse() {
        AwsServiceException exception = AwsServiceException.builder()
            .message("test")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorMessage("test")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();

        assertThat(StsCredentialsProvider.isNonRecoverableError(exception)).isFalse();
    }

    @Test
    void isNonRecoverableError_deeplyNestedCause_onlyChecksImmediateCause() {
        // The predicate only looks one level deep for AwsServiceException — a deeply nested
        // non-recoverable error should NOT be treated as non-recoverable
        AwsServiceException accessDenied = AwsServiceException.builder()
            .message("test")
            .awsErrorDetails(AwsErrorDetails.builder()
                                            .errorCode("AccessDenied")
                                            .errorMessage("test")
                                            .serviceName("STS")
                                            .build())
            .statusCode(403)
            .build();
        RuntimeException intermediate = new RuntimeException("intermediate", accessDenied);
        SdkClientException wrapper = SdkClientException.create("outer", intermediate);

        // intermediate is a RuntimeException, not AwsServiceException, so the predicate should not
        // find the deeply nested AccessDenied
        assertThat(StsCredentialsProvider.isNonRecoverableError(wrapper)).isFalse();
    }

    @Test
    void nonRecoverableErrorCodes_containsExpectedCodes() {
        assertThat(StsCredentialsProvider.NON_RECOVERABLE_ERROR_CODES).containsExactlyInAnyOrder(
            "AccessDenied",
            "IDPRejectedClaim",
            "InvalidIdentityToken",
            "MalformedPolicyDocument",
            "PackedPolicyTooLarge",
            "RegionDisabledException"
        );
    }
}
