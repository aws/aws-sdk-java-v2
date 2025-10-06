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

package software.amazon.awssdk.services.sns.messagemanager;

import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration for the SNS Message Manager.
 * <p>
 * This class allows customization of certificate caching behavior, HTTP client settings,
 * and other validation parameters for the SNS message validation process.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * MessageManagerConfiguration config = MessageManagerConfiguration.builder()
 *     .certificateCacheTimeout(Duration.ofHours(1))
 *     .build();
 * 
 * SnsMessageManager manager = SnsMessageManager.builder()
 *     .configuration(config)
 *     .build();
 * }
 * </pre>
 */
@SdkPublicApi
@Immutable
@ThreadSafe
public final class MessageManagerConfiguration 
        implements ToCopyableBuilder<MessageManagerConfiguration.Builder, MessageManagerConfiguration> {

    private static final Duration DEFAULT_CERTIFICATE_CACHE_TIMEOUT = Duration.ofMinutes(5);

    private final Duration certificateCacheTimeout;
    private final SdkHttpClient httpClient;

    private MessageManagerConfiguration(DefaultBuilder builder) {
        this.certificateCacheTimeout = builder.certificateCacheTimeout != null 
            ? builder.certificateCacheTimeout 
            : DEFAULT_CERTIFICATE_CACHE_TIMEOUT;
        this.httpClient = builder.httpClient;
    }

    /**
     * Creates a new builder for {@link MessageManagerConfiguration}.
     *
     * @return A new builder instance.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Returns the certificate cache timeout duration.
     * <p>
     * This determines how long certificates are cached before being re-fetched from AWS.
     * A longer timeout reduces HTTP requests but may delay detection of certificate changes.
     *
     * @return The certificate cache timeout (never null).
     */
    public Duration certificateCacheTimeout() {
        return certificateCacheTimeout;
    }

    /**
     * Returns the HTTP client to use for certificate retrieval.
     * <p>
     * If not specified, the default SDK HTTP client will be used.
     *
     * @return The HTTP client, or null if the default should be used.
     */
    public SdkHttpClient httpClient() {
        return httpClient;
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MessageManagerConfiguration that = (MessageManagerConfiguration) obj;
        return Objects.equals(certificateCacheTimeout, that.certificateCacheTimeout) &&
               Objects.equals(httpClient, that.httpClient);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(certificateCacheTimeout) * 31 + Objects.hashCode(httpClient);
    }

    @Override
    public String toString() {
        return ToString.builder("MessageManagerConfiguration")
                       .add("certificateCacheTimeout", certificateCacheTimeout)
                       .add("httpClient", httpClient)
                       .build();
    }

    /**
     * Builder for creating {@link MessageManagerConfiguration} instances.
     */
    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, MessageManagerConfiguration> {

        /**
         * Sets the certificate cache timeout duration.
         * <p>
         * This determines how long certificates are cached before being re-fetched from AWS.
         * Must be positive.
         *
         * @param certificateCacheTimeout The cache timeout duration.
         * @return This builder for method chaining.
         * @throws IllegalArgumentException If the timeout is null or not positive.
         */
        Builder certificateCacheTimeout(Duration certificateCacheTimeout);

        /**
         * Sets the HTTP client to use for certificate retrieval.
         * <p>
         * If not specified, the default SDK HTTP client will be used.
         *
         * @param httpClient The HTTP client to use.
         * @return This builder for method chaining.
         */
        Builder httpClient(SdkHttpClient httpClient);
        
        /**
         * Applies a mutation to this builder using the provided consumer.
         * <p>
         * This is a convenience method that allows for fluent configuration using lambda expressions.
         *
         * @param mutator A consumer that applies mutations to this builder.
         * @return This builder for method chaining.
         */
        default Builder applyMutation(java.util.function.Consumer<Builder> mutator) {
            mutator.accept(this);
            return this;
        }
    }

    private static final class DefaultBuilder implements Builder {
        private Duration certificateCacheTimeout;
        private SdkHttpClient httpClient;

        private DefaultBuilder() {
        }

        private DefaultBuilder(MessageManagerConfiguration configuration) {
            this.certificateCacheTimeout = configuration.certificateCacheTimeout;
            this.httpClient = configuration.httpClient;
        }

        @Override
        public Builder certificateCacheTimeout(Duration certificateCacheTimeout) {
            validateCertificateCacheTimeout(certificateCacheTimeout);
            this.certificateCacheTimeout = certificateCacheTimeout;
            return this;
        }

        @Override
        public Builder httpClient(SdkHttpClient httpClient) {
            // HTTP client can be null (will use default), but if provided should be valid
            if (httpClient != null) {
                validateHttpClient(httpClient);
            }
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Validates the certificate cache timeout parameter.
         */
        private void validateCertificateCacheTimeout(Duration certificateCacheTimeout) {
            Validate.paramNotNull(certificateCacheTimeout, "certificateCacheTimeout");
            
            if (certificateCacheTimeout.isNegative() || certificateCacheTimeout.isZero()) {
                throw new IllegalArgumentException(
                    "Certificate cache timeout must be positive. Received: " + certificateCacheTimeout + 
                    ". Recommended values are between 1 minute and 24 hours.");
            }
            
            // Warn about potentially problematic values
            long seconds = certificateCacheTimeout.getSeconds();
            if (seconds < 30) {
                // Very short cache timeout - might cause excessive HTTP requests
                // Note: In a real implementation, this might use a logger instead of throwing
                throw new IllegalArgumentException(
                    "Certificate cache timeout is very short (" + certificateCacheTimeout + 
                    "). This may cause excessive HTTP requests to certificate servers. " +
                    "Consider using a timeout of at least 30 seconds.");
            }
            
            long days = seconds / (24 * 60 * 60); // Convert seconds to days
            if (days > 7) {
                // Very long cache timeout - might delay certificate updates
                throw new IllegalArgumentException(
                    "Certificate cache timeout is very long (" + certificateCacheTimeout + 
                    "). This may delay detection of certificate changes or revocations. " +
                    "Consider using a timeout of 7 days or less.");
            }
        }

        /**
         * Validates the HTTP client parameter.
         */
        private void validateHttpClient(SdkHttpClient httpClient) {
            // Basic validation - ensure the client is not in a closed state
            // Note: There's no standard way to check if an SdkHttpClient is closed,
            // so we do basic validation here
            try {
                // The client should be able to provide basic information
                // This is a minimal check - in practice, the client will be validated
                // when actually used for HTTP requests
                if (httpClient.toString() == null) {
                    throw new IllegalArgumentException("HTTP client appears to be invalid or corrupted");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "HTTP client validation failed: " + e.getMessage() + 
                    ". Please ensure the HTTP client is properly configured and not closed.", e);
            }
        }

        @Override
        public MessageManagerConfiguration build() {
            return new MessageManagerConfiguration(this);
        }
    }
}