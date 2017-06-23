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

package software.amazon.awssdk.http.nio.netty;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.MAX_CONNECTIONS;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;

import java.time.Duration;
import java.util.Optional;
import software.amazon.awssdk.annotation.Immutable;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClientFactory;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Factory that allows more advanced configuration of the Netty NIO HTTP implementation. Use {@link #builder()} to
 * configure and construct an immutable instance of the factory.
 */
@Immutable
public final class NettySdkHttpClientFactory
        implements SdkAsyncHttpClientFactory, ToCopyableBuilder<NettySdkHttpClientFactory.Builder, NettySdkHttpClientFactory> {

    private final AttributeMap standardOptions;
    private final Optional<Boolean> trustAllCertificates;

    private NettySdkHttpClientFactory(DefaultBuilder builder) {
        this.standardOptions = builder.standardOptions.build();
        this.trustAllCertificates = Optional.ofNullable(builder.trustAllCertificates);
    }

    /**
     * @return Optional of the maxConnectionsPerEndpoint setting.
     * @see Builder#maxConnectionsPerEndpoint(Integer)
     */
    public Optional<Integer> maxConnectionsPerEndpoint() {
        return Optional.ofNullable(standardOptions.get(MAX_CONNECTIONS));
    }

    /**
     * @return Optional of the socketTimeout setting.
     * @see Builder#socketTimeout(Duration)
     */
    public Optional<Duration> socketTimeout() {
        return Optional.ofNullable(standardOptions.get(SOCKET_TIMEOUT));
    }

    /**
     * @return Optional of the connectionTimeout setting.
     * @see Builder#connectionTimeout(Duration)
     */
    public Optional<Duration> connectionTimeout() {
        return Optional.ofNullable(standardOptions.get(SOCKET_TIMEOUT));
    }

    /**
     * @return Optional of the trustAllCertificates setting.
     * @see Builder#trustAllCertificates(Boolean)
     */
    public Optional<Boolean> trustAllCertificates() {
        return trustAllCertificates;
    }

    /**
     * Create an HTTP client instance with global defaults applied. This client instance can be shared
     * across multiple SDK clients for better resource utilization. Note that if sharing is not needed then it is
     * recommended to pass this factory into the SDK client builders so that service defaults may be applied.
     *
     * @return Created client.
     */
    public SdkAsyncHttpClient createHttpClient() {
        return createHttpClientWithDefaults(AttributeMap.empty());
    }

    @Override
    public SdkAsyncHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults) {
        return new NettyNioAsyncHttpClient(this, standardOptions.merge(serviceDefaults)
                                                                .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(standardOptions.toBuilder())
                .trustAllCertificates(trustAllCertificates.orElse(null));
    }

    public static Builder builder() {
        return new DefaultBuilder(AttributeMap.builder());
    }


    /**
     * Builder interface for {@link NettySdkHttpClientFactory}.
     *
     * @see NettySdkHttpClientFactory#builder()
     */
    public interface Builder extends CopyableBuilder<Builder, NettySdkHttpClientFactory> {

        /**
         * Max allowed connections per endpoint allowed in the connection pool.
         *
         * @param maxConnectionsPerEndpoint New value for max connections per endpoint.
         * @return This builder for method chaining.
         */
        Builder maxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint);

        /**
         * The amount of time to wait for data to be transferred over an established, open connection before the connection is
         * timed out.
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         */
        Builder connectionTimeout(Duration socketTimeout);

        /**
         * Forces the HTTP client to trust all certificates, even invalid or self signed certificates. This should only ever
         * be used for testing purposes.
         *
         * @param trustAllCertificates Whether to trust all certificates. The default is false and only valid certificates
         *                             whose trust can be verified via the trust store will be trusted.
         * @return This builder for method chaining.
         */
        Builder trustAllCertificates(Boolean trustAllCertificates);
    }

    private static final class DefaultBuilder implements Builder {

        private Boolean trustAllCertificates;
        private final AttributeMap.Builder standardOptions;

        private DefaultBuilder(AttributeMap.Builder standardOptions) {
            this.standardOptions = standardOptions;
        }

        @Override
        public Builder maxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint) {
            standardOptions.put(MAX_CONNECTIONS, maxConnectionsPerEndpoint);
            return this;
        }

        public void setMaxConnectionsPerEndpoint(Integer maxConnectionsPerEndpoint) {
            maxConnectionsPerEndpoint(maxConnectionsPerEndpoint);
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            standardOptions.put(SOCKET_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            standardOptions.put(CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }

        @Override
        public Builder trustAllCertificates(Boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
            return this;
        }

        public void setTrustAllCertificates(Boolean trustAllCertificates) {
            trustAllCertificates(trustAllCertificates);
        }

        @Override
        public NettySdkHttpClientFactory build() {
            return new NettySdkHttpClientFactory(this);
        }
    }
}
