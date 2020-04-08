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

package software.amazon.awssdk.core.retry;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * Utility methods for checking and reacting to the current client-side clock being different from the service-side clock.
 */
@ThreadSafe
@SdkProtectedApi
public final class ClockSkew {
    private static final Logger log = Logger.loggerFor(ClockSkew.class);

    /**
     * When we get an error that may be due to a clock skew error, and our clock is different than the service clock, this is
     * the difference threshold beyond which we will recommend a clock skew adjustment.
     */
    private static final Duration CLOCK_SKEW_ADJUST_THRESHOLD = Duration.ofMinutes(4);

    private ClockSkew() {
    }

    /**
     * Determine whether the request-level client time was sufficiently skewed from the server time as to possibly cause a
     * clock skew error.
     */
    public static boolean isClockSkewed(Instant clientTime, Instant serverTime) {
        Duration requestClockSkew = getClockSkew(clientTime, serverTime);
        return requestClockSkew.abs().compareTo(CLOCK_SKEW_ADJUST_THRESHOLD) >= 0;
    }

    /**
     * Calculate the time skew between a client and server date. This value has the same semantics of
     * {@link HttpClientDependencies#timeOffset()}. Positive values imply the client clock is "fast" and negative values imply
     * the client clock is "slow".
     */
    public static Duration getClockSkew(Instant clientTime, Instant serverTime) {
        if (clientTime == null || serverTime == null) {
            // If we do not have a client or server time, 0 is the safest skew to apply
            return Duration.ZERO;
        }

        return Duration.between(serverTime, clientTime);
    }

    /**
     * Get the server time from the service response, or empty if the time could not be determined.
     */
    public static Optional<Instant> getServerTime(SdkHttpResponse serviceResponse) {
        Optional<String> responseDateHeader = serviceResponse.firstMatchingHeader("Date");

        if (responseDateHeader.isPresent()) {
            String serverDate = responseDateHeader.get();
            log.debug(() -> "Reported service date: " + serverDate);

            try {
                return Optional.of(DateUtils.parseRfc1123Date(serverDate));
            } catch (RuntimeException e) {
                log.warn(() -> "Unable to parse clock skew offset from response: " + serverDate, e);
                return Optional.empty();
            }
        }

        log.debug(() -> "Service did not return a Date header, so clock skew adjustments will not be applied.");
        return Optional.empty();
    }
}
