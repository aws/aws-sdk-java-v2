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

/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package software.amazon.awssdk.http.apache5.internal.conn;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.SchemePortResolver;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionOperator;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * This is a fork of {@link PoolingHttpClientConnectionManagerBuilder} from Apache 5. The purpose of this forked class is to
 * enable usage of the {@link SafePoolingHttpClientConnectionManager} to enable the workaround for
 * https://github.com/aws/aws-sdk-java-v2/issues/6786.
 */
// This a direct copy of PoolingHttpClientConnectionManagerBuilder with minor changes to remove methods we don't use and
// updates to follow our style guide.
@SdkInternalApi
public final class SafePoolingHttpClientConnectionManagerBuilder {

    private TlsSocketStrategy tlsSocketStrategy;
    private SchemePortResolver schemePortResolver;
    private DnsResolver dnsResolver;
    private Resolver<HttpRoute, SocketConfig> socketConfigResolver;
    private Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver;
    private Resolver<HttpHost, TlsConfig> tlsConfigResolver;

    private int maxConnTotal;
    private int maxConnPerRoute;

    public static SafePoolingHttpClientConnectionManagerBuilder create() {
        return new SafePoolingHttpClientConnectionManagerBuilder();
    }

    /**
     * Sets {@link TlsSocketStrategy} instance.
     *
     * @return this instance.
     */
    public SafePoolingHttpClientConnectionManagerBuilder setTlsSocketStrategy(TlsSocketStrategy tlsSocketStrategy) {
        this.tlsSocketStrategy = tlsSocketStrategy;
        return this;
    }

    /**
     * Sets {@link DnsResolver} instance.
     *
     * @return this instance.
     */
    public SafePoolingHttpClientConnectionManagerBuilder setDnsResolver(DnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
        return this;
    }

    /**
     * Sets {@link SchemePortResolver} instance.
     *
     * @return this instance.
     */
    public SafePoolingHttpClientConnectionManagerBuilder setSchemePortResolver(SchemePortResolver schemePortResolver) {
        this.schemePortResolver = schemePortResolver;
        return this;
    }

    /**
     * Sets maximum total connection value.
     *
     * @return this instance.
     */
    public SafePoolingHttpClientConnectionManagerBuilder setMaxConnTotal(int maxConnTotal) {
        this.maxConnTotal = maxConnTotal;
        return this;
    }

    /**
     * Sets maximum connection per route value.
     *
     * @return this instance.
     */
    public SafePoolingHttpClientConnectionManagerBuilder setMaxConnPerRoute(int maxConnPerRoute) {
        this.maxConnPerRoute = maxConnPerRoute;
        return this;
    }

    /**
     * Sets the same {@link SocketConfig} for all routes.
     *
     * @return this instance.
     */
    public SafePoolingHttpClientConnectionManagerBuilder setDefaultSocketConfig(SocketConfig config) {
        this.socketConfigResolver = route -> config;
        return this;
    }

    /**
     * Sets {@link Resolver} of {@link SocketConfig} on a per route basis.
     *
     * @return this instance.
     * @since 5.2
     */
    public SafePoolingHttpClientConnectionManagerBuilder setSocketConfigResolver(
        Resolver<HttpRoute, SocketConfig> socketConfigResolver) {
        this.socketConfigResolver = socketConfigResolver;
        return this;
    }

    /**
     * Sets the same {@link ConnectionConfig} for all routes.
     *
     * @return this instance.
     * @since 5.2
     */
    public SafePoolingHttpClientConnectionManagerBuilder setDefaultConnectionConfig(ConnectionConfig config) {
        this.connectionConfigResolver = route -> config;
        return this;
    }

    /**
     * Sets {@link Resolver} of {@link ConnectionConfig} on a per route basis.
     *
     * @return this instance.
     * @since 5.2
     */
    public SafePoolingHttpClientConnectionManagerBuilder setConnectionConfigResolver(
             Resolver<HttpRoute, ConnectionConfig> connectionConfigResolver) {
        this.connectionConfigResolver = connectionConfigResolver;
        return this;
    }

    /**
     * Sets the same {@link TlsConfig} for all hosts.
     *
     * @return this instance.
     * @since 5.2
     */
    public SafePoolingHttpClientConnectionManagerBuilder setDefaultTlsConfig(TlsConfig config) {
        this.tlsConfigResolver = host -> config;
        return this;
    }

    /**
     * Sets {@link Resolver} of {@link TlsConfig} on a per host basis.
     *
     * @return this instance.
     * @since 5.2
     */
    public SafePoolingHttpClientConnectionManagerBuilder setTlsConfigResolver(
             Resolver<HttpHost, TlsConfig> tlsConfigResolver) {
        this.tlsConfigResolver = tlsConfigResolver;
        return this;
    }

    protected HttpClientConnectionOperator createConnectionOperator(SchemePortResolver schemePortResolver,
                                                                    DnsResolver dnsResolver,
                                                                    TlsSocketStrategy tlsSocketStrategy) {
        return new DefaultHttpClientConnectionOperator(schemePortResolver, dnsResolver,
                                                       RegistryBuilder.<TlsSocketStrategy>create()
                        .register(URIScheme.HTTPS.id, tlsSocketStrategy)
                        .build());
    }

    public SafePoolingHttpClientConnectionManager build() {
        TlsSocketStrategy tlsSocketStrategyCopy;
        if (tlsSocketStrategy != null) {
            tlsSocketStrategyCopy = tlsSocketStrategy;
        } else {
            tlsSocketStrategyCopy = DefaultClientTlsStrategy.createDefault();
        }

        SafePoolingHttpClientConnectionManager poolingmgr = new SafePoolingHttpClientConnectionManager(
                createConnectionOperator(schemePortResolver, dnsResolver, tlsSocketStrategyCopy));
        poolingmgr.setSocketConfigResolver(socketConfigResolver);
        poolingmgr.setConnectionConfigResolver(connectionConfigResolver);
        poolingmgr.setTlsConfigResolver(tlsConfigResolver);
        if (maxConnTotal > 0) {
            poolingmgr.setMaxTotal(maxConnTotal);
        }
        if (maxConnPerRoute > 0) {
            poolingmgr.setDefaultMaxPerRoute(maxConnPerRoute);
        }
        return poolingmgr;
    }

}
