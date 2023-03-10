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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Wrapper pipeline that tracks the {@link CoreMetric#SERVICE_ENDPOINT} metric.
 */
@SdkInternalApi
public final class AsyncServiceEndpointMetricCollectionStage<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<OutputT>> {
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> wrapped;

    public AsyncServiceEndpointMetricCollectionStage(RequestPipeline<SdkHttpFullRequest, CompletableFuture<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public CompletableFuture<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        MetricCollector metricCollector = context.executionContext().metricCollector();

        CompletableFuture<OutputT> future = new CompletableFuture<>();

        CompletableFuture<OutputT> executeFuture = wrapped.execute(input, context);

        executeFuture.whenComplete((r, t) -> {
            metricCollector.reportMetric(CoreMetric.SERVICE_ENDPOINT, input.getUri());

            if (t != null) {
                future.completeExceptionally(t);
            } else {
                future.complete(r);
            }
        });

        return CompletableFutureUtils.forwardExceptionTo(future, executeFuture);
    }
}
