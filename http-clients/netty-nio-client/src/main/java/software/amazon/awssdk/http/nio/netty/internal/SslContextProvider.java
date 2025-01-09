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

import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SystemPropertyTlsKeyManagersProvider;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.nio.netty.internal.utils.NettyClientLogger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class SslContextProvider {
    private static final NettyClientLogger log = NettyClientLogger.getLogger(SslContextProvider.class);
    private final Protocol protocol;
    private final SslProvider sslProvider;
    private final TrustManagerFactory trustManagerFactory;
    private final KeyManagerFactory keyManagerFactory;

    public SslContextProvider(NettyConfiguration configuration, Protocol protocol, SslProvider sslProvider) {
        this.protocol = protocol;
        this.sslProvider = sslProvider;
        this.trustManagerFactory = getTrustManager(configuration);
        this.keyManagerFactory = getKeyManager(configuration);
    }

    public SslContext sslContext() {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient()
                                                         .sslProvider(sslProvider)
                                                         .ciphers(getCiphers(), SupportedCipherSuiteFilter.INSTANCE)
                                                         .trustManager(trustManagerFactory)
                                                         .keyManager(keyManagerFactory);

            if (protocol == Protocol.ALPN_AUTO || protocol == Protocol.ALPN_H2) {
                builder.applicationProtocolConfig(
                    new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                                  ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                                  ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                                  getAlpnProtocols()));
            }

            return builder.build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private String[] getAlpnProtocols() {
        if (protocol == Protocol.ALPN_AUTO) {
            return new String[]{ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1};
        }
        if (protocol == Protocol.ALPN_H2) {
            return new String[]{ApplicationProtocolNames.HTTP_2};
        }
        return null;
    }

    /**
     * HTTP/2 and ALPN_H2: per Rfc7540, there is a blocked list of cipher suites for HTTP/2, so setting
     * the recommended cipher suites directly here
     *
     * HTTP/1.1 and ALPN_AUTO: return null so that the default ciphers suites will be used
     * https://github.com/netty/netty/blob/0dc246eb129796313b58c1dbdd674aa289f72cad/handler/src/main/java/io/netty/handler/ssl
     * /SslUtils.java
     */
    private List<String> getCiphers() {
        if (protocol == Protocol.HTTP2 || protocol == Protocol.ALPN_H2) {
            return Http2SecurityUtil.CIPHERS;
        }
        return null;
    }

    private TrustManagerFactory getTrustManager(NettyConfiguration configuration) {
        TlsTrustManagersProvider tlsTrustManagersProvider = configuration.tlsTrustManagersProvider();
        Validate.isTrue(tlsTrustManagersProvider == null || !configuration.trustAllCertificates(),
                        "A TlsTrustManagerProvider can't be provided if TrustAllCertificates is also set");

        if (tlsTrustManagersProvider != null) {
            return StaticTrustManagerFactory.create(tlsTrustManagersProvider.trustManagers());
        }

        if (configuration.trustAllCertificates()) {
            log.warn(null, () -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                                 + "used for testing.");
            return InsecureTrustManagerFactory.INSTANCE;
        }

        // return null so that the system default trust manager will be used
        return null;
    }

    private KeyManagerFactory getKeyManager(NettyConfiguration configuration) {
        if (configuration.tlsKeyManagersProvider() != null) {
            KeyManager[] keyManagers = configuration.tlsKeyManagersProvider().keyManagers();
            if (keyManagers != null) {
                return StaticKeyManagerFactory.create(keyManagers);
            }
        }

        KeyManager[] systemPropertyKeyManagers = SystemPropertyTlsKeyManagersProvider.create().keyManagers();
        return systemPropertyKeyManagers == null ? null : StaticKeyManagerFactory.create(systemPropertyKeyManagers);
    }
}