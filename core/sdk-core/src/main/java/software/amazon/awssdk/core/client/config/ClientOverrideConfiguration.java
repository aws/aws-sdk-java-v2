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

package software.amazon.awssdk.core.client.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration values for which the client already provides sensible defaults. All values are optional, and not specifying them
 * will use optimal values defined by the service itself.
 *
 * <p>Use {@link #builder()} to create a set of options.</p>
 */
@SdkPublicApi
public final class ClientOverrideConfiguration
    implements ToCopyableBuilder<ClientOverrideConfiguration.Builder, ClientOverrideConfiguration> {
    private final Map<String, List<String>> headers;
    private final RetryPolicy retryPolicy;
    private final List<ExecutionInterceptor> executionInterceptors;
    private final AttributeMap advancedOptions;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientOverrideConfiguration(Builder builder) {
        this.headers = CollectionUtils.deepUnmodifiableMap(builder.headers(), () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.retryPolicy = builder.retryPolicy();
        this.executionInterceptors = Collections.unmodifiableList(new ArrayList<>(builder.executionInterceptors()));
        this.advancedOptions = builder.advancedOptions();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultClientOverrideConfigurationBuilder().advancedOptions(advancedOptions.toBuilder())
                                                              .headers(headers)
                                                              .retryPolicy(retryPolicy)
                                                              .executionInterceptors(executionInterceptors);
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientOverrideConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientOverrideConfigurationBuilder();
    }

    /**
     * An unmodifiable representation of the set of HTTP headers that should be sent with every request.
     *
     * <p>
     * If not set, this will return an empty map.
     *
     * @see Builder#headers(Map)
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * The optional retry policy that should be used when handling failure cases.
     *
     * @see Builder#retryPolicy(RetryPolicy)
     */
    public Optional<RetryPolicy> retryPolicy() {
        return Optional.ofNullable(retryPolicy);
    }

    /**
     * Load the optional requested advanced option that was configured on the client builder.
     *
     * @see Builder#putAdvancedOption(SdkAdvancedClientOption, Object)
     */
    public <T> Optional<T> advancedOption(SdkAdvancedClientOption<T> option) {
        return Optional.ofNullable(advancedOptions.get(option));
    }

    /**
     * An immutable collection of {@link ExecutionInterceptor}s that should be hooked into the execution of each request, in the
     * order that they should be applied.
     *
     */
    public List<ExecutionInterceptor> executionInterceptors() {
        return executionInterceptors;
    }

    @Override
    public String toString() {
        return ToString.builder("ClientOverrideConfiguration")
                       .add("headers", headers)
                       .add("retryPolicy", retryPolicy)
                       .add("executionInterceptors", executionInterceptors)
                       .add("advancedOptions", advancedOptions)
                       .build();
    }

    /**
     * A builder for {@link ClientOverrideConfiguration}.
     *
     * <p>All implementations of this interface are mutable and not thread safe.</p>
     */
    public interface Builder extends CopyableBuilder<Builder, ClientOverrideConfiguration> {

        /**
         * Add a single header to be set on the HTTP request.
         *
         * <p>
         * This overrides any values already configured with this header name in the builder.
         *
         * @param name The name of the header.
         * @param value The value of the header.
         * @return This object for method chaining.
         */
        default Builder putHeader(String name, String value) {
            putHeader(name, Collections.singletonList(value));
            return this;
        }

        /**
         * Add a single header with multiple values to be set on the HTTP request.
         *
         * <p>
         * This overrides any values already configured with this header name in the builder.
         *
         * @param name The name of the header.
         * @param values The values of the header.
         * @return This object for method chaining.
         */
        Builder putHeader(String name, List<String> values);

        /**
         * Configure headers to be set on the HTTP request.
         *
         * <p>
         * This overrides any values currently configured in the builder.
         *
         * @param headers The set of additional headers.
         * @return This object for method chaining.
         */
        Builder headers(Map<String, List<String>> headers);

        Map<String, List<String>> headers();

        /**
         * Configure the retry policy that should be used when handling failure cases.
         *
         * @see ClientOverrideConfiguration#retryPolicy()
         */
        Builder retryPolicy(RetryPolicy retryPolicy);

        RetryPolicy retryPolicy();

        /**
         * Configure the retry policy the should be used when handling failure cases.
         */
        default Builder retryPolicy(Consumer<RetryPolicy.Builder> retryPolicy) {
            return retryPolicy(RetryPolicy.builder().applyMutation(retryPolicy).build());
        }

        /**
         * Configure a list of execution interceptors that will have access to read and modify the request and response objcets as
         * they are processed by the SDK. These will replace any interceptors configured previously with this method or
         * {@link #addExecutionInterceptor(ExecutionInterceptor)}.
         *
         * <p>
         * The provided interceptors are executed in the order they are configured and are always later in the order than the ones
         * automatically added by the SDK. See {@link ExecutionInterceptor} for a more detailed explanation of interceptor order.
         *
         * <p>
         * This overrides any values currently configured in the builder.
         *
         * <p>
         * <b><i>This is currently an INTERNAL api, which means it is subject to change and should not be used.</i></b>
         *
         * @see ClientOverrideConfiguration#executionInterceptors()
         */
        @SdkInternalApi
        Builder executionInterceptors(List<ExecutionInterceptor> executionInterceptors);

        /**
         * Add an execution interceptor that will have access to read and modify the request and response objects as they are
         * processed by the SDK.
         *
         * <p>
         * Interceptors added using this method are executed in the order they are configured and are always later in the order
         * than the ones automatically added by the SDK. See {@link ExecutionInterceptor} for a more detailed explanation of
         * interceptor order.
         *
         * <p>
         * <b><i>This is currently an INTERNAL api, which means it is subject to change and should not be used.</i></b>
         *
         * @see ClientOverrideConfiguration#executionInterceptors()
         */
        @SdkInternalApi
        Builder addExecutionInterceptor(ExecutionInterceptor executionInterceptor);

        List<ExecutionInterceptor> executionInterceptors();

        /**
         * Configure an advanced override option. These values are used very rarely, and the majority of SDK customers can ignore
         * them.
         *
         * @param option The option to configure.
         * @param value The value of the option.
         * @param <T> The type of the option.
         */
        <T> Builder putAdvancedOption(SdkAdvancedClientOption<T> option, T value);

        /**
         * Configure the map of advanced override options. This will override all values currently configured. The values in the
         * map must match the key type of the map, or a runtime exception will be raised.
         */
        Builder advancedOptions(Map<SdkAdvancedClientOption<?>, ?> advancedOptions);

        AttributeMap advancedOptions();
    }

    /**
     * An SDK-internal implementation of {@link ClientOverrideConfiguration.Builder}.
     */
    private static final class DefaultClientOverrideConfigurationBuilder implements Builder {
        private Map<String, List<String>> headers = new HashMap<>();
        private RetryPolicy retryPolicy;
        private List<ExecutionInterceptor> executionInterceptors = new ArrayList<>();
        private AttributeMap.Builder advancedOptions = AttributeMap.builder();

        @Override
        public Builder headers(Map<String, List<String>> headers) {
            Validate.paramNotNull(headers, "headers");
            this.headers = CollectionUtils.deepCopyMap(headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            return this;
        }

        public void setHeaders(Map<String, List<String>> additionalHttpHeaders) {
            headers(additionalHttpHeaders);
        }

        @Override
        public Map<String, List<String>> headers() {
            return CollectionUtils.deepUnmodifiableMap(headers);
        }

        @Override
        public Builder putHeader(String header, List<String> values) {
            Validate.paramNotNull(header, "header");
            Validate.paramNotNull(values, "values");
            headers.put(header, new ArrayList<>(values));
            return this;
        }

        @Override
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public void setRetryPolicy(RetryPolicy retryPolicy) {
            retryPolicy(retryPolicy);
        }

        @Override
        public RetryPolicy retryPolicy() {
            return retryPolicy;
        }

        @Override
        public Builder executionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            Validate.paramNotNull(executionInterceptors, "executionInterceptors");
            this.executionInterceptors = new ArrayList<>(executionInterceptors);
            return this;
        }

        @Override
        public Builder addExecutionInterceptor(ExecutionInterceptor executionInterceptor) {
            this.executionInterceptors.add(executionInterceptor);
            return this;
        }

        public void setExecutionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            executionInterceptors(executionInterceptors);
        }

        @Override
        public List<ExecutionInterceptor> executionInterceptors() {
            return Collections.unmodifiableList(executionInterceptors);
        }

        @Override
        public <T> Builder putAdvancedOption(SdkAdvancedClientOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        @Override
        public Builder advancedOptions(Map<SdkAdvancedClientOption<?>, ?> advancedOptions) {
            this.advancedOptions = AttributeMap.builder();
            this.advancedOptions.putAll(advancedOptions);
            return this;
        }

        private Builder advancedOptions(AttributeMap.Builder attributeMap) {
            this.advancedOptions = attributeMap;
            return this;
        }

        public void setAdvancedOptions(Map<SdkAdvancedClientOption<?>, Object> advancedOptions) {
            advancedOptions(advancedOptions);
        }

        @Override
        public AttributeMap advancedOptions() {
            return advancedOptions.build();
        }

        @Override
        public ClientOverrideConfiguration build() {
            return new ClientOverrideConfiguration(this);
        }
    }
}
