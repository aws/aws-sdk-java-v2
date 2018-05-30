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

package software.amazon.awssdk.core.config;

import java.util.Map;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.config.options.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Async configuration values for which the client already provides sensible defaults. All values are optional, and not specifying
 * them will use optimal values defined by the service itself.
 *
 * <p>Use {@link #builder()} to create a set of options.</p>
 */
@Immutable
@SdkPublicApi
public final class ClientAsyncConfiguration
        implements ToCopyableBuilder<ClientAsyncConfiguration.Builder, ClientAsyncConfiguration> {
    private final AttributeMap advancedOptions;

    private ClientAsyncConfiguration(DefaultBuilder builder) {
        this.advancedOptions = builder.advancedOptions.build();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder().advancedOptions(advancedOptions);
    }

    /**
     * Load the requested advanced option that was configured on the client builder. This will return null if the value was not
     * configured.
     *
     * @see Builder#advancedOption(SdkAdvancedAsyncClientOption, Object)
     */
    public <T> T advancedOption(SdkAdvancedAsyncClientOption<T> option) {
        return advancedOptions.get(option);
    }

    /**
     * Configure and create a {@link ClientAsyncConfiguration}. Created via {@link ClientAsyncConfiguration#builder()}.
     */
    public interface Builder extends CopyableBuilder<Builder, ClientAsyncConfiguration> {
        /**
         * Configure an advanced async option. These values are used very rarely, and the majority of SDK customers can ignore
         * them.
         *
         * @param option The option to configure.
         * @param value The value of the option.
         * @param <T> The type of the option.
         */
        <T> Builder advancedOption(SdkAdvancedAsyncClientOption<T> option, T value);

        /**
         * Configure the map of advanced override options. This will override all values currently configured. The values in the
         * map must match the key type of the map, or a runtime exception will be raised.
         */
        Builder advancedOptions(Map<SdkAdvancedAsyncClientOption<?>, ?> advancedOptions);
    }

    private static class DefaultBuilder implements Builder {
        private AttributeMap.Builder advancedOptions = AttributeMap.builder();

        @Override
        public <T> Builder advancedOption(SdkAdvancedAsyncClientOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        @Override
        public Builder advancedOptions(Map<SdkAdvancedAsyncClientOption<?>, ?> advancedOptions) {
            this.advancedOptions.putAll(advancedOptions);
            return this;
        }

        private Builder advancedOptions(AttributeMap.Builder attributeMap) {
            this.advancedOptions = attributeMap;
            return this;
        }

        public void setAdvancedOptions(Map<SdkAdvancedAsyncClientOption<?>, Object> advancedOptions) {
            advancedOptions(advancedOptions);
        }

        @Override
        public ClientAsyncConfiguration build() {
            return new ClientAsyncConfiguration(this);
        }

        Builder advancedOptions(AttributeMap advancedOptions) {
            this.advancedOptions = advancedOptions.toBuilder();
            return this;
        }
    }
}
