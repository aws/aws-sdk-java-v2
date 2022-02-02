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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A collection of advanced options that can be configured on an async AWS client via
 * {@link ClientAsyncConfiguration.Builder#advancedOption(SdkAdvancedAsyncClientOption,
 * Object)}.
 *
 * <p>These options are usually not required outside of testing or advanced libraries, so most users should not need to configure
 * them.</p>
 *
 * @param <T> The type of value associated with the option.
 */
@SdkPublicApi
public final class SdkAdvancedAsyncClientOption<T> extends ClientOption<T> {
    /**
     * Configure the {@link Executor} that should be used to complete the {@link CompletableFuture} that is returned by the async
     * service client. By default, this is a dedicated, per-client {@link ThreadPoolExecutor} that is managed by the SDK.
     * <p>
     * The configured {@link Executor} will be invoked by the async HTTP client's I/O threads (e.g., EventLoops), which must be
     * reserved for non-blocking behavior. Blocking an I/O thread can cause severe performance degradation, including across
     * multiple clients, as clients are configured, by default, to share a single I/O thread pool (e.g., EventLoopGroup).
     * <p>
     * You should typically only want to customize the future-completion {@link Executor} for a few possible reasons:
     * <ol>
     *     <li>You want more fine-grained control over the {@link ThreadPoolExecutor} used, such as configuring the pool size
     *     or sharing a single pool between multiple clients.
     *     <li>You want to add instrumentation (i.e., metrics) around how the {@link Executor} is used.
     *     <li>You know, for certain, that all of your {@link CompletableFuture} usage is strictly non-blocking, and you wish to
     *     remove the minor overhead incurred by using a separate thread. In this case, you can use
     *     {@code Runnable::run} to execute the future-completion directly from within the I/O thread.
     * </ol>
     */
    public static final SdkAdvancedAsyncClientOption<Executor> FUTURE_COMPLETION_EXECUTOR =
            new SdkAdvancedAsyncClientOption<>(Executor.class);

    private SdkAdvancedAsyncClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
