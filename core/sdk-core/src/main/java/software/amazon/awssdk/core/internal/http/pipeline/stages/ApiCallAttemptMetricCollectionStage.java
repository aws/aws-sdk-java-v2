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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Wrapper pipeline that initializes and tracks the API call attempt metric collection. This wrapper and any wrapped
 * stages will track API call attempt metrics.
 */
@SdkInternalApi
public final class ApiCallAttemptMetricCollectionStage<OutputT> implements RequestToResponsePipeline<OutputT>  {
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public ApiCallAttemptMetricCollectionStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        MetricCollector apiCallAttemptMetrics = createAttemptMetricsCollector(context);
        context.metricCollector(apiCallAttemptMetrics);

        try {
            Response<OutputT> response = wrapped.execute(input, context);

            collectHttpMetrics(apiCallAttemptMetrics, response.httpResponse());

            if (!response.isSuccess() && response.exception() != null) {
                apiCallAttemptMetrics.reportMetric(CoreMetric.EXCEPTION, response.exception());
            }

            return response;
        } catch (Throwable t) {
            apiCallAttemptMetrics.reportMetric(CoreMetric.EXCEPTION, t);
            throw t;
        }
    }

    private MetricCollector createAttemptMetricsCollector(RequestExecutionContext context) {
        return context.executionContext()
                .metricCollector()
                .createChild("ApiCallAttemptMetrics");
    }

    private void collectHttpMetrics(MetricCollector metricCollector, SdkHttpFullResponse httpResponse) {
        metricCollector.reportMetric(CoreMetric.HTTP_STATUS_CODE, httpResponse.statusCode());
        httpResponse.firstMatchingHeader("x-amz-request-id")
                .ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_REQUEST_ID, v));
        httpResponse.firstMatchingHeader("x-amz-id-2")
                .ifPresent(v -> metricCollector.reportMetric(CoreMetric.AWS_EXTENDED_REQUEST_ID, v));
    }
}
