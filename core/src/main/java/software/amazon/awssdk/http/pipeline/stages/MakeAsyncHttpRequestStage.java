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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.event.ProgressEventType;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.HttpClientDependencies;
import software.amazon.awssdk.http.HttpResponse;
import software.amazon.awssdk.http.HttpStatusCodes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkHttpResponseAdapter;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.http.async.SimpleRequestProvider;
import software.amazon.awssdk.http.pipeline.RequestPipeline;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
public class MakeAsyncHttpRequestStage<OutputT>
        implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

    private final SdkAsyncHttpClient sdkAsyncHttpClient;
    private final SdkHttpResponseHandler<OutputT> responseHandler;
    private final SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler;

    public MakeAsyncHttpRequestStage(SdkHttpResponseHandler<OutputT> responseHandler,
                                     SdkHttpResponseHandler<? extends SdkBaseException> errorResponseHandler,
                                     HttpClientDependencies dependencies) {
        this.responseHandler = responseHandler;
        this.errorResponseHandler = errorResponseHandler;
        this.sdkAsyncHttpClient = dependencies.sdkAsyncHttpClient();
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                        RequestExecutionContext context) throws Exception {

        AmazonHttpClient.checkInterrupted();
        final ProgressListener listener = context.requestConfig().getProgressListener();

        publishProgress(listener, ProgressEventType.HTTP_REQUEST_STARTED_EVENT);
        return executeHttpRequest(request, context, listener);
    }

    private CompletableFuture<Response<OutputT>> executeHttpRequest(SdkHttpFullRequest request,
                                                                    RequestExecutionContext context,
                                                                    ProgressListener listener) throws Exception {
        CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();

        SdkHttpResponseHandler<Response<OutputT>> handler = new ResponseHandler(request, future, listener);

        SdkHttpRequestProvider requestProvider = context.requestProvider() == null ? new SimpleRequestProvider(request)
                : context.requestProvider();
        // Set content length if it hasn't been set already.
        SdkHttpFullRequest requestWithContentLength = getRequestWithContentLength(request, requestProvider);

        sdkAsyncHttpClient.prepareRequest(requestWithContentLength, SdkRequestContext.builder()
                                                                    .metrics(context.awsRequestMetrics())
                                                                    .build(),
                                          requestProvider,
                                          handler)
                          .run();

        // TODO client execution timer
        //        context.getClientExecutionTrackerTask().setCurrentHttpRequest(requestCallable);
        return future;
    }

    private SdkHttpFullRequest getRequestWithContentLength(SdkHttpFullRequest request, SdkHttpRequestProvider requestProvider) {
        if (shouldSetContentLength(request, requestProvider)) {
            return request.toBuilder()
                          .header("Content-Length", String.valueOf(requestProvider.contentLength()))
                          .build();
        }
        return request;
    }

    private boolean shouldSetContentLength(SdkHttpFullRequest request, SdkHttpRequestProvider requestProvider) {
        return requestProvider != null
               && !request.getFirstHeaderValue("Content-Length").isPresent()
               // Can cause issues with signing if content length is present for these method
               && request.getHttpMethod() != SdkHttpMethod.GET
               && request.getHttpMethod() != SdkHttpMethod.HEAD;

    }

    /**
     * Detects whether the response succeeded or failed and delegates to appropriate response handler.
     */
    private class ResponseHandler implements SdkHttpResponseHandler<Response<OutputT>> {

        private final ProgressListener listener;
        private final SdkHttpFullRequest request;
        private final CompletableFuture<Response<OutputT>> future;

        private volatile SdkHttpResponse response;
        private volatile boolean isSuccess = false;

        /**
         * @param request  Request being made
         * @param future   Future to notify when response has been handled.
         * @param listener Listener to report HTTP end event.
         */
        private ResponseHandler(SdkHttpFullRequest request,
                                CompletableFuture<Response<OutputT>> future,
                                ProgressListener listener) {
            this.listener = listener;
            this.request = request;
            this.future = future;
        }

        @Override
        public void headersReceived(SdkHttpResponse response) {
            if (isSuccessful(response.getStatusCode())) {
                isSuccess = true;
                responseHandler.headersReceived(response);
            } else {
                errorResponseHandler.headersReceived(response);
            }
            this.response = response;
        }

        @Override
        public void onStream(Publisher<ByteBuffer> publisher) {
            if (isSuccess) {
                // TODO handle exception as non retryable
                responseHandler.onStream(publisher);
            } else {
                errorResponseHandler.onStream(publisher);
            }
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            responseHandler.exceptionOccurred(throwable);
            future.completeExceptionally(throwable);
        }

        @Override
        public Response<OutputT> complete() {
            publishProgress(listener, ProgressEventType.HTTP_REQUEST_COMPLETED_EVENT);
            final HttpResponse httpResponse = SdkHttpResponseAdapter.adapt(false, request, (SdkHttpFullResponse) response);
            Response<OutputT> toReturn = handleResponse(httpResponse);
            future.complete(toReturn);
            return toReturn;
        }

        /**
         * If we get back any 2xx status code, then we know we should treat the service call as successful.
         */
        private boolean isSuccessful(int statusCode) {
            return statusCode / 100 == HttpStatusCodes.OK / 100;
        }

        private Response<OutputT> handleResponse(HttpResponse httpResponse) {
            if (isSuccess) {
                return Response.fromSuccess(responseHandler.complete(), httpResponse);
            } else {
                return Response.fromFailure(errorResponseHandler.complete(), httpResponse);
            }
        }
    }
}
