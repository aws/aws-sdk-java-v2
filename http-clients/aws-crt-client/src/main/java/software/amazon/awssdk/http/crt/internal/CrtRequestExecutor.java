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
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.crt.internal.request.CrtRequestAdapter;
import software.amazon.awssdk.http.crt.internal.response.InputStreamAdaptingHttpStreamResponseHandler;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.NoOpMetricCollector;

@SdkInternalApi
public final class CrtRequestExecutor {

    public CompletableFuture<SdkHttpFullResponse> execute(CrtRequestContext executionContext,
                                                          CrtStreamHandler streamHandler) {
        CompletableFuture<SdkHttpFullResponse> responseFuture = new CompletableFuture<>();

        try {
            doExecute(executionContext, responseFuture, streamHandler);
        } catch (Throwable t) {
            responseFuture.completeExceptionally(t);
        }

        return responseFuture;
    }

    private void doExecute(CrtRequestContext executionContext,
                           CompletableFuture<SdkHttpFullResponse> responseFuture,
                           CrtStreamHandler streamHandler) {
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        long acquireStartTime = 0;

        if (shouldPublishMetrics) {
            acquireStartTime = System.nanoTime();
        }

        InputStreamAdaptingHttpStreamResponseHandler crtResponseHandler =
            new InputStreamAdaptingHttpStreamResponseHandler(responseFuture, streamHandler);

        HttpRequest crtRequest = CrtRequestAdapter.toCrtRequest(executionContext);

        boolean hasBody = executionContext.sdkRequest().contentStreamProvider().isPresent();

        CompletableFuture<HttpStreamBase> streamFuture =
            executionContext.streamManager().acquireStream(crtRequest, crtResponseHandler, hasBody);

        long finalAcquireStartTime = acquireStartTime;

        streamFuture.whenComplete((streamBase, throwable) -> {
            if (shouldPublishMetrics) {
                reportMetrics(executionContext.streamManager(), metricCollector, finalAcquireStartTime);
            }

            if (throwable != null) {
                Throwable toThrow = wrapCrtException(throwable);
                responseFuture.completeExceptionally(toThrow);
            } else {
                streamHandler.setStream(streamBase);
            }
        });
    }
}
