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
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.crt.http.HttpStreamManager;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;
import software.amazon.awssdk.utils.Logger;

@SdkInternalApi
public final class CrtRequestExecutor {

    private static final Logger log = Logger.loggerFor(CrtRequestExecutor.class);

    public CompletableFuture<SdkHttpFullResponse> execute(CrtRequestContext executionContext) {
        CompletableFuture<SdkHttpFullResponse> requestFuture = new CompletableFuture<>();

        try {
            doExecute(executionContext, requestFuture);
        } catch (Throwable t) {
            requestFuture.completeExceptionally(t);
        }

        return requestFuture;
    }

    private void doExecute(CrtRequestContext executionContext, CompletableFuture<SdkHttpFullResponse> requestFuture) {
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        long acquireStartTime = 0;

        if (shouldPublishMetrics) {
            acquireStartTime = System.nanoTime();
        }

        InputStreamAdaptingHttpStreamResponseHandler crtResponseHandler =
            new InputStreamAdaptingHttpStreamResponseHandler(requestFuture);

        HttpRequest crtRequest = CrtRequestAdapter.toCrtRequest(executionContext, requestFuture::completeExceptionally);
        HttpStreamManager streamManager = executionContext.streamManager();

        CompletableFuture<HttpStreamBase> streamFuture =
            streamManager.acquireStream(crtRequest, crtResponseHandler);

        // Guard to ensure metrics are reported exactly once per request.
        AtomicBoolean metricsReported = new AtomicBoolean(false);

        long finalAcquireStartTime = acquireStartTime;

        // Evict the connection from the pool on failure so it is not reused.
        // Also report metrics on failure — this ensures concurrency metrics
        // are available during pool exhaustion timeouts.
        requestFuture.whenComplete((r, t) -> {
            if (t != null) {
                try {
                    if (shouldPublishMetrics && metricsReported.compareAndSet(false, true)) {
                        reportMetrics(streamManager, metricCollector, finalAcquireStartTime);
                    }
                } catch (RuntimeException e) {
                    log.debug(() -> "Failed to report HTTP client metrics.", e);
                } finally {
                    crtResponseHandler.closeConnection();
                }
            }
        });

        streamFuture.whenComplete((streamBase, throwable) -> {
            crtResponseHandler.onAcquireStream(streamBase);
            try {
                if (shouldPublishMetrics && metricsReported.compareAndSet(false, true)) {
                    reportMetrics(streamManager, metricCollector, finalAcquireStartTime);
                }
            } catch (RuntimeException e) {
                log.debug(() -> "Failed to report HTTP client metrics.", e);
            } finally {
                if (throwable != null) {
                    Throwable toThrow = wrapCrtException(throwable);
                    requestFuture.completeExceptionally(toThrow);
                }
            }
        });
    }
}
