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

package software.amazon.awssdk.http.apache.async;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The AWS SDK for Java's implementation of the {@code ConnectionKeepAliveStrategy} interface. Allows a user-configurable maximum
 * idle time for connections.
 */
@SdkInternalApi
class SdkConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {
    private final long maxIdleTime;

    /**
     * @param maxIdleTime the maximum time a connection may be idle
     */
    SdkConnectionKeepAliveStrategy(long maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    @Override
    public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // If there's a Keep-Alive timeout directive in the response and it's
        // shorter than our configured max, honor that. Otherwise go with the
        // configured maximum.

        long duration = DefaultConnectionKeepAliveStrategy.INSTANCE
            .getKeepAliveDuration(response, context)
            .toMilliseconds();

        if (0 < duration && duration < maxIdleTime) {
            return TimeValue.ofMilliseconds(duration);
        }

        return TimeValue.ofMilliseconds(maxIdleTime);
    }
}
