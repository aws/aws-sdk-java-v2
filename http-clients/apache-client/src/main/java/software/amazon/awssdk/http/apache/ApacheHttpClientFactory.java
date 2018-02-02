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

package software.amazon.awssdk.http.apache;

import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpRequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.Defaults;
import software.amazon.awssdk.http.apache.internal.SdkProxyRoutePlanner;
import software.amazon.awssdk.http.apache.internal.conn.ClientConnectionManagerFactory;
import software.amazon.awssdk.http.apache.internal.conn.SdkConnectionKeepAliveStrategy;
import software.amazon.awssdk.http.apache.internal.impl.ApacheConnectionManagerFactory;
import software.amazon.awssdk.http.apache.internal.impl.ApacheSdkHttpClient;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
class ApacheHttpClientFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ApacheHttpClientFactory.class);

    private final ApacheConnectionManagerFactory cmFactory = new ApacheConnectionManagerFactory();

    public SdkHttpClient create(ApacheSdkHttpClientFactory configuration,
                                AttributeMap resolvedOptions,
                                ApacheHttpRequestConfig requestConfig) {
        return new ApacheHttpClient(createClient(configuration, resolvedOptions), requestConfig, resolvedOptions);
    }

    private ConnectionManagerAwareHttpClient createClient(ApacheSdkHttpClientFactory configuration,
                                                          AttributeMap standardOptions) {
        final HttpClientBuilder builder = HttpClients.custom();
        // Note that it is important we register the original connection manager with the
        // IdleConnectionReaper as it's required for the successful deregistration of managers
        // from the reaper. See https://github.com/aws/aws-sdk-java/issues/722.
        final HttpClientConnectionManager cm = cmFactory.create(configuration, standardOptions);

        builder.setRequestExecutor(new HttpRequestExecutor())
               // SDK handles decompression
               .disableContentCompression()
               .setKeepAliveStrategy(buildKeepAliveStrategy(configuration))
               .disableRedirectHandling()
               .disableAutomaticRetries()
               .setUserAgent("") // SDK will set the user agent header in the pipeline. Don't let Apache waste time
               .setConnectionManager(ClientConnectionManagerFactory.wrap(cm));

        addProxyConfig(builder, configuration.proxyConfiguration());

        // TODO idle connection reaper
        //        if (.useReaper()) {
        //            IdleConnectionReaper.registerConnectionManager(cm, settings.getMaxIdleConnectionTime());
        //        }

        return new ApacheSdkHttpClient(builder.build(), cm);
    }

    private void addProxyConfig(HttpClientBuilder builder,
                                ProxyConfiguration proxyConfiguration) {
        if (isProxyEnabled(proxyConfiguration)) {

            LOG.debug("Configuring Proxy. Proxy Host: {}", proxyConfiguration.endpoint());

            builder.setRoutePlanner(new SdkProxyRoutePlanner(proxyConfiguration.endpoint().getHost(),
                                                             proxyConfiguration.endpoint().getPort(),
                                                             proxyConfiguration.nonProxyHosts()));

            if (isAuthenticatedProxy(proxyConfiguration)) {
                builder.setDefaultCredentialsProvider(ApacheUtils.newProxyCredentialsProvider(proxyConfiguration));
            }
        }
    }

    private ConnectionKeepAliveStrategy buildKeepAliveStrategy(ApacheSdkHttpClientFactory configuration) {
        final long maxIdle = configuration.connectionMaxIdleTime().orElse(Defaults.MAX_IDLE_CONNECTION_TIME).toMillis();
        return maxIdle > 0 ? new SdkConnectionKeepAliveStrategy(maxIdle) : null;
    }

    private boolean isAuthenticatedProxy(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration.username() != null && proxyConfiguration.password() != null;
    }

    private boolean isProxyEnabled(ProxyConfiguration proxyConfiguration) {
        return proxyConfiguration.endpoint() != null
               && proxyConfiguration.endpoint().getHost() != null
               && proxyConfiguration.endpoint().getPort() > 0;
    }
}

