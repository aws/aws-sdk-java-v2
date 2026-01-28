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

package software.amazon.awssdk.retries.api.internal;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.utils.Validate;

/**
 * Implementation for the {@link RefreshRetryTokenResponse} interface.
 */
@SdkInternalApi
public final class RefreshRetryTokenResponseImpl implements RefreshRetryTokenResponse {
    private final RetryToken token;
    private final Duration delay;

    private RefreshRetryTokenResponseImpl(RetryToken token, Duration delay) {
        this.token = Validate.paramNotNull(token, "token");
        this.delay = Validate.isNotNegative(Validate.paramNotNull(delay, "delay"), "delay");
    }

    @Override
    public RetryToken token() {
        return token;
    }

    @Override
    public Duration delay() {
        return delay;
    }

    /**
     * Creates a new {@link RefreshRetryTokenResponse} with the given token and delay.
     */
    public static RefreshRetryTokenResponse create(RetryToken token, Duration delay) {
        return new RefreshRetryTokenResponseImpl(token, delay);
    }
}
