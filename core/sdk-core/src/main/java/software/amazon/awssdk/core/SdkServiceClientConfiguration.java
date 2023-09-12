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

package software.amazon.awssdk.core;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.endpoints.EndpointProvider;

/**
 * Class to expose SDK service client settings to the user, e.g., ClientOverrideConfiguration
 */
@SdkPublicApi
public abstract class SdkServiceClientConfiguration {

    private final ClientOverrideConfiguration overrideConfiguration;
    private final URI endpointOverride;

    private final EndpointProvider endpointProvider;

    protected SdkServiceClientConfiguration(Builder builder) {
        this.overrideConfiguration = builder.overrideConfiguration();
        this.endpointOverride = builder.endpointOverride();
        this.endpointProvider = builder.endpointProvider();
    }

    /**
     *
     * @return The ClientOverrideConfiguration of the SdkClient. If this is not set, an ClientOverrideConfiguration object will
     * still be returned, with empty fields
     */
    public ClientOverrideConfiguration overrideConfiguration() {
        return this.overrideConfiguration;
    }

    /**
     *
     * @return The configured endpoint override of the SdkClient. If the endpoint was not overridden, an empty Optional will be
     * returned
     */
    public Optional<URI> endpointOverride() {
        return Optional.ofNullable(this.endpointOverride);
    }

    /**
     *
     * @return The configured endpoint provider of the SdkClient. If the endpoint provider was not configured, the default
     * endpoint provider will be returned.
     */
    public Optional<EndpointProvider> endpointProvider() {
        return Optional.ofNullable(this.endpointProvider);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SdkServiceClientConfiguration serviceClientConfiguration = (SdkServiceClientConfiguration) o;
        return Objects.equals(overrideConfiguration, serviceClientConfiguration.overrideConfiguration())
               && Objects.equals(endpointOverride, serviceClientConfiguration.endpointOverride().orElse(null))
               && Objects.equals(endpointProvider, serviceClientConfiguration.endpointProvider().orElse(null));
    }

    @Override
    public int hashCode() {
        int result = overrideConfiguration != null ? overrideConfiguration.hashCode() : 0;
        result = 31 * result + (endpointOverride != null ? endpointOverride.hashCode() : 0);
        result = 31 * result + (endpointProvider != null ? endpointProvider.hashCode() : 0);
        return result;
    }

    /**
     * The base interface for all SDK service client configuration builders
     */
    public interface Builder {
        /**
         * Return the client override configuration
         */
        default ClientOverrideConfiguration overrideConfiguration() {
            throw new UnsupportedOperationException();
        }

        /**
         * Return the endpoint override
         */
        default URI endpointOverride() {
            throw new UnsupportedOperationException();
        }

        default EndpointProvider endpointProvider() {
            throw new UnsupportedOperationException();
        }

        /**
         * Configure the client override configuration
         */
        default Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration) {
            throw new UnsupportedOperationException();
        }

        /**
         * Configure the endpoint override
         */
        default Builder endpointOverride(URI endpointOverride) {
            throw new UnsupportedOperationException();
        }


        default Builder endpointProvider(EndpointProvider endpointProvider) {
            throw new UnsupportedOperationException();
        }

        /**
         * Build the service client configuration using the configuration on this builder
         */
        SdkServiceClientConfiguration build();
    }
}
