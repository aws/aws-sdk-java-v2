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

package software.amazon.awssdk.retries.internal.ratelimiter;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class RateLimiterAcquireResponse {
    private final Duration delay;

    private RateLimiterAcquireResponse(Duration delay) {
        this.delay = delay;
    }

    public Duration delay() {
        return delay;
    }

    public static RateLimiterAcquireResponse create(Duration waitTime) {
        return new RateLimiterAcquireResponse(waitTime);
    }

}
