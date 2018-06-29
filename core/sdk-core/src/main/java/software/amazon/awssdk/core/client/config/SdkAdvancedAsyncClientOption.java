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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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
     * Configure the executor that should be used to complete the {@link CompletableFuture} that is returned by the service
     * clients. By default, this is an the {@link ExecutorService} managed by the SDK. {@link Executor#execute(Runnable)} is
     * invoked by the async HTTP client's thread, so {@code Runnable::run} will complete the future on a non-blocking async
     * thread.
     */
    public static final SdkAdvancedAsyncClientOption<Executor> FUTURE_COMPLETION_EXECUTOR =
            new SdkAdvancedAsyncClientOption<>(Executor.class);

    private SdkAdvancedAsyncClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
