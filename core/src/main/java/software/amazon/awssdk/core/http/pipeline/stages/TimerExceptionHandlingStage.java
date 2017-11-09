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

package software.amazon.awssdk.core.http.pipeline.stages;

import java.io.IOException;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.core.http.pipeline.RequestToResponsePipeline;
import software.amazon.awssdk.http.SdkHttpFullRequest;

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
        } catch (Exception e) {
            if (isTimeoutCausedException(context, e)) {
                throw new InterruptedException();
            }
            throw e;
        }
    }

    /**
     * Detects if the exception thrown was triggered by the execution timeout.
     *
     * @param context {@link RequestExecutionContext} object.
     * @param e       Exception thrown by request pipeline.
     * @return True if the exception was caused by the execution timeout, false if not.
     */
    private boolean isTimeoutCausedException(RequestExecutionContext context, Exception e) {
        return isIoException(e) && context.clientExecutionTrackerTask().hasTimeoutExpired();
    }

    /**
     * Detects if this exception is an {@link IOException} or was caused by an {@link IOException}. Will unwrap the exception
     * until an {@link IOException} is found or the cause it empty.
     *
     * @param e Exception to test.
     * @return True if exception was caused by an {@link IOException}, false otherwise.
     */
    private boolean isIoException(Exception e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof IOException) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
