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
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClientFactory;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.SdkBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Allows configuration of the underlying async HTTP client. Either a HTTP client factory may be provided, or an already
 * constructed {@link SdkAsyncHttpClient}, but not both.
 */
@ReviewBeforeRelease("Do we want this approach to force mutual exclusion or should we flatten it out" +
                     " and do mutex checks at runtime?")
public final class ClientAsyncHttpConfiguration
        implements ToCopyableBuilder<ClientAsyncHttpConfiguration.Builder, ClientAsyncHttpConfiguration> {

    private final SdkAsyncHttpClient httpClient;
    private final SdkAsyncHttpClientFactory httpClientFactory;

    private ClientAsyncHttpConfiguration(DefaultHttpConfigurationBuilder builder) {
        this.httpClient = builder.httpClient;
        this.httpClientFactory = builder.httpClientFactory;
    }

    /**
     * @return The currently configured {@link SdkAsyncHttpClient} or an empty {@link Optional} if not present.
     */
    public Optional<SdkAsyncHttpClient> httpClient() {
        return Optional.ofNullable(httpClient);
    }

    /**
     * @return The currently configured {@link SdkAsyncHttpClientFactory} or an empty {@link Optional} if not present.
     */
    public Optional<SdkAsyncHttpClientFactory> httpClientFactory() {
        return Optional.ofNullable(httpClientFactory);
    }

    @Override
    public ClientAsyncHttpConfiguration.Builder toBuilder() {
        return new DefaultHttpConfigurationBuilder()
                .httpClient(httpClient)
                .httpClientFactory(httpClientFactory);
    }

    /**
     * Transforms this configuration into an {@link Either} of {@link SdkAsyncHttpClient} and {@link SdkAsyncHttpClientFactory}
     * to ease resolution of the client. Returns an empty {@link Optional} if neither is set.
     */
    @SdkInternalApi
    Optional<Either<SdkAsyncHttpClient, SdkAsyncHttpClientFactory>> toEither() {
        return Either.fromNullable(httpClient, httpClientFactory);
    }

    /**
     * @return Builder instance to construct a {@link ClientAsyncHttpConfiguration}.
     */
    public static Builder builder() {
        return new DefaultHttpConfigurationBuilder();
    }

    /**
     * A builder for {@link ClientAsyncHttpConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientAsyncHttpConfiguration> {

        /**
         * Sets the {@link SdkAsyncHttpClient} that the SDK service client will use to make HTTP calls. This HTTP client may be
         * shared  between multiple SDK service clients to share a common connection pool. To create a client you must use an
         * implementation specific builder/factory. Note that this method is only recommended when you
         * wish to share an HTTP client across multiple SDK service clients. If you do not wish to share HTTP clients, it is
         * recommended to use {@link #httpClientFactory(SdkAsyncHttpClientFactory)} so that service specific default
         * configuration  may be applied.
         *
         * <p>
         * <b>This client must be closed by the client when it is ready to be disposed. The SDK will not close the HTTP client
         * when the service client is closed.</b>
         * </p>
         *
         * @return This builder for method chaining.
         */
        // This intentionally returns SdkBuilder so that only httpClient or httpClientFactory may be supplied.
        SdkBuilder<?, ClientAsyncHttpConfiguration> httpClient(SdkAsyncHttpClient sdkHttpClient);

        /**
         * Sets a custom HTTP client factory that will be used to obtain a configured instance of {@link SdkAsyncHttpClient}. Any
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
        SdkBuilder<?, ClientAsyncHttpConfiguration> httpClientFactory(SdkAsyncHttpClientFactory sdkClientFactory);

    }

    /**
     * Builder for a {@link ClientAsyncHttpConfiguration}.
     */
    private static final class DefaultHttpConfigurationBuilder implements Builder {

        private SdkAsyncHttpClient httpClient;
        private SdkAsyncHttpClientFactory httpClientFactory;

        private DefaultHttpConfigurationBuilder() {
        }

        @Override
        public DefaultHttpConfigurationBuilder httpClient(SdkAsyncHttpClient sdkHttpClient) {
            this.httpClient = sdkHttpClient;
            return this;
        }

        public void setHttpClient(SdkAsyncHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        public DefaultHttpConfigurationBuilder httpClientFactory(SdkAsyncHttpClientFactory sdkClientFactory) {
            this.httpClientFactory = sdkClientFactory;
            return this;
        }

        public void setHttpClientFactory(SdkAsyncHttpClientFactory httpClientFactory) {
            this.httpClientFactory = httpClientFactory;
        }

        /**
         * @return An immutable {@link ClientAsyncHttpConfiguration} object.
         */
        public ClientAsyncHttpConfiguration build() {
            return new ClientAsyncHttpConfiguration(this);
        }
    }
}
