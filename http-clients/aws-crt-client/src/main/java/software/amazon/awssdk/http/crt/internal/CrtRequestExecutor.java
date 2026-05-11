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

    public ExecutionResult execute(CrtRequestContext executionContext) {
        CompletableFuture<SdkHttpFullResponse> responseFuture = new CompletableFuture<>();
        CompletableFuture<HttpStreamBase> streamFuture;

        try {
            streamFuture = doExecute(executionContext, responseFuture);
        } catch (Throwable t) {
            responseFuture.completeExceptionally(t);
            streamFuture = new CompletableFuture<>();
            streamFuture.completeExceptionally(t);
        }

        return new ExecutionResult(streamFuture, responseFuture);
    }

    private CompletableFuture<HttpStreamBase> doExecute(CrtRequestContext executionContext,
                                                        CompletableFuture<SdkHttpFullResponse> responseFuture) {
        MetricCollector metricCollector = executionContext.metricCollector();
        boolean shouldPublishMetrics = metricCollector != null && !(metricCollector instanceof NoOpMetricCollector);

        long acquireStartTime = 0;

        if (shouldPublishMetrics) {
            acquireStartTime = System.nanoTime();
        }

        HttpStreamBaseResponseHandler crtResponseHandler = new InputStreamAdaptingHttpStreamResponseHandler(responseFuture);

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
            }
        });

        return streamFuture;
    }

    /**
     * Holds the result of submitting a request to CRT: the stream (for writing body data via
     * {@code writeData}) and the response future (for reading the response).
     */
    public static final class ExecutionResult {
        private final CompletableFuture<HttpStreamBase> streamFuture;
        private final CompletableFuture<SdkHttpFullResponse> responseFuture;

        ExecutionResult(CompletableFuture<HttpStreamBase> streamFuture,
                        CompletableFuture<SdkHttpFullResponse> responseFuture) {
            this.streamFuture = streamFuture;
            this.responseFuture = responseFuture;
        }

        public CompletableFuture<HttpStreamBase> streamFuture() {
            return streamFuture;
        }

        public CompletableFuture<SdkHttpFullResponse> responseFuture() {
            return responseFuture;
        }
    }
}
