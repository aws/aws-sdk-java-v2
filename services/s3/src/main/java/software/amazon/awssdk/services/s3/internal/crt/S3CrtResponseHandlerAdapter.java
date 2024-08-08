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

package software.amazon.awssdk.services.s3.internal.crt;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.async.SimplePublisher;

/**
 * Adapts {@link SdkAsyncHttpResponseHandler} to {@link S3MetaRequestResponseHandler}.
 */
@SdkInternalApi
public final class S3CrtResponseHandlerAdapter implements S3MetaRequestResponseHandler {
    private static final Logger log = Logger.loggerFor(S3CrtResponseHandlerAdapter.class);
    private static final Duration META_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final CompletableFuture<Void> resultFuture;
    private final SdkAsyncHttpResponseHandler responseHandler;

    private final SimplePublisher<ByteBuffer> responsePublisher = new SimplePublisher<>();

    private final SdkHttpResponse.Builder initialHeadersResponse = SdkHttpResponse.builder();
    private final CompletableFuture<S3MetaRequestWrapper> metaRequestFuture;

    private final PublisherListener<S3MetaRequestProgress> progressListener;
    private final Duration s3MetaRequestTimeout;

    private volatile boolean responseHandlingInitiated;

    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture,
                                       SdkAsyncHttpResponseHandler responseHandler,
                                       PublisherListener<S3MetaRequestProgress> progressListener,
                                       CompletableFuture<S3MetaRequestWrapper> metaRequestFuture) {
        this(executeFuture, responseHandler, progressListener, metaRequestFuture, META_REQUEST_TIMEOUT);
    }

    @SdkTestInternalApi
    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture,
                                       SdkAsyncHttpResponseHandler responseHandler,
                                       PublisherListener<S3MetaRequestProgress> progressListener,
                                       CompletableFuture<S3MetaRequestWrapper> metaRequestFuture,
                                       Duration s3MetaRequestTimeout) {
        this.resultFuture = executeFuture;
        this.metaRequestFuture = metaRequestFuture;

        resultFuture.whenComplete((r, t) -> {
            S3MetaRequestWrapper s3MetaRequest = s3MetaRequest();
            if (s3MetaRequest == null) {
                return;
            }

            if (t != null) {
                s3MetaRequest.cancel();
            }
            s3MetaRequest.close();
        });

        this.responseHandler = responseHandler;
        this.progressListener = progressListener == null ? new NoOpPublisherListener() : progressListener;
        this.s3MetaRequestTimeout = s3MetaRequestTimeout;
    }

    private S3MetaRequestWrapper s3MetaRequest() {
        try {
            return metaRequestFuture.get(s3MetaRequestTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            failResponseHandlerAndFuture(
                new RuntimeException("Timeout waiting for metaRequest to be ready", e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failResponseHandlerAndFuture(new RuntimeException(e));
        }
        return null;
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        // Note, we cannot call responseHandler.onHeaders() here because the response status code and headers may not represent
        // whether the request has succeeded or not (e.g. if this is for a HeadObject call that CRT calls under the hood). We
        // need to rely on onResponseBody/onFinished being called to determine this.
        populateSdkHttpResponse(initialHeadersResponse, statusCode, headers);
    }

    @Override
    public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
        // See reasoning in onResponseHeaders for why we call this here and not there.
        initiateResponseHandling(initialHeadersResponse.build());

        if (bodyBytesIn == null) {
            failResponseHandlerAndFuture(new IllegalStateException("ByteBuffer delivered is null"));
            return 0;
        }

        int bytesReceived = bodyBytesIn.remaining();
        CompletableFuture<Void> writeFuture = responsePublisher.send(bodyBytesIn);

        writeFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                failResponseHandlerAndFuture(failure);
                return;
            }

            S3MetaRequestWrapper metaRequest = s3MetaRequest();
            if (metaRequest == null) {
                return;
            }
            metaRequest.incrementReadWindow(bytesReceived);
        });

        // Returning 0 to disable flow control because we manually increase read window above
        return 0;
    }

    @Override
    public void onFinished(S3FinishedResponseContext context) {
        int crtCode = context.getErrorCode();
        if (crtCode != CRT.AWS_CRT_SUCCESS) {
            handleError(context);
        } else {
            // onResponseBody() is not invoked for responses with no content, so we may not have invoked
            // SdkAsyncHttpResponseHandler#onHeaders yet.
            // See also  reasoning in onResponseHeaders for why we call this here and not there.
            initiateResponseHandling(initialHeadersResponse.build());
            onSuccessfulResponseComplete();
        }
    }

    private void onSuccessfulResponseComplete() {
        responsePublisher.complete().whenComplete((result, failure) -> {
            if (failure != null) {
                failResponseHandlerAndFuture(failure);
                return;
            }
            this.progressListener.subscriberOnComplete();
            resultFuture.complete(null);
        });
    }

    private void handleError(S3FinishedResponseContext context) {
        int crtCode = context.getErrorCode();
        HttpHeader[] headers = context.getErrorHeaders();
        int responseStatus = context.getResponseStatus();
        byte[] errorPayload = context.getErrorPayload();

        if (isErrorResponse(responseStatus) && errorPayload != null) {
            SdkHttpResponse.Builder errorResponse = populateSdkHttpResponse(SdkHttpResponse.builder(),
                                                                            responseStatus, headers);
            initiateResponseHandling(errorResponse.build());
            onErrorResponseComplete(errorPayload);
        } else {
            Throwable cause = context.getCause();

            SdkClientException sdkClientException =
                SdkClientException.create("Failed to send the request: " +
                                          CRT.awsErrorString(crtCode), cause);
            failResponseHandlerAndFuture(sdkClientException);
        }
    }

    private void initiateResponseHandling(SdkHttpResponse response) {
        if (!responseHandlingInitiated) {
            responseHandlingInitiated = true;
            responseHandler.onHeaders(response);
            responseHandler.onStream(responsePublisher);
        }
    }

    private void onErrorResponseComplete(byte[] errorPayload) {
        responsePublisher.send(ByteBuffer.wrap(errorPayload))
                         .thenRun(responsePublisher::complete)
                         .handle((ignore, throwable) -> {
                             if (throwable != null) {
                                 failResponseHandlerAndFuture(throwable);
                                 return null;
                             }
                             resultFuture.complete(null);
                             return null;
                         });
    }

    private void failResponseHandlerAndFuture(Throwable exception) {
        runAndLogError(log.logger(), "Exception thrown in SdkAsyncHttpResponseHandler#onError, ignoring",
                       () -> responseHandler.onError(exception));
        resultFuture.completeExceptionally(exception);
    }

    private static boolean isErrorResponse(int responseStatus) {
        return responseStatus != 0;
    }

    @Override
    public void onProgress(S3MetaRequestProgress progress) {
        this.progressListener.subscriberOnNext(progress);
    }

    private static SdkHttpResponse.Builder populateSdkHttpResponse(SdkHttpResponse.Builder respBuilder,
                                                                   int statusCode, HttpHeader[] headers) {
        if (headers != null) {
            for (HttpHeader h : headers) {
                respBuilder.appendHeader(h.getName(), h.getValue());
            }
        }
        respBuilder.statusCode(statusCode);
        return respBuilder;
    }

    private static class NoOpPublisherListener implements PublisherListener<S3MetaRequestProgress> {
    }
}
