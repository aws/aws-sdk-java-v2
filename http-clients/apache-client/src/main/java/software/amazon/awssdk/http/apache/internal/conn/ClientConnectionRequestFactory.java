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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import org.apache.http.conn.ConnectionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger log = LoggerFactory.getLogger(ClientConnectionRequestFactory.class);
    private static final Class<?>[] INTERFACES = {
            ConnectionRequest.class,
            Wrapped.class
    };

    private ClientConnectionRequestFactory() {
    }

    /**
     * Returns a wrapped instance of {@link ConnectionRequest}
     * to capture the necessary performance metrics.
     *
     * @param orig the target instance to be wrapped
     */
    static ConnectionRequest wrap(ConnectionRequest orig) {
        if (orig instanceof Wrapped) {
            throw new IllegalArgumentException();
        }
        return (ConnectionRequest) Proxy.newProxyInstance(
                // https://github.com/aws/aws-sdk-java/pull/48#issuecomment-29454423
                ClientConnectionRequestFactory.class.getClassLoader(),
                INTERFACES,
                new Handler(orig));
    }

    /**
     * The handler behind the dynamic proxy for {@link ConnectionRequest}
     * so that the latency of the
     * {@link ConnectionRequest#get(long, java.util.concurrent.TimeUnit)}
     * can be captured.
     */
    private static class Handler implements InvocationHandler {
        private final ConnectionRequest orig;

        Handler(ConnectionRequest orig) {
            this.orig = orig;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                Instant startGet = null;
                if ("get".equals(method.getName())) {
                    startGet = Instant.now();
                }
                try {
                    return method.invoke(orig, args);
                } finally {
                    if (startGet != null) {
                        recordGetTime(startGet);
                    }
                }
            } catch (InvocationTargetException e) {
                log.debug("", e);
                throw e.getCause();
            }
        }

        private void recordGetTime(Instant startGet) {
            Duration elapsed = Duration.between(startGet, Instant.now());
            MetricCollector metricCollector = THREAD_LOCAL_REQUEST_METRIC_COLLECTOR.get();
            metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, elapsed);
        }
    }
}
