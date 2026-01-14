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
import static software.amazon.awssdk.http.crt.internal.CrtUtils.wrapConnectionFailureException;
import static software.amazon.awssdk.http.crt.internal.CrtUtils.wrapWithIoExceptionIfRetryable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.http.HttpClientConnection;
import software.amazon.awssdk.crt.http.HttpException;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpStreamResponseHandler;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.response.CrtResponseAdapter;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class CrtAsyncRequestExecutor {

    private static final Logger log = Logger.loggerFor(CrtAsyncRequestExecutor.class);

    public CompletableFuture<Void> execute(CrtAsyncRequestContext executionContext) {
        // go ahead and get a reference to the metricCollector since multiple futures will
        // need it regardless.
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        long acquireStartTime = 0;

        if (shouldPublishMetrics) {
            // go ahead and get acquireStartTime for the concurrency timer as early as possible,
            // so it's as accurate as possible, but only do it in a branch since clock_gettime()
            // results in a full sys call barrier (multiple mutexes and a hw interrupt).
            acquireStartTime = System.nanoTime();
        }

        CompletableFuture<Void> requestFuture = createAsyncExecutionFuture(executionContext.sdkRequest());

        // When a Connection is ready from the Connection Pool, schedule the Request on the connection
        CompletableFuture<HttpClientConnection> httpClientConnectionCompletableFuture =
            executionContext.crtConnPool().acquireConnection();

        long finalAcquireStartTime = acquireStartTime;

        httpClientConnectionCompletableFuture.whenComplete((crtConn, throwable) -> {
            AsyncExecuteRequest asyncRequest = executionContext.sdkRequest();

            if (shouldPublishMetrics) {
                reportMetrics(executionContext.crtConnPool(), metricCollector, finalAcquireStartTime);
            }

            // If we didn't get a connection for some reason, fail the request
            if (throwable != null) {
                Throwable toThrow = wrapConnectionFailureException(throwable);
                reportAsyncFailure(crtConn, toThrow, requestFuture, asyncRequest.responseHandler());
                return;
            }

            executeRequest(executionContext, requestFuture, crtConn, asyncRequest);
        });

        return requestFuture;
    }

    private void executeRequest(CrtAsyncRequestContext executionContext,
                                CompletableFuture<Void> requestFuture,
                                HttpClientConnection crtConn,
                                AsyncExecuteRequest asyncRequest) {
        // Submit the request on the connection
        try {
            HttpRequest crtRequest = CrtRequestAdapter.toAsyncCrtRequest(executionContext);
            HttpStreamResponseHandler crtResponseHandler =
                CrtResponseAdapter.toCrtResponseHandler(crtConn, requestFuture, asyncRequest.responseHandler());

            crtConn.makeRequest(crtRequest, crtResponseHandler).activate();
        } catch (HttpException e) {
            Throwable toThrow = wrapWithIoExceptionIfRetryable(e);
            reportAsyncFailure(crtConn,
                          toThrow,
                          requestFuture,
                          asyncRequest.responseHandler());
        } catch (IllegalStateException | CrtRuntimeException e) {
            // CRT throws IllegalStateException if the connection is closed
            reportAsyncFailure(crtConn, new IOException("An exception occurred when making the request", e),
                          requestFuture,
                          asyncRequest.responseHandler());
        } catch (Throwable throwable) {
            reportAsyncFailure(crtConn, throwable,
                               requestFuture,
                               asyncRequest.responseHandler());
        }
    }

    /**
     * Create the execution future and set up the cancellation logic.
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
    private void reportAsyncFailure(HttpClientConnection crtConn,
                               Throwable cause,
                               CompletableFuture<Void> executeFuture,
                               SdkAsyncHttpResponseHandler responseHandler) {
        if (crtConn != null) {
            crtConn.close();
        }

        try {
            responseHandler.onError(cause);
        } catch (Exception e) {
            log.error(() -> "SdkAsyncHttpResponseHandler " + responseHandler + " threw an exception in onError. It will be "
                            + "ignored.", e);
        }
        executeFuture.completeExceptionally(cause);
    }
}
