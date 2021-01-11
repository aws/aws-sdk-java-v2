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

package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.pool.ChannelPool;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * A {@link ChannelPool} implementation that allows a caller to asynchronously retrieve channel-pool related metrics via
 * {@link #collectChannelPoolMetrics(MetricCollector)}.
 */
@SdkInternalApi
public interface SdkChannelPool extends ChannelPool {
    /**
     * Collect channel pool metrics into the provided {@link MetricCollector} collection, completing the returned future when
     * all metric publishing is complete.
     *
     * @param metrics The collection to which all metrics should be added.
     * @return A future that is completed when all metric publishing is complete.
     */
    CompletableFuture<Void> collectChannelPoolMetrics(MetricCollector metrics);
}
