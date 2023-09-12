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
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequest;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.SdkCancellationException;
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
    private final CompletableFuture<Void> resultFuture;
    private final SdkAsyncHttpResponseHandler responseHandler;

    private final SimplePublisher<ByteBuffer> responsePublisher = new SimplePublisher<>();

    private final SdkHttpResponse.Builder respBuilder = SdkHttpResponse.builder();
    private volatile S3MetaRequest metaRequest;

    private final PublisherListener<S3MetaRequestProgress> progressListener;

    public S3CrtResponseHandlerAdapter(CompletableFuture<Void> executeFuture,
                                       SdkAsyncHttpResponseHandler responseHandler,
                                       PublisherListener<S3MetaRequestProgress> progressListener) {
        this.resultFuture = executeFuture;
        this.responseHandler = responseHandler;
        this.progressListener = progressListener == null ? new NoOpPublisherListener() : progressListener;
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        for (HttpHeader h : headers) {
            respBuilder.appendHeader(h.getName(), h.getValue());
        }

        respBuilder.statusCode(statusCode);
        responseHandler.onHeaders(respBuilder.build());
        responseHandler.onStream(responsePublisher);
    }

    @Override
    public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
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

            metaRequest.incrementReadWindow(bytesReceived);
        });

        // Returning 0 to disable flow control because we manually increase read window above
        return 0;
    }

    @Override
    public void onFinished(S3FinishedResponseContext context) {
        int crtCode = context.getErrorCode();
        int responseStatus = context.getResponseStatus();
        byte[] errorPayload = context.getErrorPayload();
        if (crtCode != CRT.AWS_CRT_SUCCESS) {
            handleError(crtCode, responseStatus, errorPayload);
        } else {
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
            completeFutureAndCloseRequest();
        });
    }

    private void completeFutureAndCloseRequest() {
        resultFuture.complete(null);
        runAndLogError(log.logger(), "Exception thrown in S3MetaRequest#close, ignoring",
                       () -> metaRequest.close());
    }

    public void cancelRequest() {
        SdkCancellationException sdkClientException =
            new SdkCancellationException("request is cancelled");
        failResponseHandlerAndFuture(sdkClientException);
    }

    private void handleError(int crtCode, int responseStatus, byte[] errorPayload) {
        if (isErrorResponse(responseStatus) && errorPayload != null) {
            onErrorResponseComplete(errorPayload);
        } else {
            SdkClientException sdkClientException =
                SdkClientException.create("Failed to send the request: " +
                                          CRT.awsErrorString(crtCode));
            failResponseHandlerAndFuture(sdkClientException);
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
                             completeFutureAndCloseRequest();
                             return null;
                         });
    }

    private void failResponseHandlerAndFuture(Throwable exception) {
        resultFuture.completeExceptionally(exception);
        runAndLogError(log.logger(), "Exception thrown in SdkAsyncHttpResponseHandler#onError, ignoring",
                       () -> responseHandler.onError(exception));
        runAndLogError(log.logger(), "Exception thrown in S3MetaRequest#close, ignoring",
                       () -> metaRequest.close());
    }

    private static boolean isErrorResponse(int responseStatus) {
        return responseStatus != 0;
    }

    public void metaRequest(S3MetaRequest s3MetaRequest) {
        metaRequest = s3MetaRequest;
    }

    @Override
    public void onProgress(S3MetaRequestProgress progress) {
        this.progressListener.subscriberOnNext(progress);
    }

    private static class NoOpPublisherListener implements PublisherListener<S3MetaRequestProgress> {
    }
}
