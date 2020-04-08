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

package software.amazon.awssdk.awscore.internal;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Contains AWS error codes.
 */
@SdkInternalApi
public final class AwsErrorCode {

    public static final Set<String> RETRYABLE_ERROR_CODES;
    public static final Set<String> THROTTLING_ERROR_CODES;
    public static final Set<String> DEFINITE_CLOCK_SKEW_ERROR_CODES;
    public static final Set<String> POSSIBLE_CLOCK_SKEW_ERROR_CODES;

    static {
        Set<String> throttlingErrorCodes = new HashSet<>(9);
        throttlingErrorCodes.add("Throttling");
        throttlingErrorCodes.add("ThrottlingException");
        throttlingErrorCodes.add("ThrottledException");
        throttlingErrorCodes.add("ProvisionedThroughputExceededException");
        throttlingErrorCodes.add("SlowDown");
        throttlingErrorCodes.add("TooManyRequestsException");
        throttlingErrorCodes.add("RequestLimitExceeded");
        throttlingErrorCodes.add("BandwidthLimitExceeded");
        throttlingErrorCodes.add("RequestThrottled");
        throttlingErrorCodes.add("RequestThrottledException");
        throttlingErrorCodes.add("EC2ThrottledException");
        THROTTLING_ERROR_CODES = unmodifiableSet(throttlingErrorCodes);

        Set<String> definiteClockSkewErrorCodes = new HashSet<>(3);
        definiteClockSkewErrorCodes.add("RequestTimeTooSkewed");
        definiteClockSkewErrorCodes.add("RequestExpired");
        definiteClockSkewErrorCodes.add("RequestInTheFuture");
        DEFINITE_CLOCK_SKEW_ERROR_CODES = unmodifiableSet(definiteClockSkewErrorCodes);

        Set<String> possibleClockSkewErrorCodes = new HashSet<>(3);
        possibleClockSkewErrorCodes.add("InvalidSignatureException");
        possibleClockSkewErrorCodes.add("SignatureDoesNotMatch");
        possibleClockSkewErrorCodes.add("AuthFailure");
        POSSIBLE_CLOCK_SKEW_ERROR_CODES = unmodifiableSet(possibleClockSkewErrorCodes);

        Set<String> retryableErrorCodes = new HashSet<>(1);
        retryableErrorCodes.add("PriorRequestNotComplete");
        retryableErrorCodes.add("RequestTimeout");
        retryableErrorCodes.add("RequestTimeoutException");
        RETRYABLE_ERROR_CODES = unmodifiableSet(retryableErrorCodes);
    }

    private AwsErrorCode() {
    }

    public static boolean isThrottlingErrorCode(String errorCode) {
        return THROTTLING_ERROR_CODES.contains(errorCode);
    }

    public static boolean isDefiniteClockSkewErrorCode(String errorCode) {
        return DEFINITE_CLOCK_SKEW_ERROR_CODES.contains(errorCode);
    }

    public static boolean isPossibleClockSkewErrorCode(String errorCode) {
        return POSSIBLE_CLOCK_SKEW_ERROR_CODES.contains(errorCode);
    }

    public static boolean isRetryableErrorCode(String errorCode) {
        return RETRYABLE_ERROR_CODES.contains(errorCode);
    }
}
