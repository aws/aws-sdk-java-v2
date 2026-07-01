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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter.SyncCrtRequest;
import software.amazon.awssdk.http.crt.internal.request.SyncRequestBodyPump;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

@SdkInternalApi
public final class CrtRequestExecutor {

    public Result execute(CrtRequestContext executionContext) {
        CompletableFuture<SdkHttpFullResponse> requestFuture = new CompletableFuture<>();
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        // get acquireStartTime as early as possible for the concurrency timer, but only when metrics are
        // enabled since clock_gettime() is a full sys call barrier (multiple mutexes and a hw interrupt).
        long acquireStartTime = shouldPublishMetrics ? System.nanoTime() : 0;

        try {
            InputStreamAdaptingHttpStreamResponseHandler crtResponseHandler =
                new InputStreamAdaptingHttpStreamResponseHandler(requestFuture);
            SyncCrtRequest syncCrtRequest = CrtRequestAdapter.toCrtRequest(executionContext);
            CompletableFuture<HttpStreamBase> streamFuture =
                executionContext.streamManager().acquireStream(syncCrtRequest.httpRequest(), crtResponseHandler);

            // Evict the connection from the pool on failure so it is not reused.
            requestFuture.whenComplete((r, t) -> {
                if (t != null) {
                    crtResponseHandler.closeConnection();
                }
            });

            long finalAcquireStartTime = acquireStartTime;
            streamFuture.whenComplete((streamBase, throwable) -> {
                if (throwable != null) {
                    requestFuture.completeExceptionally(wrapCrtException(throwable));

                } else {
                    crtResponseHandler.onAcquireStream(streamBase);
                    if (shouldPublishMetrics) {
                        reportMetrics(executionContext.streamManager(), metricCollector, finalAcquireStartTime);
                    }
                }
            });

            return new Result(requestFuture, syncCrtRequest.pump(), streamFuture);
        } catch (Throwable t) {
            requestFuture.completeExceptionally(t);
            return new Result(requestFuture, null, null);
        }
    }

    /**
     * Result of {@link #execute(CrtRequestContext)}: bundles the response future with the optional
     * caller-thread body pump (null when the request has no body) and the future that completes
     * when the CRT stream has been acquired from the connection pool.
     */
    public static final class Result {
        private final CompletableFuture<SdkHttpFullResponse> responseFuture;
        private final SyncRequestBodyPump pump;
        private final CompletableFuture<HttpStreamBase> streamFuture;

        Result(CompletableFuture<SdkHttpFullResponse> responseFuture,
               SyncRequestBodyPump pump,
               CompletableFuture<HttpStreamBase> streamFuture) {
            this.responseFuture = responseFuture;
            this.pump = pump;
            this.streamFuture = streamFuture;
        }

        public CompletableFuture<SdkHttpFullResponse> responseFuture() {
            return responseFuture;
        }

        public SyncRequestBodyPump pump() {
            return pump;
        }

        /**
         * Future that completes when the CRT stream has been acquired (or acquisition has failed).
         * The caller blocks on this before running the body pump so per-request body buffers are
         * not allocated while a request is queued on the connection pool.
         */
        public CompletableFuture<HttpStreamBase> streamFuture() {
            return streamFuture;
        }
    }
}
