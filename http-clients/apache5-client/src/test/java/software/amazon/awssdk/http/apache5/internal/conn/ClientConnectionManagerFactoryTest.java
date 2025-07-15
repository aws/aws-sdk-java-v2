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

import org.apache.hc.client5.http.io.ConnectionEndpoint;
import org.apache.hc.client5.http.io.LeaseRequest;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.Test;

public class ClientConnectionManagerFactoryTest {
    HttpClientConnectionManager noop = new HttpClientConnectionManager() {

        @Override
        public void close() throws IOException {

        }

        @Override
        public void close(CloseMode closeMode) {

        }

        @Override
        public LeaseRequest lease(String id, HttpRoute route, Timeout requestTimeout, Object state) {
            return null;
        }

        @Override
        public void release(ConnectionEndpoint endpoint, Object newState, TimeValue validDuration) {

        }

        @Override
        public void connect(ConnectionEndpoint endpoint, TimeValue connectTimeout, HttpContext context) throws IOException {

        }

        @Override
        public void upgrade(ConnectionEndpoint endpoint, HttpContext context) throws IOException {

        }
    };

    @Test
    public void wrapOnce() {
        ClientConnectionManagerFactory.wrap(noop);
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrapTwice() {
        HttpClientConnectionManager wrapped = ClientConnectionManagerFactory.wrap(noop);
        ClientConnectionManagerFactory.wrap(wrapped);
    }
}
