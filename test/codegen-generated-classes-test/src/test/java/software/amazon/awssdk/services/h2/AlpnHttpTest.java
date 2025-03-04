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

package software.amazon.awssdk.services.h2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.ConnectException;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.ProtocolNegotiation;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;

public class AlpnHttpTest {

    @Test
    @EnabledIf("alpnSupported")
    public void clientWithDefaultSettingAlpn_httpRequest_doesNotThrowUnsupportedOperationException() {
        H2AsyncClient client = clientBuilderWithHttpEndpoint().build();

        CompletionException e = assertThrows(CompletionException.class, () -> makeRequest(client));
        assertThat(e).hasCauseInstanceOf(SdkClientException.class);
        assertThat(e.getCause()).hasCauseInstanceOf(ConnectException.class);
        assertThat(e.getMessage()).contains("Connection refused");
        assertThat(e.getMessage()).doesNotContain("ALPN can only be used with HTTPS, not HTTP. Use ProtocolNegotiation.ASSUME_PROTOCOL instead.");
    }

    @Test
    @EnabledIf("alpnSupported")
    public void clientWithUserConfiguredAlpn_httpRequest_throwsUnsupportedOperationException() {
        H2AsyncClient client = clientBuilderWithHttpEndpoint().httpClient(NettyNioAsyncHttpClient.builder()
                                                                                                 .protocolNegotiation(ProtocolNegotiation.ALPN)
                                                                                                 .protocol(Protocol.HTTP2)
                                                                                                 .build())
                                                              .build();

        CompletionException e = assertThrows(CompletionException.class, () -> makeRequest(client));
        assertThat(e).hasCauseInstanceOf(SdkClientException.class);
        assertThat(e.getCause()).hasCauseInstanceOf(UnsupportedOperationException.class);
        assertThat(e.getMessage()).contains("ALPN can only be used with HTTPS, not HTTP. Use ProtocolNegotiation.ASSUME_PROTOCOL instead.");
    }

    private H2AsyncClientBuilder clientBuilderWithHttpEndpoint() {
        return H2AsyncClient.builder()
                            .endpointOverride(URI.create("http://localhost:8080"));
    }

    private void makeRequest(H2AsyncClient client) {
        client.oneOperation(c -> c.stringMember("payload")).join();
    }

    private static boolean alpnSupported() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            SSLEngine engine = context.createSSLEngine();
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            MethodHandle getApplicationProtocol = AccessController.doPrivileged(
                (PrivilegedExceptionAction<MethodHandle>) () ->
                    lookup.findVirtual(SSLEngine.class, "getApplicationProtocol", MethodType.methodType(String.class)));

            getApplicationProtocol.invoke(engine);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
