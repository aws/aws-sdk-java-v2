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
 * Metrics collected by HTTP clients for HTTP/1 and HTTP/2 operations. See {@link Http2Metric} for metrics that are only available
 * on HTTP/2 operations.
 */
@SdkPublicApi
public final class HttpMetric {
    /**
     * The name of the HTTP client.
     */
    public static final SdkMetric<String> HTTP_CLIENT_NAME =
        metric("HttpClientName", String.class, MetricLevel.INFO);

    /**
     * The maximum number of concurrent requests that is supported by the HTTP client.
     *
     * <p>For HTTP/1 operations, this is equal to the maximum number of TCP connections that can be be pooled by the HTTP client.
     * For HTTP/2 operations, this is equal to the maximum number of streams that can be pooled by the HTTP client.
     *
     * <p>Note: Depending on the HTTP client, this is either a value for all endpoints served by the HTTP client, or a value
     * that applies only to the specific endpoint/host used in the request. For 'apache-http-client', this value is
     * for the entire HTTP client. For 'netty-nio-client', this value is per-endpoint. In all cases, this value is scoped to an
     * individual HTTP client instance, and does not include concurrency that may be available in other HTTP clients running
     * within the same JVM.
     */
    public static final SdkMetric<Integer> MAX_CONCURRENCY =
        metric("MaxConcurrency", Integer.class, MetricLevel.INFO);

    /**
     * The number of additional concurrent requests that can be supported by the HTTP client without needing to establish
     * additional connections to the target server.
     *
     * <p>For HTTP/1 operations, this is equal to the number of TCP connections that have been established with the service,
     * but are currently idle/unused. For HTTP/2 operations, this is equal to the number of streams that are currently
     * idle/unused.
     *
     * <p>Note: Depending on the HTTP client, this is either a value for all endpoints served by the HTTP client, or a value
     * that applies only to the specific endpoint/host used in the request. For 'apache-http-client', this value is
     * for the entire HTTP client. For 'netty-nio-client', this value is per-endpoint. In all cases, this value is scoped to an
     * individual HTTP client instance, and does not include concurrency that may be available in other HTTP clients running
     * within the same JVM.
     */
    public static final SdkMetric<Integer> AVAILABLE_CONCURRENCY =
        metric("AvailableConcurrency", Integer.class, MetricLevel.INFO);

    /**
     * The number of requests that are currently being executed by the HTTP client.
     *
     * <p>For HTTP/1 operations, this is equal to the number of TCP connections currently in active communication with the service
     * (excluding idle connections). For HTTP/2 operations, this is equal to the number of HTTP streams currently in active
     * communication with the service (excluding idle stream capacity).
     *
     * <p>Note: Depending on the HTTP client, this is either a value for all endpoints served by the HTTP client, or a value
     * that applies only to the specific endpoint/host used in the request. For 'apache-http-client', this value is
     * for the entire HTTP client. For 'netty-nio-client', this value is per-endpoint. In all cases, this value is scoped to an
     * individual HTTP client instance, and does not include concurrency that may be available in other HTTP clients running
     * within the same JVM.
     */
    public static final SdkMetric<Integer> LEASED_CONCURRENCY =
        metric("LeasedConcurrency", Integer.class, MetricLevel.INFO);

    /**
     * The number of requests that are awaiting concurrency to be made available from the HTTP client.
     *
     * <p>For HTTP/1 operations, this is equal to the number of requests currently blocked, waiting for a TCP connection to be
     * established or returned from the connection pool. For HTTP/2 operations, this is equal to the number of requests currently
     * blocked, waiting for a new stream (and possibly a new HTTP/2 connection) from the connection pool.
     *
     * <p>Note: Depending on the HTTP client, this is either a value for all endpoints served by the HTTP client, or a value
     * that applies only to the specific endpoint/host used in the request. For 'apache-http-client', this value is
     * for the entire HTTP client. For 'netty-nio-client', this value is per-endpoint. In all cases, this value is scoped to an
     * individual HTTP client instance, and does not include concurrency that may be available in other HTTP clients running
     * within the same JVM.
     */
    public static final SdkMetric<Integer> PENDING_CONCURRENCY_ACQUIRES =
        metric("PendingConcurrencyAcquires", Integer.class, MetricLevel.INFO);

    /**
     * The status code of the HTTP response.
     *
     * @implSpec This is reported by the SDK core, and should not be reported by an individual HTTP client implementation.
     */
    public static final SdkMetric<Integer> HTTP_STATUS_CODE =
        metric("HttpStatusCode", Integer.class, MetricLevel.TRACE);

    private HttpMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz, MetricLevel level) {
        return SdkMetric.create(name, clzz, level, MetricCategory.CORE, MetricCategory.HTTP_CLIENT);
    }
}
