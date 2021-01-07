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
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class SslContextProvider {
    private static final Logger log = Logger.loggerFor(SslContextProvider.class);
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
            return SslContextBuilder.forClient()
                                    .sslProvider(sslProvider)
                                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .trustManager(trustManagerFactory)
                                    .keyManager(keyManagerFactory)
                                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private TrustManagerFactory getTrustManager(NettyConfiguration configuration) {
        Validate.isTrue(configuration.tlsTrustManagersProvider() == null || !configuration.trustAllCertificates(),
                        "A TlsTrustManagerProvider can't be provided if TrustAllCertificates is also set");

        if (configuration.tlsTrustManagersProvider() != null) {
            return StaticTrustManagerFactory.create(configuration.tlsTrustManagersProvider().trustManagers());
        }

        if (configuration.trustAllCertificates()) {
            log.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                           + "used for testing.");
            return InsecureTrustManagerFactory.INSTANCE;
        }

        return null;
    }

    private KeyManagerFactory getKeyManager(NettyConfiguration configuration) {
        if (configuration.tlsKeyManagersProvider() != null) {
            KeyManager[] keyManagers = configuration.tlsKeyManagersProvider().keyManagers();
            if (keyManagers != null) {
                return StaticKeyManagerFactory.create(keyManagers);
            }
        }
        return null;
    }
}
