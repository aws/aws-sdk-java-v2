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
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.SdkHttpFullResponse;

/**
 * Pipeline stage that executes an {@link HttpResponseHandler} to transform the response into a {@link Response}
 * object that contains a flag indicating success of failure and an unmarshalled response object or exception as
 * appropriate.
 */
@SdkInternalApi
public class HandleResponseStage<OutputT> implements RequestPipeline<SdkHttpFullResponse, Response<OutputT>> {
    private final HttpResponseHandler<Response<OutputT>> responseHandler;

    public HandleResponseStage(HttpResponseHandler<Response<OutputT>> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullResponse httpResponse, RequestExecutionContext context) throws Exception {
        return responseHandler.handle(httpResponse, context.executionAttributes());
    }
}
