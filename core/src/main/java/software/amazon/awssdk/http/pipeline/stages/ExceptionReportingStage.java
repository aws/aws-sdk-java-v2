/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.http.pipeline.stages;

import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.retry.RetryUtils;

/**
 * Reports an exceptions to the metrics system.
 */
public class ExceptionReportingStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public ExceptionReportingStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest input, RequestExecutionContext context) throws Exception {
        try {
            Response<OutputT> response = wrapped.execute(input, context);
            if (response.isFailure()) {
                captureExceptionMetrics(context.awsRequestMetrics(), response.getException());
            }
            return response;
        } catch (Exception e) {
            throw captureExceptionMetrics(context.awsRequestMetrics(), e);
        }
    }

    /**
     * Capture the metrics for the given throwable.
     */
    private <T extends Throwable> T captureExceptionMetrics(AwsRequestMetrics awsRequestMetrics, T throwable) {
        awsRequestMetrics.incrementCounterWith(AwsRequestMetrics.Field.Exception)
                .addProperty(AwsRequestMetrics.Field.Exception, throwable);
        if (throwable instanceof SdkBaseException) {
            if (RetryUtils.isThrottlingException((SdkBaseException) throwable)) {
                awsRequestMetrics.incrementCounterWith(AwsRequestMetrics.Field.ThrottleException)
                        .addProperty(AwsRequestMetrics.Field.ThrottleException, throwable);
            }
        }
        return throwable;
    }
}
