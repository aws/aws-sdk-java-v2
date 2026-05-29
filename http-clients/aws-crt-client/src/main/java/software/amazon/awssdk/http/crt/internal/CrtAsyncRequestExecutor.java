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
import software.amazon.awssdk.http.crt.internal.request.CrtRequestBodyPublisherSubscriber;
import software.amazon.awssdk.http.crt.internal.response.CrtResponseAdapter;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

@SdkInternalApi
public final class CrtAsyncRequestExecutor {

    public CompletableFuture<Void> execute(CrtAsyncRequestContext executionContext) {
        AsyncExecuteRequest asyncRequest = executionContext.sdkRequest();
        CompletableFuture<Void> requestFuture = createAsyncExecutionFuture(asyncRequest);
        ResponseHandlerErrorNotifier errorNotifier = new ResponseHandlerErrorNotifier(asyncRequest.responseHandler());

        try {
            doExecute(executionContext, asyncRequest, requestFuture, errorNotifier);
        } catch (Throwable t) {
            reportAsyncFailure(t, requestFuture, errorNotifier);
        }

        return requestFuture;
    }

    private void doExecute(CrtAsyncRequestContext executionContext,
                           AsyncExecuteRequest asyncRequest,
                           CompletableFuture<Void> requestFuture,
                           ResponseHandlerErrorNotifier errorNotifier) {
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        long acquireStartTime = 0;

        if (shouldPublishMetrics) {
            // go ahead and get acquireStartTime for the concurrency timer as early as possible,
            // so it's as accurate as possible, but only do it in a branch since clock_gettime()
            // results in a full sys call barrier (multiple mutexes and a hw interrupt).
            acquireStartTime = System.nanoTime();
        }

        HttpRequestBase crtRequest = toAsyncCrtRequest(executionContext);

        CompletableFuture<HttpStreamBase> streamFuture = new CompletableFuture<>();
        CrtStreamHandler streamHandler = new CrtStreamHandler(streamFuture);

        HttpStreamBaseResponseHandler crtResponseHandler =
            CrtResponseAdapter.toCrtResponseHandler(requestFuture, asyncRequest.responseHandler(), streamHandler, errorNotifier);

        CrtRequestBodyPublisherSubscriber bodySubscriber =
            new CrtRequestBodyPublisherSubscriber(streamHandler, requestFuture, errorNotifier);

        long finalAcquireStartTime = acquireStartTime;

        executionContext.streamManager().acquireStream(crtRequest, crtResponseHandler, true)
                        .handle((stream, throwable) -> {
                            if (shouldPublishMetrics) {
                                reportMetrics(executionContext.streamManager(), metricCollector, finalAcquireStartTime);
                            }

                            if (throwable != null) {
                                handleAcquireFailure(throwable, streamFuture, requestFuture, errorNotifier);
                                return null;
                            }
                            try {
                                stream.activate();
                            } catch (Throwable t) {
                                // Stream is acquired but not activated and not yet published via
                                // streamFuture. No other path can reach it, so clean it up directly.
                                stream.cancel();
                                stream.close();
                                handleAcquireFailure(t, streamFuture, requestFuture, errorNotifier);
                                return null;
                            }
                            streamFuture.complete(stream);
                            asyncRequest.requestContentPublisher().subscribe(bodySubscriber);
                            return null;
                        }).exceptionally(t -> {
                            // Reached when the handle lambda throws (e.g., publisher.subscribe).
                            // closeConnection is a no-op if the stream isn't in streamFuture yet;
                            // otherwise it tears down the published stream.
                            streamHandler.closeConnection();
                            handleAcquireFailure(t, streamFuture, requestFuture, errorNotifier);
                            return null;
                        });
    }

    private void handleAcquireFailure(Throwable t,
                                      CompletableFuture<HttpStreamBase> streamFuture,
                                      CompletableFuture<Void> requestFuture,
                                      ResponseHandlerErrorNotifier errorNotifier) {
        Throwable toThrow = wrapCrtException(t);
        streamFuture.completeExceptionally(toThrow);
        reportAsyncFailure(toThrow, requestFuture, errorNotifier);
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

    private void reportAsyncFailure(Throwable cause,
                                    CompletableFuture<Void> executeFuture,
                                    ResponseHandlerErrorNotifier errorNotifier) {
        errorNotifier.tryNotifyError(cause);
        executeFuture.completeExceptionally(cause);
    }
}
