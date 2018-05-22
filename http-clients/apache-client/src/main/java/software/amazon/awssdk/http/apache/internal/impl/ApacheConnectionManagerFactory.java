/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.apache.internal.impl;

import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLInitializationException;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.apache.ApacheSdkHttpClientFactory;
import software.amazon.awssdk.http.apache.internal.Defaults;
import software.amazon.awssdk.http.apache.internal.conn.SdkTlsSocketFactory;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Factory class to create connection manager used by the apache client.
 */
public class ApacheConnectionManagerFactory {

    public HttpClientConnectionManager create(ApacheSdkHttpClientFactory configuration,
                                              AttributeMap standardOptions) {
        ConnectionSocketFactory sslsf = getPreferredSocketFactory(standardOptions);

        final PoolingHttpClientConnectionManager cm = new
            PoolingHttpClientConnectionManager(
            createSocketFactoryRegistry(sslsf),
            null,
            DefaultSchemePortResolver.INSTANCE,
            null,
            configuration.connectionTimeToLive().orElse(Defaults.CONNECTION_POOL_TTL).toMillis(),
            TimeUnit.MILLISECONDS);

        cm.setDefaultMaxPerRoute(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
        cm.setMaxTotal(standardOptions.get(SdkHttpConfigurationOption.MAX_CONNECTIONS));
        cm.setDefaultSocketConfig(buildSocketConfig(standardOptions));

        return cm;
    }

    private ConnectionSocketFactory getPreferredSocketFactory(AttributeMap standardOptions) {
        // TODO v2 custom socket factory
        return new SdkTlsSocketFactory(getPreferredSslContext(),
                                       getHostNameVerifier(standardOptions));
    }

    private static SSLContext getPreferredSslContext() {
        try {
            final SSLContext sslcontext = SSLContext.getInstance("TLS");
            // http://download.java.net/jdk9/docs/technotes/guides/security/jsse/JSSERefGuide.html
            sslcontext.init(null, null, null);
            return sslcontext;
        } catch (final NoSuchAlgorithmException | KeyManagementException ex) {
            throw new SSLInitializationException(ex.getMessage(), ex);
        }
    }

    private SocketConfig buildSocketConfig(AttributeMap standardOptions) {
        return SocketConfig.custom()
                           // TODO do we want to keep SO keep alive
                           .setSoKeepAlive(false)
                           .setSoTimeout(
                               saturatedCast(standardOptions.get(SdkHttpConfigurationOption.SOCKET_TIMEOUT).toMillis()))
                           .setTcpNoDelay(true)
                           .build();
    }

    @ReviewBeforeRelease("Need to have a way to communicate with HTTP impl supports disabling of strict" +
                         "hostname verification. If it doesn't we either need to fail in S3 or switch to path style" +
                         "addressing.")
    private HostnameVerifier getHostNameVerifier(AttributeMap standardOptions) {
        // TODO Need to find a better way to handle these deprecations.
        return standardOptions.get(SdkHttpConfigurationOption.USE_STRICT_HOSTNAME_VERIFICATION)
               ? SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER
               : SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
    }

    private Registry<ConnectionSocketFactory> createSocketFactoryRegistry(ConnectionSocketFactory sslSocketFactory) {
        // TODO v2 disable cert checking
        return RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslSocketFactory)
            .build();
    }

}
