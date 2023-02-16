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

package software.amazon.awssdk.retriesapi;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Exception thrown by {@link RetryStrategy} when a new token cannot be acquired.
 */
@SdkPublicApi
public final class TokenAcquisitionFailedException extends RuntimeException {
    /**
     * Exception construction accepting message with no root cause.
     */
    public TokenAcquisitionFailedException(String msg) {
        super(msg);
    }

    /**
     * Exception constructor accepting message and a root cause.
     */
    public TokenAcquisitionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
