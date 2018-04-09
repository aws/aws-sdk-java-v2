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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
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
public class ClientOverrideConfiguration
        implements ToCopyableBuilder<ClientOverrideConfiguration.Builder, ClientOverrideConfiguration> {
    private final Duration httpRequestTimeout;
    private final Duration totalExecutionTimeout;
    private final Map<String, List<String>> additionalHttpHeaders;
    private final Boolean gzipEnabled;
    private final RetryPolicy retryPolicy;
    private final List<ExecutionInterceptor> lastExecutionInterceptors;
    private final AttributeMap advancedOptions;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientOverrideConfiguration(DefaultClientOverrideConfigurationBuilder builder) {
        this.httpRequestTimeout = builder.httpRequestTimeout;
        this.totalExecutionTimeout = builder.totalExecutionTimeout;
        this.additionalHttpHeaders = CollectionUtils.deepUnmodifiableMap(builder.additionalHttpHeaders);
        this.gzipEnabled = builder.gzipEnabled;
        this.retryPolicy = builder.retryPolicy;
        this.lastExecutionInterceptors = Collections.unmodifiableList(new ArrayList<>(builder.lastExecutionInterceptors));
        this.advancedOptions = builder.advancedOptions.build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultClientOverrideConfigurationBuilder().advancedOptions(advancedOptions.toBuilder())
                                                              .httpRequestTimeout(httpRequestTimeout)
                                                              .totalExecutionTimeout(totalExecutionTimeout)
                                                              .additionalHttpHeaders(additionalHttpHeaders)
                                                              .gzipEnabled(gzipEnabled)
                                                              .retryPolicy(retryPolicy)
                                                              .lastExecutionInterceptors(lastExecutionInterceptors);
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientOverrideConfiguration}.
     */
    public static Builder builder() {
        return new DefaultClientOverrideConfigurationBuilder();
    }

    /**
     * The amount of time to wait for the request to complete before giving up and timing out. An empty value disables this
     * feature.
     *
     * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout when
     * reading the response. For APIs that return large responses this could be expensive.</p>
     *
     * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
     * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that don't
     * get aborted until several seconds after the timer has been breached. Because of this, the request timeout feature should
     * not be used when absolute precision is needed.</p>
     *
     * @see Builder#httpRequestTimeout(Duration)
     */
    @ReviewBeforeRelease("This doesn't currently work.")
    public Duration httpRequestTimeout() {
        return httpRequestTimeout;
    }

    /**
     * The amount of time to allow the client to complete the execution of an API call. This timeout covers the entire client
     * execution except for marshalling. This includes request handler execution, all HTTP requests including retries,
     * unmarshalling, etc. An empty value disables this feature.
     *
     * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout when
     * reading the response. For APIs that return large responses this could be expensive.</p>
     *
     * <p>The client execution timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
     * timeout
     * is breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
     * don't get aborted until several seconds after the timer has been breached. Because of this, the client execution timeout
     * feature should not be used when absolute precision is needed.</p>
     *
     * <p>This may be used together with {@link #httpRequestTimeout()} to enforce both a timeout on each individual HTTP request
     * (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'client execution' time). A
     * non-positive value disables this feature.</p>
     *
     * @see Builder#totalExecutionTimeout(Duration)
     */
    public Duration totalExecutionTimeout() {
        return totalExecutionTimeout;
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
     * @see Builder#advancedOption(AdvancedClientOption, Object)
     */
    public <T> T advancedOption(AdvancedClientOption<T> option) {
        return advancedOptions.get(option);
    }

    /**
     * An immutable collection of {@link ExecutionInterceptor}s that should be hooked into the execution of each request, in the
     * order that they should be applied.
     *
     * @see Builder#lastExecutionInterceptors(List)
     */
    public List<ExecutionInterceptor> lastExecutionInterceptors() {
        return lastExecutionInterceptors;
    }

    @Override
    public String toString() {
        return ToString.builder("ClientOverrideConfiguration")
                       .add("httpRequestTimeout", httpRequestTimeout)
                       .add("totalExecutionTimeout", totalExecutionTimeout)
                       .add("additionalHttpHeaders", additionalHttpHeaders)
                       .add("gzipEnabled", gzipEnabled)
                       .add("retryPolicy", retryPolicy)
                       .add("lastExecutionInterceptors", lastExecutionInterceptors)
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
         * Configure the amount of time to wait for the request to complete before giving up and timing out. A non-positive value
         * disables this feature.
         *
         * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout
         * when reading the response. For APIs that return large responses this could be expensive.</p>
         *
         * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
         * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
         * don't get aborted until several seconds after the timer has been breached. Because of this, the request timeout
         * feature
         * should not be used when absolute precision is needed.</p>
         *
         * @see ClientOverrideConfiguration#httpRequestTimeout()
         */
        Builder httpRequestTimeout(Duration httpRequestTimeout);

        /**
         * Configure the amount of time to allow the client to complete the execution of an API call. This timeout covers the
         * entire client execution except for marshalling. This includes request handler execution, all HTTP request including
         * retries, unmarshalling, etc.
         *
         * <p>This feature requires buffering the entire response (for non-streaming APIs) into memory to enforce a hard timeout
         * when reading the response. For APIs that return large responses this could be expensive.</p>
         *
         * <p>The client execution timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
         * timeout is breached. The typical case aborts the request within a few milliseconds but there may occasionally be
         * requests that don't get aborted until several seconds after the timer has been breached. Because of this, the client
         * execution timeout feature should not be used when absolute precision is needed.</p>
         *
         * <p>This may be used together with {@link #httpRequestTimeout()} to enforce both a timeout on each individual HTTP
         * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'client execution' time).
         * A non-positive value disables this feature.</p>
         *
         * @see ClientOverrideConfiguration#totalExecutionTimeout()
         */
        Builder totalExecutionTimeout(Duration totalExecutionTimeout);

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
         * {@link #addLastExecutionInterceptor(ExecutionInterceptor)}.
         *
         * The provided interceptors are executed in the order they are configured and are always later in the order than the ones
         * automatically added by the SDK. See {@link ExecutionInterceptor} for a more detailed explanation of interceptor order.
         *
         * @see ClientOverrideConfiguration#lastExecutionInterceptors()
         */
        Builder lastExecutionInterceptors(List<ExecutionInterceptor> executionInterceptors);

        /**
         * Add an execution interceptor that will have access to read and modify the request and response objects as they are
         * processed by the SDK.
         *
         * Interceptors added using this method are executed in the order they are configured and are always later in the order
         * than the ones automatically added by the SDK. See {@link ExecutionInterceptor} for a more detailed explanation of
         * interceptor order.
         *
         * @see ClientOverrideConfiguration#lastExecutionInterceptors()
         */
        Builder addLastExecutionInterceptor(ExecutionInterceptor executionInterceptor);

        /**
         * Configure an advanced override option. These values are used very rarely, and the majority of SDK customers can ignore
         * them.
         *
         * @param option The option to configure.
         * @param value The value of the option.
         * @param <T> The type of the option.
         */
        <T> Builder advancedOption(AdvancedClientOption<T> option, T value);

        /**
         * Configure the map of advanced override options. This will override all values currently configured. The values in the
         * map must match the key type of the map, or a runtime exception will be raised.
         */
        Builder advancedOptions(Map<AdvancedClientOption<?>, ?> advancedOptions);
    }

    /**
     * An SDK-internal implementation of {@link ClientOverrideConfiguration.Builder}.
     */
    private static final class DefaultClientOverrideConfigurationBuilder implements Builder {
        private Duration httpRequestTimeout;
        private Duration totalExecutionTimeout;
        private Map<String, List<String>> additionalHttpHeaders = new HashMap<>();
        private Boolean gzipEnabled;
        private RetryPolicy retryPolicy;
        private List<ExecutionInterceptor> lastExecutionInterceptors = new ArrayList<>();
        private AttributeMap.Builder advancedOptions = AttributeMap.builder();

        @Override
        public Builder httpRequestTimeout(Duration httpRequestTimeout) {
            this.httpRequestTimeout = httpRequestTimeout;
            return this;
        }

        public void setHttpRequestTimeout(Duration httpRequestTimeout) {
            httpRequestTimeout(httpRequestTimeout);
        }

        @Override
        public Builder totalExecutionTimeout(Duration totalExecutionTimeout) {
            this.totalExecutionTimeout = totalExecutionTimeout;
            return this;
        }

        public void setTotalExecutionTimeout(Duration totalExecutionTimeout) {
            totalExecutionTimeout(totalExecutionTimeout);
        }

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
        public Builder lastExecutionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            this.lastExecutionInterceptors.clear();
            this.lastExecutionInterceptors.addAll(executionInterceptors);
            return this;
        }

        @Override
        public Builder addLastExecutionInterceptor(ExecutionInterceptor executionInterceptors) {
            this.lastExecutionInterceptors.add(executionInterceptors);
            return this;
        }

        public void setLastExecutionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            lastExecutionInterceptors(executionInterceptors);
        }

        @Override
        public <T> Builder advancedOption(AdvancedClientOption<T> option, T value) {
            this.advancedOptions.put(option, value);
            return this;
        }

        @Override
        public Builder advancedOptions(Map<AdvancedClientOption<?>, ?> advancedOptions) {
            this.advancedOptions.putAll(advancedOptions);
            return this;
        }

        private Builder advancedOptions(AttributeMap.Builder attributeMap) {
            this.advancedOptions = attributeMap;
            return this;
        }

        public void setAdvancedOptions(Map<AdvancedClientOption<?>, Object> advancedOptions) {
            advancedOptions(advancedOptions);
        }

        @Override
        public ClientOverrideConfiguration build() {
            return new ClientOverrideConfiguration(this);
        }
    }
}
