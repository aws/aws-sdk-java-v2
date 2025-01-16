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

package software.amazon.awssdk.http.nio.netty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createProvider;
import static software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClientTestUtils.createRequest;

import io.netty.handler.ssl.SslProvider;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyUtils;
import software.amazon.awssdk.utils.AttributeMap;

public class NettyClientAlpnTest {

    private static MockH2Server mockServer;
    private static SdkAsyncHttpClient sdkHttpClient;

    @AfterEach
    public void reset() throws Exception {
        sdkHttpClient.close();
        mockServer.stop();
    }

    private static void initServer(boolean useAlpn) throws Exception {
        mockServer = new MockH2Server(useAlpn);
        mockServer.start();
    }

    @Test
    @EnabledIf("alpnSupported")
    public void alpnClientJdkProvider_serverWithAlpnSupport_requestSucceeds() throws Exception {
        initClient(ProtocolNegotiation.ALPN, SslProvider.JDK);
        initServer(true);
        makeSimpleRequest();
    }

    @Test
    public void alpnClientOpenSslProvider_serverWithAlpnSupport_requestSucceeds() throws Exception {
        initClient(ProtocolNegotiation.ALPN, SslProvider.OPENSSL);
        initServer(true);
        makeSimpleRequest();
    }

    @Test
    @EnabledIf("alpnSupported")
    public void alpnClient_serverWithoutAlpnSupport_throwsException() throws Exception {
        initClient(ProtocolNegotiation.ALPN, SslProvider.JDK);
        initServer(false);
        ExecutionException e = assertThrows(ExecutionException.class, this::makeSimpleRequest);
        assertThat(e).hasCauseInstanceOf(UnsupportedOperationException.class);
        assertThat(e.getMessage()).contains("The server does not support ALPN with H2");
    }

    @Test
    @EnabledIf("alpnSupported")
    public void priorKnowledgeClient_serverWithAlpnSupport_requestSucceeds() throws Exception {
        initClient(ProtocolNegotiation.ASSUME_PROTOCOL, SslProvider.JDK);
        initServer(true);
        makeSimpleRequest();
    }

    @Test
    public void priorKnowledgeClient_serverWithoutAlpnSupport_requestSucceeds() throws Exception {
        initClient(ProtocolNegotiation.ASSUME_PROTOCOL, SslProvider.JDK);
        initServer(false);
        makeSimpleRequest();
    }

    private void makeSimpleRequest() throws Exception {
        SdkHttpRequest request = createRequest(mockServer.getHttpsUri());
        RecordingResponseHandler recorder = new RecordingResponseHandler();
        sdkHttpClient.execute(AsyncExecuteRequest.builder().request(request).requestContentPublisher(createProvider("")).responseHandler(recorder).build());
        recorder.completeFuture.get(5, TimeUnit.SECONDS);
    }

    private static void initClient(ProtocolNegotiation protocolNegotiation, SslProvider sslProvider) {
        sdkHttpClient =  NettyNioAsyncHttpClient.builder()
                                                .sslProvider(sslProvider)
                                                .protocol(Protocol.HTTP2)
                                                .protocolNegotiation(protocolNegotiation)
                                                .buildWithDefaults(AttributeMap.builder().put(TRUST_ALL_CERTIFICATES, true)
                                                                               .build());
    }

    private static boolean alpnSupported(){
        return NettyUtils.isAlpnSupported(SslProvider.JDK);
    }
}
