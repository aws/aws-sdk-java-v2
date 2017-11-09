/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

public final class ApacheUtils {

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
        final HttpClientContext clientContext = new HttpClientContext();
        addPreemptiveAuthenticationProxy(clientContext, proxyConfiguration);
        return clientContext;

    }

    /**
     * Returns a new Credentials Provider for use with proxy authentication.
     */
    public static CredentialsProvider newProxyCredentialsProvider(ProxyConfiguration proxyConfiguration) {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(newAuthScope(proxyConfiguration), newNtCredentials(proxyConfiguration));
        return provider;
    }

    /**
     * Returns a new instance of NTCredentials used for proxy authentication.
     */
    private static Credentials newNtCredentials(ProxyConfiguration proxyConfiguration) {
        return new NTCredentials(proxyConfiguration.username(),
                                 proxyConfiguration.password(),
                                 proxyConfiguration.ntlmWorkstation(),
                                 proxyConfiguration.ntlmDomain());
    }

    /**
     * Returns a new instance of AuthScope used for proxy authentication.
     */
    private static AuthScope newAuthScope(ProxyConfiguration proxyConfiguration) {
        return new AuthScope(proxyConfiguration.endpoint().getHost(), proxyConfiguration.endpoint().getPort());
    }

    private static void addPreemptiveAuthenticationProxy(HttpClientContext clientContext,
                                                         ProxyConfiguration proxyConfiguration) {

        if (proxyConfiguration.preemptiveBasicAuthenticationEnabled()) {
            HttpHost targetHost = new HttpHost(proxyConfiguration.endpoint().getHost(), proxyConfiguration.endpoint().getPort());
            final CredentialsProvider credsProvider = newProxyCredentialsProvider(proxyConfiguration);
            // Create AuthCache instance
            AuthCache authCache = new BasicAuthCache();
            // Generate BASIC scheme object and add it to the local auth cache
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(targetHost, basicAuth);

            clientContext.setCredentialsProvider(credsProvider);
            clientContext.setAuthCache(authCache);
        }
    }
}
