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
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.retries.api.internal.RefreshRetryTokenRequestImpl;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Request that the calling code makes to the {@link RetryStrategy} using
 * {@link RetryStrategy#refreshRetryToken(RefreshRetryTokenRequest)} to notify that the attempted execution failed and the
 * {@link RetryToken} needs to be refreshed.
 */
@SdkPublicApi
@ThreadSafe
public interface RefreshRetryTokenRequest extends ToCopyableBuilder<RefreshRetryTokenRequest.Builder, RefreshRetryTokenRequest> {
    /**
     * A {@link RetryToken} acquired a previous {@link RetryStrategy#acquireInitialToken} or
     * {@link RetryStrategy#refreshRetryToken} call.
     */
    RetryToken token();

    /**
     * A suggestion of how long to wait from the last attempt failure. For HTTP calls, this is usually extracted from a "retry
     * after" header from the downstream service.
     */
    Optional<Duration> suggestedDelay();

    /**
     * The cause of the last attempt failure.
     */
    Throwable failure();

    /**
     * Returns a new builder to configure the {@link RefreshRetryTokenRequest} instance.
     */
    static Builder builder() {
        return RefreshRetryTokenRequestImpl.builder();
    }

    interface Builder extends CopyableBuilder<Builder, RefreshRetryTokenRequest> {
        /**
         * Configures the {@link RetryToken} to be refreshed.
         */
        Builder token(RetryToken token);

        /**
         * Configures the suggested delay to used when refreshing the token.
         */
        Builder suggestedDelay(Duration duration);

        /**
         * Configures the latest caught exception.
         */
        Builder failure(Throwable throwable);

        /**
         * Builds and returns a new instance of {@linke RefreshRetryTokenRequest}.
         */
        RefreshRetryTokenRequest build();
    }
}
