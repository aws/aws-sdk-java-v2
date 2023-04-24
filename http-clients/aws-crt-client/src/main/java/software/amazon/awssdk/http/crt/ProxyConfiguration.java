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

package software.amazon.awssdk.http.crt;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.crtcore.CrtProxyConfiguration;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;


/**
 * Proxy configuration for {@link AwsCrtAsyncHttpClient}. This class is used to configure an HTTPS or HTTP proxy to be used by the
 * {@link AwsCrtAsyncHttpClient}.
 *
 * @see AwsCrtAsyncHttpClient.Builder#proxyConfiguration(ProxyConfiguration)
 */
@SdkPublicApi
public final class ProxyConfiguration extends CrtProxyConfiguration
    implements ToCopyableBuilder<ProxyConfiguration.Builder, ProxyConfiguration>  {

    private ProxyConfiguration(DefaultBuilder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Builder for {@link ProxyConfiguration}.
     */
    public interface Builder extends CrtProxyConfiguration.Builder, CopyableBuilder<Builder, ProxyConfiguration> {

        /**
         * Set the hostname of the proxy.
         *
         * @param host The proxy host.
         * @return This object for method chaining.
         */
        @Override
        Builder host(String host);

        /**
         * Set the port that the proxy expects connections on.
         *
         * @param port The proxy port.
         * @return This object for method chaining.
         */
        @Override
        Builder port(int port);

        /**
         * The HTTP scheme to use for connecting to the proxy. Valid values are {@code http} and {@code https}.
         * <p>
         * The client defaults to {@code http} if none is given.
         *
         * @param scheme The proxy scheme.
         * @return This object for method chaining.
         */
        @Override
        Builder scheme(String scheme);

        /**
         * The username to use for basic proxy authentication
         * <p>
         * If not set, the client will not use basic authentication
         *
         * @param username The basic authentication username.
         * @return This object for method chaining.
         */
        @Override
        Builder username(String username);

        /**
         * The password to use for basic proxy authentication
         * <p>
         * If not set, the client will not use basic authentication
         *
         * @param password The basic authentication password.
         * @return This object for method chaining.
         */
        @Override
        Builder password(String password);

        /**
         * The option whether to use system property values from {@link ProxySystemSetting} if any of the config options are
         * missing. The value is set to "true" by default which means SDK will automatically use system property values if
         * options
         * are not provided during building the {@link ProxyConfiguration} object. To disable this behaviour, set this
         * value to
         * false.
         *
         * @param useSystemPropertyValues The option whether to use system property values
         * @return This object for method chaining.
         */
        @Override
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);


        @Override
        ProxyConfiguration build();
    }

    private static final class DefaultBuilder extends CrtProxyConfiguration.DefaultBuilder<DefaultBuilder> implements Builder {

        private DefaultBuilder(ProxyConfiguration proxyConfiguration) {
            super(proxyConfiguration);
        }

        private DefaultBuilder() {

        }

        @Override
        public ProxyConfiguration build() {
            return new ProxyConfiguration(this);
        }
    }
}