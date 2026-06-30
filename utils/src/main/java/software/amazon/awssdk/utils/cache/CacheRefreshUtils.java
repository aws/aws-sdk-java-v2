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

package software.amazon.awssdk.utils.cache;

import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utility methods for credential cache refresh timing computation.
 */
@SdkProtectedApi
public final class CacheRefreshUtils {

    private static final Duration WINDOW_SHORT = Duration.ofMinutes(5);
    private static final Duration WINDOW_MEDIUM = Duration.ofMinutes(15);
    private static final Duration WINDOW_LONG = Duration.ofMinutes(60);

    private static final long THRESHOLD_MEDIUM_MINUTES = 20;
    private static final long THRESHOLD_LONG_MINUTES = 90;

    private CacheRefreshUtils() {
    }

    /**
     * Compute the dynamic advisory refresh window (prefetch time) based on the credential's remaining lifetime.
     * The window scales with the credential's time-to-expiry so that longer-lived credentials begin refreshing
     * earlier and shorter-lived credentials do not attempt a refresh the moment they are issued.
     *
     * <ul>
     *   <li>remaining lifetime &lt; 20 minutes → 5 minute window</li>
     *   <li>20 minutes ≤ remaining lifetime &lt; 90 minutes → 15 minute window</li>
     *   <li>remaining lifetime ≥ 90 minutes → 60 minute window</li>
     * </ul>
     *
     * @param expiration the credential's expiration time
     * @param now the current time
     * @return the Duration to use as the advisory refresh window
     */
    public static Duration computeDynamicPrefetchWindow(Instant expiration, Instant now) {
        Duration remainingLifetime = Duration.between(now, expiration);
        long remainingMinutes = remainingLifetime.toMinutes();

        if (remainingMinutes < THRESHOLD_MEDIUM_MINUTES) {
            return WINDOW_SHORT;
        } else if (remainingMinutes < THRESHOLD_LONG_MINUTES) {
            return WINDOW_MEDIUM;
        } else {
            return WINDOW_LONG;
        }
    }
}
