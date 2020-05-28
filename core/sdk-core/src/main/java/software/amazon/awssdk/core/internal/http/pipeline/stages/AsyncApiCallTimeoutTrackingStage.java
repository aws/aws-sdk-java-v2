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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.resolveTimeoutInMillis;
import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.timeAsyncTaskIfNeeded;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@SdkInternalApi
public class AsyncApiCallTimeoutTrackingStage<OutputT>
    implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> {
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> requestPipeline;
    private final SdkClientConfiguration clientConfig;
    private final ScheduledExecutorService scheduledExecutor;

    public AsyncApiCallTimeoutTrackingStage(HttpClientDependencies dependencies,
                                            RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> requestPipeline) {
        this.requestPipeline = requestPipeline;
        this.scheduledExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.clientConfig = dependencies.clientConfiguration();
    }

    @Override
    public CompletableFuture<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        CompletableFuture<OutputT> future = new CompletableFuture<>();

        long apiCallTimeoutInMillis = resolveTimeoutInMillis(() -> context.requestConfig().apiCallTimeout(),
                                                             clientConfig.option(SdkClientOption.API_CALL_TIMEOUT));

        Supplier<SdkClientException> exceptionSupplier = () -> ApiCallTimeoutException.create(apiCallTimeoutInMillis);
        TimeoutTracker timeoutTracker = timeAsyncTaskIfNeeded(future,
                                                              scheduledExecutor,
                                                              exceptionSupplier,
                                                              apiCallTimeoutInMillis);
        context.apiCallTimeoutTracker(timeoutTracker);

        CompletableFuture<OutputT> executeFuture = requestPipeline.execute(input, context);
        executeFuture.whenComplete((r, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
            } else {
                future.complete(r);
            }
        });

        return CompletableFutureUtils.forwardExceptionTo(future, executeFuture);
    }
}
