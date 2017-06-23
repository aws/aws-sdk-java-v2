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

import static software.amazon.awssdk.event.SdkProgressPublisher.publishProgress;

import java.util.List;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.pipeline.RequestToResponsePipeline;

/**
 * Calls the {@link RequestHandler#afterResponse(SdkHttpFullRequest, Response)} or
 * {@link RequestHandler#afterError(SdkHttpFullRequest, Response, Exception)} callbacks, depending on whether the request
 * succeeded or failed.
 */
public class AfterCallbackStage<OutputT> implements RequestToResponsePipeline<OutputT> {

    private final RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped;

    public AfterCallbackStage(RequestPipeline<SdkHttpFullRequest, Response<OutputT>> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Response<OutputT> execute(SdkHttpFullRequest request, RequestExecutionContext context) throws Exception {
        Response<OutputT> response = null;
        try {
            response = wrapped.execute(request, context);
            afterResponse(context.requestHandlers(), request, response);
            return response;
        } catch (Exception e) {
            publishProgress(context.requestConfig().getProgressListener(), ProgressEventType.CLIENT_REQUEST_FAILED_EVENT);
            afterError(context.requestHandlers(), request, response, e);
            throw e;
        }
    }

    private <T> void afterResponse(List<RequestHandler> requestHandlers,
                                   SdkHttpFullRequest request,
                                   Response<T> response) throws InterruptedException {
        for (RequestHandler handler : requestHandlers) {
            handler.afterResponse(request, response);
            AmazonHttpClient.checkInterrupted(response);
        }
    }

    private void afterError(List<RequestHandler> requestHandlers,
                            SdkHttpFullRequest request,
                            Response<?> response,
                            Exception e) throws InterruptedException {
        for (RequestHandler handler : requestHandlers) {
            handler.afterError(request, response, e);
            AmazonHttpClient.checkInterrupted(response);
        }
    }
}
