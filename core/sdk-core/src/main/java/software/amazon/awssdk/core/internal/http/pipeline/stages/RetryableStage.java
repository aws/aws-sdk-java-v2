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

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper;
import software.amazon.awssdk.core.internal.retry.RateLimitingTokenBucket;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around the pipeline for a single request to provide retry, clock-skew and request throttling functionality.
 */
@SdkInternalApi
public final class RetryableStage<OutputT> implements RequestToResponsePipeline<OutputT> {
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;
    private final HttpClientDependencies dependencies;
    private final RateLimitingTokenBucket rateLimitingTokenBucket;

    public RetryableStage(HttpClientDependencies dependencies,
                          RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.requestPipeline = requestPipeline;
        this.rateLimitingTokenBucket = new RateLimitingTokenBucket();
    }

    @SdkTestInternalApi
    public RetryableStage(HttpClientDependencies dependencies,
                          RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline,
                          RateLimitingTokenBucket rateLimitingTokenBucket) {
        this.dependencies = dependencies;
        this.requestPipeline = requestPipeline;
        this.rateLimitingTokenBucket = rateLimitingTokenBucket;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        RetryableStageHelper retryableStageHelper = new RetryableStageHelper(request, context, rateLimitingTokenBucket,
                                                                             dependencies);

        while (true) {
            retryableStageHelper.startingAttempt();

            if (!retryableStageHelper.retryPolicyAllowsRetry()) {
                throw retryableStageHelper.retryPolicyDisallowedRetryException();
            }

            retryableStageHelper.getSendToken();

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
                retryableStageHelper.updateClientSendingRateForErrorResponse();
                continue;
            }

            retryableStageHelper.setLastResponse(response.httpResponse());

            if (!response.isSuccess()) {
                retryableStageHelper.adjustClockIfClockSkew(response);
                retryableStageHelper.setLastException(response.exception());
                retryableStageHelper.updateClientSendingRateForErrorResponse();
                continue;
            }

            retryableStageHelper.updateClientSendingRateForSuccessResponse();

            retryableStageHelper.attemptSucceeded();
            return response;
        }
    }
}
