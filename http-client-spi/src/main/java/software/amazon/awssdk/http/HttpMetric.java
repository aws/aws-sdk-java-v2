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
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * Metrics collected by HTTP clients.
 */
@SdkPublicApi
public final class HttpMetric {
    /**
     * The name of the HTTP client.
     */
    public static final SdkMetric<String> HTTP_CLIENT_NAME = metric("HttpClientName", String.class);

    /**
     * The maximum number of connections that will be pooled by the HTTP client.
     */
    public static final SdkMetric<Integer> MAX_CONNECTIONS = metric("MaxConnections", Integer.class);

    /**
     * The number of idle connections in the connection pool that are ready to serve a request.
     */
    public static final SdkMetric<Integer> AVAILABLE_CONNECTIONS = metric("AvailableConnections", Integer.class);

    /**
     * The number of connections from the connection pool that are busy serving requests.
     */
    public static final SdkMetric<Integer> LEASED_CONNECTIONS = metric("LeasedConnections", Integer.class);

    /**
     * The number of requests awaiting a free connection from the pool.
     */
    public static final SdkMetric<Integer> PENDING_CONNECTION_ACQUIRES = metric("PendingConnectionAcquires", Integer.class);

    private HttpMetric() {
    }

    private static <T> SdkMetric<T> metric(String name, Class<T> clzz) {
        return SdkMetric.create(name, clzz, MetricCategory.DEFAULT, MetricCategory.HTTP_CLIENT);
    }
}
