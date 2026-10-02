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

package software.amazon.awssdk.http.apache5;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

public class Apache5Expect100ContinueTest {
    private static TestServer server;
    private static ExecutorService exec;

    @BeforeAll
    static void setup() throws IOException {
        server = new TestServer(0);
        exec = Executors.newSingleThreadExecutor();
        exec.submit(server::serve);
    }

    @AfterAll
    static void teardown() throws IOException, InterruptedException {
        server.close();
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void methodSetup() {
        server.clientSockets.clear();
    }

    @Test
    void execute_serverResponds3xx_closesConnection() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.PUT)
                                                       .host("localhost")
                                                       .protocol("http")
                                                       .port(server.getPort())
                                                       .encodedPath("/")
                                                       .appendHeader("Expect", "100-continue")
                                                       .appendHeader("Content-Length", Integer.toString(Integer.MAX_VALUE))
                                                       .build();

        ContentStreamProvider provider = ContentStreamProvider.fromInputStream(new EndlessInputStream());

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                                                              .request(request)
                                                              .contentStreamProvider(provider)
                                                              .metricCollector(NoOpMetricCollector.create())
                                                              .build();

        try (SdkHttpClient client = Apache5HttpClient.create()) {
            ExecutableHttpRequest executableRequest = client.prepareRequest(executeRequest);
            HttpExecuteResponse executeResponse = executableRequest.call();
            assertThat(executeResponse.httpResponse().statusCode()).isEqualTo(301);

            assertThatThrownBy(() -> server.clientSockets.get(0).getInputStream().read())
                .isInstanceOf(IOException.class)
                .hasMessage("Connection reset");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Test HTTP server: accepts a connection, reads all request headers, replies with a 301 permanent redirect; connections
     * are not closed by the server.
     */
    public static class TestServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final List<Socket> clientSockets = new ArrayList<>();

        public TestServer(int port) throws IOException {
            this.serverSocket = new ServerSocket(port);
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }

        public void serve() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket connection = serverSocket.accept();
                    clientSockets.add(connection);
                    handle(connection);
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        break; // closed during accept() — normal shutdown
                    }
                }
            }
        }

        private void handle(Socket connection) throws IOException {
            // Read the request line + headers (everything up to the blank line).
            // We don't decode the body; only headers are required here.
            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));

            List<String> requestLines = new ArrayList<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                requestLines.add(line);
            }

            String response =
                "HTTP/1.1 301 Moved Permanently\r\n"
                + "Location: https://example.com/\r\n"
                + "Content-Length: 0\r\n"
                + "Connection: close\r\n"
                + "\r\n";

            OutputStream out = connection.getOutputStream();
            out.write(response.getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }

    private static class EndlessInputStream extends InputStream {
        @Override
        public int read() {
            return 0xFF;
        }
    }
}
