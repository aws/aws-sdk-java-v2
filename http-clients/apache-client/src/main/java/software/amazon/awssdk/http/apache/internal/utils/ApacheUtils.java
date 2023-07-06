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

package software.amazon.awssdk.http.apache.internal.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.ReflectionMethodInvoker;

@SdkInternalApi
public final class ApacheUtils {
    private static final Logger logger = Logger.loggerFor(ApacheUtils.class);
    private static final ReflectionMethodInvoker<RequestConfig.Builder, RequestConfig.Builder> NORMALIZE_URI_INVOKER;

    static {
        // Attempt to initialize the invoker once on class-load. If it fails, it will not be attempted again, but we'll
        // use that opportunity to log a warning.
        NORMALIZE_URI_INVOKER =
            new ReflectionMethodInvoker<>(RequestConfig.Builder.class,
                                          RequestConfig.Builder.class,
                                          "setNormalizeUri",
                                          boolean.class);

        try {
            NORMALIZE_URI_INVOKER.initialize();
        } catch (NoSuchMethodException ignored) {
            noSuchMethodThrownByNormalizeUriInvoker();
        }
    }

    private ApacheUtils() {
    }

    /**
     * Utility function for creating a new BufferedEntity and wrapping any errors
     * as a SdkClientException.
     *
     * @param entity The HTTP entity to wrap with a buffered HTTP entity.
     * @return A new BufferedHttpEntity wrapping the specified entity.
     */
    public static HttpEntity newBufferedHttpEntity(HttpEntity entity) {
        try {
            return new BufferedHttpEntity(entity);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create HTTP entity: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a new HttpClientContext used for request execution.
     */
    public static HttpClientContext newClientContext(ProxyConfiguration proxyConfiguration) {
        HttpClientContext clientContext = new HttpClientContext();
        addPreemptiveAuthenticationProxy(clientContext, proxyConfiguration);

        RequestConfig.Builder builder = RequestConfig.custom();
        disableNormalizeUri(builder);

        clientContext.setRequestConfig(builder.build());
        return clientContext;

    }

    /**
     * From Apache v4.5.8, normalization should be disabled or AWS requests with special characters in URI path will fail
     * with Signature Errors.
     * <p>
     *    setNormalizeUri is added only in 4.5.8, so customers using the latest version of SDK with old versions (4.5.6 or less)
     *    of Apache httpclient will see NoSuchMethodError. Hence this method will suppress the error.
     *
     *    Do not use Apache version 4.5.7 as it breaks URI paths with special characters and there is no option
     *    to disable normalization.
     * </p>
     *
     * For more information, See https://github.com/aws/aws-sdk-java/issues/1919
     */
    public static void disableNormalizeUri(RequestConfig.Builder requestConfigBuilder) {
        // For efficiency, do not attempt to call the invoker again if it failed to initialize on class-load
        if (NORMALIZE_URI_INVOKER.isInitialized()) {
            try {
                NORMALIZE_URI_INVOKER.invoke(requestConfigBuilder, false);
            } catch (NoSuchMethodException ignored) {
                noSuchMethodThrownByNormalizeUriInvoker();
            }
        }
    }

    /**
     * Returns a new instance of a CredentialsProvider for both HTTP and HTTPS.
     */
    public static CredentialsProvider newProxyCredentialsProvider(ProxyConfiguration proxyConfiguration) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        String httpHost = proxyConfiguration.host("http");
        Optional<Credentials> httpCredentials = newNtCredentials(proxyConfiguration, "http");
        if (httpHost != null && httpCredentials.isPresent()) {
            credsProvider.setCredentials(newAuthScope(proxyConfiguration, "http"), httpCredentials.get());
        }

        String httpsHost = proxyConfiguration.host("https");
        Optional<Credentials> httpsCredentials = newNtCredentials(proxyConfiguration, "https");
        if (httpsHost != null && httpsCredentials.isPresent()) {
            credsProvider.setCredentials(newAuthScope(proxyConfiguration, "https"), httpsCredentials.get());
        }

        return credsProvider;
    }

    /**
     * Returns a new instance of NTCredentials used for proxy authentication.
     */
    private static Optional<Credentials> newNtCredentials(ProxyConfiguration proxyConfiguration, String scheme) {
        String user = proxyConfiguration.username(scheme);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new NTCredentials(user,
                                 proxyConfiguration.password(scheme),
                                 proxyConfiguration.ntlmWorkstation(scheme),
                                 proxyConfiguration.ntlmDomain(scheme)));
    }

    /**
     * Returns a new instance of AuthScope used for proxy authentication.
     */
    private static AuthScope newAuthScope(ProxyConfiguration proxyConfiguration, String scheme) {
        return new AuthScope(
            proxyConfiguration.host(scheme),
            proxyConfiguration.port(scheme),
            AuthScope.ANY_REALM,
            scheme
        );
    }

    private static void addPreemptiveAuthenticationProxy(HttpClientContext clientContext,
                                                         ProxyConfiguration proxyConfiguration) {

        if (proxyConfiguration.preemptiveBasicAuthenticationEnabled()) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();

            String httpHost = proxyConfiguration.host("http");
            Optional<Credentials> httpCredentials = newNtCredentials(proxyConfiguration, "http");
            if (httpHost != null && httpCredentials.isPresent()) {
                credsProvider.setCredentials(newAuthScope(proxyConfiguration, "http"), httpCredentials.get());
                authCache.put(new HttpHost(httpHost, proxyConfiguration.port("http")), basicAuth);
            }

            String httpsHost = proxyConfiguration.host("https");
            Optional<Credentials> httpsCredentials = newNtCredentials(proxyConfiguration, "https");
            if (httpsHost != null && httpsCredentials.isPresent()) {
                credsProvider.setCredentials(newAuthScope(proxyConfiguration, "https"), httpsCredentials.get());
                authCache.put(new HttpHost(httpsHost, proxyConfiguration.port("https")), basicAuth);
            }

            clientContext.setCredentialsProvider(credsProvider);
            clientContext.setAuthCache(authCache);
        }
    }

    // Just log and then swallow the exception
    private static void noSuchMethodThrownByNormalizeUriInvoker() {
        // setNormalizeUri method was added in httpclient 4.5.8
        logger.warn(() -> "NoSuchMethodException was thrown when disabling normalizeUri. This indicates you are using "
                 + "an old version (< 4.5.8) of Apache http client. It is recommended to use http client "
                 + "version >= 4.5.9 to avoid the breaking change introduced in apache client 4.5.7 and "
                 + "the latency in exception handling. See https://github.com/aws/aws-sdk-java/issues/1919"
                 + " for more information");
    }
}
