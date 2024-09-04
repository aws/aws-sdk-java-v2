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

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.CompressionConfiguration;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * A set of internal options required by the SDK via {@link SdkClientConfiguration}.
 */
@SdkProtectedApi
public final class SdkClientOption<T> extends ClientOption<T> {
    /**
     * @see ClientOverrideConfiguration#headers()
     */
    public static final SdkClientOption<Map<String, List<String>>> ADDITIONAL_HTTP_HEADERS =
            new SdkClientOption<>(new UnsafeValueType(Map.class));

    /**
     * @see ClientOverrideConfiguration#retryPolicy()
     */
    public static final SdkClientOption<RetryPolicy> RETRY_POLICY = new SdkClientOption<>(RetryPolicy.class);

    /**
     * @see ClientOverrideConfiguration#retryStrategy()
     */
    public static final SdkClientOption<RetryStrategy> RETRY_STRATEGY = new SdkClientOption<>(RetryStrategy.class);

    /**
     * The retry strategy set by the customer using {@link ClientOverrideConfiguration.Builder#retryStrategy(RetryStrategy)}. This
     * is likely only useful within configuration classes, and will be converted into a {@link #RETRY_STRATEGY} for the SDK's
     * runtime.
     *
     * @see ClientOverrideConfiguration#retryMode()
     */
    public static final SdkClientOption<RetryStrategy> CONFIGURED_RETRY_STRATEGY = new SdkClientOption<>(RetryStrategy.class);

    /**
     * The retry mode set by the customer using {@link ClientOverrideConfiguration.Builder#retryStrategy(RetryMode)}. This is
     * likely only useful within configuration classes, and will be converted into a {@link #RETRY_STRATEGY} for the SDK's
     * runtime.
     *
     * @see ClientOverrideConfiguration#retryMode()
     */
    public static final SdkClientOption<RetryMode> CONFIGURED_RETRY_MODE = new SdkClientOption<>(RetryMode.class);

    /**
     * The retry strategy builder consumer set by the customer using
     * {@link ClientOverrideConfiguration.Builder#retryStrategy(Consumer<RetryStrategy.Builder>)}. This is likely only useful
     * within configuration classes, and will be converted into a {@link #RETRY_STRATEGY} for the SDK's runtime.
     *
     * @see ClientOverrideConfiguration#retryStrategy()
     */
    public static final SdkClientOption<Consumer<RetryStrategy.Builder<?, ?>>> CONFIGURED_RETRY_CONFIGURATOR =
        new SdkClientOption<>(new UnsafeValueType(RetryStrategy.Builder.class));

    /**
     * @see ClientOverrideConfiguration#executionInterceptors()
     */
    public static final SdkClientOption<List<ExecutionInterceptor>> EXECUTION_INTERCEPTORS =
            new SdkClientOption<>(new UnsafeValueType(List.class));

    /**
     * The effective endpoint the client is configured to make requests to. If the client has been configured with
     * an endpoint override then this value will be the provided endpoint value.
     *
     * @deprecated Use {@link #CLIENT_ENDPOINT_PROVIDER} for client-level endpoint configuration, or
     * {@link #ENDPOINT_PROVIDER} for deriving the request-level endpoint.
     */
    @Deprecated
    public static final SdkClientOption<URI> ENDPOINT = new SdkClientOption<>(URI.class);

    /**
     * A flag that when set to true indicates the endpoint stored in {@link SdkClientOption#ENDPOINT} was a customer
     * supplied value and not generated by the client based on Region metadata.
     *
     * @deprecated Use {@link #CLIENT_ENDPOINT_PROVIDER}'s {@link ClientEndpointProvider#isEndpointOverridden()}.
     */
    @Deprecated
    public static final SdkClientOption<Boolean> ENDPOINT_OVERRIDDEN = new SdkClientOption<>(Boolean.class);

    /**
     * A provider for the client-level endpoint configuration. This includes the default endpoint determined by the
     * endpoint metadata or endpoint overrides specified by the customer.
     * <p>
     * {@link #ENDPOINT_PROVIDER} determines the request-level endpoint configuration.
     */
    public static final SdkClientOption<ClientEndpointProvider> CLIENT_ENDPOINT_PROVIDER =
        new SdkClientOption<>(ClientEndpointProvider.class);

    /**
     * Service-specific configuration used by some services, like S3.
     */
    public static final SdkClientOption<ServiceConfiguration> SERVICE_CONFIGURATION =
            new SdkClientOption<>(ServiceConfiguration.class);

