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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
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
    private final Duration apiCallAttemptTimeout;
    private final Duration apiCallTimeout;
    private final ProfileFile defaultProfileFile;
    private final String defaultProfileName;
    private final List<MetricPublisher> metricPublishers;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    private ClientOverrideConfiguration(Builder builder) {
        this.headers = CollectionUtils.deepUnmodifiableMap(builder.headers(), () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        this.retryPolicy = builder.retryPolicy();
        this.executionInterceptors = Collections.unmodifiableList(new ArrayList<>(builder.executionInterceptors()));
        this.advancedOptions = builder.advancedOptions();
        this.apiCallTimeout = Validate.isPositiveOrNull(builder.apiCallTimeout(), "apiCallTimeout");
        this.apiCallAttemptTimeout = Validate.isPositiveOrNull(builder.apiCallAttemptTimeout(), "apiCallAttemptTimeout");
        this.defaultProfileFile = builder.defaultProfileFile();
        this.defaultProfileName = builder.defaultProfileName();
        this.metricPublishers = Collections.unmodifiableList(new ArrayList<>(builder.metricPublishers()));
    }

    @Override
    public Builder toBuilder() {
        return new DefaultClientOverrideConfigurationBuilder().advancedOptions(advancedOptions.toBuilder())
                                                              .headers(headers)
                                                              .retryPolicy(retryPolicy)
                                                              .apiCallTimeout(apiCallTimeout)
                                                              .apiCallAttemptTimeout(apiCallAttemptTimeout)
                                                              .executionInterceptors(executionInterceptors)
                                                              .defaultProfileFile(defaultProfileFile)
                                                              .defaultProfileName(defaultProfileName);
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

    /**
     * The amount of time to allow the client to complete the execution of an API call. This timeout covers the entire client
     * execution except for marshalling. This includes request handler execution, all HTTP requests including retries,
     * unmarshalling, etc. This value should always be positive, if present.
     *
     * <p>The api call timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
     * timeout is breached. The typical case aborts the request within a few milliseconds but there may occasionally be
     * requests that don't get aborted until several seconds after the timer has been breached. Because of this, the client
     * execution timeout feature should not be used when absolute precision is needed.
     *
     * <p>This may be used together with {@link #apiCallAttemptTimeout()} to enforce both a timeout on each individual HTTP
     * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
     *
     * @see Builder#apiCallTimeout(Duration)
     */
    public Optional<Duration> apiCallTimeout() {
        return Optional.ofNullable(apiCallTimeout);
    }

    /**
     * The amount of time to wait for the http request to complete before giving up and timing out. This value should always be
     * positive, if present.
     *
     * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
     * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
     * don't get aborted until several seconds after the timer has been breached. Because of this, the request timeout
     * feature should not be used when absolute precision is needed.
     *
     * <p>This may be used together with {@link #apiCallTimeout()} to enforce both a timeout on each individual HTTP
     * request
     * (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
     *
     * @see Builder#apiCallAttemptTimeout(Duration)
     */
    public Optional<Duration> apiCallAttemptTimeout() {
        return Optional.ofNullable(apiCallAttemptTimeout);
    }

    /**
     * The profile file that should be used by default for all profile-based configuration in the SDK client.
     *
     * @see Builder#defaultProfileFile(ProfileFile)
     */
    public Optional<ProfileFile> defaultProfileFile() {
        return Optional.ofNullable(defaultProfileFile);
    }

    /**
     * The profile name that should be used by default for all profile-based configuration in the SDK client.
     *
     * @see Builder#defaultProfileName(String)
     */
    public Optional<String> defaultProfileName() {
        return Optional.ofNullable(defaultProfileName);
    }

    /**
     * The metric publishers to use to publisher metrics collected for this client.
     *
     * @return The metric publisher.
     */
    public List<MetricPublisher> metricPublishers() {
        return metricPublishers;
    }

    @Override
    public String toString() {
        return ToString.builder("ClientOverrideConfiguration")
                       .add("headers", headers)
                       .add("retryPolicy", retryPolicy)
                       .add("apiCallTimeout", apiCallTimeout)
                       .add("apiCallAttemptTimeout", apiCallAttemptTimeout)
                       .add("executionInterceptors", executionInterceptors)
                       .add("advancedOptions", advancedOptions)
                       .add("profileFile", defaultProfileFile)
                       .add("profileName", defaultProfileName)
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
         * <p>
         * This overrides any values for the given header set on the request by default by the SDK.
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
         * <p>
         * This overrides any values for the given header set on the request by default by the SDK.
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
         * <p>
         * This overrides any values for the given headers set on the request by default by the SDK.
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

        /**
         * Configure the retry policy the should be used when handling failure cases.
         */
        default Builder retryPolicy(Consumer<RetryPolicy.Builder> retryPolicy) {
            return retryPolicy(RetryPolicy.builder().applyMutation(retryPolicy).build());
        }

        /**
         * Configure the retry mode used to determine the retry policy that is used when handling failure cases. This is
         * shorthand for {@code retryPolicy(RetryPolicy.forRetryMode(retryMode))}, and overrides any configured retry policy on
         * this builder.
         */
        default Builder retryPolicy(RetryMode retryMode) {
            return retryPolicy(RetryPolicy.forRetryMode(retryMode));
        }

        RetryPolicy retryPolicy();

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
         * @see ClientOverrideConfiguration#executionInterceptors()
         */
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
         * @see ClientOverrideConfiguration#executionInterceptors()
         */
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

        /**
         * Configure the amount of time to allow the client to complete the execution of an API call. This timeout covers the
         * entire client execution except for marshalling. This includes request handler execution, all HTTP requests including
         * retries, unmarshalling, etc. This value should always be positive, if present.
         *
         * <p>The api call timeout feature doesn't have strict guarantees on how quickly a request is aborted when the
         * timeout is breached. The typical case aborts the request within a few milliseconds but there may occasionally be
         * requests that don't get aborted until several seconds after the timer has been breached. Because of this, the client
         * execution timeout feature should not be used when absolute precision is needed.
         *
         * <p>
         * For synchronous streaming operations, implementations of {@link ResponseTransformer} must handle interrupt
         * properly to allow the the SDK to timeout the request in a timely manner.
         *
         * <p>This may be used together with {@link #apiCallAttemptTimeout()} to enforce both a timeout on each individual HTTP
         * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
         *
         *
         * @see ClientOverrideConfiguration#apiCallTimeout()
         */
        Builder apiCallTimeout(Duration apiCallTimeout);

        Duration apiCallTimeout();

        /**
         * Configure the amount of time to wait for the http request to complete before giving up and timing out. This value
         * should always be positive, if present.
         *
         * <p>The request timeout feature doesn't have strict guarantees on how quickly a request is aborted when the timeout is
         * breached. The typical case aborts the request within a few milliseconds but there may occasionally be requests that
         * don't get aborted until several seconds after the timer has been breached. Because of this, the api call attempt
         * timeout feature should not be used when absolute precision is needed.
         *
         * <p>For synchronous streaming operations, the process in {@link ResponseTransformer} is not timed and will not
         * be aborted.
         *
         * <p>This may be used together with {@link #apiCallTimeout()} to enforce both a timeout on each individual HTTP
         * request (i.e. each retry) and the total time spent on all requests across retries (i.e. the 'api call' time).
         *
         * @see ClientOverrideConfiguration#apiCallAttemptTimeout()
         */
        Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout);

        Duration apiCallAttemptTimeout();

        /**
         * Configure the profile file that should be used by default for all profile-based configuration in the SDK client.
         *
         * <p>This is equivalent to setting the {@link ProfileFileSystemSetting#AWS_CONFIG_FILE} and
         * {@link ProfileFileSystemSetting#AWS_SHARED_CREDENTIALS_FILE} environment variables or system properties.
         *
         * <p>Like the system settings, this value is only used when determining default values. For example, directly configuring
         * the retry policy, credentials provider or region will mean that the configured values will be used instead of those
         * from the profile file.
         *
         * <p>Like the {@code --profile} setting in the CLI, profile-based configuration loaded from this profile file has lower
         * priority than more specific environment variables, like the {@code AWS_REGION} environment variable.
         *
         * <p>If this is not set, the {@link ProfileFile#defaultProfileFile()} is used.
         *
         * @see #defaultProfileName(String)
         */
        Builder defaultProfileFile(ProfileFile defaultProfileFile);

        ProfileFile defaultProfileFile();

        /**
         * Configure the profile name that should be used by default for all profile-based configuration in the SDK client.
         *
         * <p>This is equivalent to setting the {@link ProfileFileSystemSetting#AWS_PROFILE} environment variable or system
         * property.
         *
         * <p>Like the system setting, this value is only used when determining default values. For example, directly configuring
         * the retry policy, credentials provider or region will mean that the configured values will be used instead of those
         * from this profile.
         *
         * <p>If this is not set, the {@link ProfileFileSystemSetting#AWS_PROFILE} (or {@code "default"}) is used.
         *
         * @see #defaultProfileFile(ProfileFile)
         */
        Builder defaultProfileName(String defaultProfileName);

        String defaultProfileName();

        /**
         * Set the Metric publishers to be use to publish metrics for this client. This overwrites the current list of
         * metric publishers set on the builder.
         *
         * @param metricPublishers The metric publishers.
         */
        Builder metricPublishers(List<MetricPublisher> metricPublishers);


        /**
         * Add a metric publisher to the existing list of previously set publishers to be used for publishing metrics
         * for this client.
         *
         * @param metricPublisher The metric publisher to add.
         */
        Builder addMetricPublisher(MetricPublisher metricPublisher);

        List<MetricPublisher> metricPublishers();
    }

    /**
     * An SDK-internal implementation of {@link ClientOverrideConfiguration.Builder}.
     */
    private static final class DefaultClientOverrideConfigurationBuilder implements Builder {
        private Map<String, List<String>> headers = new HashMap<>();
        private RetryPolicy retryPolicy;
        private List<ExecutionInterceptor> executionInterceptors = new ArrayList<>();
        private AttributeMap.Builder advancedOptions = AttributeMap.builder();
        private Duration apiCallTimeout;
        private Duration apiCallAttemptTimeout;
        private ProfileFile defaultProfileFile;
        private String defaultProfileName;
        private List<MetricPublisher> metricPublishers = new ArrayList<>();

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
            return CollectionUtils.unmodifiableMapOfLists(headers);
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
        public Builder apiCallTimeout(Duration apiCallTimeout) {
            this.apiCallTimeout = apiCallTimeout;
            return this;
        }

        public void setApiCallTimeout(Duration apiCallTimeout) {
            apiCallTimeout(apiCallTimeout);
        }

        @Override
        public Duration apiCallTimeout() {
            return apiCallTimeout;
        }

        @Override
        public Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            this.apiCallAttemptTimeout = apiCallAttemptTimeout;
            return this;
        }

        public void setApiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            apiCallAttemptTimeout(apiCallAttemptTimeout);
        }

        @Override
        public Duration apiCallAttemptTimeout() {
            return apiCallAttemptTimeout;
        }

        @Override
        public ProfileFile defaultProfileFile() {
            return this.defaultProfileFile;
        }

        @Override
        public Builder defaultProfileFile(ProfileFile defaultProfileFile) {
            this.defaultProfileFile = defaultProfileFile;
            return this;
        }

        @Override
        public String defaultProfileName() {
            return this.defaultProfileName;
        }

        @Override
        public Builder defaultProfileName(String defaultProfileName) {
            this.defaultProfileName = defaultProfileName;
            return this;
        }

        @Override
        public Builder metricPublishers(List<MetricPublisher> metricPublishers) {
            Validate.paramNotNull(metricPublishers, "metricPublishers");
            this.metricPublishers = new ArrayList<>(metricPublishers);
            return this;
        }

        public void setMetricPublishers(List<MetricPublisher> metricPublishers) {
            metricPublishers(metricPublishers);
        }

        @Override
        public Builder addMetricPublisher(MetricPublisher metricPublisher) {
            Validate.paramNotNull(metricPublisher, "metricPublisher");
            this.metricPublishers.add(metricPublisher);
            return this;
        }

        @Override
        public List<MetricPublisher> metricPublishers() {
            return Collections.unmodifiableList(metricPublishers);
        }

        @Override
        public ClientOverrideConfiguration build() {
            return new ClientOverrideConfiguration(this);
        }
    }
}
