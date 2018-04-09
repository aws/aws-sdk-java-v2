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
 * Extension of {@link SdkClientException} that is thrown whenever the
 * client-side computed CRC32 does not match the server-side computed CRC32.
 *
 * This exception will not be retried by the SDK but may be retryable by the client.
 * Retrying may succeed if the mismatch occurred during the transmission of the response
 * over the network or during the write to disk. The SDK does not retry this exception
 * as this may result in additional calls being made that may contain large payloads.
 */
@SdkPublicApi
public class Crc32MismatchException extends SdkClientException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CRC32MismatchException with the specified message, and root
     * cause.
     *
     * @param message
     *            An error message describing why this exception was thrown.
     * @param t
     *            The underlying cause of this exception.
     */
    public Crc32MismatchException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new CRC32MismatchException with the specified message.
     *
     * @param message
     *            An error message describing why this exception was thrown.
     */
    public Crc32MismatchException(String message) {
        super(message);
    }

}
