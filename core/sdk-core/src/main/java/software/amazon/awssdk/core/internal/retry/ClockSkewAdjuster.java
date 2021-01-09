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

package software.amazon.awssdk.core.internal.retry;

import java.time.Duration;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.retry.ClockSkew;
import software.amazon.awssdk.core.retry.RetryUtils;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * Suggests a clock skew adjustment that should be applied to future requests.
 */
@ThreadSafe
@SdkInternalApi
public final class ClockSkewAdjuster {
    private static final Logger log = Logger.loggerFor(ClockSkewAdjuster.class);

    /**
     * Returns true if the clock should be adjusted for future requests.
     */
    public boolean shouldAdjust(SdkException exception) {
        return RetryUtils.isClockSkewException(exception);
    }

    /**
     * Returns the recommended clock adjustment that should be used for future requests (in seconds). The result has the same
     * semantics of {@link HttpClientDependencies#timeOffset()}. Positive values imply the client clock is "fast" and negative
     * values imply the client clock is "slow".
     */
    public Integer getAdjustmentInSeconds(SdkHttpResponse response) {
        Instant now = Instant.now();
        Instant serverTime = ClockSkew.getServerTime(response).orElse(null);
        Duration skew = ClockSkew.getClockSkew(now, serverTime);
        try {
            return Math.toIntExact(skew.getSeconds());
        } catch (ArithmeticException e) {
            log.warn(() -> "The clock skew between the client and server was too large to be compensated for (" + now +
                           " versus " + serverTime + ").");
            return 0;
        }
    }
}