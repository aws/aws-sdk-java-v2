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

import java.io.IOException;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.pipeline.RequestToResponsePipeline;

/**
 * Translates an {@link IOException} to an {@link InterruptedException} if that IOException was caused by the
 * {@link software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer}. This is important for consistent handling
 * of timeouts in {@link ClientExecutionTimedStage}.
 */
public class TimerExceptionHandlingStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline;

    public TimerExceptionHandlingStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> requestPipeline) {
        this.requestPipeline = requestPipeline;
    }

    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        try {
            return requestPipeline.execute(request, context);
        } catch (IOException ioe) {
            if (context.clientExecutionTrackerTask().hasTimeoutExpired()) {
                throw new InterruptedException();
            }
            throw ioe;
        }
    }
}
