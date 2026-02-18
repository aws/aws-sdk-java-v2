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
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.crt.http.HttpStreamBase;
import software.amazon.awssdk.crt.http.HttpStreamBaseResponseHandler;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

@SdkInternalApi
public final class CrtRequestExecutor {

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
            // go ahead and get acquireStartTime for the concurrency timer as early as possible,
            // so it's as accurate as possible, but only do it in a branch since clock_gettime()
            // results in a full sys call barrier (multiple mutexes and a hw interrupt).
            acquireStartTime = System.nanoTime();
        }

        HttpStreamBaseResponseHandler crtResponseHandler = new InputStreamAdaptingHttpStreamResponseHandler(requestFuture);

        HttpRequest crtRequest = CrtRequestAdapter.toCrtRequest(executionContext);

        CompletableFuture<HttpStreamBase> streamFuture =
            executionContext.crtConnPool().acquireStream(crtRequest, crtResponseHandler);

        long finalAcquireStartTime = acquireStartTime;

        streamFuture.whenComplete((streamBase, throwable) -> {
            if (shouldPublishMetrics) {
                reportMetrics(executionContext.crtConnPool(), metricCollector, finalAcquireStartTime);
            }

            if (throwable != null) {
                Throwable toThrow = wrapCrtException(throwable);
                requestFuture.completeExceptionally(toThrow);
            }
        });
    }
}
