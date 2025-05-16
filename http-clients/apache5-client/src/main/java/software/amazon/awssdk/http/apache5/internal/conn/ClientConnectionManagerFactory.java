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

import java.io.IOException;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
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
     * Further wraps {@link LeaseRequest} to capture performance metrics.
     */
    private static class InstrumentedHttpClientConnectionManager extends DelegatingHttpClientConnectionManager {

        private InstrumentedHttpClientConnectionManager(HttpClientConnectionManager delegate) {
            super(delegate);
        }

        @Override
        public LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state) {
            LeaseRequest connectionRequest =  super.lease(id, route, requestTimeout, state);
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
        public LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state) {
            return delegate.lease(id, route, requestTimeout, state);
        }

        @Override
        public void release(ConnectionEndpoint endpoint, Object newState, TimeValue validDuration) {
            delegate.release(endpoint, newState, validDuration);

        }

        @Override
        public void connect(ConnectionEndpoint endpoint, TimeValue connectTimeout, HttpContext context) throws IOException {
            delegate.connect(endpoint, connectTimeout, context);

        }

        @Override
        public void upgrade(ConnectionEndpoint endpoint, HttpContext context) throws IOException {
            delegate.upgrade(endpoint, context);
        }

        @Override
        public void close(CloseMode closeMode) {
            delegate.close(closeMode);

        }

        @Override
        public void close() throws IOException {
            delegate.close();

        }
    }
}
