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

package software.amazon.awssdk.http.nio.netty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES;

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.SslProvider;
import javax.net.ssl.TrustManager;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.utils.AttributeMap;

public class SslContextProviderTest {

    @Test
    public void sslContext_h2WithJdk_h2CiphersShouldBeUsed() {
        SslContextProvider sslContextProvider = new SslContextProvider(new NettyConfiguration(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS),
                                                                       Protocol.HTTP2,
                                                                       SslProvider.JDK);

        assertThat(sslContextProvider.sslContext().cipherSuites()).isSubsetOf(Http2SecurityUtil.CIPHERS);
    }

    @Test
    public void sslContext_h2WithOpenSsl_h2CiphersShouldBeUsed() {
        SslContextProvider sslContextProvider = new SslContextProvider(new NettyConfiguration(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS),
                                                                       Protocol.HTTP2,
                                                                       SslProvider.OPENSSL);

        assertThat(sslContextProvider.sslContext().cipherSuites()).isSubsetOf(Http2SecurityUtil.CIPHERS);
    }

    @Test
    public void sslContext_h1_defaultCipherShouldBeUsed() {
        SslContextProvider sslContextProvider = new SslContextProvider(new NettyConfiguration(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS),
                                                                       Protocol.HTTP1_1,
                                                                       SslProvider.JDK);

        assertThat(sslContextProvider.sslContext().cipherSuites()).isNotIn(Http2SecurityUtil.CIPHERS);
    }

    @Test
    public void customizedKeyManagerPresent_shouldUseCustomized() {
        TlsKeyManagersProvider mockProvider = Mockito.mock(TlsKeyManagersProvider.class);
        SslContextProvider sslContextProvider = new SslContextProvider(new NettyConfiguration(AttributeMap.builder()
                                                                                                          .put(TRUST_ALL_CERTIFICATES, false)
                                                                                                          .put(TLS_KEY_MANAGERS_PROVIDER, mockProvider)
                                                                                                          .build()),
                                                                       Protocol.HTTP1_1,
                                                                       SslProvider.JDK);

        sslContextProvider.sslContext();
        Mockito.verify(mockProvider).keyManagers();
    }

    @Test
    public void customizedTrustManagerPresent_shouldUseCustomized() {
        TlsTrustManagersProvider mockProvider = Mockito.mock(TlsTrustManagersProvider.class);
        TrustManager mockTrustManager = Mockito.mock(TrustManager.class);
        Mockito.when(mockProvider.trustManagers()).thenReturn(new TrustManager[] {mockTrustManager});
        SslContextProvider sslContextProvider = new SslContextProvider(new NettyConfiguration(AttributeMap.builder()
                                                                                                          .put(TRUST_ALL_CERTIFICATES, false)
                                                                                                          .put(TLS_TRUST_MANAGERS_PROVIDER, mockProvider)
                                                                                                          .build()),
                                                                       Protocol.HTTP1_1,
                                                                       SslProvider.JDK);

        sslContextProvider.sslContext();
        Mockito.verify(mockProvider).trustManagers();
    }

    @Test
    public void TlsTrustManagerAndTrustAllCertificates_shouldThrowException() {
        TlsTrustManagersProvider mockProvider = Mockito.mock(TlsTrustManagersProvider.class);
        assertThatThrownBy(() -> new SslContextProvider(new NettyConfiguration(AttributeMap.builder()
                                                                                           .put(TRUST_ALL_CERTIFICATES, true)
                                                                                           .put(TLS_TRUST_MANAGERS_PROVIDER,
                                                                                                mockProvider)
                                                                                           .build()),
                                                        Protocol.HTTP1_1,
                                                        SslProvider.JDK)).isInstanceOf(IllegalArgumentException.class)
                                                                         .hasMessageContaining("A TlsTrustManagerProvider can't"
                                                                                               + " be provided if "
                                                                                               + "TrustAllCertificates is also "
                                                                                               + "set");

    }
}
