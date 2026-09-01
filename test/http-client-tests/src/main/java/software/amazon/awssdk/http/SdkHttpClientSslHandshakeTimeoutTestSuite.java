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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static software.amazon.awssdk.core.internal.util.ResponseHandlerTestUtils.combinedSyncResponseHandler;
import static utils.HttpTestUtils.executionContext;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.AmazonSyncHttpClient;
import software.amazon.awssdk.core.internal.http.response.NullErrorResponseHandler;
import software.amazon.awssdk.http.server.MockServer;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import utils.HttpTestUtils;

/**
 * Verifies that a client honors its configured socket timeout during the SSL handshake. The client is pointed at a server that
 * accepts the connection and then sends nothing, so a client that ignores the socket timeout hangs until the test times out.
 *
 * <p>This suite applies to sync clients that expose a socket timeout, which today means {@code apache-client},
 * {@code apache5-client} and {@code url-connection-client}. It started as a regression test for a bug in Apache HttpClient
 * 4.3, where the socket timeout was ignored during the SSL handshake.</p>
 *
 * @link https://issues.apache.org/jira/browse/HTTPCLIENT-1478
 */
public abstract class SdkHttpClientSslHandshakeTimeoutTestSuite {

    private static final Duration CLIENT_SOCKET_TO = Duration.ofSeconds(1);

    private MockServer server;

    /**
     * Returns a client that waits no longer than {@code socketTimeout} for data from the server.
     */
    protected abstract SdkHttpClient createSdkHttpClient(Duration socketTimeout);

    @BeforeEach
    public void setupBaseFixture() {
        server = MockServer.createMockServer(MockServer.ServerBehavior.UNRESPONSIVE);
        server.startServer();
    }

    @AfterEach
    public void tearDownBaseFixture() {
        server.stopServer();
    }

    @Test
    @Timeout(60)
    public void testSslHandshakeTimeout() {
        AmazonSyncHttpClient httpClient = HttpTestUtils.testClientBuilder()
                                                       .retryStrategy(DefaultRetryStrategy.doNotRetry())
                                                       .httpClient(createSdkHttpClient(CLIENT_SOCKET_TO))
                                                       .build();

        try {
            SdkHttpFullRequest request = server.configureHttpsEndpoint(SdkHttpFullRequest.builder())
                                               .method(SdkHttpMethod.GET)
                                               .build();
            httpClient.requestExecutionBuilder()
                      .request(request)
                      .originalRequest(NoopTestRequest.builder().build())
                      .executionContext(executionContext(request))
                      .execute(combinedSyncResponseHandler(null, new NullErrorResponseHandler()));
            fail("Client-side socket read timeout is expected!");

        } catch (SdkClientException e) {
            e.printStackTrace();
            /**
             * Http client catches the SocketTimeoutException and throws a
             * ConnectTimeoutException.
             * {@link DefaultHttpClientConnectionOperator#connect(ManagedHttpClientConnection, HttpHost,
             * InetSocketAddress, int, SocketConfig, HttpContext)}
             */
            assertThat(e).hasCauseInstanceOf(IOException.class);
        }
    }
}
