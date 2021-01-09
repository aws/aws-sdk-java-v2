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

import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Stream;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Http2Metric;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

/**
 * Utilities for collecting and publishing request-level metrics.
 */
@SdkInternalApi
public class NettyRequestMetrics {
    private NettyRequestMetrics() {
    }

    /**
     * Determine whether metrics are enabled, based on the provided metric collector.
     */
    public static boolean metricsAreEnabled(MetricCollector metricCollector) {
        return metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);
    }

    /**
     * Publish stream metrics for the provided stream channel to the provided collector. This should only be invoked after
     * the stream has been initialized. If the stream is not initialized when this is invoked, an exception will be thrown.
     */
    public static void publishHttp2StreamMetrics(MetricCollector metricCollector, Channel channel) {
        if (!metricsAreEnabled(metricCollector)) {
            return;
        }

        getHttp2Connection(channel).ifPresent(http2Connection -> {
            writeHttp2RequestMetrics(metricCollector, channel, http2Connection);
        });
    }

    private static Optional<Http2Connection> getHttp2Connection(Channel channel) {
        Channel parentChannel = channel.parent();
        if (parentChannel == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(parentChannel.attr(ChannelAttributeKey.HTTP2_CONNECTION).get());
    }

    private static void writeHttp2RequestMetrics(MetricCollector metricCollector,
                                                 Channel channel,
                                                 Http2Connection http2Connection) {
        int streamId = channel.attr(ChannelAttributeKey.HTTP2_FRAME_STREAM).get().id();

        Http2Stream stream = http2Connection.stream(streamId);
        metricCollector.reportMetric(Http2Metric.LOCAL_STREAM_WINDOW_SIZE_IN_BYTES,
                                     http2Connection.local().flowController().windowSize(stream));
        metricCollector.reportMetric(Http2Metric.REMOTE_STREAM_WINDOW_SIZE_IN_BYTES,
                                     http2Connection.remote().flowController().windowSize(stream));
    }
}
