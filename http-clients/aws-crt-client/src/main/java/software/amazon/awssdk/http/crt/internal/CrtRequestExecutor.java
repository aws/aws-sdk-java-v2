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
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

@SdkInternalApi
public final class CrtRequestExecutor {

    public CompletableFuture<SdkHttpFullResponse> execute(CrtRequestContext executionContext) {
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

        CompletableFuture<SdkHttpFullResponse> requestFuture = new CompletableFuture<>();

        // When a Connection is ready from the Connection Pool, schedule the Request on the connection
        CompletableFuture<HttpClientConnection> httpClientConnectionCompletableFuture =
            executionContext.crtConnPool().acquireConnection();

        long finalAcquireStartTime = acquireStartTime;

        httpClientConnectionCompletableFuture.whenComplete((crtConn, throwable) -> {
            if (shouldPublishMetrics) {
                reportMetrics(executionContext.crtConnPool(), metricCollector, finalAcquireStartTime);
            }

            // If we didn't get a connection for some reason, fail the request
            if (throwable != null) {
                Throwable toThrow = wrapConnectionFailureException(throwable);
                requestFuture.completeExceptionally(toThrow);
                return;
            }

            executeRequest(executionContext, requestFuture, crtConn);
        });

        return requestFuture;
    }

    private void executeRequest(CrtRequestContext executionContext,
                                CompletableFuture<SdkHttpFullResponse> requestFuture,
                                HttpClientConnection crtConn) {
        HttpRequest crtRequest = CrtRequestAdapter.toCrtRequest(executionContext);

        try {
            HttpStreamResponseHandler crtResponseHandler = new InputStreamAdaptingHttpStreamResponseHandler(crtConn,
                                                                                                            requestFuture);

            // Submit the request on the connection
            crtConn.makeRequest(crtRequest, crtResponseHandler).activate();
        } catch (Throwable throwable) {
            handleException(requestFuture, crtConn, throwable);
        }
    }

    private static void handleException(CompletableFuture<SdkHttpFullResponse> requestFuture, HttpClientConnection crtConn,
                          Throwable throwable) {

        crtConn.close();

        if (throwable instanceof HttpException) {
            Throwable toThrow = wrapWithIoExceptionIfRetryable((HttpException) throwable);
            requestFuture.completeExceptionally(toThrow);
            return;
        }

        if (throwable instanceof CrtRuntimeException || throwable instanceof IllegalStateException) {
            // CRT throws IllegalStateException if the connection is closed
            requestFuture.completeExceptionally(new IOException(throwable));
            return;
        }

        requestFuture.completeExceptionally(throwable);
    }
}
