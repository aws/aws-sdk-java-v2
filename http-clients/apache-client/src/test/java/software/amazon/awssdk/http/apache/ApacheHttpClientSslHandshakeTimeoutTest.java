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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.server.MockServer;

/**
 * Verifies that {@link ApacheHttpClient} honors the configured socket timeout <em>during</em> the TLS handshake.
 *
 * <p>The client is an HTTPS client pointed at a server that accepts the TCP connection but never completes the TLS
 * handshake (a plain, non-TLS listener that simply holds the socket open without ever sending a ServerHello). A correct
 * client must fail with a socket read timeout instead of hanging forever waiting for the handshake to complete. This
 * guards against the Apache HttpClient bug where the socket timeout was incorrectly ignored during the SSL handshake;
 * against a problematic httpclient version this test would hang and only fail once the surrounding time bound trips.
 *
 * @see <a href="https://issues.apache.org/jira/browse/HTTPCLIENT-1478">HTTPCLIENT-1478</a>
 */
public class ApacheHttpClientSslHandshakeTimeoutTest {

    private static final Duration CLIENT_SOCKET_TIMEOUT = Duration.ofSeconds(1);

    private SdkHttpClient client;
    private MockServer server;

    @AfterEach
    public void teardown() {
        if (server != null) {
            server.stopServer();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void sslHandshakeHonorsSocketTimeout() {
        server = MockServer.createMockServer(MockServer.ServerBehavior.UNRESPONSIVE);
        // Plain (non-TLS) TCP listener: it accepts the connection but never completes the TLS handshake.
        server.startServer();

        client = ApacheHttpClient.builder()
                                 .socketTimeout(CLIENT_SOCKET_TIMEOUT)
                                 .build();

        SdkHttpFullRequest request = server.configureHttpsEndpoint(SdkHttpFullRequest.builder())
                                           .method(SdkHttpMethod.GET)
                                           .build();
        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                              .request(request)
                                                              .build();

        // Bound the whole assertion so a regression (a hang during the handshake) fails fast instead of blocking
        // the build forever. Expect a socket read timeout (a SocketTimeoutException, i.e. an IOException) rather
        // than a hang.
        assertTimeoutPreemptively(Duration.ofSeconds(60), () ->
            assertThatThrownBy(() -> client.prepareRequest(executeRequest).call())
                .isInstanceOf(IOException.class));
    }
}
