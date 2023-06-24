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

package software.amazon.awssdk.core.internal.metrics;

import java.io.IOException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryUtils;

/**
 * General categories of errors that can be encountered when making an API call attempt.
 * <p>
 * This class is <b>NOT</b> intended to fully distinguish the details of every error that is possible to encounter when making
 * an API call attempt; for example, it is not a replacement for detailed logs. Instead, the categories are intentionally
 * broad to make it easy at-a-glance what is causing issues with requests, and to help direct further investigation.
 */
@SdkInternalApi
public enum SdkErrorType {
    /**
     * The service responded with a throttling error.
     */
    THROTTLING("Throttling"),

    /**
     * The service responded with an error other than {@link #THROTTLING}.
     */
    SERVER_ERROR("ServerError"),

    /**
     * A clientside timeout occurred, either an attempt level timeout, or API call level.
     */
    CONFIGURED_TIMEOUT("ConfiguredTimeout"),

    /**
     * An I/O error.
     */
    IO("IO"),

    /**
     * Catch-all type for errors that don't fit into the other categories.
     */
    OTHER("Other"),

    ;

    private final String name;

    SdkErrorType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static SdkErrorType fromException(Throwable e) {
        if (e instanceof IOException) {
            return IO;
        }

        if (e instanceof SdkException) {
            SdkException sdkError = (SdkException) e;
            if (sdkError instanceof ApiCallTimeoutException || sdkError instanceof ApiCallAttemptTimeoutException) {
                return CONFIGURED_TIMEOUT;
            }

            if (RetryUtils.isThrottlingException(sdkError)) {
                return THROTTLING;
            }

            if (e instanceof SdkServiceException) {
                return SERVER_ERROR;
            }
        }

        return OTHER;
    }
}
