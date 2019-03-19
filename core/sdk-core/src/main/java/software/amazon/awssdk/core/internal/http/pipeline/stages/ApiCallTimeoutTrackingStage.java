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
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkInterruptedException;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.timers.SyncTimeoutTask;
import software.amazon.awssdk.core.internal.http.timers.TimeoutTracker;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around a {@link RequestPipeline} to manage the api call timeout feature.
 */
@SdkInternalApi
public final class ApiCallTimeoutTrackingStage<OutputT> implements RequestToResponsePipeline<OutputT> {
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;
    private final SdkClientConfiguration clientConfig;
    private final ScheduledExecutorService timeoutExecutor;
    private final Duration apiCallTimeout;

    public ApiCallTimeoutTrackingStage(HttpClientDependencies dependencies,
                                       RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
        this.clientConfig = dependencies.clientConfiguration();
        this.timeoutExecutor = dependencies.clientConfiguration().option(SdkClientOption.SCHEDULED_EXECUTOR_SERVICE);
        this.apiCallTimeout = clientConfig.option(SdkClientOption.API_CALL_TIMEOUT);
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        try {
            return executeWithTimer(request, context);
        } catch (Exception e) {
            throw translatePipelineException(context, e);
        }
    }

    /**
     * Start and end client execution timer around the execution of the request. It's important
     * that the client execution task is canceled before the InterruptedException is handled by
     * {@link ApiCallTimeoutTrackingStage#wrapped#execute(SdkHttpFullRequest)} so the interrupt status
     * doesn't leak out to the callers code
     */
    private Response<OutputT> executeWithTimer(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {

        long timeoutInMillis = resolveTimeoutInMillis(context.requestConfig()::apiCallTimeout, apiCallTimeout);

        TimeoutTracker timeoutTracker = timeSyncTaskIfNeeded(timeoutExecutor, timeoutInMillis, Thread.currentThread());

        try {
            context.apiCallTimeoutTracker(timeoutTracker);
            return wrapped.execute(request, context);
        } finally {
            timeoutTracker.cancel();
        }
    }

    /**
     * Take the given exception thrown from the wrapped pipeline and return a more appropriate
     * timeout related exception based on its type and the the execution status.
     *
     * @param context The execution context.
     * @param e The exception thrown from the inner pipeline.
     * @return The translated exception.
     */
    private Exception translatePipelineException(RequestExecutionContext context, Exception e) {
        if (e instanceof InterruptedException) {
            return handleInterruptedException(context, (InterruptedException) e);
        }

        // InterruptedException was not rethrown and instead the interrupted flag was set
        if (Thread.currentThread().isInterrupted() && context.apiCallTimeoutTracker().hasExecuted()) {
            Thread.interrupted();
            return generateApiCallTimeoutException(context);
        }

        return e;
    }

    /**
     * Determine if an interrupted exception is caused by the api call timeout task
     * interrupting the current thread or some other task interrupting the thread for another
     * purpose.
     *
     * @return {@link ApiCallTimeoutException} if the {@link InterruptedException} was
     * caused by the {@link SyncTimeoutTask}. Otherwise re-interrupts the current thread
     * and returns a {@link AbortedException} wrapping an {@link InterruptedException}
     */
    private RuntimeException handleInterruptedException(RequestExecutionContext context, InterruptedException e) {
        if (e instanceof SdkInterruptedException) {
            ((SdkInterruptedException) e).getResponseStream().ifPresent(r -> invokeSafely(r::close));
        }
        if (context.apiCallTimeoutTracker().hasExecuted()) {
            // Clear the interrupt status
            Thread.interrupted();
            return generateApiCallTimeoutException(context);
        }

        Thread.currentThread().interrupt();
        return AbortedException.create("Thread was interrupted", e);
    }

    private ApiCallTimeoutException generateApiCallTimeoutException(RequestExecutionContext context) {
        return ApiCallTimeoutException.create(
            resolveTimeoutInMillis(context.requestConfig()::apiCallTimeout, apiCallTimeout));
    }
}
