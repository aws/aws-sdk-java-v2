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

package software.amazon.awssdk.http.urlconnection;

import static software.amazon.awssdk.http.SdkHttpConfigurationOption.CONNECTION_TIMEOUT;
import static software.amazon.awssdk.http.SdkHttpConfigurationOption.SOCKET_TIMEOUT;

import java.time.Duration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A factory for an instance of {@link SdkHttpClient} that uses JDKs build-in {@link java.net.URLConnection} HTTP implementation.
 * The factory can be configured via the {@link #builder()}, once built it can be use to create a {@link SdkHttpClient} via
 * the {@link #createHttpClient()} method.
 *
 * <pre class="brush: java">
 * SdkHttpClient httpClient = UrlConnectionSdkHttpClientFactory.builder()
 * .socketTimeout(Duration.ofSeconds(10))
 * .connectionTimeout(Duration.ofSeconds(1))
 * .build()
 * .createHttpClient();
 * </pre>
 */
public final class UrlConnectionSdkHttpClientFactory
    implements SdkHttpClientFactory,
               ToCopyableBuilder<UrlConnectionSdkHttpClientFactory.Builder, UrlConnectionSdkHttpClientFactory> {
    private final AttributeMap options;

    private UrlConnectionSdkHttpClientFactory(DefaultBuilder builder) {
        this.options = builder.options.build();
    }

    /**
     * Used by the SDK to create a {@link SdkHttpClient} with service-default values if no other values have been configured
     *
     * @param serviceDefaults Service specific defaults. Keys will be one of the constants defined in
     * {@link SdkHttpConfigurationOption}.
     * @return an instance of {@link SdkHttpClient}
     */
    @Override
    public SdkHttpClient createHttpClientWithDefaults(AttributeMap serviceDefaults) {
        return new UrlConnectionHttpClient(options.merge(serviceDefaults)
                                                  .merge(SdkHttpConfigurationOption.GLOBAL_HTTP_DEFAULTS));
    }

    /**
     * Create a {@link SdkHttpClient} with the values configured on the {@link #builder()}.
     *
     * @return an instance of {@link SdkHttpClient}
     */
    public SdkHttpClient createHttpClient() {
        return createHttpClientWithDefaults(AttributeMap.empty());
    }

    public static Builder builder() {
        return new DefaultBuilder(AttributeMap.builder());
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(options.toBuilder());
    }

    public interface Builder extends CopyableBuilder<Builder, UrlConnectionSdkHttpClientFactory> {

        /**
         * Sets the read timeout to a specified timeout.
         * A timeout of zero is interpreted as an infinite timeout.
         *
         * @param socketTimeout the timeout as a {@link Duration}
         * @return this object for method chaining
         */
        Builder socketTimeout(Duration socketTimeout);

        /**
         * Sets the read timeout to a specified timeout.
         * A timeout of zero is interpreted as an infinite timeout.
         *
         * @param socketTimeout the timeout as a {@link Duration}
         * @return this object for method chaining
         */
        Builder connectionTimeout(Duration socketTimeout);

    }

    private static final class DefaultBuilder implements Builder {

        private final AttributeMap.Builder options;

        private DefaultBuilder(AttributeMap.Builder options) {
            this.options = options;
        }

        @Override
        public UrlConnectionSdkHttpClientFactory build() {
            return new UrlConnectionSdkHttpClientFactory(this);
        }

        @Override
        public Builder socketTimeout(Duration socketTimeout) {
            options.put(SOCKET_TIMEOUT, socketTimeout);
            return this;
        }

        public void setSocketTimeout(Duration socketTimeout) {
            socketTimeout(socketTimeout);
        }

        @Override
        public Builder connectionTimeout(Duration connectionTimeout) {
            options.put(CONNECTION_TIMEOUT, connectionTimeout);
            return this;
        }

        public void setConnectionTimeout(Duration connectionTimeout) {
            connectionTimeout(connectionTimeout);
        }
    }
}
