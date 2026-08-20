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

package software.amazon.awssdk.http.apache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.server.MockServer;

/**
 * Apache-client relocation of the former sdk-core
 * {@code software.amazon.awssdk.core.http.ConnectionPoolMaxConnectionsIntegrationTest}, rewritten to drive the Apache
 * {@link SdkHttpClient} directly instead of going through the sdk-core execution pipeline.
 *
 * <p>Intent (unchanged from the original): with a connection pool limited to a single connection, if that sole connection is
 * already occupied by an in-flight request, leasing a second connection must fail with an
 * {@link ConnectionPoolTimeoutException} once the acquire (pool lease) timeout elapses.
 *
 * <p>The {@link MockServer.ServerBehavior#OVERLOADED} server sends response headers plus a partial body and then holds the
 * connection open forever, so the first request keeps its connection leased (its response body is never drained), leaving no
 * connection available for the second request.
 */
public class ApacheConnectionPoolMaxConnectionsTest {

    private MockServer server;
    private SdkHttpClient client;
    private HttpExecuteResponse firstResponse;

    @AfterEach
    public void tearDown() {
        // Close the retained (still-leased) response body stream first so nothing keeps holding the connection.
        if (firstResponse != null) {
            firstResponse.responseBody().ifPresent(stream -> {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ignore - test is tearing down
                }
            });
        }
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    @Timeout(30)
    public void leasingSecondConnectionFailsWithConnectionPoolTimeout() throws IOException {
        server = MockServer.createMockServer(MockServer.ServerBehavior.OVERLOADED);
        server.startServer();

        client = ApacheHttpClient.builder()
                                 .maxConnections(1)
                                 // Pool lease timeout -> ConnectionPoolTimeoutException when the sole connection is busy.
                                 .connectionAcquisitionTimeout(Duration.ofMillis(100))
                                 .connectionTimeout(Duration.ofMillis(100))
                                 .build();

        SdkHttpFullRequest request = server.configureHttpEndpoint(SdkHttpFullRequest.builder())
                                           .method(SdkHttpMethod.GET)
                                           .build();
        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder().request(request).build();

        // Occupy the single pooled connection: issue the first request and DO NOT drain/close its response body. The
        // OVERLOADED server keeps the connection open, so it stays leased. Retain the response so nothing auto-closes it.
        firstResponse = client.prepareRequest(executeRequest).call();
        assertThat(firstResponse.httpResponse().statusCode()).isEqualTo(200);

        // The second lease attempt cannot obtain a connection within the acquire timeout and fails. The
        // ConnectionPoolTimeoutException (an IOException) propagates directly out of ApacheHttpClient#execute; assert on the
        // exception itself, or its cause chain, to stay robust to any intermediate wrapping.
        assertThatThrownBy(() -> client.prepareRequest(executeRequest).call())
            .satisfiesAnyOf(
                thrown -> assertThat(thrown).isInstanceOf(ConnectionPoolTimeoutException.class),
                thrown -> assertThat(thrown).hasCauseInstanceOf(ConnectionPoolTimeoutException.class),
                thrown -> assertThat(thrown).hasRootCauseInstanceOf(ConnectionPoolTimeoutException.class));
    }
}
