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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.util.Timeout;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.metrics.MetricCollector;

@SdkInternalApi
public final class ClientConnectionRequestFactory {

    /**
     * {@link ThreadLocal}, request-level {@link MetricCollector}, set and removed by {@link Apache5HttpClient}.
     */
    public static final ThreadLocal<MetricCollector> THREAD_LOCAL_REQUEST_METRIC_COLLECTOR = new ThreadLocal<>();

    private ClientConnectionRequestFactory() {
    }

    /**
     * Returns a wrapped instance of {@link LeaseRequest}
     * to capture the necessary performance metrics.
     *
     * @param orig the target instance to be wrapped
     */
    static LeaseRequest wrap(LeaseRequest orig) {
        if (orig instanceof DelegatingConnectionRequest) {
            throw new IllegalArgumentException();
        }
        return new InstrumentedConnectionRequest(orig);
    }

    /**
     * Measures the latency of {@link LeaseRequest#get(Timeout)}.
     */
    private static class InstrumentedConnectionRequest extends DelegatingConnectionRequest {

        private InstrumentedConnectionRequest(LeaseRequest delegate) {
            super(delegate);
        }


        @Override
        public ConnectionEndpoint get(Timeout timeout) throws InterruptedException, ExecutionException, TimeoutException {
            Instant startTime = Instant.now();
            try {
                return super.get(timeout);
            } finally {
                Duration elapsed = Duration.between(startTime, Instant.now());
                MetricCollector metricCollector = THREAD_LOCAL_REQUEST_METRIC_COLLECTOR.get();
                metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, elapsed);
            }
        }

    }

    /**
     * Delegates all methods to {@link LeaseRequest}. Subclasses can override select methods to change behavior.
     */
    private static class DelegatingConnectionRequest implements LeaseRequest {

        private final LeaseRequest delegate;

        private DelegatingConnectionRequest(LeaseRequest delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean cancel() {
            return delegate.cancel();
        }

        @Override
        public ConnectionEndpoint get(Timeout timeout) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout);
        }
    }
}
