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

package software.amazon.awssdk.retries.api;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.internal.AcquireInitialTokenResponseImpl;

/**
 * Encapsulates the response from the {@link RetryStrategy} to the request to start the attempts to be executed.
 */
@SdkPublicApi
@ThreadSafe
public interface AcquireInitialTokenResponse {
    /**
     * A {@link RetryToken} acquired by this invocation, used in subsequent {@link RetryStrategy#refreshRetryToken} or
     * {@link RetryStrategy#recordSuccess} calls.
     */
    RetryToken token();

    /**
     * The amount of time to wait before performing the first attempt.
     */
    Duration delay();

    /**
     * Creates a new {@link AcquireInitialTokenRequest} instance with the given scope.
     */
    static AcquireInitialTokenResponse create(RetryToken token, Duration delay) {
        return AcquireInitialTokenResponseImpl.create(token, delay);
    }
}