    /**
     * Whether to calculate the CRC 32 checksum of a message based on the uncompressed data. By default, this is false.
     */
    public static final SdkClientOption<Boolean> CRC32_FROM_COMPRESSED_DATA_ENABLED =
        new SdkClientOption<>(Boolean.class);

    /**
     * The internal SDK scheduled executor service that is used for scheduling tasks such as async retry attempts
     * and timeout task.
     */
    public static final SdkClientOption<ScheduledExecutorService> SCHEDULED_EXECUTOR_SERVICE =
            new SdkClientOption<>(ScheduledExecutorService.class);

    /**
     * The internal SDK scheduled executor service that is set by the customer. This is likely only useful within configuration
     * classes, and will be converted into a {@link #SCHEDULED_EXECUTOR_SERVICE} for the SDK's runtime.
     */
    public static final SdkClientOption<ScheduledExecutorService> CONFIGURED_SCHEDULED_EXECUTOR_SERVICE =
        new SdkClientOption<>(ScheduledExecutorService.class);

    /**
     * The asynchronous HTTP client implementation to make HTTP requests with.
     */
    public static final SdkClientOption<SdkAsyncHttpClient> ASYNC_HTTP_CLIENT =
            new SdkClientOption<>(SdkAsyncHttpClient.class);

    /**
     * An asynchronous HTTP client set by the customer. This is likely only useful within configuration classes, and
     * will be converted into a {@link #ASYNC_HTTP_CLIENT} for the SDK's runtime.
     */
    public static final SdkClientOption<SdkAsyncHttpClient> CONFIGURED_ASYNC_HTTP_CLIENT =
        new SdkClientOption<>(SdkAsyncHttpClient.class);

    /**
     * An asynchronous HTTP client builder set by the customer. This is likely only useful within configuration classes, and
     * will be converted into a {@link #ASYNC_HTTP_CLIENT} for the SDK's runtime.
     */
    public static final SdkClientOption<SdkAsyncHttpClient.Builder<?>> CONFIGURED_ASYNC_HTTP_CLIENT_BUILDER =
        new SdkClientOption<>(new UnsafeValueType(SdkAsyncHttpClient.Builder.class));

    /**
     * The HTTP client implementation to make HTTP requests with.
     */
    public static final SdkClientOption<SdkHttpClient> SYNC_HTTP_CLIENT =
            new SdkClientOption<>(SdkHttpClient.class);

    /**
     * An HTTP client set by the customer. This is likely only useful within configuration classes, and
     * will be converted into a {@link #SYNC_HTTP_CLIENT} for the SDK's runtime.
     */
    public static final SdkClientOption<SdkHttpClient> CONFIGURED_SYNC_HTTP_CLIENT =
        new SdkClientOption<>(SdkHttpClient.class);

    /**
     * An HTTP client builder set by the customer. This is likely only useful within configuration classes, and
     * will be converted into a {@link #SYNC_HTTP_CLIENT} for the SDK's runtime.
     */
    public static final SdkClientOption<SdkHttpClient.Builder<?>> CONFIGURED_SYNC_HTTP_CLIENT_BUILDER =
        new SdkClientOption<>(new UnsafeValueType(SdkAsyncHttpClient.Builder.class));

    /**
     * Configuration that should be used to build the {@link #SYNC_HTTP_CLIENT} or {@link #ASYNC_HTTP_CLIENT}.
     */
    public static final SdkClientOption<AttributeMap> HTTP_CLIENT_CONFIG = new SdkClientOption<>(AttributeMap.class);

    /**
     * The type of client used to make requests.
     */
    public static final SdkClientOption<ClientType> CLIENT_TYPE = new SdkClientOption<>(ClientType.class);

    /**
     * @see ClientOverrideConfiguration#apiCallAttemptTimeout()
     */
    public static final SdkClientOption<Duration> API_CALL_ATTEMPT_TIMEOUT = new SdkClientOption<>(Duration.class);

    /**
     * @see ClientOverrideConfiguration#apiCallTimeout()
     */
    public static final SdkClientOption<Duration> API_CALL_TIMEOUT = new SdkClientOption<>(Duration.class);

    /**
     * Descriptive name for the service. Used primarily for metrics and also in metadata like AwsErrorDetails.
     */
    public static final SdkClientOption<String> SERVICE_NAME = new SdkClientOption<>(String.class);

