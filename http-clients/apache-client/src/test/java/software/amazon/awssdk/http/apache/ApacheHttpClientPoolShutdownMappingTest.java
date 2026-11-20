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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * How {@link ApacheHttpClient} reports Apache's "Connection pool shut down". This client does not support connection pool
 * recovery, so the pool is shut down either because the client was closed or because an Error escaped a request.
 */
public class ApacheHttpClientPoolShutdownMappingTest {

    private ConnectionManagerAwareHttpClient httpClient;
    private HttpClientConnectionManager connectionManager;
    private ApacheHttpClient client;

    @BeforeEach
    public void setup() {
        httpClient = mock(ConnectionManagerAwareHttpClient.class);
        connectionManager = mock(HttpClientConnectionManager.class);
        when(httpClient.getHttpClientConnectionManager()).thenReturn(connectionManager);
        client = newClient(httpClient);
    }

    static ApacheHttpClient newClient(ConnectionManagerAwareHttpClient httpClient) {
        ApacheHttpRequestConfig config = ApacheHttpRequestConfig.builder()
                                                                .connectionAcquireTimeout(Duration.ofSeconds(1))
                                                                .connectionTimeout(Duration.ofSeconds(1))
                                                                .socketTimeout(Duration.ofSeconds(1))
                                                                .proxyConfiguration(ProxyConfiguration.builder().build())
                                                                .build();
        return new ApacheHttpClient(httpClient, config, AttributeMap.empty());
    }

    @Test
    public void execute_poolShutDownIllegalState_recoveryDisabled_mapsToShutDownAfterError() throws Exception {
        IllegalStateException apacheException = new IllegalStateException("Connection pool shut down");
        when(httpClient.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenThrow(apacheException);

        Throwable thrown = catchThrowable(() -> call(client));

        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class).hasMessage("Connection pool shut down");
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(Reason.SHUT_DOWN_AFTER_ERROR);
        assertThat(thrown.getCause()).isSameAs(apacheException);
    }

    @Test
    public void execute_poolShutDownIllegalState_afterClose_mapsToClientClosed() throws Exception {
        IllegalStateException apacheException = new IllegalStateException("Connection pool shut down");
        when(httpClient.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenThrow(apacheException);

        client.close();
        Throwable thrown = catchThrowable(() -> call(client));

        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class);
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(Reason.CLIENT_CLOSED);
        assertThat(thrown.getCause()).isSameAs(apacheException);
    }

    /**
     * A request that fails because close() is shutting the pool down must report CLIENT_CLOSED, which needs the closed
     * flag to be written before the pool shutdown starts.
     */
    @Test
    public void close_requestDuringPoolShutdown_reportsClientClosed() throws Exception {
        when(httpClient.execute(any(HttpUriRequest.class), any(HttpContext.class)))
            .thenThrow(new IllegalStateException("Connection pool shut down"));
        AtomicReference<Throwable> duringShutdown = new AtomicReference<>();
        doAnswer(invocation -> {
            duringShutdown.set(catchThrowable(() -> call(client)));
            return null;
        }).when(connectionManager).shutdown();

        client.close();

        assertThat(duringShutdown.get()).isInstanceOf(ConnectionPoolShutdownException.class);
        assertThat(((ConnectionPoolShutdownException) duringShutdown.get()).reason()).isEqualTo(Reason.CLIENT_CLOSED);
    }

    @Test
    public void execute_otherIllegalState_propagatesUnchanged() throws Exception {
        // Equivalence guard: also passes without this change, by design.
        IllegalStateException other = new IllegalStateException("something else");
        when(httpClient.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenThrow(other);

        assertThat(catchThrowable(() -> call(client))).isSameAs(other);
    }

    @Test
    public void execute_typedExceptionFromBelow_isNotWrappedAgain() throws Exception {
        // Equivalence guard: also passes without this change, by design.
        ConnectionPoolShutdownException typed = ConnectionPoolShutdownException.create(Reason.RECOVERY_BACKOFF, null);
        when(httpClient.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenThrow(typed);

        assertThat(catchThrowable(() -> call(client))).isSameAs(typed);
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
