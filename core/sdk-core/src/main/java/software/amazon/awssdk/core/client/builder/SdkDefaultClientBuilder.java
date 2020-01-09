/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.client.builder;

import static software.amazon.awssdk.core.ClientType.ASYNC;
import static software.amazon.awssdk.core.ClientType.SYNC;
import static software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_PREFIX;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.USER_AGENT_SUFFIX;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ADDITIONAL_HTTP_HEADERS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_ATTEMPT_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.API_CALL_TIMEOUT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.ASYNC_HTTP_CLIENT;
import static software.amazon.awssdk.core.client.config.SdkClientOption.CRC32_FROM_COMPRESSED_DATA_ENABLED;
import static software.amazon.awssdk.core.client.config.SdkClientOption.EXECUTION_INTERCEPTORS;
import static software.amazon.awssdk.core.client.config.SdkClientOption.RETRY_POLICY;
import static software.amazon.awssdk.core.client.config.SdkClientOption.SCHEDULED_EXECUTOR_SERVICE;
import static software.amazon.awssdk.utils.CollectionUtils.mergeLists;
import static software.amazon.awssdk.utils.Validate.paramNotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.interceptor.ClasspathInterceptorChainFactory;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkAsyncHttpClientBuilder;
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder;
import software.amazon.awssdk.core.internal.util.UserAgentUtils;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.awssdk.utils.Validate;

/**
 * An SDK-internal implementation of the methods in {@link SdkClientBuilder}, {@link SdkAsyncClientBuilder} and
 * {@link SdkSyncClientBuilder}. This implements all methods required by those interfaces, allowing service-specific builders to
 * just implement the configuration they wish to add.
 *
 * <p>By implementing both the sync and async interface's methods, service-specific builders can share code between their sync
 * and
 * async variants without needing one to extend the other. Note: This only defines the methods in the sync and async builder
 * interfaces. It does not implement the interfaces themselves. This is because the sync and async client builder interfaces both
 * require a type-constrained parameter for use in fluent chaining, and a generic type parameter conflict is introduced into the
 * class hierarchy by this interface extending the builder interfaces themselves.</p>
 *
 * <p>Like all {@link SdkClientBuilder}s, this class is not thread safe.</p>
 *
 * @param <B> The type of builder, for chaining.
 * @param <C> The type of client generated by this builder.
 */
@SdkProtectedApi
public abstract class SdkDefaultClientBuilder<B extends SdkClientBuilder<B, C>, C> implements SdkClientBuilder<B, C> {

    private static final SdkHttpClient.Builder DEFAULT_HTTP_CLIENT_BUILDER = new DefaultSdkHttpClientBuilder();
    private static final SdkAsyncHttpClient.Builder DEFAULT_ASYNC_HTTP_CLIENT_BUILDER = new DefaultSdkAsyncHttpClientBuilder();

    protected final SdkClientConfiguration.Builder clientConfiguration = SdkClientConfiguration.builder();

    private final SdkHttpClient.Builder defaultHttpClientBuilder;
    private final SdkAsyncHttpClient.Builder defaultAsyncHttpClientBuilder;

    private SdkHttpClient.Builder httpClientBuilder;
    private SdkAsyncHttpClient.Builder asyncHttpClientBuilder;

    protected SdkDefaultClientBuilder() {
        this(DEFAULT_HTTP_CLIENT_BUILDER, DEFAULT_ASYNC_HTTP_CLIENT_BUILDER);
    }

    @SdkTestInternalApi
    protected SdkDefaultClientBuilder(SdkHttpClient.Builder defaultHttpClientBuilder,
                                      SdkAsyncHttpClient.Builder defaultAsyncHttpClientBuilder) {
        this.defaultHttpClientBuilder = defaultHttpClientBuilder;
        this.defaultAsyncHttpClientBuilder = defaultAsyncHttpClientBuilder;
    }

    /**
     * Build a client using the current state of this builder. This is marked final in order to allow this class to add standard
     * "build" logic between all service clients. Service clients are expected to implement the {@link #buildClient} method, that
     * accepts the immutable client configuration generated by this build method.
     */
    public final C build() {
        return buildClient();
    }

