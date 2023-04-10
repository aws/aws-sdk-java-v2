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

/**
 * Class to expose SDK service client settings to the user, e.g., ClientOverrideConfiguration
 */
@SdkPublicApi
public abstract class SdkServiceClientConfiguration {

    private final ClientOverrideConfiguration overrideConfiguration;
    private final URI endpointOverride;

    protected SdkServiceClientConfiguration(Builder builder) {
        this.overrideConfiguration = builder.overrideConfiguration();
        this.endpointOverride = builder.endpointOverride();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SdkServiceClientConfiguration serviceClientConfiguration = (SdkServiceClientConfiguration) o;
        return Objects.equals(overrideConfiguration, serviceClientConfiguration.overrideConfiguration());
    }

    @Override
    public int hashCode() {
        return overrideConfiguration != null ? overrideConfiguration.hashCode() : 0;
    }

    /**
     * The base interface for all SDK service client configuration builders
     */
    public interface Builder {
        /**
         * Return the client override configuration
         */
        ClientOverrideConfiguration overrideConfiguration();

        /**
         * Return the endpoint override
         */
        URI endpointOverride();

        /**
         * Configure the client override configuration
         */
        Builder overrideConfiguration(ClientOverrideConfiguration clientOverrideConfiguration);

        /**
         * Configure the endpoint override
         */
        Builder endpointOverride(URI endpointOverride);

        /**
         * Build the service client configuration using the configuration on this builder
         */
        SdkServiceClientConfiguration build();
    }
}
