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

package software.amazon.awssdk.core.exception;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Base type for all client exceptions thrown by the SDK.
 *
 * This exception is thrown when service could not be contacted for a response,
 * or when client is unable to parse the response from service.
 *
 * All exceptions that extend {@link SdkClientException} are assumed to be
 * not retryable.
 *
 * @see SdkServiceException
 */
@SdkPublicApi
public class SdkClientException extends SdkException {

    /**
     * Creates a new SdkClientException with the specified message, and root
     * cause.
     *
     * @param message
     *            An error message describing why this exception was thrown.
     * @param t
     *            The underlying cause of this exception.
     */
    public SdkClientException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new SdkClientException with the specified message.
     *
     * @param message
     *            An error message describing why this exception was thrown.
     */
    public SdkClientException(String message) {
        super(message);
    }

    /**
     * Creates a new SdkClientException with the root cause.
     *
     * @param t
     *          The underlying cause of this exception.
     */
    public SdkClientException(Throwable t) {
        super(t);
    }
}
