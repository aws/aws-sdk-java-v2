/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
public final class AwsErrorCodes {

    public static final Set<String> RETRYABLE_ERROR_CODES;
    public static final Set<String> THROTTLING_ERROR_CODES;
    public static final Set<String> CLOCK_SKEW_ERROR_CODES;

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
        THROTTLING_ERROR_CODES = unmodifiableSet(throttlingErrorCodes);

        Set<String> clockSkewErrorCodes = new HashSet<>(6);
        clockSkewErrorCodes.add("RequestTimeTooSkewed");
        clockSkewErrorCodes.add("RequestExpired");
        clockSkewErrorCodes.add("InvalidSignatureException");
        clockSkewErrorCodes.add("SignatureDoesNotMatch");
        clockSkewErrorCodes.add("AuthFailure");
        clockSkewErrorCodes.add("RequestInTheFuture");
        CLOCK_SKEW_ERROR_CODES = unmodifiableSet(clockSkewErrorCodes);

        Set<String> retryableErrorCodes = new HashSet<>(1);
        retryableErrorCodes.add("PriorRequestNotComplete");
        RETRYABLE_ERROR_CODES = unmodifiableSet(retryableErrorCodes);
    }

    private AwsErrorCodes() {
    }
}
