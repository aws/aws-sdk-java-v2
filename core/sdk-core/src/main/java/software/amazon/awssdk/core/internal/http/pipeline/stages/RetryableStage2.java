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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.http.HttpClientDependencies;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.internal.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.RetryableStageHelper2;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Wrapper around the pipeline for a single request to provide retry, clock-skew and request throttling functionality.
 */
@SdkInternalApi
public final class RetryableStage2<OutputT> implements RequestToResponsePipeline<OutputT> {
    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;
    private final HttpClientDependencies dependencies;

    public RetryableStage2(HttpClientDependencies dependencies,
                           RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.dependencies = dependencies;
        this.requestPipeline = requestPipeline;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        RetryableStageHelper2 retryableStageHelper = new RetryableStageHelper2(request, context, dependencies);
        Duration initialDelay = retryableStageHelper.acquireInitialToken();
        TimeUnit.MILLISECONDS.sleep(initialDelay.toMillis());
        while (true) {
            try {
                retryableStageHelper.startingAttempt();
                Response<OutputT> response = executeRequest(retryableStageHelper, context);
                retryableStageHelper.recordAttemptSucceeded();
                return response;
            } catch (SdkException | IOException e) {
                retryableStageHelper.setLastException(e);
                Duration suggestedDelay = suggestedDelay(e);
                Optional<Duration> backoffDelay = retryableStageHelper.tryRefreshToken(suggestedDelay);
                if (backoffDelay.isPresent()) {
                    Duration delay = backoffDelay.get();
                    retryableStageHelper.logBackingOff(delay);
                    TimeUnit.MILLISECONDS.sleep(delay.toMillis());
                } else {
                    throw retryableStageHelper.retryPolicyDisallowedRetryException();
                }
            }
        }
    }

    private Duration suggestedDelay(Exception e) {
        // fixme, I'm not sure where this value should come from if any.
        return Duration.ZERO;
    }

    /**
     * Executes the requests and returns the result. If the response is not successful throws the wrapped exception.
     */
    private Response<OutputT> executeRequest(RetryableStageHelper2 retryableStageHelper,
                                             RequestExecutionContext context) throws Exception {
        retryableStageHelper.logSendingRequest();
        Response<OutputT> response = requestPipeline.execute(retryableStageHelper.requestToSend(), context);
        retryableStageHelper.setLastResponse(response.httpResponse());
        if (!response.isSuccess()) {
            retryableStageHelper.adjustClockIfClockSkew(response);
            throw response.exception();
        }
        return response;
    }
}
