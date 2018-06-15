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

package software.amazon.awssdk.core.internal.client.config;

import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A collection of configuration that is required by an AWS client in order to operate.
 *
 * Configuration can be set via {@link SdkClientConfiguration.Builder#option(ClientOption, Object)} and checked via
 * {@link SdkClientConfiguration#option(ClientOption)}.
 *
 * This configuration can be merged with other configuration using {@link SdkClientConfiguration#merge}.
 *
 * This configuration object can be {@link #close()}d to release all closeable resources configured within it.
 */
@SdkInternalApi
public final class SdkClientConfiguration
        implements ToCopyableBuilder<SdkClientConfiguration.Builder, SdkClientConfiguration>, SdkAutoCloseable {
    private final AttributeMap attributes;

    private SdkClientConfiguration(AttributeMap attributes) {
        this.attributes = attributes;
    }

    /**
     * Create a builder for a {@link SdkClientConfiguration}.
     */
    public static SdkClientConfiguration.Builder builder() {
        return new Builder(AttributeMap.builder());
    }

    /**
     * Retrieve the value of a specific option.
     */
    public <T> T option(ClientOption<T> option) {
        return attributes.get(option);
    }

    /**
     * Merge this configuration with another configuration, where this configuration's values take precedence.
     */
    public SdkClientConfiguration merge(SdkClientConfiguration configuration) {
        return new SdkClientConfiguration(attributes.merge(configuration.attributes));
    }

    public SdkClientConfiguration merge(Consumer<SdkClientConfiguration.Builder> configuration) {
        return merge(SdkClientConfiguration.builder().apply(configuration).build());
    }

    @Override
    public Builder toBuilder() {
        return new Builder(attributes.toBuilder());
    }

    /**
     * Close this configuration, which closes all closeable attributes.
     */
    @Override
    public void close() {
        attributes.close();
    }

    public static final class Builder implements CopyableBuilder<Builder, SdkClientConfiguration> {
        private final AttributeMap.Builder attributes;

        private Builder(AttributeMap.Builder attributes) {
            this.attributes = attributes;
        }

        /**
         * Configure the value of a specific option.
         */
        public <T> Builder option(ClientOption<T> option, T value) {
            this.attributes.put(option, value);
            return this;
        }

        @Override
        public SdkClientConfiguration build() {
            return new SdkClientConfiguration(attributes.build());
        }
    }
}