    /**
     * Implemented by child classes to create a client using the provided immutable configuration objects. The async and sync
     * configurations are not yet immutable. Child classes will need to make them immutable in order to validate them and pass
     * them to the client's constructor.
     *
     * @return A client based on the provided configuration.
     */
    protected abstract C buildClient();

    /**
     * Return a client configuration object, populated with the following chain of priorities.
     * <ol>
     * <li>Customer Configuration</li>
     * <li>Service-Specific Defaults</li>
     * <li>Global Defaults</li>
     * </ol>
     */
    protected final SdkClientConfiguration syncClientConfiguration() {
        SdkClientConfiguration configuration = clientConfiguration.build();

        // Apply defaults
        configuration = mergeChildDefaults(configuration);
        configuration = mergeGlobalDefaults(configuration);

        // Create additional configuration from the default-applied configuration
        configuration = finalizeChildConfiguration(configuration);
        configuration = finalizeSyncConfiguration(configuration);
        configuration = finalizeConfiguration(configuration);

        return configuration;
    }

    /**
     * Return a client configuration object, populated with the following chain of priorities.
     * <ol>
     * <li>Customer Configuration</li>
     * <li>Implementation/Service-Specific Configuration</li>
     * <li>Global Default Configuration</li>
     * </ol>
     */
    protected final SdkClientConfiguration asyncClientConfiguration() {
        SdkClientConfiguration configuration = clientConfiguration.build();

        // Apply defaults
        configuration = mergeChildDefaults(configuration);
        configuration = mergeGlobalDefaults(configuration);

        // Create additional configuration from the default-applied configuration
        configuration = finalizeChildConfiguration(configuration);
        configuration = finalizeAsyncConfiguration(configuration);
        configuration = finalizeConfiguration(configuration);

        return configuration;
    }

    /**
     * Optionally overridden by child implementations to apply implementation-specific default configuration.
     * (eg. AWS's default credentials providers)
     */
    protected SdkClientConfiguration mergeChildDefaults(SdkClientConfiguration configuration) {
        return configuration;
    }

    /**
     * Apply global default configuration
     */
    private SdkClientConfiguration mergeGlobalDefaults(SdkClientConfiguration configuration) {
        return configuration.merge(c -> c.option(EXECUTION_INTERCEPTORS, new ArrayList<>())
                                         .option(ADDITIONAL_HTTP_HEADERS, new LinkedHashMap<>())
                                         .option(RETRY_POLICY, RetryPolicy.defaultRetryPolicy())
                                         .option(USER_AGENT_PREFIX, UserAgentUtils.getUserAgent())
                                         .option(USER_AGENT_SUFFIX, "")
                                         .option(CRC32_FROM_COMPRESSED_DATA_ENABLED, false));
    }

    /**
     * Optionally overridden by child implementations to derive implementation-specific configuration from the
     * default-applied configuration. (eg. AWS's endpoint, derived from the region).
     */
    protected SdkClientConfiguration finalizeChildConfiguration(SdkClientConfiguration configuration) {
        return configuration;
    }

    /**
     * Finalize sync-specific configuration from the default-applied configuration.
     */
    private SdkClientConfiguration finalizeSyncConfiguration(SdkClientConfiguration config) {
        return config.toBuilder()
                     .option(SdkClientOption.SYNC_HTTP_CLIENT, resolveSyncHttpClient(config))
                     .option(SdkClientOption.CLIENT_TYPE, SYNC)
                     .build();
    }

    /**
     * Finalize async-specific configuration from the default-applied configuration.
     */
    private SdkClientConfiguration finalizeAsyncConfiguration(SdkClientConfiguration config) {
        return config.toBuilder()
                     .option(FUTURE_COMPLETION_EXECUTOR, resolveAsyncFutureCompletionExecutor(config))
                     .option(ASYNC_HTTP_CLIENT, resolveAsyncHttpClient(config))
                     .option(SdkClientOption.CLIENT_TYPE, ASYNC)
                     .build();
    }

    /**
     * Finalize global configuration from the default-applied configuration.
     */
    private SdkClientConfiguration finalizeConfiguration(SdkClientConfiguration config) {
        return config.toBuilder()
                     .option(SCHEDULED_EXECUTOR_SERVICE, resolveScheduledExecutorService())
                     .option(EXECUTION_INTERCEPTORS, resolveExecutionInterceptors(config))
                     .build();
    }

