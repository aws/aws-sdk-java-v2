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

import static software.amazon.awssdk.core.interceptor.MetricExecutionAttribute.METRIC_REGISTRY;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.MetricUtils;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.metrics.metrics.SdkDefaultMetric;
import software.amazon.awssdk.metrics.registry.MetricRegistry;

/**
 * Wrapper around the pipeline for a single request to provide retry, clock-skew and request throttling functionality.
 */
@SdkInternalApi
public final class RetryableStage<OutputT> implements RequestToResponsePipeline<OutputT> {
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;
    private final HttpClientDependencies dependencies;

    public RetryableStage(HttpClientDependencies dependencies,
                          RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.requestPipeline = requestPipeline;
    }

    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        try {
            return doExecute(request, context);
        } finally {
            MetricUtils.timer(context.executionContext().executionAttributes()
                            .getAttribute(METRIC_REGISTRY),
                    SdkDefaultMetric.API_CALL)
                    .end();
        }
    }

    public Response<OutputT> doExecute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        RetryableStageHelper retryableStageHelper = new RetryableStageHelper(request, context, dependencies);

        while (true) {
            MetricRegistry attemptRegistry = MetricUtils.newRegistry(context.executionAttributes());
            Timer apiCallAttemptTimer = MetricUtils.timer(attemptRegistry, SdkDefaultMetric.API_CALL_ATTEMPT_LATENCY);
            apiCallAttemptTimer.start();

            retryableStageHelper.startingAttempt();

            if (!retryableStageHelper.retryPolicyAllowsRetry()) {
                throw retryableStageHelper.retryPolicyDisallowedRetryException();
            }

            Duration backoffDelay = retryableStageHelper.getBackoffDelay();
            if (!backoffDelay.isZero()) {
                retryableStageHelper.logBackingOff(backoffDelay);
                TimeUnit.MILLISECONDS.sleep(backoffDelay.toMillis());
            }

            Response<OutputT> response;
            try {
                retryableStageHelper.logSendingRequest();
                response = requestPipeline.execute(retryableStageHelper.requestToSend(), context);
            } catch (SdkException | IOException e) {
                retryableStageHelper.setLastException(e);
                continue;
            } finally {
                apiCallAttemptTimer.end();
            }

            retryableStageHelper.setLastResponse(response.httpResponse());

            if (!response.isSuccess()) {
                retryableStageHelper.adjustClockIfClockSkew(response);
                retryableStageHelper.setLastException(response.exception());
                continue;
            }

            retryableStageHelper.attemptSucceeded();
            return response;
        }
    }
}