    /**
     * Whether or not endpoint discovery is enabled for this client.
     */
    public static final SdkClientOption<Boolean> ENDPOINT_DISCOVERY_ENABLED = new SdkClientOption<>(Boolean.class);

    /**
     * The profile file to use for this client.
     *
     * @deprecated This option was used to:
     *             - Read configuration options in profile files in aws-core, sdk-core
     *             - Build service configuration objects from profile files in codegen, s3control
     *             - Build service configuration objects from profile files, set endpoint options in s3
     *             - Set retry mode in dynamodb, kinesis
     * This has been replaced with {@code PROFILE_FILE_SUPPLIER.get()}.
     */
    @Deprecated
    public static final SdkClientOption<ProfileFile> PROFILE_FILE = new SdkClientOption<>(ProfileFile.class);

    /**
     * The profile file supplier to use for this client.
     */
    public static final SdkClientOption<Supplier<ProfileFile>> PROFILE_FILE_SUPPLIER =
        new SdkClientOption<>(new UnsafeValueType(Supplier.class));

    /**
     * The profile name to use for this client.
     */
    public static final SdkClientOption<String> PROFILE_NAME = new SdkClientOption<>(String.class);

    public static final SdkClientOption<List<MetricPublisher>> METRIC_PUBLISHERS =
            new SdkClientOption<>(new UnsafeValueType(List.class));

    /**
     * Option to specify if the default signer has been overridden on the client.
     */
    public static final SdkClientOption<Boolean> SIGNER_OVERRIDDEN = new SdkClientOption<>(Boolean.class);

    /**
     * Option to specify additional execution attributes to each client call.
     */
    public static final SdkClientOption<ExecutionAttributes> EXECUTION_ATTRIBUTES =
            new SdkClientOption<>(new UnsafeValueType(ExecutionAttributes.class));
    /**
     * Option to specify the internal user agent.
     */
    public static final SdkClientOption<String> INTERNAL_USER_AGENT = new SdkClientOption<>(String.class);

    /**
     * A user agent prefix that is specific to the client (agnostic of the request).
     */
    public static final SdkClientOption<String> CLIENT_USER_AGENT = new SdkClientOption<>(String.class);

    /**
     * Option to specify the default retry mode.
     *
     * @see RetryMode.Resolver#defaultRetryMode(RetryMode)
     */
    public static final SdkClientOption<RetryMode> DEFAULT_RETRY_MODE = new SdkClientOption<>(RetryMode.class);

    /**
     * The {@link EndpointProvider} configured on the client.
     */
    public static final SdkClientOption<EndpointProvider> ENDPOINT_PROVIDER = new SdkClientOption<>(EndpointProvider.class);

    /**
     * The {@link AuthSchemeProvider} configured on the client.
     */
    public static final SdkClientOption<AuthSchemeProvider> AUTH_SCHEME_PROVIDER =
        new SdkClientOption<>(AuthSchemeProvider.class);

    /**
     * The {@link AuthScheme}s configured on the client.
     */
    public static final SdkClientOption<Map<String, AuthScheme<?>>> AUTH_SCHEMES =
        new SdkClientOption<>(new UnsafeValueType(Map.class));

    /**
     * The IdentityProviders configured on the client.
     */
    public static final SdkClientOption<IdentityProviders> IDENTITY_PROVIDERS = new SdkClientOption<>(IdentityProviders.class);

    /**
     * The container for any client contexts parameters set on the client.
     */
    public static final SdkClientOption<AttributeMap> CLIENT_CONTEXT_PARAMS =
        new SdkClientOption<>(AttributeMap.class);

    /**
     * Configuration of the COMPRESSION_CONFIGURATION. Unlike {@link #COMPRESSION_CONFIGURATION}, this may contain null values.
     */
    public static final SdkClientOption<CompressionConfiguration> CONFIGURED_COMPRESSION_CONFIGURATION =
        new SdkClientOption<>(CompressionConfiguration.class);

    /**
     * Option used by the rest of the SDK to read the {@link CompressionConfiguration}. This will never contain null values.
     */
    public static final SdkClientOption<CompressionConfiguration> COMPRESSION_CONFIGURATION =
        new SdkClientOption<>(CompressionConfiguration.class);

    /**
     * Option to specify a reference to the SDK client in use.
     */
    public static final SdkClientOption<SdkClient> SDK_CLIENT = new SdkClientOption<>(SdkClient.class);

    private SdkClientOption(Class<T> valueClass) {
        super(valueClass);
    }

    private SdkClientOption(UnsafeValueType valueType) {
        super(valueType);
    }
}
