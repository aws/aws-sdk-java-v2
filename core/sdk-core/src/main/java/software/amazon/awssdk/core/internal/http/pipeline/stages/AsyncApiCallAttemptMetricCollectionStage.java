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

import static software.amazon.awssdk.core.internal.util.MetricUtils.collectHttpMetrics;
import static software.amazon.awssdk.core.internal.util.MetricUtils.createAttemptMetricsCollector;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.CompletableFutureUtils;

/**
 * Wrapper pipeline that initializes and tracks the API call attempt metric collection. This wrapper and any wrapped
 * stages will track API call attempt metrics.
 */
@SdkInternalApi
public final class AsyncApiCallAttemptMetricCollectionStage<OutputT> implements RequestPipeline<SdkHttpFullRequest,
    CompletableFuture<Response<OutputT>>> {
    private final RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> wrapped;

    public AsyncApiCallAttemptMetricCollectionStage(RequestPipeline<SdkHttpFullRequest,
        CompletableFuture<Response<OutputT>>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest input,
                                                        RequestExecutionContext context) throws Exception {

        MetricCollector apiCallAttemptMetrics = createAttemptMetricsCollector(context);
        context.attemptMetricCollector(apiCallAttemptMetrics);
        reportBackoffDelay(context);

        CompletableFuture<Response<OutputT>> executeFuture = wrapped.execute(input, context);
        CompletableFuture<Response<OutputT>> metricsCollectedFuture = executeFuture.whenComplete((r, t) -> {
            if (t == null) {
                collectHttpMetrics(apiCallAttemptMetrics, r.httpResponse());
            }
        });
        CompletableFutureUtils.forwardExceptionTo(metricsCollectedFuture, executeFuture);

        return metricsCollectedFuture;
    }

    private void reportBackoffDelay(RequestExecutionContext context) {
        Duration lastBackoffDelay = context.executionAttributes().getAttribute(RetryableStageHelper.LAST_BACKOFF_DELAY_DURATION);
        if (lastBackoffDelay != null) {
            context.attemptMetricCollector().reportMetric(CoreMetric.BACKOFF_DELAY_DURATION, lastBackoffDelay);
        }
    }
}
