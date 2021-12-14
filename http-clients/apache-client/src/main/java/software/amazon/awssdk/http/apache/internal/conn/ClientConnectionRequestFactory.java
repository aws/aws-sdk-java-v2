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


package software.amazon.awssdk.http.apache.internal.conn;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class ClientConnectionRequestFactory {

    /**
     * {@link ThreadLocal}, request-level {@link MetricCollector}, set and removed by {@link ApacheHttpClient}.
     */
    public static final ThreadLocal<MetricCollector> THREAD_LOCAL_REQUEST_METRIC_COLLECTOR = new ThreadLocal<>();

    private ClientConnectionRequestFactory() {
    }

    /**
     * Returns a wrapped instance of {@link ConnectionRequest}
     * to capture the necessary performance metrics.
     *
     * @param orig the target instance to be wrapped
     */
    static ConnectionRequest wrap(ConnectionRequest orig) {
        if (orig instanceof DelegatingConnectionRequest) {
            throw new IllegalArgumentException();
        }
        return new InstrumentedConnectionRequest(orig);
    }

    /**
     * Measures the latency of {@link ConnectionRequest#get(long, java.util.concurrent.TimeUnit)}.
     */
    private static class InstrumentedConnectionRequest extends DelegatingConnectionRequest {

        private InstrumentedConnectionRequest(ConnectionRequest delegate) {
            super(delegate);
        }

        @Override
        public HttpClientConnection get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException,
                                                                                ConnectionPoolTimeoutException {
            Instant startTime = Instant.now();
            try {
                return super.get(timeout, timeUnit);
            } finally {
                Duration elapsed = Duration.between(startTime, Instant.now());
                MetricCollector metricCollector = THREAD_LOCAL_REQUEST_METRIC_COLLECTOR.get();
                metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, elapsed);
            }
        }
    }

    /**
     * Delegates all methods to {@link ConnectionRequest}. Subclasses can override select methods to change behavior.
     */
    private static class DelegatingConnectionRequest implements ConnectionRequest {

        private final ConnectionRequest delegate;

        private DelegatingConnectionRequest(ConnectionRequest delegate) {
            this.delegate = delegate;
        }

        @Override
        public HttpClientConnection get(long timeout, TimeUnit timeUnit)
                throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
            return delegate.get(timeout, timeUnit);
        }

        @Override
        public boolean cancel() {
            return delegate.cancel();
        }
    }
}
