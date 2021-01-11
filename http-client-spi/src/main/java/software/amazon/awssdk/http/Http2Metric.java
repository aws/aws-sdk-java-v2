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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * Metrics collected by HTTP clients for HTTP/2 operations. See {@link HttpMetric} for metrics that are available on both HTTP/1
 * and HTTP/2 operations.
 */
@SdkPublicApi
public final class Http2Metric {
    /**
     * The local HTTP/2 window size in bytes for the stream that this request was executed on.
     *
     * <p>See https://http2.github.io/http2-spec/#FlowControl for more information on HTTP/2 window sizes.
     */
    public static final SdkMetric<Integer> LOCAL_STREAM_WINDOW_SIZE_IN_BYTES =
        metric("LocalStreamWindowSize", Integer.class, MetricLevel.TRACE);

    /**
     * The remote HTTP/2 window size in bytes for the stream that this request was executed on.
     *
     * <p>See https://http2.github.io/http2-spec/#FlowControl for more information on HTTP/2 window sizes.
     */
    public static final SdkMetric<Integer> REMOTE_STREAM_WINDOW_SIZE_IN_BYTES =
        metric("RemoteStreamWindowSize", Integer.class, MetricLevel.TRACE);

    private Http2Metric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz, MetricLevel level) {
        return SdkMetric.create(name, clzz, level, MetricCategory.CORE, MetricCategory.HTTP_CLIENT);
    }
}
