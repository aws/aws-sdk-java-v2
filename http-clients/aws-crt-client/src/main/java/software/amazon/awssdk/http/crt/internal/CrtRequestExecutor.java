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

    public Result execute(CrtRequestContext executionContext) {
        CompletableFuture<SdkHttpFullResponse> responseFuture = new CompletableFuture<>();
        CompletableFuture<HttpStreamBase> streamFuture = new CompletableFuture<>();
        CrtStreamHandler streamHandler = new CrtStreamHandler(streamFuture);

        try {
            doExecute(executionContext, responseFuture, streamHandler, streamFuture);
        } catch (Throwable t) {
            // Fail streamFuture too so any caller blocked in waitForStream() unblocks.
            streamFuture.completeExceptionally(t);
            responseFuture.completeExceptionally(t);
        }

        return new Result(responseFuture, streamHandler);
    }

    private void doExecute(CrtRequestContext executionContext,
                           CompletableFuture<SdkHttpFullResponse> responseFuture,
                           CrtStreamHandler streamHandler,
                           CompletableFuture<HttpStreamBase> streamFuture) {
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

        long finalAcquireStartTime = acquireStartTime;

        executionContext.streamManager().acquireStream(crtRequest, crtResponseHandler, hasBody)
                        .handle((streamBase, throwable) -> {
                            if (shouldPublishMetrics) {
                                reportMetrics(executionContext.streamManager(), metricCollector, finalAcquireStartTime);
                            }

                            if (throwable != null) {
                                handleAcquireFailure(throwable, streamFuture, responseFuture);
                                return null;
                            }
                            try {
                                streamBase.activate();
                            } catch (Throwable t) {
                                // Stream is acquired but not activated and not yet published via
                                // streamFuture. No other path can reach it, so clean it up directly.
                                streamBase.cancel();
                                streamBase.close();
                                handleAcquireFailure(t, streamFuture, responseFuture);
                                return null;
                            }
                            streamFuture.complete(streamBase);
                            return null;
                        }).exceptionally(t -> {
                            // Defensive: only reached if the handle lambda itself throws.
                            handleAcquireFailure(t, streamFuture, responseFuture);
                            return null;
                        });
    }

    private void handleAcquireFailure(Throwable t,
                                      CompletableFuture<HttpStreamBase> streamFuture,
                                      CompletableFuture<SdkHttpFullResponse> responseFuture) {
        Throwable toThrow = wrapCrtException(t);
        streamFuture.completeExceptionally(toThrow);
        responseFuture.completeExceptionally(toThrow);
    }

    @SdkInternalApi
    public static final class Result {
        private final CompletableFuture<SdkHttpFullResponse> responseFuture;
        private final CrtStreamHandler streamHandler;

        private Result(CompletableFuture<SdkHttpFullResponse> responseFuture, CrtStreamHandler streamHandler) {
            this.responseFuture = responseFuture;
            this.streamHandler = streamHandler;
        }

        public CompletableFuture<SdkHttpFullResponse> responseFuture() {
            return responseFuture;
        }

        public CrtStreamHandler streamHandler() {
            return streamHandler;
        }
    }
}