    /**
     * Finalize which sync HTTP client will be used for the created client.
     */
    private SdkHttpClient resolveSyncHttpClient(SdkClientConfiguration config) {
        Validate.isTrue(config.option(SdkClientOption.SYNC_HTTP_CLIENT) == null || httpClientBuilder == null,
                        "The httpClient and the httpClientBuilder can't both be configured.");

        return Either.fromNullable(config.option(SdkClientOption.SYNC_HTTP_CLIENT), httpClientBuilder)
                     .map(e -> e.map(NonManagedSdkHttpClient::new, b -> b.buildWithDefaults(childHttpConfig())))
                     .orElseGet(() -> defaultHttpClientBuilder.buildWithDefaults(childHttpConfig()));
    }

    /**
     * Finalize which async HTTP client will be used for the created client.
     */
    private SdkAsyncHttpClient resolveAsyncHttpClient(SdkClientConfiguration config) {
        Validate.isTrue(config.option(ASYNC_HTTP_CLIENT) == null || asyncHttpClientBuilder == null,
                        "The asyncHttpClient and the asyncHttpClientBuilder can't both be configured.");
        return Either.fromNullable(config.option(ASYNC_HTTP_CLIENT), asyncHttpClientBuilder)
                     .map(e -> e.map(NonManagedSdkAsyncHttpClient::new, b -> b.buildWithDefaults(childHttpConfig())))
                     .orElseGet(() -> defaultAsyncHttpClientBuilder.buildWithDefaults(childHttpConfig()));
    }

    /**
     * Optionally overridden by child implementations to provide implementation-specific default HTTP configuration.
     */
    protected AttributeMap childHttpConfig() {
        return AttributeMap.empty();
    }

    /**
     * Finalize which async executor service will be used for the created client. The default async executor
     * service has at least 8 core threads and can scale up to at least 64 threads when needed depending
     * on the number of processors available.
     */
    private Executor resolveAsyncFutureCompletionExecutor(SdkClientConfiguration config) {
        Supplier<Executor> defaultExecutor = () -> {
            int processors = Runtime.getRuntime().availableProcessors();
            int corePoolSize = Math.max(8, processors);
            int maxPoolSize = Math.max(64, processors * 2);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                                                                 10, TimeUnit.SECONDS,
                                                                 new LinkedBlockingQueue<>(1_000),
                                                                 new ThreadFactoryBuilder()
                                                                     .threadNamePrefix("sdk-async-response").build());
            // Allow idle core threads to time out
            executor.allowCoreThreadTimeOut(true);
            return executor;
        };

