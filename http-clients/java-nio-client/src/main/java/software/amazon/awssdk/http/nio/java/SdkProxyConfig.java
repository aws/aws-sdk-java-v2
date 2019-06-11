/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.nio.java;

import java.net.*;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.ProxySystemSetting;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configurations that defines how to communicate via an HTTP proxy.
 */
@SdkProtectedApi
public final class SdkProxyConfig implements ToCopyableBuilder<SdkProxyConfig.Builder, SdkProxyConfig> {
    private final ProxySelector proxySelector;
    private final Authenticator authenticator;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Boolean useSystemPropertyValues;


    private SdkProxyConfig(DefaultSdkProxyConfigurationBuilder builder) {
        this.useSystemPropertyValues = builder.useSystemPropertyValues;
        this.host = resolveHost(builder);
        this.port = resolvePort(builder);
        this.username = resolveUserName(builder);
        this.password = resolvePassWord(builder);
        this.proxySelector = resolveProxySelector(builder);
        this.authenticator = resolveAuthenticator(builder);
    }

    public ProxySelector getProxySelector() {
        return proxySelector;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Creates a new SdkProxyConfig object with provided URI
     * @param uri The URI of the proxy to use.
     * @return SdkProxyConfig object created with the host name and port number from the input URI.
     */
    public static SdkProxyConfig create(URI uri) {
        return SdkProxyConfig.builder()
                .host(uri.getHost())
                .port(uri.getPort())
                .build();
    }

    /**
     * Create a {@link Builder}, used to create a {@link SdkProxyConfig}.
     * @return
     */
    public static Builder builder() {
        return new DefaultSdkProxyConfigurationBuilder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .proxySelector(proxySelector)
                .authenticator(authenticator);
    }

    private String resolveHost(DefaultSdkProxyConfigurationBuilder builder) {
        return builder.host != null ? builder.host
                                    : resolveValue(null, ProxySystemSetting.PROXY_HOST);
    }

    private int resolvePort(DefaultSdkProxyConfigurationBuilder builder) {
        int port = 0;

        if (builder.port != 0) {
            port = builder.port;
        } else if (useSystemPropertyValues) {
            port = ProxySystemSetting.PROXY_PORT.getStringValue()
                                                .map(Integer::parseInt)
                                                .orElse(0);
        }
        return port;
    }

    private String resolveUserName(DefaultSdkProxyConfigurationBuilder builder) {
        return builder.username != null ? builder.username
                                    : resolveValue(null, ProxySystemSetting.PROXY_USERNAME);
    }

    private String resolvePassWord(DefaultSdkProxyConfigurationBuilder builder) {
        return builder.password != null ? builder.password
                                    : resolveValue(null, ProxySystemSetting.PROXY_PASSWORD);
    }


    private ProxySelector resolveProxySelector(DefaultSdkProxyConfigurationBuilder builder) {
        if (builder.proxySelector != null) {
            return builder.proxySelector;
        } else if (host != null && port != 0) {
            return ProxySelector.of(new InetSocketAddress(host, port));
        } else {
            return null;
        }
    }

    private Authenticator resolveAuthenticator(DefaultSdkProxyConfigurationBuilder builder) {
        if (builder.authenticator != null) {
            return builder.authenticator;
        } else if (username != null && password != null) {
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            };
        } else {
            return null;
        }
    }

    /**
     * Uses the configuration options, system setting property and returns the final value of the given member.
     */
    private String resolveValue(String value, ProxySystemSetting systemSetting) {
        return value == null && useSystemPropertyValues ? systemSetting.getStringValue().orElse(null)
                : value;
    }


    /**
     * A builder for {@link SdkProxyConfig}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, SdkProxyConfig> {

        /**
         * Set the host name.
         */
        Builder host(String host);

        /**
         * Set the port number.
         */
        Builder port(int port);

        /**
         * Set the username.
         */
        Builder username(String username);

        /**
         * Set the password.
         */
        Builder password(String password);

        /**
         * Set the ProxySelector with a provided ProxySelector object.
         */
        Builder proxySelector(ProxySelector proxySelector);

        /**
         * Set the Authenticator with a provided Authenticator object.
         */
        Builder authenticator(Authenticator authenticator);

        /**
         * Option whether to use system property values from {@link ProxySystemSetting} if any of the config options
         * are missing.
         *
         * This value is set to "true" by default which means SDK will automatically use system property values
         * for options that are not provided during building the {@link SdkProxyConfig} object. To disable this behavior,
         * set this value to "false".
         */
        Builder useSystemPropertyValues(Boolean useSystemPropertyValues);

    }

    public static final class DefaultSdkProxyConfigurationBuilder implements Builder {
        private ProxySelector proxySelector = null;
        private Authenticator authenticator = null;
        private String host;
        private int port = 0;
        private String username;
        private String password;
        private Boolean useSystemPropertyValues = true;

        @Override
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public void setHost(String host) {
            host(host);
        }

        @Override
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public void setPort(int port) {
            port(port);
        }

        @Override
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public void setUsername(String username) {
            username(username);
        }

        @Override
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public void setPassword(String password) {
            password(password);
        }

        @Override
        public Builder proxySelector(ProxySelector proxySelector) {
            this.proxySelector = proxySelector;
            return this;
        }

        @Override
        public Builder authenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        @Override
        public Builder useSystemPropertyValues(Boolean useSystemPropertyValues) {
            this.useSystemPropertyValues = useSystemPropertyValues;
            return this;
        }

        @Override
        public SdkProxyConfig build() {
            return new SdkProxyConfig(this);
        }
    }
}
