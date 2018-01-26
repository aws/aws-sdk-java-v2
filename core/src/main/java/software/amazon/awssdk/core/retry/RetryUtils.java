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

package software.amazon.awssdk.core.retry;

import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.HttpStatusCodes;

public final class RetryUtils {

    static final Set<String> THROTTLING_ERROR_CODES = new HashSet<>(9);
    static final Set<String> CLOCK_SKEW_ERROR_CODES = new HashSet<>(6);
    static final Set<Integer> RETRYABLE_STATUS_CODES = new HashSet<>(4);

    static {
        THROTTLING_ERROR_CODES.add("Throttling");
        THROTTLING_ERROR_CODES.add("ThrottlingException");
        THROTTLING_ERROR_CODES.add("ProvisionedThroughputExceededException");
        THROTTLING_ERROR_CODES.add("SlowDown");
        THROTTLING_ERROR_CODES.add("TooManyRequestsException");
        THROTTLING_ERROR_CODES.add("RequestLimitExceeded");
        THROTTLING_ERROR_CODES.add("BandwidthLimitExceeded");
        THROTTLING_ERROR_CODES.add("RequestThrottled");

        CLOCK_SKEW_ERROR_CODES.add("RequestTimeTooSkewed");
        CLOCK_SKEW_ERROR_CODES.add("RequestExpired");
        CLOCK_SKEW_ERROR_CODES.add("InvalidSignatureException");
        CLOCK_SKEW_ERROR_CODES.add("SignatureDoesNotMatch");
        CLOCK_SKEW_ERROR_CODES.add("AuthFailure");
        CLOCK_SKEW_ERROR_CODES.add("RequestInTheFuture");

        RETRYABLE_STATUS_CODES.add(HttpStatusCodes.INTERNAL_SERVER_ERROR);
        RETRYABLE_STATUS_CODES.add(HttpStatusCodes.BAD_GATEWAY);
        RETRYABLE_STATUS_CODES.add(HttpStatusCodes.SERVICE_UNAVAILABLE);
        RETRYABLE_STATUS_CODES.add(HttpStatusCodes.GATEWAY_TIMEOUT);
    }

    private RetryUtils() {
    }

    /**
     * Returns true if the specified exception is a retryable service side exception.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a retryable service error, otherwise false.
     */
    public static boolean isRetryableServiceException(SdkException exception) {
        return isServiceException(exception) && RETRYABLE_STATUS_CODES.contains(toServiceException(exception).statusCode());
    }

    /**
     * @deprecated In favor of {@link RetryUtils#isThrottlingException(SdkException)}
     */
    @Deprecated
    public static boolean isThrottlingException(SdkServiceException exception) {
        return isThrottlingException((SdkException) exception);
    }

    /**
     * Returns true if the specified exception is a throttling error.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a throttling error message from a service, otherwise false.
     */
    public static boolean isThrottlingException(SdkException exception) {
        return isServiceException(exception) && THROTTLING_ERROR_CODES.contains(toServiceException(exception).errorCode());
    }

    /**
     * Returns true if the specified exception is a request entity too large error.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a request entity too large error message from a service, otherwise false.
     */
    public static boolean isRequestEntityTooLargeException(SdkException exception) {
        return isServiceException(exception) && toServiceException(exception).statusCode() == HttpStatusCodes.REQUEST_TOO_LONG;
    }

    /**
     * Returns true if the specified exception is a clock skew error.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a clock skews error message from a service, otherwise false.
     */
    public static boolean isClockSkewError(SdkException exception) {
        return isServiceException(exception) && CLOCK_SKEW_ERROR_CODES.contains(toServiceException(exception).errorCode());
    }

    private static boolean isServiceException(SdkException e) {
        return e instanceof SdkServiceException;
    }

    private static SdkServiceException toServiceException(SdkException e) {
        if (!(e instanceof SdkServiceException)) {
            throw new IllegalStateException("Received non-SdkServiceException where one was expected.", e);
        }
        return (SdkServiceException) e;
    }

}
