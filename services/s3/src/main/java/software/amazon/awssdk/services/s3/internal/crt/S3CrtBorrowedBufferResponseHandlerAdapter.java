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

import java.nio.ByteBuffer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class S3CrtBorrowedBufferResponseHandlerAdapter implements S3MetaRequestResponseHandler {
    private final S3CrtResponseHandlerAdapter delegate;
    private final CompletableFuture<Void> executeFuture;
    private final S3CrtBorrowedBufferStreamHandler streamHandler;
    private final CompletableFuture<S3MetaRequestWrapper> metaRequestFuture;
    private final AtomicBoolean streamInitiated = new AtomicBoolean();

    S3CrtBorrowedBufferResponseHandlerAdapter(
        CompletableFuture<Void> executeFuture,
        SdkAsyncHttpResponseHandler responseHandler,
        PublisherListener<S3MetaRequestProgress> progressListener,
        CompletableFuture<S3MetaRequestWrapper> metaRequestFuture,
        S3CrtBorrowedBufferStreamHandler streamHandler) {
        this.executeFuture = Validate.paramNotNull(executeFuture, "executeFuture");
        this.delegate = new S3CrtResponseHandlerAdapter(this.executeFuture, responseHandler, progressListener,
                                                       metaRequestFuture);
        this.streamHandler = Validate.paramNotNull(streamHandler, "streamHandler");
        this.metaRequestFuture = Validate.paramNotNull(metaRequestFuture, "metaRequestFuture");
        executeFuture.whenComplete((ignored, error) -> {
            if (error != null) {
                try {
                    Throwable timeout = findTimeout(error);
                    if (timeout != null) {
                        streamHandler.onBorrowedStreamAbort(timeout);
                    } else if (isCancellation(error)) {
                        streamHandler.onBorrowedStreamAbort();
                    } else {
                        streamHandler.onBorrowedStreamError(error);
                    }
                } catch (Throwable notificationFailure) {
                    addSuppressed(error, notificationFailure);
                }
            }
        });
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        delegate.onResponseHeaders(statusCode, headers);
    }

    @Override
    public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
        delegate.failResponseHandlerAndFuture(
            new IllegalStateException("Borrowed-buffer delivery did not use the CRT borrowed-body callback"));
        return 0;
    }

    @Override
    public int onResponseBody(S3BorrowedBuffer crtBuffer, long objectRangeStart, long objectRangeEnd) {
        S3CrtBorrowedBuffer sdkBuffer = null;
        try {
            Validate.paramNotNull(crtBuffer, "crtBuffer");
            delegate.initiateResponseHandlingForBorrowedResponse();
            ByteBuffer directView = crtBuffer.asByteBuffer();
            sdkBuffer = new S3CrtBorrowedBuffer(
                directView,
                directView.remaining(),
                crtBuffer::close,
                this::incrementReadWindow);
            streamHandler.onBorrowedBuffer(sdkBuffer);
            initiateStream();
        } catch (Throwable t) {
            failBorrowedResponse(t, sdkBuffer, crtBuffer);
        }
        return 0;
    }

    @Override
    public void onFinished(S3FinishedResponseContext context) {
        if (context.getErrorCode() == CRT.AWS_CRT_SUCCESS) {
            try {
                delegate.initiateResponseHandlingForBorrowedResponse();
                initiateStream();
                streamHandler.onBorrowedStreamComplete();
            } catch (Throwable t) {
                failBorrowedResponse(t, null, null);
                return;
            }
        }
        delegate.onFinished(context);
    }

    @Override
    public void onProgress(S3MetaRequestProgress progress) {
        delegate.onProgress(progress);
    }

    private static Throwable findTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ApiCallAttemptTimeoutException || current instanceof ApiCallTimeoutException) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }

    private static boolean isCancellation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof CancellationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void failBorrowedResponse(
        Throwable failure,
        S3CrtBorrowedBuffer sdkBuffer,
        S3BorrowedBuffer crtBuffer) {
        try {
            if (sdkBuffer != null) {
                sdkBuffer.discard();
            } else if (crtBuffer != null) {
                crtBuffer.close();
            }
        } catch (Throwable cleanupFailure) {
            addSuppressed(failure, cleanupFailure);
        }
        try {
            delegate.failResponseHandlerAndFuture(failure);
        } catch (Throwable notificationFailure) {
            addSuppressed(failure, notificationFailure);
        }
    }

    private static void addSuppressed(Throwable failure, Throwable additional) {
        if (failure != additional) {
            failure.addSuppressed(additional);
        }
    }

    private void initiateStream() {
        if (streamInitiated.compareAndSet(false, true)) {
            streamHandler.onBorrowedStreamStart(() -> executeFuture.cancel(true));
        }
    }

    private void incrementReadWindow(long bytes) {
        metaRequestFuture.whenComplete((metaRequest, error) -> {
            if (error != null) {
                delegate.failResponseHandlerAndFuture(error);
                return;
            }
            if (metaRequest == null) {
                delegate.failResponseHandlerAndFuture(
                    new IllegalStateException("The CRT S3 meta request is not available"));
                return;
            }
            try {
                metaRequest.incrementReadWindow(bytes);
            } catch (Throwable t) {
                delegate.failResponseHandlerAndFuture(t);
            }
        });
    }
}
