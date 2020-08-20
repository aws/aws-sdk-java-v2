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

package software.amazon.awssdk.http.apache.async;

import static software.amazon.awssdk.utils.NumericUtils.saturatedCast;

import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

@SdkInternalApi
final class ApacheAsyncUtils {
    private ApacheAsyncUtils() {
    }

    /**
     * Returns a new HttpClientContext used for request execution.
     */
    public static HttpClientContext newClientContext(ProxyConfiguration proxyConfiguration) {
        HttpClientContext clientContext = new HttpClientContext();
        addPreemptiveAuthenticationProxy(clientContext, proxyConfiguration);
        return clientContext;
    }

    public static RequestConfig getRequestConfig(final SdkHttpRequest request,
                                                 final ApacheAsyncRequestConfig requestConfig) {
        int connectTimeout = saturatedCast(requestConfig.connectionTimeout().toMillis());
        int connectAcquireTimeout = saturatedCast(requestConfig.connectionAcquireTimeout().toMillis());
        RequestConfig.Builder requestConfigBuilder = RequestConfig
            .custom()
            .setConnectionRequestTimeout(connectAcquireTimeout, TimeUnit.MILLISECONDS)
            .setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);

        /*
         * Enable 100-continue support for PUT operations, since this is
         * where we're potentially uploading large amounts of data and want
         * to find out as early as possible if an operation will fail. We
         * don't want to do this for all operations since it will cause
         * extra latency in the network interaction.
         */
        if (SdkHttpMethod.PUT == request.method() && requestConfig.expectContinueEnabled()) {
            requestConfigBuilder.setExpectContinueEnabled(true);
        }

        return requestConfigBuilder.build();
    }

    /**
     * Returns a new Credentials Provider for use with proxy authentication.
     */
    public static CredentialsProvider newProxyCredentialsProvider(ProxyConfiguration proxyConfiguration) {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(newAuthScope(proxyConfiguration), newNtCredentials(proxyConfiguration));
        return provider;
    }

    /**
     * Returns a new instance of NTCredentials used for proxy authentication.
     */
    private static Credentials newNtCredentials(ProxyConfiguration proxyConfiguration) {
        return new NTCredentials(proxyConfiguration.username(),
                                 proxyConfiguration.password().toCharArray(),
                                 proxyConfiguration.ntlmWorkstation(),
                                 proxyConfiguration.ntlmDomain());
    }

    /**
     * Returns a new instance of AuthScope used for proxy authentication.
     */
    private static AuthScope newAuthScope(ProxyConfiguration proxyConfiguration) {
        return new AuthScope(proxyConfiguration.host(), proxyConfiguration.port());
    }

    private static void addPreemptiveAuthenticationProxy(HttpClientContext clientContext, ProxyConfiguration proxyConfiguration) {
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
