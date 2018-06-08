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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.HttpStatusCode;

@SdkProtectedApi
public final class RetryUtils {

    private RetryUtils() {
    }

    /**
     * Returns true if the specified exception is a request entity too large error.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a request entity too large error message from a service, otherwise false.
     */
    public static boolean isRequestEntityTooLargeException(SdkException exception) {
        return isServiceException(exception) && toServiceException(exception).statusCode() == HttpStatusCode.REQUEST_TOO_LONG;
    }

    public static boolean isServiceException(SdkException e) {
        return e instanceof SdkServiceException;
    }

    public static SdkServiceException toServiceException(SdkException e) {
        if (!(e instanceof SdkServiceException)) {
            throw new IllegalStateException("Received non-SdkServiceException where one was expected.", e);
        }
        return (SdkServiceException) e;
    }

    /**
     * Returns true if the specified exception is a clock skew error.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a clock skews error message from a service, otherwise false.
     */
    public static boolean isClockSkewException(SdkException exception) {
        return isServiceException(exception) && toServiceException(exception).isClockSkewException();
    }

    /**
     * Returns true if the specified exception is a throttling error.
     *
     * @param exception The exception to test.
     * @return True if the exception resulted from a throttling error message from a service, otherwise false.
     */
    public static boolean isThrottlingException(SdkException exception) {
        return isServiceException(exception) && toServiceException(exception).isThrottlingException();
    }
}
