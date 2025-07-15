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

package software.amazon.awssdk.http.apache5.internal.conn;

import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The AWS SDK for Java's implementation of the
 * {@code ConnectionKeepAliveStrategy} interface. Allows a user-configurable
 * maximum idle time for connections.
 */
@SdkInternalApi
public class SdkConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    private final TimeValue maxIdleTime;

    /**
     * @param maxIdleTime the maximum time a connection may be idle
     */
    public SdkConnectionKeepAliveStrategy(long maxIdleTime) {
        this.maxIdleTime = TimeValue.of(maxIdleTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public TimeValue getKeepAliveDuration(
        HttpResponse response,
        HttpContext context) {

        // If there's a Keep-Alive timeout directive in the response and it's
        // shorter than our configured max, honor that. Otherwise go with the
        // configured maximum.

        TimeValue duration = DefaultConnectionKeepAliveStrategy.INSTANCE
            .getKeepAliveDuration(response, context);

        // Check if duration is positive and less than maxIdleTime
        if (TimeValue.isPositive(duration) && duration.compareTo(maxIdleTime) < 0) {
            return duration;
        }

        return maxIdleTime;
    }
}
