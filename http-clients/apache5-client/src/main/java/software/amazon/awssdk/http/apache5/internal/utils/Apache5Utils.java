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

package software.amazon.awssdk.http.apache5.internal.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.apache5.ProxyConfiguration;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class Apache5Utils {
    private static final Logger logger = Logger.loggerFor(Apache5Utils.class);

    private Apache5Utils() {
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
        clientContext.setRequestConfig(builder.build());
        return clientContext;

    }


    /**
     * Returns a new Credentials Provider for use with proxy authentication.
     */
    public static CredentialsProvider newProxyCredentialsProvider(ProxyConfiguration proxyConfiguration) {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        //  TODO : NTCredentials is deprecated.
        // provider.setCredentials(newAuthScope(proxyConfiguration), newNtCredentials(proxyConfiguration));
        return provider;
    }

    // /**
    //  * Returns a new instance of NTCredentials used for proxy authentication.
    //  */
    // private static Credentials newNtCredentials(ProxyConfiguration proxyConfiguration) {
    //     return new NTCredentials(proxyConfiguration.username(),
    //                              proxyConfiguration.password(),
    //                              proxyConfiguration.ntlmWorkstation(),
    //                              proxyConfiguration.ntlmDomain());
    // }

    // /**
    //  * Returns a new instance of AuthScope used for proxy authentication.
    //  */
    // private static AuthScope newAuthScope(ProxyConfiguration proxyConfiguration) {
    //     return new AuthScope(proxyConfiguration.host(), proxyConfiguration.port());
    // }

    private static void addPreemptiveAuthenticationProxy(HttpClientContext clientContext,
                                                         ProxyConfiguration proxyConfiguration) {

        if (proxyConfiguration.preemptiveBasicAuthenticationEnabled()) {
            HttpHost targetHost = new HttpHost(proxyConfiguration.host(), proxyConfiguration.port());
            CredentialsProvider credsProvider = newProxyCredentialsProvider(proxyConfiguration);
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
