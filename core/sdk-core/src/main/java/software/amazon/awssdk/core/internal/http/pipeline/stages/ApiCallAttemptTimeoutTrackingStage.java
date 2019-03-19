/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static software.amazon.awssdk.core.internal.http.timers.TimerUtils.timeSyncTaskIfNeeded;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around a {@link RequestPipeline} to manage the api call attempt timeout feature.
 */
@SdkInternalApi
public final class ApiCallAttemptTimeoutTrackingStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;
    private final Duration apiCallAttemptTimeout;
    private final ScheduledExecutorService timeoutExecutor;

    public ApiCallAttemptTimeoutTrackingStage(HttpClientDependencies dependencies, RequestPipeline<SdkHttpFullRequest,
        Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
        this.timeoutExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.apiCallAttemptTimeout = dependencies.clientConfiguration().option(SdkClientOption.API_CALL_ATTEMPT_TIMEOUT);
    }

    /**
     * Start and end api call attempt timer around the execution of the api call attempt. It's important
     * that the client execution task is canceled before the InterruptedException is handled by
     * {@link ApiCallTimeoutTrackingStage#wrapped#execute(SdkHttpFullRequest)} so the interrupt status doesn't leak out to the
     * callers code.
     */
    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        long timeoutInMillis = resolveTimeoutInMillis(context.requestConfig()::apiCallAttemptTimeout, apiCallAttemptTimeout);

        TimeoutTracker timeoutTracker = timeSyncTaskIfNeeded(timeoutExecutor, timeoutInMillis, Thread.currentThread());

        try {
            context.apiCallAttemptTimeoutTracker(timeoutTracker);
            return wrapped.execute(request, context);
        } finally {
            timeoutTracker.cancel();
        }
    }
}
