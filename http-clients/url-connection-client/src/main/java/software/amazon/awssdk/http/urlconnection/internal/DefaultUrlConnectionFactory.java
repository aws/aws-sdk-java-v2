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

package software.amazon.awssdk.http.urlconnection.internal;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;
import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.net.HttpURLConnection;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.TlsKeyManagersProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionFactory;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of {@link UrlConnectionFactory}.
 */
@SdkInternalApi
public class DefaultUrlConnectionFactory implements UrlConnectionFactory {
    private static final Logger log = UrlConnectionLogger.LOG;

    private final SSLSocketFactory socketFactory;
    private final AttributeMap options;

    private DefaultUrlConnectionFactory(AttributeMap options) {
        this.options = options;

        // Note: This socket factory MUST be reused between requests because the connection pool in the JVM is keyed by both
        // URL and SSLSocketFactory. If the socket factory is not reused, connections will not be reused between requests.
        this.socketFactory = getSslContext().getSocketFactory();

    }

    public static UrlConnectionFactory get(AttributeMap options, UrlConnectionFactory configuredFactory) {
        if (configuredFactory != null) {
            return configuredFactory;
        }

        return new DefaultUrlConnectionFactory(options);
    }

    @Override
    public HttpURLConnection createConnection(URI uri) {
        return createDefaultConnection(uri, socketFactory);
    }

    private SSLContext getSslContext() {
        Validate.isTrue(options.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER) == null ||
                        !options.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES),
                        "A TlsTrustManagerProvider can't be provided if TrustAllCertificates is also set");

        TrustManager[] trustManagers = null;
        if (options.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER) != null) {
            trustManagers = options.get(SdkHttpConfigurationOption.TLS_TRUST_MANAGERS_PROVIDER).trustManagers();
        }

        if (options.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)) {
            log.warn(() -> "SSL Certificate verification is disabled. This is not a safe setting and should only be "
                           + "used for testing.");
            trustManagers = new TrustManager[] { TrustAllManager.INSTANCE };
        }

        TlsKeyManagersProvider provider = options.get(SdkHttpConfigurationOption.TLS_KEY_MANAGERS_PROVIDER);
        KeyManager[] keyManagers = provider.keyManagers();

        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, null);
            return context;
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private HttpURLConnection createDefaultConnection(URI uri, SSLSocketFactory socketFactory) {
        HttpURLConnection connection = invokeSafely(() -> (HttpURLConnection) uri.toURL().openConnection());

        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            if (options.get(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES)) {
                httpsConnection.setHostnameVerifier(NoOpHostNameVerifier.INSTANCE);
            }
            httpsConnection.setSSLSocketFactory(socketFactory);
        }

        connection.setConnectTimeout(saturatedCast(options.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT).toMillis()));
        connection.setReadTimeout(saturatedCast(options.get(SdkHttpConfigurationOption.READ_TIMEOUT).toMillis()));

        return connection;
    }
}
