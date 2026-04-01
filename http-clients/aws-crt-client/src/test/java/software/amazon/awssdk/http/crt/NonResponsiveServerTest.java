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

package software.amazon.awssdk.http.crt;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.crt.CrtHttpClientTestUtils.createRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.Log;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.RecordingResponseHandler;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Functional tests verifying that the default connection health configuration
 * (applied when no explicit {@link ConnectionHealthConfiguration} is set)
 * correctly terminates connections to non-responding servers.
 */
class NonResponsiveServerTest {

    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(2);
    private static final AttributeMap SHORT_TIMEOUTS = AttributeMap.builder()
                                                                   .put(SdkHttpConfigurationOption.READ_TIMEOUT, SHORT_TIMEOUT)
                                                                   .put(SdkHttpConfigurationOption.WRITE_TIMEOUT, SHORT_TIMEOUT)
                                                                   .build();

    private ServerSocket serverSocket;

    @BeforeEach
    void setUp() throws IOException {
        Log.initLoggingToStdout(Log.LogLevel.Warn);
        serverSocket = new ServerSocket(0);
        // Accept connections in a daemon thread but never respond
        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    // Hold the connection open, never send a response
                    Thread.sleep(Long.MAX_VALUE);
                } catch (Exception e) {
                    // Server shutting down
                }
            }
        });
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Test
    void syncClient_noExplicitHealthConfig_serverNeverResponds_shouldThrow() {
        try (SdkHttpClient client = AwsCrtHttpClient.builder().buildWithDefaults(SHORT_TIMEOUTS)) {
            URI uri = URI.create("http://localhost:" + serverSocket.getLocalPort());
            SdkHttpRequest request = createRequest(uri);
            ExecutableHttpRequest executableRequest = client.prepareRequest(
                HttpExecuteRequest.builder().request(request)
                                  .contentStreamProvider(() -> new ByteArrayInputStream(new byte[0]))
                                  .build());
            assertThatThrownBy(executableRequest::call).isInstanceOf(IOException.class)
                                                       .hasMessageContaining("failure to meet throughput minimum");
        }
    }

    @Test
    void asyncClient_noExplicitHealthConfig_serverNeverResponds_shouldCompleteExceptionally()
        throws InterruptedException, TimeoutException {
        try (SdkAsyncHttpClient client = AwsCrtAsyncHttpClient.builder().buildWithDefaults(SHORT_TIMEOUTS)) {
            URI uri = URI.create("http://localhost:" + serverSocket.getLocalPort());
            SdkHttpRequest request = createRequest(uri);
            RecordingResponseHandler recorder = new RecordingResponseHandler();

            client.execute(AsyncExecuteRequest.builder()
                                              .request(request)
                                              .requestContentPublisher(new EmptyPublisher())
                                              .responseHandler(recorder)
                                              .build());

            assertThatThrownBy(() -> recorder.completeFuture().get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("failure to meet throughput minimum");
        }
    }
}
