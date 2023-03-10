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
import software.amazon.awssdk.metrics.MetricCollector;

/**
 * Wrapper pipeline that tracks the {@link CoreMetric#SERVICE_ENDPOINT} metric.
 */
@SdkInternalApi
public class ServiceEndpointMetricCollectionStage<OutputT> implements RequestToResponsePipeline<OutputT> {
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>>  wrapped;

    public ServiceEndpointMetricCollectionStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        MetricCollector metricCollector = context.executionContext().metricCollector();
        metricCollector.reportMetric(CoreMetric.SERVICE_ENDPOINT, input.getUri());

        return wrapped.execute(input, context);
    }
}
