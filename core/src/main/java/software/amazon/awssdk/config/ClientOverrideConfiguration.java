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

package software.amazon.awssdk.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.CollectionUtils;
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
    private final RequestMetricCollector requestMetricCollector;
    private final RetryPolicy retryPolicy;
    private final List<RequestHandler> requestListeners;
    private final AttributeMap advancedOptions;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientOverrideConfiguration(DefaultClientOverrideConfigurationBuilder builder) {
        this.httpRequestTimeout = builder.httpRequestTimeout;
        this.totalExecutionTimeout = builder.totalExecutionTimeout;
        this.additionalHttpHeaders = CollectionUtils.deepCopiedUnmodifiableMap(builder.additionalHttpHeaders);
        this.gzipEnabled = builder.gzipEnabled;
        this.requestMetricCollector = builder.requestMetricCollector;
        this.retryPolicy = builder.retryPolicy;
        this.requestListeners = Collections.unmodifiableList(new ArrayList<>(builder.requestListeners));
        this.advancedOptions = builder.advancedOptions.build();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultClientOverrideConfigurationBuilder().advancedOptions(advancedOptions.toBuilder())
                                                              .httpRequestTimeout(httpRequestTimeout)
                                                              .totalExecutionTimeout(totalExecutionTimeout)
                                                              .additionalHttpHeaders(additionalHttpHeaders)
                                                              .gzipEnabled(gzipEnabled)
                                                              .requestMetricCollector(requestMetricCollector)
                                                              .retryPolicy(retryPolicy)
                                                              .requestListeners(requestListeners);
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
     * The metric collector that should be notified of each request event.
     *
     * @see Builder#requestMetricCollector(RequestMetricCollector)
     */
    public RequestMetricCollector requestMetricCollector() {
        return requestMetricCollector;
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
     * An immutable collection of request listeners that should be hooked into the execution of each request, in the order that
     * they should be applied.
     *
     * @see Builder#requestListeners(List)
     */
    @ReviewBeforeRelease("We are probably going to update the request handler interface. The description should be updated to "
                         + "detail the functionality of the new interface.")
    public List<RequestHandler> requestListeners() {
        return requestListeners;
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
         * Set the metric collector that should be notified of each request event.
         *
         * @see ClientOverrideConfiguration#requestMetricCollector()
         */
        Builder requestMetricCollector(RequestMetricCollector metricCollector);

        /**
         * Configure the retry policy that should be used when handling failure cases.
         *
         * @see ClientOverrideConfiguration#retryPolicy()
         */
        Builder retryPolicy(RetryPolicy retryPolicy);

        /**
         * Configure an immutable collection of request listeners that should be hooked into the execution of each request, in
         * the order that they should be applied. These will override any listeners already configured.
         *
         * @see ClientOverrideConfiguration#requestListeners()
         */
        Builder requestListeners(List<RequestHandler> requestListeners);

        /**
         * Add a request listener that will be hooked into the execution of each request after the listeners that have previously
         * been configured have all been executed.
         *
         * @see ClientOverrideConfiguration#requestListeners()
         */
        Builder addRequestListener(RequestHandler requestListener);

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
        private RequestMetricCollector requestMetricCollector;
        private RetryPolicy retryPolicy;
        private List<RequestHandler> requestListeners = new ArrayList<>();
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
        public Builder requestMetricCollector(RequestMetricCollector requestMetricCollector) {
            this.requestMetricCollector = requestMetricCollector;
            return this;
        }

        public void setRequestMetricCollector(RequestMetricCollector requestMetricCollector) {
            requestMetricCollector(requestMetricCollector);
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
        public Builder requestListeners(List<RequestHandler> requestListeners) {
            this.requestListeners.clear();
            this.requestListeners.addAll(requestListeners);
            return this;
        }

        @Override
        public Builder addRequestListener(RequestHandler requestListener) {
            this.requestListeners.add(requestListener);
            return this;
        }

        public void setRequestListeners(List<RequestHandler> requestListeners) {
            requestListeners(requestListeners);
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
