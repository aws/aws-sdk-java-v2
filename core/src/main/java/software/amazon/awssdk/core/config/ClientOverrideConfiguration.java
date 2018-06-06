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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.config.options.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.ToString;
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
    private final Map<String, List<String>> additionalHttpHeaders;
    private final Boolean gzipEnabled;
    private final RetryPolicy retryPolicy;
    private final List<ExecutionInterceptor> executionInterceptors;
    private final AttributeMap advancedOptions;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientOverrideConfiguration(DefaultClientOverrideConfigurationBuilder builder) {
        this.additionalHttpHeaders = CollectionUtils.deepUnmodifiableMap(builder.additionalHttpHeaders);
        this.gzipEnabled = builder.gzipEnabled;
        this.retryPolicy = builder.retryPolicy;
        this.executionInterceptors = Collections.unmodifiableList(new ArrayList<>(builder.executionInterceptors));
        this.advancedOptions = builder.advancedOptions.build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultClientOverrideConfigurationBuilder().advancedOptions(advancedOptions.toBuilder())
                                                              .additionalHttpHeaders(additionalHttpHeaders)
                                                              .gzipEnabled(gzipEnabled)
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
     * An unmodifiable representation of the set of HTTP headers that should be sent with every request. If not set, this will
     * return an empty map.
     *
     * @see Builder#additionalHttpHeaders(Map)
     */
    public Map<String, List<String>> additionalHttpHeaders() {
        return additionalHttpHeaders;
    }

    /**
     * Whether GZIP should be used when communication with AWS.
     *
     * @see Builder#gzipEnabled(Boolean)
     */
    public Boolean gzipEnabled() {
        return gzipEnabled;
    }

    /**
     * The retry policy that should be used when handling failure cases.
     *
     * @see Builder#retryPolicy(RetryPolicy)
     */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    /**
     * Load the requested advanced option that was configured on the client builder. This will return null if the value was not
     * configured.
     *
     * @see Builder#advancedOption(SdkAdvancedClientOption, Object)
     */
    public <T> T advancedOption(SdkAdvancedClientOption<T> option) {
        return advancedOptions.get(option);
    }

    /**
     * An immutable collection of {@link ExecutionInterceptor}s that should be hooked into the execution of each request, in the
     * order that they should be applied.
     *
     * @see Builder#executionInterceptors(List)
     */
    public List<ExecutionInterceptor> executionInterceptors() {
        return executionInterceptors;
    }

    @Override
    public String toString() {
        return ToString.builder("ClientOverrideConfiguration")
                       .add("additionalHttpHeaders", additionalHttpHeaders)
                       .add("gzipEnabled", gzipEnabled)
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
         * Define a set of headers that should be added to every HTTP request sent to AWS. This will override any headers
         * previously added to the builder.
         *
         * @see ClientOverrideConfiguration#additionalHttpHeaders()
         */
        Builder additionalHttpHeaders(Map<String, List<String>> additionalHttpHeaders);

        /**
         * Add a header that should be sent with every HTTP request to AWS. This will always add a new header to the request,
         * even if that particular header had already been defined.
         *
         * <p>For example, the following code will result in two different "X-Header" values sent to AWS.</p>
         * <pre>
         * httpConfiguration.addAdditionalHttpHeader("X-Header", "Value1");
         * httpConfiguration.addAdditionalHttpHeader("X-Header", "Value2");
         * </pre>
         *
         * @see ClientOverrideConfiguration#additionalHttpHeaders()
         */
        Builder addAdditionalHttpHeader(String header, String... values);

        /**
         * Configure whether GZIP should be used when communicating with AWS. Enabling GZIP increases CPU utilization and memory
         * usage, while decreasing the amount of data sent over the network.
         *
         * @see ClientOverrideConfiguration#gzipEnabled()
         */
        Builder gzipEnabled(Boolean gzipEnabled);

        /**
         * Configure the retry policy that should be used when handling failure cases.
         *
         * @see ClientOverrideConfiguration#retryPolicy()
         */
        Builder retryPolicy(RetryPolicy retryPolicy);

        /**
         * Configure the retry policy the should be used when handling failure cases.
         */
        default Builder retryPolicy(Consumer<RetryPolicy.Builder> retryPolicy) {
            return retryPolicy(RetryPolicy.builder().apply(retryPolicy).build());
        }

        /**
         * Configure a list of execution interceptors that will have access to read and modify the request and response objcets as
         * they are processed by the SDK. These will replace any interceptors configured previously with this method or
         * {@link #addExecutionInterceptor(ExecutionInterceptor)}.
         *
         * The provided interceptors are executed in the order they are configured and are always later in the order than the ones
         * automatically added by the SDK. See {@link ExecutionInterceptor} for a more detailed explanation of interceptor order.
         *
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
         * Interceptors added using this method are executed in the order they are configured and are always later in the order
         * than the ones automatically added by the SDK. See {@link ExecutionInterceptor} for a more detailed explanation of
         * interceptor order.
         *
         * <b><i>This is currently an INTERNAL api, which means it is subject to change and should not be used.</i></b>
         *
         * @see ClientOverrideConfiguration#executionInterceptors()
         */
        @SdkInternalApi
        Builder addExecutionInterceptor(ExecutionInterceptor executionInterceptor);

        /**
         * Configure an advanced override option. These values are used very rarely, and the majority of SDK customers can ignore
         * them.
         *
         * @param option The option to configure.
         * @param value The value of the option.
         * @param <T> The type of the option.
         */
        <T> Builder advancedOption(SdkAdvancedClientOption<T> option, T value);

        /**
         * Configure the map of advanced override options. This will override all values currently configured. The values in the
         * map must match the key type of the map, or a runtime exception will be raised.
         */
        Builder advancedOptions(Map<SdkAdvancedClientOption<?>, ?> advancedOptions);
    }

    /**
     * An SDK-internal implementation of {@link ClientOverrideConfiguration.Builder}.
     */
    private static final class DefaultClientOverrideConfigurationBuilder implements Builder {
        private Map<String, List<String>> additionalHttpHeaders = new HashMap<>();
        private Boolean gzipEnabled;
        private RetryPolicy retryPolicy;
        private List<ExecutionInterceptor> executionInterceptors = new ArrayList<>();
        private AttributeMap.Builder advancedOptions = AttributeMap.builder();

        @Override
        public Builder additionalHttpHeaders(Map<String, List<String>> additionalHttpHeaders) {
            this.additionalHttpHeaders = CollectionUtils.deepCopyMap(additionalHttpHeaders);
            return this;
        }

        @Override
        public Builder addAdditionalHttpHeader(String header, String... values) {
            List<String> currentHeaderValues = this.additionalHttpHeaders.computeIfAbsent(header, k -> new ArrayList<>());
            Collections.addAll(currentHeaderValues, values);
            return this;
        }

        public void setAdditionalHttpHeaders(Map<String, List<String>> additionalHttpHeaders) {
            additionalHttpHeaders(additionalHttpHeaders);
        }

        @Override
        public Builder gzipEnabled(Boolean gzipEnabled) {
            this.gzipEnabled = gzipEnabled;
            return this;
        }

        public void setGzipEnabled(Boolean gzipEnabled) {
            gzipEnabled(gzipEnabled);
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
        public Builder executionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            this.executionInterceptors.clear();
            this.executionInterceptors.addAll(executionInterceptors);
            return this;
        }

        @Override
        public Builder addExecutionInterceptor(ExecutionInterceptor executionInterceptors) {
            this.executionInterceptors.add(executionInterceptors);
            return this;
        }

        public void setExecutionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            executionInterceptors(executionInterceptors);
        }

        @Override
        public <T> Builder advancedOption(SdkAdvancedClientOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        @Override
        public Builder advancedOptions(Map<SdkAdvancedClientOption<?>, ?> advancedOptions) {
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
        public ClientOverrideConfiguration build() {
            return new ClientOverrideConfiguration(this);
        }
    }
}
