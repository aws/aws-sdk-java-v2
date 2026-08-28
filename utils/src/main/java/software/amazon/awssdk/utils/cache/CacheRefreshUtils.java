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

    private static final Duration THRESHOLD_MEDIUM = Duration.ofMinutes(20);
    private static final Duration THRESHOLD_LONG = Duration.ofMinutes(90);

    private CacheRefreshUtils() {
    }

    /**
     * Compute the advisory refresh window (prefetch time) for a credential. If {@code prefetchTime} is non-null
     * (i.e., explicitly configured by the user), it is returned directly. Otherwise, the window is computed
     * dynamically based on the credential's remaining lifetime so that longer-lived credentials begin refreshing
     * earlier and shorter-lived credentials do not attempt a refresh the moment they are issued.
     *
     * <p>Dynamic window selection:
     * <ul>
     *   <li>remaining lifetime &lt;= 20 minutes → 5 minute window</li>
     *   <li>20 minutes &lt; remaining lifetime &lt; 90 minutes → 15 minute window</li>
     *   <li>remaining lifetime &gt;= 90 minutes → 60 minute window</li>
     * </ul>
     *
     * <p>This assumes the credential source does not vend credentials with a lifetime shorter than the smallest window
     * above. That holds for AWS credential services, which have a minimum session duration of 15 minutes. A credential
     * shorter-lived than its window is inside its advisory refresh window from the moment it is issued.
     *
     * @param expiration the credential's expiration time
     * @param prefetchTime the explicitly configured prefetch window, or {@code null} to compute dynamically
     * @param now the current time
     * @return the Duration to use as the advisory refresh window
     */
    public static Duration computePrefetchWindow(Instant expiration, Duration prefetchTime, Instant now) {
        if (prefetchTime != null) {
            return prefetchTime;
        }

        Duration remainingLifetime = Duration.between(now, expiration);
        if (remainingLifetime.isNegative() || remainingLifetime.isZero()) {
            // Already expired. Any window puts the prefetch time in the past, which refreshes on the next request.
            return WINDOW_SHORT;
        }

        // Thresholds are compared as durations rather than whole minutes, so that a lifetime just over a boundary selects
        // the larger window instead of being truncated down into the smaller one.
        if (remainingLifetime.compareTo(THRESHOLD_MEDIUM) <= 0) {
            return WINDOW_SHORT;
        } else if (remainingLifetime.compareTo(THRESHOLD_LONG) < 0) {
            return WINDOW_MEDIUM;
        } else {
            return WINDOW_LONG;
        }
    }
}
