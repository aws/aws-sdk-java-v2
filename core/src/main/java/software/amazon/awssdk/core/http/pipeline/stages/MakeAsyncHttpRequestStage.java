/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.RequestExecutionContext;
import software.amazon.awssdk.core.Response;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.http.HttpAsyncClientDependencies;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.InterruptMonitor;
import software.amazon.awssdk.core.http.SdkHttpResponseAdapter;
import software.amazon.awssdk.core.http.async.SimpleRequestProvider;
import software.amazon.awssdk.core.http.pipeline.RequestPipeline;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;
import software.amazon.awssdk.utils.FunctionalUtils.UnsafeRunnable;

/**
 * Delegate to the HTTP implementation to make an HTTP request and receive the response.
 */
public class MakeAsyncHttpRequestStage<OutputT>
    implements RequestPipeline<SdkHttpFullRequest, CompletableFuture<Response<OutputT>>> {

    private static final Logger log = LoggerFactory.getLogger(MakeAsyncHttpRequestStage.class);

    private final SdkAsyncHttpClient sdkAsyncHttpClient;
    private final SdkHttpResponseHandler<OutputT> responseHandler;
    private final SdkHttpResponseHandler<? extends SdkException> errorResponseHandler;

    public MakeAsyncHttpRequestStage(SdkHttpResponseHandler<OutputT> responseHandler,
                                     SdkHttpResponseHandler<? extends SdkException> errorResponseHandler,
                                     HttpAsyncClientDependencies dependencies) {
        this.responseHandler = responseHandler;
        this.errorResponseHandler = errorResponseHandler;
        this.sdkAsyncHttpClient = dependencies.asyncClientConfiguration().asyncHttpClient();
    }

    /**
     * Returns the response from executing one httpClientSettings request; or null for retry.
     */
    public CompletableFuture<Response<OutputT>> execute(SdkHttpFullRequest request,
                                                        RequestExecutionContext context) throws Exception {

        InterruptMonitor.checkInterrupted();
        return executeHttpRequest(request, context);
    }

    private CompletableFuture<Response<OutputT>> executeHttpRequest(SdkHttpFullRequest request,
                                                                    RequestExecutionContext context) throws Exception {
        CompletableFuture<Response<OutputT>> future = new CompletableFuture<>();

        SdkHttpResponseHandler<Response<OutputT>> handler = new ResponseHandler(request, future);

        SdkHttpRequestProvider requestProvider = context.requestProvider() == null
                ? new SimpleRequestProvider(request, context.executionAttributes())
                : context.requestProvider();
        // Set content length if it hasn't been set already.
        SdkHttpFullRequest requestWithContentLength = getRequestWithContentLength(request, requestProvider);

        sdkAsyncHttpClient.prepareRequest(requestWithContentLength, SdkRequestContext.builder().build(),
                                          requestProvider,
                                          handler)
                          .run();

        // TODO client execution timer
        //        context.clientExecutionTrackerTask().setCurrentHttpRequest(requestCallable);
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
               && !request.firstMatchingHeader("Content-Length").isPresent()
               // Can cause issues with signing if content length is present for these method
               && request.method() != SdkHttpMethod.GET
               && request.method() != SdkHttpMethod.HEAD;

    }

    /**
     * Runs a given {@link UnsafeRunnable} and logs an error without throwing.
     *
     * @param errorMsg Message to log with exception thrown.
     * @param runnable Action to perform.
     */
    private static void runAndLogError(String errorMsg, UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    /**
     * Detects whether the response succeeded or failed and delegates to appropriate response handler.
     */
    private class ResponseHandler implements SdkHttpResponseHandler<Response<OutputT>> {
        private final SdkHttpFullRequest request;
        private final CompletableFuture<Response<OutputT>> future;

        private volatile SdkHttpResponse response;
        private volatile boolean isSuccess = false;

        /**
         * @param request  Request being made
         * @param future   Future to notify when response has been handled.
         */
        private ResponseHandler(SdkHttpFullRequest request,
                                CompletableFuture<Response<OutputT>> future) {
            this.request = request;
            this.future = future;
        }

        @Override
        public void headersReceived(SdkHttpResponse response) {
            if (HttpStatusFamily.of(response.statusCode()) == HttpStatusFamily.SUCCESSFUL) {
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
            runAndLogError("SdkHttpResponseHandler threw an exception.",
                () -> responseHandler.exceptionOccurred(throwable));
            future.completeExceptionally(throwable);
        }

        @Override
        public Response<OutputT> complete() {
            try {
                SdkHttpFullResponse httpFullResponse = (SdkHttpFullResponse) this.response;
                final HttpResponse httpResponse = SdkHttpResponseAdapter.adapt(false, request, httpFullResponse);
                Response<OutputT> toReturn = handleResponse(httpResponse);
                future.complete(toReturn);
                return toReturn;
            } catch (Exception e) {
                future.completeExceptionally(e);
                throw e;
            }
        }

        private Response<OutputT> handleResponse(HttpResponse httpResponse) {
            if (isSuccess) {
                OutputT response = responseHandler.complete();
                return Response.fromSuccess(response, httpResponse);
            } else {
                return Response.fromFailure(errorResponseHandler.complete(), httpResponse);
            }
        }

    }
}
