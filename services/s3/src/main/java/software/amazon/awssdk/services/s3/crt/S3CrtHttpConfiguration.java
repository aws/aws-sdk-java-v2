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

package software.amazon.awssdk.services.s3.crt;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * HTTP configuration for AWS CRT-based S3 client.
 *
 * @see S3CrtAsyncClientBuilder#httpConfiguration
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3CrtHttpConfiguration implements ToCopyableBuilder<S3CrtHttpConfiguration.Builder,
    S3CrtHttpConfiguration> {
    private final Duration connectionTimeout;
    private final S3CrtProxyConfiguration proxyConfiguration;
    private final S3CrtConnectionHealthConfiguration healthConfiguration;
    private final Boolean trustAllCertificatesEnabled;

    private S3CrtHttpConfiguration(DefaultBuilder builder) {
        this.connectionTimeout = builder.connectionTimeout;
        this.proxyConfiguration = builder.proxyConfiguration;
        this.healthConfiguration = builder.healthConfiguration;
        this.trustAllCertificatesEnabled = builder.trustAllCertificatesEnabled;
    }

    /**
     * Creates a default builder for {@link S3CrtHttpConfiguration}.
     */
    public static Builder builder() {
        return new S3CrtHttpConfiguration.DefaultBuilder();
    }

    /**
     * Return the amount of time to wait when initially establishing a connection before giving up and timing out.
     */
    public Duration connectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Return the configured {@link S3CrtProxyConfiguration}.
     */
    public S3CrtProxyConfiguration proxyConfiguration() {
        return proxyConfiguration;
    }

    /**
     * Return the configured {@link S3CrtConnectionHealthConfiguration}.
     */
    public S3CrtConnectionHealthConfiguration healthConfiguration() {
        return healthConfiguration;
    }

    /**
     * Return the configured {@link Builder#trustAllCertificatesEnabled}.
     */
    public Boolean trustAllCertificatesEnabled() {
        return trustAllCertificatesEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3CrtHttpConfiguration that = (S3CrtHttpConfiguration) o;

        if (!Objects.equals(connectionTimeout, that.connectionTimeout)) {
            return false;
        }
        if (!Objects.equals(proxyConfiguration, that.proxyConfiguration)) {
            return false;
        }
        if (!Objects.equals(healthConfiguration, that.healthConfiguration)) {
            return false;
        }
        return Objects.equals(trustAllCertificatesEnabled, that.trustAllCertificatesEnabled);
    }

    @Override
    public int hashCode() {
        int result = connectionTimeout != null ? connectionTimeout.hashCode() : 0;
        result = 31 * result + (proxyConfiguration != null ? proxyConfiguration.hashCode() : 0);
        result = 31 * result + (healthConfiguration != null ? healthConfiguration.hashCode() : 0);
        result = 31 * result + (trustAllCertificatesEnabled != null ? trustAllCertificatesEnabled.hashCode() : 0);
        return result;
    }

    @Override
    public Builder toBuilder() {
        return new S3CrtHttpConfiguration.DefaultBuilder(this);
    }

    public interface Builder extends CopyableBuilder<S3CrtHttpConfiguration.Builder, S3CrtHttpConfiguration> {
        /**
         * The amount of time to wait when initially establishing a connection before giving up and timing out.
         *
         * @param connectionTimeout timeout
         * @return The builder of the method chaining.
         */
        Builder connectionTimeout(Duration connectionTimeout);


        /**
         * <p>
         *     Option to disable SSL cert validation and SSL host name verification.
         *     This turns off x.509 validation.
         *     By default, this option is off.
         *     Only enable this option for testing purposes.
         * @param trustAllCertificatesEnabled True if SSL cert validation is disabled.
         * @return The builder of the method chaining.
         */
        Builder trustAllCertificatesEnabled(Boolean trustAllCertificatesEnabled);

        /**
         * Sets the http proxy configuration to use for this client.
         *
         * @param proxyConfiguration The http proxy configuration to use
         * @return The builder of the method chaining.
         */
        Builder proxyConfiguration(S3CrtProxyConfiguration proxyConfiguration);

        /**
         * A convenience method that creates an instance of the {@link S3CrtProxyConfiguration} builder, avoiding the
         * need to create one manually via {@link S3CrtProxyConfiguration#builder()}.
         *
         * @param configurationBuilder The config builder to use
         * @return The builder of the method chaining.
         * @see #proxyConfiguration(S3CrtProxyConfiguration)
         */
        Builder proxyConfiguration(Consumer<S3CrtProxyConfiguration.Builder> configurationBuilder);

        /**
         * Configure the health checks for all connections established by this client.
         *
         * <p>
         * You can set a throughput threshold for a connection to be considered healthy. If a connection falls below this
         * threshold ({@link S3CrtConnectionHealthConfiguration#minimumThroughputInBps() }) for the configurable amount of time
         * ({@link S3CrtConnectionHealthConfiguration#minimumThroughputTimeout()}), then the connection is considered unhealthy
         * and will be shut down.
         *
         * @param healthConfiguration The health checks config to use
         * @return The builder of the method chaining.
         */
        Builder connectionHealthConfiguration(S3CrtConnectionHealthConfiguration healthConfiguration);

        /**
         * A convenience method that creates an instance of the {@link S3CrtConnectionHealthConfiguration} builder, avoiding the
         * need to create one manually via {@link S3CrtConnectionHealthConfiguration#builder()}.
         *
         * @param configurationBuilder The health checks config builder to use
         * @return The builder of the method chaining.
         * @see #connectionHealthConfiguration(S3CrtConnectionHealthConfiguration)
         */
        Builder connectionHealthConfiguration(Consumer<S3CrtConnectionHealthConfiguration.Builder>
                                                  configurationBuilder);

        @Override
        S3CrtHttpConfiguration build();
    }

    private static final class DefaultBuilder implements Builder {
        private S3CrtConnectionHealthConfiguration healthConfiguration;
        private Duration connectionTimeout;
        private Boolean trustAllCertificatesEnabled;
        private S3CrtProxyConfiguration proxyConfiguration;

        private DefaultBuilder() {
        }

        private DefaultBuilder(S3CrtHttpConfiguration httpConfiguration) {
            this.healthConfiguration = httpConfiguration.healthConfiguration;
            this.connectionTimeout = httpConfiguration.connectionTimeout;
            this.proxyConfiguration = httpConfiguration.proxyConfiguration;
            this.trustAllCertificatesEnabled = httpConfiguration.trustAllCertificatesEnabled;
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        @Override
        public Builder trustAllCertificatesEnabled(Boolean trustAllCertificatesEnabled) {
            this.trustAllCertificatesEnabled = trustAllCertificatesEnabled;
            return this;
        }

        @Override
        public Builder proxyConfiguration(S3CrtProxyConfiguration proxyConfiguration) {
            this.proxyConfiguration = proxyConfiguration;
            return this;
        }

        @Override
        public Builder proxyConfiguration(Consumer<S3CrtProxyConfiguration.Builder> configurationBuilder) {
            return proxyConfiguration(S3CrtProxyConfiguration.builder()
                                                             .applyMutation(configurationBuilder)
                                                             .build());
        }

        @Override
        public Builder connectionHealthConfiguration(S3CrtConnectionHealthConfiguration healthConfiguration) {
            this.healthConfiguration = healthConfiguration;
            return this;
        }

        @Override
        public Builder connectionHealthConfiguration(Consumer<S3CrtConnectionHealthConfiguration.Builder>
                                                         configurationBuilder) {
            return connectionHealthConfiguration(S3CrtConnectionHealthConfiguration.builder()
                                                                                   .applyMutation(configurationBuilder)
                                                                                   .build());
        }

        @Override
        public S3CrtHttpConfiguration build() {
            return new S3CrtHttpConfiguration(this);
        }
    }
}
