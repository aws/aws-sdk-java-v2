/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.pipeline.stages;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import software.amazon.awssdk.AbortedException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.SdkClientException;
import software.amazon.awssdk.config.ClientConfiguration;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.exception.ClientExecutionTimeoutException;
import software.amazon.awssdk.http.exception.SdkInterruptedException;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionAbortTrackerTask;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer;

/**
 * Wrapper around a {@link RequestPipeline} to manage the client execution timeout feature.
 */
public class ClientExecutionTimedStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;
    private final ClientExecutionTimer clientExecutionTimer;
    private final ClientConfiguration clientConfig;

    public ClientExecutionTimedStage(HttpClientDependencies dependencies,
                                     RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
        this.clientExecutionTimer = dependencies.clientExecutionTimer();
        this.clientConfig = dependencies.clientConfiguration();
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        try {
            return executeWithTimer(request, context);
        } catch (InterruptedException ie) {
            throw handleInterruptedException(context, ie);
        } catch (AbortedException ae) {
            throw handleAbortedException(context, ae);
        }
    }

    /**
     * Start and end client execution timer around the execution of the request. It's important
     * that the client execution task is canceled before the InterruptedExecption is handled by
     * {@link #wrapped#execute(Request)} so the interrupt status doesn't leak out to the callers code
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
     * Determine if an aborted exception is caused by the client execution timer interrupting
     * the current thread. If so throws {@link ClientExecutionTimeoutException} else throws the
     * original {@link AbortedException}
     *
     * @param ae aborted exception that occurred
     * @return {@link ClientExecutionTimeoutException} if the {@link AbortedException} was
     * caused by the {@link ClientExecutionTimer}. Otherwise throws the original {@link AbortedException}
     */
    private RuntimeException handleAbortedException(RequestExecutionContext context, final AbortedException ae) {
        if (context.clientExecutionTrackerTask().hasTimeoutExpired()) {
            return new ClientExecutionTimeoutException();
        } else {
            return ae;
        }
    }

    /**
     * Gets the correct client execution timeout taking into account precedence of the
     * configuration in {@link AmazonWebServiceRequest} versus {@link ClientConfiguration}.
     *
     * @param requestConfig Current request configuration
     * @return Client Execution timeout value or 0 if none is set
     */
    private long getClientExecutionTimeoutInMillis(RequestConfig requestConfig) {
        if (requestConfig.getClientExecutionTimeout() != null) {
            return requestConfig.getClientExecutionTimeout();
        } else if (clientConfig.overrideConfiguration().totalExecutionTimeout() != null) {
            return clientConfig.overrideConfiguration().totalExecutionTimeout().toMillis();
        } else {
            return 0;
        }
    }
}
