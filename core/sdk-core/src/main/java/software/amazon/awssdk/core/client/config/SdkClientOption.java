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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;

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
     * @see ClientOverrideConfiguration#executionInterceptors()
     */
    public static final SdkClientOption<List<ExecutionInterceptor>> EXECUTION_INTERCEPTORS =
            new SdkClientOption<>(new UnsafeValueType(List.class));

    /**
     * @see SdkClientBuilder#endpointOverride(URI)
     */
    public static final SdkClientOption<URI> ENDPOINT = new SdkClientOption<>(URI.class);

    /**
     * Service-specific configuration used by some services, like S3.
     */
    public static final SdkClientOption<ServiceConfiguration> SERVICE_CONFIGURATION =
            new SdkClientOption<>(ServiceConfiguration.class);

    /**
     * Whether to calculate the CRC 32 checksum of a message based on the uncompressed data. By default, this is false.
     */
    @ReviewBeforeRelease("Move this to aws-core module once HttpResponseAdaptingStage is removed")
    public static final SdkClientOption<Boolean> CRC32_FROM_COMPRESSED_DATA_ENABLED =
        new SdkClientOption<>(Boolean.class);

    /**
     * The executor used for scheduling async retry attempts.
     */
    public static final SdkClientOption<ScheduledExecutorService> ASYNC_RETRY_EXECUTOR_SERVICE =
            new SdkClientOption<>(ScheduledExecutorService.class);

    /**
     * The asynchronous HTTP client implementation to make HTTP requests with.
     */
    public static final SdkClientOption<SdkAsyncHttpClient> ASYNC_HTTP_CLIENT =
            new SdkClientOption<>(SdkAsyncHttpClient.class);

    /**
     * The HTTP client implementation to make HTTP requests with.
     */
    public static final SdkClientOption<SdkHttpClient> SYNC_HTTP_CLIENT =
            new SdkClientOption<>(SdkHttpClient.class);

    private SdkClientOption(Class<T> valueClass) {
        super(valueClass);
    }

    private SdkClientOption(UnsafeValueType valueType) {
        super(valueType);
    }
}
