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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ADDITIONAL_HTTP_HEADERS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_ATTEMPT_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.COMPRESSION_CONFIGURATION;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_COMPRESSION_CONFIGURATION;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_RETRY_CONFIGURATOR;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_RETRY_MODE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_RETRY_STRATEGY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CONFIGURED_SCHEDULED_EXECUTOR_SERVICE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.EXECUTION_ATTRIBUTES;
import static software.amazon.awssdk.core.client.config.SdkClientOption.EXECUTION_INTERCEPTORS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.METRIC_PUBLISHERS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.PROFILE_FILE_SUPPLIER;
import static software.amazon.awssdk.core.client.config.SdkClientOption.PROFILE_NAME;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_POLICY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_STRATEGY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SCHEDULED_EXECUTOR_SERVICE;
import static software.amazon.awssdk.core.client.config.SdkClientOption.USER_AGENT_APP_ID;
import static software.amazon.awssdk.utils.ScheduledExecutorUtils.unmanagedScheduledExecutor;
import static software.amazon.awssdk.utils.ScheduledExecutorUtils.unwrapUnmanagedScheduledExecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ToBuilderIgnoreField;
import software.amazon.awssdk.core.CompressionConfiguration;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.retries.api.RetryStrategy;
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
    /**
     * The set of options modified by this ClientOverrideConfiguration. This is used when the ClientOverrideConfiguration
     * is created from a {@link SdkClientConfiguration} to filter out properties that this object doesn't use.
     *
     * This is important so that unrelated configuration values don't "pass through" from when this object is created
     * from a SdkClientConfiguration and then converted back.
     */
    private static final Set<ClientOption<?>> CLIENT_OVERRIDE_OPTIONS;

    /**
     * The set of options that can be visible from this ClientOverrideConfiguration, but can't be modified directly. For
     * example, when this ClientOverrideConfiguration is created from an SdkClientConfiguration, we want the
     * {@link SdkClientOption#COMPRESSION_CONFIGURATION} to be visible to {@link #compressionConfiguration()} even though
     * the setting that this object manipulates is {@link SdkClientOption#CONFIGURED_COMPRESSION_CONFIGURATION}.
     *
     * In practice, this means that when we create a ClientOverrideConfiguration from a SdkClientConfiguration, these
     * values can be read by users of the ClientOverrideConfiguration, but these values won't be included in the result
     * of {@link #asSdkClientConfiguration()}.
     */
    private static final Set<ClientOption<?>> RESOLVED_OPTIONS;

    static {
        Set<ClientOption<?>> options = new HashSet<>();
        options.add(ADDITIONAL_HTTP_HEADERS);
        options.add(EXECUTION_INTERCEPTORS);
        options.add(METRIC_PUBLISHERS);
        options.add(EXECUTION_ATTRIBUTES);
        options.add(CONFIGURED_COMPRESSION_CONFIGURATION);
        options.add(CONFIGURED_SCHEDULED_EXECUTOR_SERVICE);
        options.add(RETRY_POLICY);
        options.add(RETRY_STRATEGY);
        options.add(API_CALL_TIMEOUT);
        options.add(API_CALL_ATTEMPT_TIMEOUT);
        options.add(PROFILE_FILE_SUPPLIER);
        options.add(PROFILE_NAME);
        options.add(CONFIGURED_RETRY_STRATEGY);
        options.add(CONFIGURED_RETRY_CONFIGURATOR);
        options.add(CONFIGURED_RETRY_MODE);
        options.add(USER_AGENT_APP_ID);
        CLIENT_OVERRIDE_OPTIONS = Collections.unmodifiableSet(options);

        Set<ClientOption<?>> resolvedOptions = new HashSet<>();
        resolvedOptions.add(COMPRESSION_CONFIGURATION);
        resolvedOptions.add(SCHEDULED_EXECUTOR_SERVICE);
        RESOLVED_OPTIONS = Collections.unmodifiableSet(resolvedOptions);
    }

    private final SdkClientConfiguration config;
    private final SdkClientConfiguration resolvedConfig;

    private final Map<String, List<String>> headers;
    private final List<ExecutionInterceptor> executionInterceptors;
    private final List<MetricPublisher> metricPublishers;
    private final ExecutionAttributes executionAttributes;

    /**
     * Initialize this configuration. Private to require use of {@link #builder()}.
     */
    @SdkInternalApi
    ClientOverrideConfiguration(SdkClientConfiguration config, SdkClientConfiguration resolvedConfig) {
        this.config = config;
        this.resolvedConfig = resolvedConfig;

        // Store separately any mutable types, so that modifications to the underlying option (e.g. from the builder) would not
        // be visible to users of this configuration
        Map<String, List<String>> headers = config.option(ADDITIONAL_HTTP_HEADERS);
        this.headers = headers == null
                       ? emptyMap()
                       : CollectionUtils.deepUnmodifiableMap(headers, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER));

        List<ExecutionInterceptor> interceptors = config.option(EXECUTION_INTERCEPTORS);
        this.executionInterceptors = interceptors == null
                                     ? emptyList()
                                     : Collections.unmodifiableList(new ArrayList<>(interceptors));


        List<MetricPublisher> metricPublishers = config.option(METRIC_PUBLISHERS);
        this.metricPublishers = metricPublishers == null
                                ? emptyList()
                                : Collections.unmodifiableList(new ArrayList<>(metricPublishers));

        ExecutionAttributes executionAttributes = config.option(EXECUTION_ATTRIBUTES);
        this.executionAttributes = executionAttributes == null
                                   ? new ExecutionAttributes()
                                   : ExecutionAttributes.unmodifiableExecutionAttributes(executionAttributes);

        Validate.isPositiveOrNull(apiCallTimeout().orElse(null), "apiCallTimeout");
        Validate.isPositiveOrNull(apiCallAttemptTimeout().orElse(null), "apiCallAttemptTimeout");
    }

    @Override
    @ToBuilderIgnoreField({"config", "resolvedConfig"})
    public Builder toBuilder() {
        return new DefaultBuilder(this.config.toBuilder(), this.resolvedConfig.toBuilder())
            .headers(headers)
            .executionInterceptors(executionInterceptors)
            .executionAttributes(executionAttributes)
            .metricPublishers(metricPublishers);
    }

    /**
     * Create a {@link Builder}, used to create a {@link ClientOverrideConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @SdkInternalApi
    SdkClientConfiguration asSdkClientConfiguration() {
        return config;
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
        return Optional.ofNullable(config.option(RETRY_POLICY));
    }

    /**
     * The optional retry strategy that should be used when handling failure cases.
     *
     * @see Builder#retryStrategy(RetryStrategy)
     */
    public Optional<RetryStrategy> retryStrategy() {
        RetryStrategy configured = config.option(CONFIGURED_RETRY_STRATEGY);
        if (configured != null) {
            return Optional.of(configured);
        }
        return Optional.ofNullable(config.option(RETRY_STRATEGY));
    }

    /**
     * The optional retry mode that should be used when handling failure cases.
     *
     * @see Builder#retryStrategy(RetryMode)
     */
    public Optional<Consumer<RetryStrategy.Builder<?, ?>>> retryStrategyConfigurator() {
        return Optional.ofNullable(config.option(CONFIGURED_RETRY_CONFIGURATOR));
    }

    /**
     * The optional retry mode that should be used when handling failure cases.
     *
     * @see Builder#retryStrategy(RetryMode)
     */
    public Optional<RetryMode> retryMode() {
        return Optional.ofNullable(config.option(CONFIGURED_RETRY_MODE));
    }

    /**
     * Load the optional requested advanced option that was configured on the client builder.
     *
     * @see Builder#putAdvancedOption(SdkAdvancedClientOption, Object)
     */
    public <T> Optional<T> advancedOption(SdkAdvancedClientOption<T> option) {
        return Optional.ofNullable(config.option(option));
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
     * The optional scheduled executor service that should be used for scheduling tasks such as async retry attempts
     * and timeout task.
     * <p>
     * <b>The SDK will not automatically close the executor when the client is closed. It is the responsibility of the
     * user to manually close the executor once all clients utilizing it have been closed.</b>
     */
    public Optional<ScheduledExecutorService> scheduledExecutorService() {
        // If the client override configuration is accessed from a plugin or a client, we want the actual executor service we're
        // using to be available. For that reason, we should check the SCHEDULED_EXECUTOR_SERVICE.
        ScheduledExecutorService scheduledExecutorService = resolvedConfig.option(SCHEDULED_EXECUTOR_SERVICE);
        if (scheduledExecutorService == null) {
            // Unwrap the executor to ensure that read-after-write returns the same values.
            scheduledExecutorService = unwrapUnmanagedScheduledExecutor(config.option(CONFIGURED_SCHEDULED_EXECUTOR_SERVICE));
        }
        return Optional.ofNullable(scheduledExecutorService);
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
        return Optional.ofNullable(config.option(API_CALL_TIMEOUT));
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
        return Optional.ofNullable(config.option(API_CALL_ATTEMPT_TIMEOUT));
    }

    /**
     * The profile file supplier that should be used by default for all profile-based configuration in the SDK client.
     *
     * @see Builder#defaultProfileFileSupplier(Supplier)
     */
    public Optional<Supplier<ProfileFile>> defaultProfileFileSupplier() {
        return Optional.ofNullable(config.option(PROFILE_FILE_SUPPLIER));
    }

    /**
     * The profile file that should be used by default for all profile-based configuration in the SDK client.
     *
     * @see Builder#defaultProfileFile(ProfileFile)
     */
    public Optional<ProfileFile> defaultProfileFile() {
        return Optional.ofNullable(config.option(PROFILE_FILE_SUPPLIER)).map(Supplier::get);
    }

    /**
     * The profile name that should be used by default for all profile-based configuration in the SDK client.
     *
     * @see Builder#defaultProfileName(String)
     */
    public Optional<String> defaultProfileName() {
        return Optional.ofNullable(config.option(PROFILE_NAME));
    }

    /**
     * The metric publishers to use to publisher metrics collected for this client.
     *
     * @return The metric publisher.
     */
    public List<MetricPublisher> metricPublishers() {
        return metricPublishers;
    }

    /**
     *  Returns the additional execution attributes to be added for this client.
     *
     * @Return Map of execution attributes.
     */
    public ExecutionAttributes executionAttributes() {
        return executionAttributes;
    }

    /**
     * The compression configuration object, which includes options to enable/disable compression and set the minimum
     * compression threshold.
     *
     * @see Builder#compressionConfiguration(CompressionConfiguration)
     */
    public Optional<CompressionConfiguration> compressionConfiguration() {

        // If the client override configuration is accessed from a plugin or a client, we want the compression configuration
        // we're using to be available. For that reason, we should check the COMPRESSION_CONFIGURATION.
        CompressionConfiguration compressionConfig = resolvedConfig.option(COMPRESSION_CONFIGURATION);
        if (compressionConfig == null) {
            compressionConfig = config.option(CONFIGURED_COMPRESSION_CONFIGURATION);
        }
        return Optional.ofNullable(compressionConfig);
    }

    /**
     * An optional user specified identification value to be appended to the user agent header.
     * For more information, see {@link SdkClientOption#USER_AGENT_APP_ID}.
     */
    public Optional<String> appId() {
        return Optional.ofNullable(config.option(USER_AGENT_APP_ID));
    }

    @Override
    public String toString() {
        return ToString.builder("ClientOverrideConfiguration")
                       .add("headers", headers())
                       .add("retryPolicy", retryPolicy().orElse(null))
                       .add("retryStrategy", retryStrategy().orElse(null))
                       .add("apiCallTimeout", apiCallTimeout().orElse(null))
                       .add("apiCallAttemptTimeout", apiCallAttemptTimeout().orElse(null))
                       .add("executionInterceptors", executionInterceptors())
                       .add("profileFileSupplier", defaultProfileFileSupplier().orElse(null))
                       .add("profileFile", defaultProfileFile().orElse(null))
                       .add("profileName", defaultProfileName().orElse(null))
                       .add("scheduledExecutorService", scheduledExecutorService().orElse(null))
                       .add("compressionConfiguration", compressionConfiguration().orElse(null))
                       .add("appId", appId().orElse(null))
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
         * @deprecated Use instead {@link #retryStrategy(RetryStrategy)}
         */
        @Deprecated
        Builder retryPolicy(RetryPolicy retryPolicy);

        /**
         * Configure the retry policy the should be used when handling failure cases.
         *
         * @deprecated Use instead {@link #retryStrategy(Consumer<RetryStrategy.Builder>)}
         */
        @Deprecated
        default Builder retryPolicy(Consumer<RetryPolicy.Builder> retryPolicy) {
            return retryPolicy(RetryPolicy.builder().applyMutation(retryPolicy).build());
        }

        /**
         * Configure the retry mode used to determine the retry policy that is used when handling failure cases. This is
         * shorthand for {@code retryPolicy(RetryPolicy.forRetryMode(retryMode))}, and overrides any configured retry policy on
         * this builder.
         *
         * @deprecated Use instead {@link #retryStrategy(RetryMode)}
         */
        @Deprecated
        default Builder retryPolicy(RetryMode retryMode) {
            return retryPolicy(RetryPolicy.forRetryMode(retryMode));
        }

        RetryPolicy retryPolicy();

        /**
         * Configure the retry strategy that should be used when handling failure cases.
         *
         * <p>
         * Note that retryStrategy options are mutually exclusive
         */
        Builder retryStrategy(RetryStrategy retryStrategy);

        /**
         * Configure the retry mode used to resolve the corresponding {@link RetryStrategy} that should be used when handling
         * failure cases.
         * <p>
         * Note that retryStrategy options are mutually exclusive
         *
         * @see RetryMode
         */
        default Builder retryStrategy(RetryMode retryMode) {
            throw new UnsupportedOperationException();
        }

        /**
         * Configure a consumer to customize the default retry strategy. The default retry strategy is obtained by using the
         * default {@link RetryMode} that is resolved by looking at (in this order)
         *
         * <ol>
         *  <li>The {@code AWS_RETRY_MODE} environment variable</li>
         *  <li>The {@code aws.retryMode} JVM system property</li>
         *  <li>The {@code retry_mode} setting in the profile file for the active profile</li>
         * </ol>
         *
         * <p>
         * Defaults to {@link RetryMode#LEGACY} if no configuration setting is found.
         * <p>
         * Note that retryStrategy options are mutually exclusive
         */
        default Builder retryStrategy(Consumer<RetryStrategy.Builder<?, ?>> configurator) {
            throw new UnsupportedOperationException();
        }

        RetryStrategy retryStrategy();

        RetryMode retryMode();

        Consumer<RetryStrategy.Builder<?, ?>> retryStrategyConfigurator();

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
         * Configure the scheduled executor service that should be used for scheduling tasks such as async retry attempts
         * and timeout task.
         *
         * <p>
         * <b>The SDK will not automatically close the executor when the client is closed. It is the responsibility of the
         * user to manually close the executor once all clients utilizing it have been closed.</b>
         *
         * <p>
         * When modifying this option from an {@link SdkPlugin}, it is strongly recommended to decorate the
         * {@link #scheduledExecutorService()}. If you will be replacing it entirely, you MUST shut it down to prevent the
         * resources being leaked.
         *
         * @see ClientOverrideConfiguration#scheduledExecutorService()
         */
        Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);

        ScheduledExecutorService scheduledExecutorService();        

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
         * <p>
         * You can also configure it on a per-request basis via
         * {@link RequestOverrideConfiguration.Builder#apiCallTimeout(Duration)}.
         * Note that request-level timeout takes precedence.
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
         * <p>
         * You can also configure it on a per-request basis via
         * {@link RequestOverrideConfiguration.Builder#apiCallAttemptTimeout(Duration)}.
         * Note that request-level timeout takes precedence.
         *
         * @see ClientOverrideConfiguration#apiCallAttemptTimeout()
         */
        Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout);

        Duration apiCallAttemptTimeout();

        /**
         * Configure a {@link ProfileFileSupplier} that should be used by default for all profile-based configuration in the SDK
         * client.
         *
         * <p>This is equivalent to setting {@link #defaultProfileFile(ProfileFile)}, except the supplier is read every time
         * the configuration is requested. It's recommended to use {@link ProfileFileSupplier} that provides configurable
         * caching for the reading of the profile file.
         *
         * <p>If this is not set, the {@link ProfileFile#defaultProfileFile()} is used.
         *
         * @see #defaultProfileFile(ProfileFile)
         * @see #defaultProfileName(String)
         */
        Builder defaultProfileFileSupplier(Supplier<ProfileFile> defaultProfileFile);

        Supplier<ProfileFile> defaultProfileFileSupplier();

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
         * @see #defaultProfileFileSupplier(Supplier)
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

        /**
         * Sets the additional execution attributes collection for this client.
         * @param executionAttributes Execution attributes map for this client.
         * @return This object for method chaining.
         */
        Builder executionAttributes(ExecutionAttributes executionAttributes);

        /**
         * Put an execution attribute into to the existing collection of execution attributes.
         * @param attribute The execution attribute object
         * @param value The value of the execution attribute.
         */
        <T> Builder putExecutionAttribute(ExecutionAttribute<T> attribute, T value);

        ExecutionAttributes executionAttributes();

        /**
         * Sets the {@link CompressionConfiguration} for this client.
         */
        Builder compressionConfiguration(CompressionConfiguration compressionConfiguration);

        /**
         * Sets the {@link CompressionConfiguration} for this client.
         */
        default Builder compressionConfiguration(Consumer<CompressionConfiguration.Builder> compressionConfiguration) {
            return compressionConfiguration(CompressionConfiguration.builder()
                                                                    .applyMutation(compressionConfiguration)
                                                                    .build());
        }

        CompressionConfiguration compressionConfiguration();

        /**
         * Sets the appId for this client. See {@link SdkClientOption#USER_AGENT_APP_ID}.
         */
        Builder appId(String appId);

        /**
         * The appId for this client. See {@link SdkClientOption#USER_AGENT_APP_ID}.
         */
        String appId();
    }

    /**
     * An SDK-internal implementation of {@link ClientOverrideConfiguration.Builder}.
     */
    @SdkInternalApi
    static final class DefaultBuilder implements Builder {
        private final SdkClientConfiguration.Builder config;
        private final SdkClientConfiguration.Builder resolvedConfig;

        @SdkInternalApi
        DefaultBuilder(SdkClientConfiguration.Builder config) {
            this();
            RESOLVED_OPTIONS.forEach(o -> copyValue(o, config, this.resolvedConfig));
            CLIENT_OVERRIDE_OPTIONS.forEach(o -> copyValue(o, config, this.config));
            SdkAdvancedClientOption.options().forEach(o -> copyValue(o, config, this.config));
        }

        private DefaultBuilder() {
            this(SdkClientConfiguration.builder(), SdkClientConfiguration.builder());
        }

        private DefaultBuilder(SdkClientConfiguration.Builder config,
                               SdkClientConfiguration.Builder resolvedConfig) {
            this.config = config;
            this.resolvedConfig = resolvedConfig;
        }

        @Override
        public Builder headers(Map<String, List<String>> headers) {
            Validate.paramNotNull(headers, "headers");
            this.config.option(ADDITIONAL_HTTP_HEADERS, CollectionUtils.deepCopyMap(headers, this::newHeaderMap));
            return this;
        }

        public void setHeaders(Map<String, List<String>> additionalHttpHeaders) {
            headers(additionalHttpHeaders);
        }

        @Override
        public Map<String, List<String>> headers() {
            Map<String, List<String>> option = Validate
                .getOrDefault(config.option(ADDITIONAL_HTTP_HEADERS), Collections::emptyMap);
            return CollectionUtils.unmodifiableMapOfLists(option);
        }

        @Override
        public Builder putHeader(String header, List<String> values) {
            Validate.paramNotNull(header, "header");
            Validate.paramNotNull(values, "values");
            config.computeOptionIfAbsent(ADDITIONAL_HTTP_HEADERS, this::newHeaderMap)
                  .put(header, new ArrayList<>(values));
            return this;
        }

        @Override
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            config.option(RETRY_POLICY, retryPolicy);
            config.option(CONFIGURED_RETRY_STRATEGY, null);
            config.option(CONFIGURED_RETRY_CONFIGURATOR, null);
            config.option(CONFIGURED_RETRY_MODE, null);
            return this;
        }

        public void setRetryPolicy(RetryPolicy retryPolicy) {
            retryPolicy(retryPolicy);
        }

        @Override
        public RetryPolicy retryPolicy() {
            return config.option(RETRY_POLICY);
        }

        @Override
        public Builder retryStrategy(RetryStrategy retryStrategy) {
            Validate.paramNotNull(retryStrategy, "retryStrategy");
            config.option(CONFIGURED_RETRY_STRATEGY, retryStrategy);
            config.option(CONFIGURED_RETRY_CONFIGURATOR, null);
            config.option(CONFIGURED_RETRY_MODE, null);
            config.option(RETRY_POLICY, null);
            return this;
        }

        @Override
        public Builder retryStrategy(Consumer<RetryStrategy.Builder<?, ?>> configurator) {
            Validate.paramNotNull(configurator, "configurator");
            config.option(CONFIGURED_RETRY_CONFIGURATOR, configurator);
            config.option(CONFIGURED_RETRY_MODE, null);
            config.option(CONFIGURED_RETRY_STRATEGY, null);
            config.option(RETRY_POLICY, null);
            return this;
        }

        @Override
        public Builder retryStrategy(RetryMode retryMode) {
            Validate.paramNotNull(retryMode, "retryMode");
            config.option(CONFIGURED_RETRY_MODE, retryMode);
            config.option(CONFIGURED_RETRY_CONFIGURATOR, null);
            config.option(CONFIGURED_RETRY_STRATEGY, null);
            config.option(RETRY_POLICY, null);
            return this;
        }

        public void setRetryStrategy(RetryStrategy retryStrategy) {
            retryStrategy(retryStrategy);
        }

        @Override
        public RetryStrategy retryStrategy() {
            RetryStrategy retryStrategy = config.option(CONFIGURED_RETRY_STRATEGY);
            if (retryStrategy != null) {
                return retryStrategy;
            }
            if (config.option(CONFIGURED_RETRY_CONFIGURATOR) != null) {
                return null;
            }
            if (config.option(CONFIGURED_RETRY_MODE) != null) {
                return null;
            }
            return config.option(RETRY_STRATEGY);
        }

        @Override
        public RetryMode retryMode() {
            return config.option(CONFIGURED_RETRY_MODE);
        }

        @Override
        public Consumer<RetryStrategy.Builder<?, ?>> retryStrategyConfigurator() {
            return config.option(CONFIGURED_RETRY_CONFIGURATOR);
        }

        @Override
        public Builder executionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            Validate.paramNotNull(executionInterceptors, "executionInterceptors");
            config.option(EXECUTION_INTERCEPTORS, new ArrayList<>(executionInterceptors));
            return this;
        }

        @Override
        public Builder addExecutionInterceptor(ExecutionInterceptor executionInterceptor) {
            config.computeOptionIfAbsent(EXECUTION_INTERCEPTORS, ArrayList::new).add(executionInterceptor);
            return this;
        }

        public void setExecutionInterceptors(List<ExecutionInterceptor> executionInterceptors) {
            executionInterceptors(executionInterceptors);
        }

        @Override
        public List<ExecutionInterceptor> executionInterceptors() {
            List<ExecutionInterceptor> interceptors = config.option(EXECUTION_INTERCEPTORS);
            return Collections.unmodifiableList(interceptors == null ? emptyList() : interceptors);
        }

        @Override
        public ScheduledExecutorService scheduledExecutorService() {
            // If the client override configuration is accessed from a plugin or a client, we want the actual executor service
            // we're using to be available. For that reason, we should check the SCHEDULED_EXECUTOR_SERVICE.
            ScheduledExecutorService resolvedExecutor = resolvedConfig.option(SCHEDULED_EXECUTOR_SERVICE);
            if (resolvedExecutor != null) {
                return resolvedExecutor;
            }

            // Unwrap the unmanaged executor to preserve read-after-write consistency.
            return unwrapUnmanagedScheduledExecutor(config.option(CONFIGURED_SCHEDULED_EXECUTOR_SERVICE));
        }

        @Override
        public Builder scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            // For read-after-write consistency, just remove the SCHEDULED_EXECUTOR_SERVICE when this is set.
            resolvedConfig.option(SCHEDULED_EXECUTOR_SERVICE, null);
            config.option(CONFIGURED_SCHEDULED_EXECUTOR_SERVICE,
                          unmanagedScheduledExecutor(scheduledExecutorService));
            return this;
        }

        @Override
        public <T> Builder putAdvancedOption(SdkAdvancedClientOption<T> option, T value) {
            config.option(option, value);
            return this;
        }

        @Override
        public Builder advancedOptions(Map<SdkAdvancedClientOption<?>, ?> advancedOptions) {
            SdkAdvancedClientOption.options().forEach(o -> this.config.option(o, null));
            this.config.putAll(advancedOptions);
            return this;
        }

        public void setAdvancedOptions(Map<SdkAdvancedClientOption<?>, Object> advancedOptions) {
            advancedOptions(advancedOptions);
        }

        @Override
        public AttributeMap advancedOptions() {
            AttributeMap.Builder resultBuilder = AttributeMap.builder();
            SdkAdvancedClientOption.options().forEach(o -> setValue(o, resultBuilder));
            return resultBuilder.build();
        }

        @Override
        public Builder apiCallTimeout(Duration apiCallTimeout) {
            config.option(API_CALL_TIMEOUT, apiCallTimeout);
            return this;
        }

        public void setApiCallTimeout(Duration apiCallTimeout) {
            apiCallTimeout(apiCallTimeout);
        }

        @Override
        public Duration apiCallTimeout() {
            return config.option(API_CALL_TIMEOUT);
        }

        @Override
        public Builder apiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            config.option(API_CALL_ATTEMPT_TIMEOUT, apiCallAttemptTimeout);
            return this;
        }

        public void setApiCallAttemptTimeout(Duration apiCallAttemptTimeout) {
            apiCallAttemptTimeout(apiCallAttemptTimeout);
        }

        @Override
        public Duration apiCallAttemptTimeout() {
            return config.option(API_CALL_ATTEMPT_TIMEOUT);
        }

        @Override
        public Builder defaultProfileFileSupplier(Supplier<ProfileFile> defaultProfileFileSupplier) {
            config.option(PROFILE_FILE_SUPPLIER, defaultProfileFileSupplier);
            return this;
        }

        @Override
        public Supplier<ProfileFile> defaultProfileFileSupplier() {
            return config.option(PROFILE_FILE_SUPPLIER);
        }

        @Override
        public ProfileFile defaultProfileFile() {
            Supplier<ProfileFile> supplier = defaultProfileFileSupplier();
            return supplier == null ? null : supplier.get();
        }

        @Override
        public Builder defaultProfileFile(ProfileFile defaultProfileFile) {
            defaultProfileFileSupplier(ProfileFileSupplier.fixedProfileFile(defaultProfileFile));
            return this;
        }

        @Override
        public String defaultProfileName() {
            return config.option(PROFILE_NAME);
        }

        @Override
        public Builder defaultProfileName(String defaultProfileName) {
            config.option(PROFILE_NAME, defaultProfileName);
            return this;
        }

        @Override
        public Builder metricPublishers(List<MetricPublisher> metricPublishers) {
            Validate.paramNotNull(metricPublishers, "metricPublishers");
            config.option(METRIC_PUBLISHERS, new ArrayList<>(metricPublishers));
            return this;
        }

        public void setMetricPublishers(List<MetricPublisher> metricPublishers) {
            metricPublishers(metricPublishers);
        }

        @Override
        public Builder addMetricPublisher(MetricPublisher metricPublisher) {
            Validate.paramNotNull(metricPublisher, "metricPublisher");
            config.computeOptionIfAbsent(METRIC_PUBLISHERS, ArrayList::new).add(metricPublisher);
            return this;
        }

        @Override
        public List<MetricPublisher> metricPublishers() {
            List<MetricPublisher> metricPublishers = config.option(METRIC_PUBLISHERS);
            return Collections.unmodifiableList(metricPublishers == null ? emptyList() : metricPublishers);
        }

        @Override
        public Builder executionAttributes(ExecutionAttributes executionAttributes) {
            Validate.paramNotNull(executionAttributes, "executionAttributes");
            config.option(EXECUTION_ATTRIBUTES, executionAttributes);
            return this;
        }

        @Override
        public <T> Builder putExecutionAttribute(ExecutionAttribute<T> executionAttribute, T value) {
            config.computeOptionIfAbsent(EXECUTION_ATTRIBUTES, ExecutionAttributes::new)
                  .putAttribute(executionAttribute, value);
            return this;
        }

        @Override
        public ExecutionAttributes executionAttributes() {
            ExecutionAttributes attributes = config.option(EXECUTION_ATTRIBUTES);
            return attributes == null ? new ExecutionAttributes() : attributes;
        }

        @Override
        public Builder compressionConfiguration(CompressionConfiguration compressionConfiguration) {
            // For read-after-write consistency, just remove the COMPRESSION_CONFIGURATION when this is set.
            resolvedConfig.option(COMPRESSION_CONFIGURATION, null);
            config.option(CONFIGURED_COMPRESSION_CONFIGURATION, compressionConfiguration);
            return this;
        }

        public void setRequestCompressionEnabled(CompressionConfiguration compressionConfiguration) {
            compressionConfiguration(compressionConfiguration);
        }

        @Override
        public CompressionConfiguration compressionConfiguration() {
            // If the client override configuration is accessed from a plugin or a client, we want the actual configuration
            // we're using to be available. For that reason, we should check the COMPRESSION_CONFIGURATION.
            CompressionConfiguration resolvedCompressionConfig = resolvedConfig.option(COMPRESSION_CONFIGURATION);
            if (resolvedCompressionConfig != null) {
                return resolvedCompressionConfig;
            }
            return config.option(CONFIGURED_COMPRESSION_CONFIGURATION);
        }

        @Override
        public String appId() {
            return config.option(USER_AGENT_APP_ID);
        }

        @Override
        public Builder appId(String appId) {
            config.option(USER_AGENT_APP_ID, appId);
            return this;
        }

        @Override
        public ClientOverrideConfiguration build() {
            return new ClientOverrideConfiguration(config.build(), resolvedConfig.build());
        }

        private Map<String, List<String>> newHeaderMap() {
            return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }

        private <T> void copyValue(ClientOption<T> option,
                                   SdkClientConfiguration.Builder src,
                                   SdkClientConfiguration.Builder dst) {
            T value = src.option(option);
            if (value != null) {
                dst.option(option, value);
            }
        }


        private <T> void setValue(ClientOption<T> option,
                                  AttributeMap.Builder dst) {

            T value = config.option(option);
            if (value != null) {
                dst.put(option, value);
            }
        }
    }
}
