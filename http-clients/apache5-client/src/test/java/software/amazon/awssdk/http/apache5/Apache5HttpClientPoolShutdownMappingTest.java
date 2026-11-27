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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache5.internal.Apache5HttpRequestConfig;
import software.amazon.awssdk.http.apache5.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * How {@link Apache5HttpClient} reports Apache's "Connection pool shut down" when recovery is disabled, which is the
 * case whenever the installed connection manager is not the SDK's recreating wrapper.
 */
public class Apache5HttpClientPoolShutdownMappingTest {

    private ConnectionManagerAwareHttpClient httpClient;
    private HttpClientConnectionManager connectionManager;
    private Apache5HttpClient client;

    @BeforeEach
    public void setup() {
        httpClient = mock(ConnectionManagerAwareHttpClient.class);
        connectionManager = mock(HttpClientConnectionManager.class);
        when(httpClient.getHttpClientConnectionManager()).thenReturn(connectionManager);
        client = newClient(httpClient);
    }

    static Apache5HttpClient newClient(ConnectionManagerAwareHttpClient httpClient) {
        Apache5HttpRequestConfig config = Apache5HttpRequestConfig.builder()
                                                                .connectionAcquireTimeout(Duration.ofSeconds(1))
                                                                .socketTimeout(Duration.ofSeconds(1))
                                                                .proxyConfiguration(ProxyConfiguration.builder().build())
                                                                .build();
        return new Apache5HttpClient(httpClient, config, AttributeMap.empty());
    }

    @Test
    public void execute_poolShutDownIllegalState_recoveryDisabled_mapsToShutDownAfterError() throws Exception {
        IllegalStateException apacheException = new IllegalStateException("Connection pool shut down");
        when(httpClient.executeOpen(any(), any(), any())).thenThrow(apacheException);

        Throwable thrown = catchThrowable(() -> call(client));

        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class).hasMessage("Connection pool shut down");
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(Reason.SHUT_DOWN_AFTER_ERROR);
        assertThat(thrown.getCause()).isSameAs(apacheException);
    }

    @Test
    public void execute_poolShutDownIllegalState_afterClose_mapsToClientClosed() throws Exception {
        IllegalStateException apacheException = new IllegalStateException("Connection pool shut down");
        when(httpClient.executeOpen(any(), any(), any())).thenThrow(apacheException);

        client.close();
        Throwable thrown = catchThrowable(() -> call(client));

        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class);
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(Reason.CLIENT_CLOSED);
        assertThat(thrown.getCause()).isSameAs(apacheException);
    }

    /**
     * A request that fails because close() is closing the pool must report CLIENT_CLOSED, which needs the closed
     * flag to be written before the pool close starts.
     */
    @Test
    public void close_requestDuringPoolShutdown_reportsClientClosed() throws Exception {
        when(httpClient.executeOpen(any(), any(), any()))
            .thenThrow(new IllegalStateException("Connection pool shut down"));
        AtomicReference<Throwable> duringShutdown = new AtomicReference<>();
        doAnswer(invocation -> {
            duringShutdown.set(catchThrowable(() -> call(client)));
            return null;
        }).when(connectionManager).close(CloseMode.IMMEDIATE);

        client.close();

        assertThat(duringShutdown.get()).isInstanceOf(ConnectionPoolShutdownException.class);
        assertThat(((ConnectionPoolShutdownException) duringShutdown.get()).reason()).isEqualTo(Reason.CLIENT_CLOSED);
    }

    @Test
    public void execute_otherIllegalState_propagatesUnchanged() throws Exception {
        // Equivalence guard: also passes without this change, by design.
        IllegalStateException other = new IllegalStateException("something else");
        when(httpClient.executeOpen(any(), any(), any())).thenThrow(other);

        assertThat(catchThrowable(() -> call(client))).isSameAs(other);
    }

    @Test
    public void execute_typedExceptionFromBelow_isNotWrappedAgain() throws Exception {
        // Equivalence guard: also passes without this change, by design.
        ConnectionPoolShutdownException typed = ConnectionPoolShutdownException.create(Reason.RECOVERY_BACKOFF, null);
        when(httpClient.executeOpen(any(), any(), any())).thenThrow(typed);

        assertThat(catchThrowable(() -> call(client))).isSameAs(typed);
    }

    @Test
    public void execute_endpointNotAcquired_stillMapsToIOException() throws Exception {
        // Equivalence guard: also passes without this change, by design.
        IllegalStateException racy = new IllegalStateException("Endpoint not acquired / already released");
        when(httpClient.executeOpen(any(), any(), any())).thenThrow(racy);

        Throwable thrown = catchThrowable(() -> call(client));

        assertThat(thrown).isExactlyInstanceOf(IOException.class);
        assertThat(thrown.getCause()).isSameAs(racy);
    }

    static void call(SdkHttpClient client) throws Exception {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .protocol("http")
                                                       .host("localhost")
                                                       .port(8080)
                                                       .build();
        client.prepareRequest(HttpExecuteRequest.builder().request(request).build()).call();
    }
}
