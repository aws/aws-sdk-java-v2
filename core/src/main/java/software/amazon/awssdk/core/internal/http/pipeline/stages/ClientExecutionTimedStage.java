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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkInterruptedException;
import software.amazon.awssdk.core.internal.Response;
import software.amazon.awssdk.core.internal.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.timers.client.ClientExecutionAbortTrackerTask;
import software.amazon.awssdk.core.internal.http.timers.client.ClientExecutionTimer;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around a {@link RequestPipeline} to manage the client execution timeout feature.
 */
@SdkInternalApi
public class ClientExecutionTimedStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;
    private final ClientExecutionTimer clientExecutionTimer;

    public ClientExecutionTimedStage(HttpClientDependencies dependencies,
                                     RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
        this.clientExecutionTimer = dependencies.clientExecutionTimer();
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
     * {@link #wrapped#execute(SdkHttpFullRequest)} so the interrupt status doesn't leak out to the callers code
     */
    private Response<OutputT> executeWithTimer(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        ClientExecutionAbortTrackerTask task =
                clientExecutionTimer.startTimer(getClientExecutionTimeoutInMillis(context.requestConfig()));
        try {
            context.clientExecutionTrackerTask(task);
            return wrapped.execute(request, context);
        } finally {
            context.clientExecutionTrackerTask().cancelTask();
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
        if (Thread.currentThread().isInterrupted() && context.clientExecutionTrackerTask().hasTimeoutExpired()) {
            Thread.interrupted();
            return new ClientExecutionTimeoutException();
        }

        return e;
    }

    /**
     * Determine if an interrupted exception is caused by the client execution timer
     * interrupting the current thread or some other task interrupting the thread for another
     * purpose.
     *
     * @return {@link ClientExecutionTimeoutException} if the {@link InterruptedException} was
     * caused by the {@link ClientExecutionTimer}. Otherwise re-interrupts the current thread
     * and returns a {@link SdkClientException} wrapping an {@link InterruptedException}
     */
    private RuntimeException handleInterruptedException(RequestExecutionContext context, InterruptedException e) {
        if (e instanceof SdkInterruptedException) {
            ((SdkInterruptedException) e).getResponseStream().ifPresent(r -> invokeSafely(r::close));
        }
        if (context.clientExecutionTrackerTask().hasTimeoutExpired()) {
            // Clear the interrupt status
            Thread.interrupted();
            return new ClientExecutionTimeoutException();
        } else {
            Thread.currentThread().interrupt();
            return new AbortedException(e);
        }
    }

    /**
     * Gets the correct client execution timeout taking into account precedence of the
     * configuration in {@link SdkRequest} versus {@link SdkClientConfiguration}.
     *
     * @param requestConfig Current request configuration
     * @return Client Execution timeout value or 0 if none is set
     */
    private long getClientExecutionTimeoutInMillis(RequestOverrideConfiguration requestConfig) {
        return 0;
    }
}
