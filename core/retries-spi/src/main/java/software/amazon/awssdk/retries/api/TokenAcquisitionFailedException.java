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
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Exception thrown by {@link RetryStrategy} when a new token cannot be acquired.
 */
@SdkPublicApi
public final class TokenAcquisitionFailedException extends RuntimeException {
    private final transient RetryToken token;
    private final transient Duration delay;

    /**
     * Exception construction accepting message with no root cause.
     */
    public TokenAcquisitionFailedException(String msg) {
        super(msg);
        token = null;
        delay = null;
    }

    /**
     * Exception constructor accepting message and a root cause.
     */
    public TokenAcquisitionFailedException(String msg, Throwable cause) {
        super(msg, cause);
        token = null;
        delay = null;
    }

    /**
     * Exception constructor accepting message, retry token, and a root cause.
     */
    public TokenAcquisitionFailedException(String msg, RetryToken token, Throwable cause) {
        super(msg, cause);
        this.token = token;
        this.delay = null;
    }

    /**
     * Exception constructor accepting message, retry token, a root cause, and delay.
     */
    public TokenAcquisitionFailedException(String msg, RetryToken token, Throwable cause, Duration delay) {
        super(msg, cause);
        this.token = token;
        this.delay = delay;
    }

    /**
     * The amount of time to wait before returning to the caller.
     */
    public Optional<Duration> delay() {
        return Optional.ofNullable(delay);
    }

    /**
     * Returns the retry token that tracked the execution.
     * @return the retry token that tracked the execution.
     */
    public RetryToken token() {
        return token;
    }
}
