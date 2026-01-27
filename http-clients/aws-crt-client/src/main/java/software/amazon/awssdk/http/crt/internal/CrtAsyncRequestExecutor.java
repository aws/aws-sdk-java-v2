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

package software.amazon.awssdk.http.crt.internal;

import static software.amazon.awssdk.http.crt.internal.CrtUtils.reportMetrics;
import static software.amazon.awssdk.http.crt.internal.CrtUtils.wrapCrtException;
import static software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter.toAsyncCrtRequest;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBase;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.crt.http.HttpStreamBaseResponseHandler;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.internal.response.CrtResponseAdapter;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class CrtAsyncRequestExecutor {

    private static final Logger log = Logger.loggerFor(CrtAsyncRequestExecutor.class);

    public CompletableFuture<Void> execute(CrtAsyncRequestContext executionContext) {
        AsyncExecuteRequest asyncRequest = executionContext.sdkRequest();
        CompletableFuture<Void> requestFuture = createAsyncExecutionFuture(asyncRequest);

        try {
            doExecute(executionContext, asyncRequest, requestFuture);
        } catch (Throwable t) {
            reportAsyncFailure(t, requestFuture, asyncRequest.responseHandler());
        }

        return requestFuture;
    }

    private void doExecute(CrtAsyncRequestContext executionContext,
                           AsyncExecuteRequest asyncRequest,
                           CompletableFuture<Void> requestFuture) {
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        long acquireStartTime = 0;

        if (shouldPublishMetrics) {
            acquireStartTime = System.nanoTime();
        }

        HttpRequestBase crtRequest = toAsyncCrtRequest(executionContext);

        HttpStreamBaseResponseHandler crtResponseHandler =
            CrtResponseAdapter.toCrtResponseHandler(requestFuture, asyncRequest.responseHandler());

        CompletableFuture<HttpStreamBase> streamFuture =
            executionContext.crtConnPool().acquireStream(crtRequest, crtResponseHandler);

        long finalAcquireStartTime = acquireStartTime;

        streamFuture.whenComplete((stream, throwable) -> {
            if (shouldPublishMetrics) {
                reportMetrics(executionContext.crtConnPool(), metricCollector, finalAcquireStartTime);
            }

            if (throwable != null) {
                Throwable toThrow = wrapCrtException(throwable);
                reportAsyncFailure(toThrow, requestFuture, asyncRequest.responseHandler());
            }
        });
    }

    /**
     * Create the execution future and set up the cancellation logic.
     *
     * @return The created execution future.
     */
    private CompletableFuture<Void> createAsyncExecutionFuture(AsyncExecuteRequest request) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        future.whenComplete((r, t) -> {
            if (t == null) {
                return;
            }

            // TODO: Aborting request once it's supported in CRT
            if (future.isCancelled()) {
                request.responseHandler().onError(new SdkCancellationException("The request was cancelled"));
            }
        });

        return future;
    }

    /**
     * Notify the provided response handler and future of the failure.
     */
    private void reportAsyncFailure(Throwable cause,
                                    CompletableFuture<Void> executeFuture,
                                    SdkAsyncHttpResponseHandler responseHandler) {
        try {
            responseHandler.onError(cause);
        } catch (Exception e) {
            log.error(() -> "SdkAsyncHttpResponseHandler " + responseHandler + " threw an exception in onError. It will be "
                            + "ignored.", e);
        }
        executeFuture.completeExceptionally(cause);
    }
}
