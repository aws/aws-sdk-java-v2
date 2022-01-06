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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class ClientConnectionManagerFactory {

    private ClientConnectionManagerFactory() {
    }

    /**
     * Returns a wrapped instance of {@link HttpClientConnectionManager}
     * to capture the necessary performance metrics.
     *
     * @param orig the target instance to be wrapped
     */
    public static HttpClientConnectionManager wrap(HttpClientConnectionManager orig) {
        if (orig instanceof DelegatingHttpClientConnectionManager) {
            throw new IllegalArgumentException();
        }
        return new InstrumentedHttpClientConnectionManager(orig);
    }

    /**
     * Further wraps {@link ConnectionRequest} to capture performance metrics.
     */
    private static class InstrumentedHttpClientConnectionManager extends DelegatingHttpClientConnectionManager {

        private InstrumentedHttpClientConnectionManager(HttpClientConnectionManager delegate) {
            super(delegate);
        }

        @Override
        public ConnectionRequest requestConnection(HttpRoute route, Object state) {
            ConnectionRequest connectionRequest = super.requestConnection(route, state);
            return ClientConnectionRequestFactory.wrap(connectionRequest);
        }
    }

    /**
     * Delegates all methods to {@link HttpClientConnectionManager}. Subclasses can override select methods to change behavior.
     */
    private static class DelegatingHttpClientConnectionManager implements HttpClientConnectionManager {

        private final HttpClientConnectionManager delegate;

        protected DelegatingHttpClientConnectionManager(HttpClientConnectionManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public ConnectionRequest requestConnection(HttpRoute route, Object state) {
            return delegate.requestConnection(route, state);
        }

        @Override
        public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
            delegate.releaseConnection(conn, newState, validDuration, timeUnit);
        }

        @Override
        public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context)
                throws IOException {
            delegate.connect(conn, route, connectTimeout, context);
        }

        @Override
        public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
            delegate.upgrade(conn, route, context);
        }

        @Override
        public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
            delegate.routeComplete(conn, route, context);
        }

        @Override
        public void closeIdleConnections(long idletime, TimeUnit timeUnit) {
            delegate.closeIdleConnections(idletime, timeUnit);
        }

        @Override
        public void closeExpiredConnections() {
            delegate.closeExpiredConnections();
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }
    }
}
