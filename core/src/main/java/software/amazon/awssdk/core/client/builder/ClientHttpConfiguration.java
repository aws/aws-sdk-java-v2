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

package software.amazon.awssdk.core.client.builder;

import java.util.Optional;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Allows configuration of the underlying HTTP client. Either a HTTP client factory may be provided, or an already
 * constructed {@link SdkHttpClient}, but not both.
 */
@ReviewBeforeRelease("Do we want this approach to force mutual exclusion or should we flatten it out" +
                     " and do mutex checks at runtime?")
public final class ClientHttpConfiguration
        implements ToCopyableBuilder<ClientHttpConfiguration.Builder, ClientHttpConfiguration> {

    private final SdkHttpClient httpClient;
    private final SdkHttpClientFactory httpClientFactory;

    private ClientHttpConfiguration(DefaultHttpConfigurationBuilder builder) {
        this.httpClient = builder.httpClient;
        this.httpClientFactory = builder.httpClientFactory;
    }

    /**
     * @return The currently configured {@link SdkHttpClient} or an empty {@link Optional} if not present.
     */
    public Optional<SdkHttpClient> httpClient() {
        return Optional.ofNullable(httpClient);
    }

    /**
     * @return The currently configured {@link SdkHttpClientFactory} or an empty {@link Optional} if not present.
     */
    public Optional<SdkHttpClientFactory> httpClientFactory() {
        return Optional.ofNullable(httpClientFactory);
    }

    @Override
    public ClientHttpConfiguration.Builder toBuilder() {
        return new DefaultHttpConfigurationBuilder()
                .httpClient(httpClient)
                .httpClientFactory(httpClientFactory);
    }

    /**
     * Transforms this configuration into an {@link Either} of {@link SdkHttpClient} and {@link SdkHttpClientFactory} to ease
     * resolution of the client. Returns an empty {@link Optional} if neither is set.
     */
    @SdkInternalApi
    Optional<Either<SdkHttpClient, SdkHttpClientFactory>> toEither() {
        return Either.fromNullable(httpClient, httpClientFactory);
    }

    /**
     * @return Builder instance to construct a {@link ClientHttpConfiguration}.
     */
    public static Builder builder() {
        return new DefaultHttpConfigurationBuilder();
    }

    /**
     * A builder for {@link ClientHttpConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientHttpConfiguration> {

        // TODO revisit link to apache builder

        /**
         * Sets the {@link SdkHttpClient} that the SDK service client will use to make HTTP calls. This HTTP client may be shared
         * between multiple SDK service clients to share a common connection pool. To create a client you must use an
         * implementation specific builder/factory, the default implementation for the SDK is {@link
         * ApacheSdkHttpClientFactory}. Note that this method is only recommended when you
         * wish to share an HTTP client across multiple SDK service clients. If you do not wish to share HTTP clients, it is
         * recommended to use {@link #httpClientFactory(SdkHttpClientFactory)} so that service specific default configuration may
         * be applied.
         *
         * <p>
         * <b>This client must be closed by the client when it is ready to be disposed. The SDK will not close the HTTP client
         * when the service client is closed.</b>
         * </p>
         *
         * @return This builder for method chaining.
         */
        // This intentionally returns SdkBuilder so that only httpClient or httpClientFactory may be supplied.
        SdkBuilder<?, ClientHttpConfiguration> httpClient(SdkHttpClient sdkHttpClient);

        /**
         * Sets a custom HTTP client factory that will be used to obtain a configured instance of {@link SdkHttpClient}. Any
         * service specific HTTP configuration will be merged with the factory's configuration prior to creating the client. When
         * there is no desire to share HTTP clients across multiple service clients, the client factory is the preferred way to
         * customize the HTTP client as it benefits from service specific defaults.
         *
         * <p>
         * <b>Clients created by the factory are managed by the SDK and will be closed when the service client is closed.</b>
         * </p>
         *
         * @return This builder for method chaining.
         */
        // This intentionally returns SdkBuilder so that only httpClient or httpClientFactory may be supplied.
        SdkBuilder<?, ClientHttpConfiguration> httpClientFactory(SdkHttpClientFactory sdkClientFactory);
    }

    /**
     * Builder for a {@link ClientHttpConfiguration}.
     */
    private static final class DefaultHttpConfigurationBuilder implements Builder {

        private SdkHttpClient httpClient;
        private SdkHttpClientFactory httpClientFactory;

        private DefaultHttpConfigurationBuilder() {
        }

        @Override
        public DefaultHttpConfigurationBuilder httpClient(SdkHttpClient sdkHttpClient) {
            this.httpClient = sdkHttpClient;
            return this;
        }

        public void setHttpClient(SdkHttpClient httpClient) {
            httpClient(httpClient);
        }

        @Override
        public DefaultHttpConfigurationBuilder httpClientFactory(SdkHttpClientFactory sdkClientFactory) {
            this.httpClientFactory = sdkClientFactory;
            return this;
        }

        public void setHttpClientFactory(SdkHttpClientFactory httpClientFactory) {
            httpClientFactory(httpClientFactory);
        }

        /**
         * @return An immutable {@link ClientHttpConfiguration} object.
         */
        public ClientHttpConfiguration build() {
            return new ClientHttpConfiguration(this);
        }
    }
}