        return Optional.ofNullable(config.option(FUTURE_COMPLETION_EXECUTOR))
                       .orElseGet(defaultExecutor);
    }

    /**
     * Finalize the internal SDK scheduled executor service that is used for scheduling tasks such
     * as async retry attempts and timeout task.
     */
    private ScheduledExecutorService resolveScheduledExecutorService() {
        return Executors.newScheduledThreadPool(5, new ThreadFactoryBuilder()
            .threadNamePrefix("sdk-ScheduledExecutor").build());
    }

    /**
     * Finalize which execution interceptors will be used for the created client.
     */
    private List<ExecutionInterceptor> resolveExecutionInterceptors(SdkClientConfiguration config) {
        List<ExecutionInterceptor> globalInterceptors = new ClasspathInterceptorChainFactory().getGlobalInterceptors();
        return mergeLists(globalInterceptors, config.option(EXECUTION_INTERCEPTORS));
    }

    @Override
    public final B endpointOverride(URI endpointOverride) {
        Validate.paramNotNull(endpointOverride, "endpointOverride");
        Validate.paramNotNull(endpointOverride.getScheme(), "The URI scheme of endpointOverride");
        clientConfiguration.option(SdkClientOption.ENDPOINT, endpointOverride);
        clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN, true);
        return thisBuilder();
    }

    public final void setEndpointOverride(URI endpointOverride) {
        endpointOverride(endpointOverride);
    }

    public final B asyncConfiguration(ClientAsyncConfiguration asyncConfiguration) {
        clientConfiguration.option(FUTURE_COMPLETION_EXECUTOR, asyncConfiguration.advancedOption(FUTURE_COMPLETION_EXECUTOR));
        return thisBuilder();
    }

    public final void setAsyncConfiguration(ClientAsyncConfiguration asyncConfiguration) {
        asyncConfiguration(asyncConfiguration);
    }

    @Override
    public final B overrideConfiguration(ClientOverrideConfiguration overrideConfig) {
        clientConfiguration.option(EXECUTION_INTERCEPTORS, overrideConfig.executionInterceptors());
        clientConfiguration.option(RETRY_POLICY, overrideConfig.retryPolicy().orElse(null));
        clientConfiguration.option(ADDITIONAL_HTTP_HEADERS, overrideConfig.headers());
        clientConfiguration.option(SIGNER, overrideConfig.advancedOption(SIGNER).orElse(null));
        clientConfiguration.option(USER_AGENT_SUFFIX, overrideConfig.advancedOption(USER_AGENT_SUFFIX).orElse(null));
        clientConfiguration.option(USER_AGENT_PREFIX, overrideConfig.advancedOption(USER_AGENT_PREFIX).orElse(null));
        clientConfiguration.option(API_CALL_TIMEOUT, overrideConfig.apiCallTimeout().orElse(null));
        clientConfiguration.option(API_CALL_ATTEMPT_TIMEOUT, overrideConfig.apiCallAttemptTimeout().orElse(null));
        clientConfiguration.option(DISABLE_HOST_PREFIX_INJECTION,
                                   overrideConfig.advancedOption(DISABLE_HOST_PREFIX_INJECTION).orElse(null));
        return thisBuilder();
    }

    public final void setOverrideConfiguration(ClientOverrideConfiguration overrideConfiguration) {
        overrideConfiguration(overrideConfiguration);
    }

    public final B httpClient(SdkHttpClient httpClient) {
        clientConfiguration.option(SdkClientOption.SYNC_HTTP_CLIENT, httpClient);
        return thisBuilder();
    }

    public final B httpClientBuilder(SdkHttpClient.Builder httpClientBuilder) {
        this.httpClientBuilder = httpClientBuilder;
        return thisBuilder();
    }

    public final B httpClient(SdkAsyncHttpClient httpClient) {
        clientConfiguration.option(ASYNC_HTTP_CLIENT, httpClient);
        return thisBuilder();
    }

    public final B httpClientBuilder(SdkAsyncHttpClient.Builder httpClientBuilder) {
        this.asyncHttpClientBuilder = httpClientBuilder;
        return thisBuilder();
    }

    /**
     * Return "this" for method chaining.
     */
    @SuppressWarnings("unchecked")
    protected B thisBuilder() {
        return (B) this;
    }

    /**
     * Wrapper around {@link SdkHttpClient} to prevent it from being closed. Used when the customer provides
     * an already built client in which case they are responsible for the lifecycle of it.
     */
    @SdkTestInternalApi
    public static final class NonManagedSdkHttpClient implements SdkHttpClient {

        private final SdkHttpClient delegate;

        private NonManagedSdkHttpClient(SdkHttpClient delegate) {
            this.delegate = paramNotNull(delegate, "SdkHttpClient");
        }

        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return delegate.prepareRequest(request);
        }

        @Override
        public void close() {
            // Do nothing, this client is managed by the customer.
        }

        @Override
        public String clientName() {
            return delegate.clientName();
        }
    }

    /**
     * Wrapper around {@link SdkAsyncHttpClient} to prevent it from being closed. Used when the customer provides
     * an already built client in which case they are responsible for the lifecycle of it.
     */
    @SdkTestInternalApi
    public static final class NonManagedSdkAsyncHttpClient implements SdkAsyncHttpClient {

        private final SdkAsyncHttpClient delegate;

        NonManagedSdkAsyncHttpClient(SdkAsyncHttpClient delegate) {
            this.delegate = paramNotNull(delegate, "SdkAsyncHttpClient");
        }

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest request) {
            return delegate.execute(request);
        }

        @Override
        public void close() {
            // Do nothing, this client is managed by the customer.
        }
    }


}
