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

package software.amazon.awssdk.core.client.config;

import static software.amazon.awssdk.core.client.config.SdkClientOption.SIGNER_OVERRIDDEN;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ToString;
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
@SdkProtectedApi
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
     * Create a {@link SdkClientConfiguration} from the provided {@link ClientOverrideConfiguration}. This copies the
     * properties out of the configuration and ensures that _OVERRIDDEN properties are properly set, like
     * {@link SdkClientOption#SIGNER_OVERRIDDEN}.
     */
    public static SdkClientConfiguration fromOverrideConfiguration(ClientOverrideConfiguration configuration) {
        SdkClientConfiguration result = configuration.asSdkClientConfiguration();

        Signer signerFromOverride = result.option(SdkAdvancedClientOption.SIGNER);

        if (signerFromOverride == null) {
            return result;
        }

        return result.toBuilder()
                     .option(SIGNER_OVERRIDDEN, true)
                     .build();
    }

    /**
     * Retrieve the value of a specific option.
     */
    public <T> T option(ClientOption<T> option) {
        return attributes.get(option);
    }

    /**
     * Create a {@link ClientOverrideConfiguration} using the values currently in this configuration.
     */
    public ClientOverrideConfiguration asOverrideConfiguration() {
        return new ClientOverrideConfiguration.DefaultBuilder(toBuilder()).build();
    }

    /**
     * Merge this configuration with another configuration, where this configuration's values take precedence.
     */
    public SdkClientConfiguration merge(SdkClientConfiguration configuration) {
        return new SdkClientConfiguration(attributes.merge(configuration.attributes));
    }

    public SdkClientConfiguration merge(Consumer<SdkClientConfiguration.Builder> configuration) {
        return merge(builder().applyMutation(configuration).build());
    }

    @Override
    public String toString() {
        return ToString.builder("SdkClientConfiguration")
                       .add("attributes", attributes)
                       .build();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SdkClientConfiguration that = (SdkClientConfiguration) o;

        return attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return attributes.hashCode();
    }

    public static final class Builder implements CopyableBuilder<Builder, SdkClientConfiguration> {
        private final AttributeMap.Builder attributes;

        private Builder(AttributeMap.Builder attributes) {
            this.attributes = attributes;
        }

        /**
         * Create a {@link ClientOverrideConfiguration.Builder} using the values currently in this builder.
         */
        public ClientOverrideConfiguration.Builder asOverrideConfigurationBuilder() {
            return new ClientOverrideConfiguration.DefaultBuilder(this);
        }

        /**
         * Configure the value of a specific option.
         */
        public <T> Builder option(ClientOption<T> option, T value) {
            this.attributes.put(option, value);
            return this;
        }

        /**
         * Add a mapping between the provided option and value provider.
         *
         * The lazy value will only be resolved when the value is needed. During resolution, the lazy value is provided with a
         * value reader. The value reader will fail if the reader attempts to read its own value (directly, or indirectly
         * through other lazy values).
         *
         * If a value is updated that a lazy value is depended on, the lazy value will be re-resolved the next time the lazy
         * value is accessed.
         */
        public <T> Builder lazyOption(ClientOption<T> option, AttributeMap.LazyValue<T> lazyValue) {
            this.attributes.putLazy(option, lazyValue);
            return this;
        }

        /**
         * Equivalent to {@link #lazyOption(ClientOption, AttributeMap.LazyValue)}, but does not assign the value if there is
         * already a non-null value assigned for the provided option.
         */
        public <T> Builder lazyOptionIfAbsent(ClientOption<T> option, AttributeMap.LazyValue<T> lazyValue) {
            this.attributes.putLazyIfAbsent(option, lazyValue);
            return this;
        }

        /**
         * Retrieve the value of a specific option.
         */
        public <T> T option(ClientOption<T> option) {
            return this.attributes.get(option);
        }

        /**
         * Add a mapping between the provided key and value, if the current value for the option is null. Returns the value.
         */
        public <T> T computeOptionIfAbsent(ClientOption<T> option, Supplier<T> valueSupplier) {
            return this.attributes.computeIfAbsent(option, valueSupplier);
        }

        /**
         * Adds all the options from the map provided. This is not type safe, and will throw an exception during creation if
         * a value in the map is not of the correct type for its option.
         */
        public Builder putAll(Map<? extends ClientOption<?>, ?> options) {
            this.attributes.putAll(options);
            return this;
        }

        /**
         * Put all of the attributes from the provided override configuration into this one.
         */
        public Builder putAll(ClientOverrideConfiguration configuration) {
            this.attributes.putAll(fromOverrideConfiguration(configuration).attributes);
            return this;
        }

        @Override
        public Builder copy() {
            return new Builder(attributes.copy());
        }

        @Override
        public SdkClientConfiguration build() {
            return new SdkClientConfiguration(attributes.build());
        }
    }
}
