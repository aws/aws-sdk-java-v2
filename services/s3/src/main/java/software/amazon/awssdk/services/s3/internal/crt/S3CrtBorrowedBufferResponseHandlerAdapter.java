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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.listener.PublisherListener;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.crt.s3.S3BorrowedBufferDelivery;
import software.amazon.awssdk.crt.s3.S3FinishedResponseContext;
import software.amazon.awssdk.crt.s3.S3MetaRequestProgress;
import software.amazon.awssdk.crt.s3.S3MetaRequestResponseHandler;
import software.amazon.awssdk.http.async.SdkAsyncHttpResponseHandler;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.ContentRangeParser;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

/**
 * Adapts CRT borrowed-buffer {@link GetObjectResponse} callbacks to an {@link S3CrtBorrowedBufferStreamHandler}.
 *
 * <p>Headers, progress and request completion go through {@link S3CrtResponseHandlerAdapter}.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferResponseHandlerAdapter
    implements S3MetaRequestResponseHandler, S3BorrowedBufferDelivery {
    private static final Logger log = Logger.loggerFor(S3CrtBorrowedBufferResponseHandlerAdapter.class);

    private final S3CrtResponseHandlerAdapter delegate;
    private final CompletableFuture<Void> executeFuture;
    private final S3CrtBorrowedBufferStreamHandler streamHandler;
    private final CompletableFuture<S3MetaRequestWrapper> metaRequestFuture;
    private final AtomicBoolean streamInitiated = new AtomicBoolean();
    private long nextObjectRangeStart;

    private S3CrtBorrowedBufferResponseHandlerAdapter(Builder builder) {
        this.executeFuture = builder.executeFuture;
        this.delegate = new S3CrtResponseHandlerAdapter(this.executeFuture, builder.responseHandler, builder.progressListener,
                                                       builder.metaRequestFuture);
        this.streamHandler = Validate.paramNotNull(builder.streamHandler, "streamHandler");
        this.metaRequestFuture = builder.metaRequestFuture;
        executeFuture.whenComplete((ignored, error) -> {
            if (error != null) {
                try {
                    if (!streamInitiated.get()) {
                        streamHandler.onBorrowedStreamAbort(error);
                    } else {
                        streamHandler.onBorrowedStreamError(error);
                    }
                } catch (Throwable notificationFailure) {
                    log.error(() -> "Failed to notify borrowed response stream of request failure", notificationFailure);
                }
            }
        });
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public void onResponseHeaders(int statusCode, HttpHeader[] headers) {
        nextObjectRangeStart = initialRangeStart(headers);
        delegate.onResponseHeaders(statusCode, headers);
    }

    @Override
    public int onResponseBody(ByteBuffer bodyBytesIn, long objectRangeStart, long objectRangeEnd) {
        delegate.failResponseHandlerAndFuture(
            new IllegalStateException("Borrowed-buffer delivery did not use the CRT borrowed-body callback"));
        return 0;
    }

    @Override
    public int onResponseBodyBorrowed(S3BorrowedBuffer crtBuffer, long objectRangeStart, long objectRangeEnd) {
        boolean acceptedByStream = false;
        try {
            Validate.paramNotNull(crtBuffer, "crtBuffer");
            delegate.initiateResponseHandlingForBorrowedResponse();
            // The stream appends buffers in arrival order, so fail if CRT ever delivers them out of order.
            Validate.validState(objectRangeStart == nextObjectRangeStart,
                                "CRT borrowed buffer started at object offset %d, but the expected offset was %d",
                                objectRangeStart, nextObjectRangeStart);
            nextObjectRangeStart = objectRangeEnd;
            acceptedByStream = streamHandler.onBorrowedBuffer(crtBuffer);
            initiateStream();
        } catch (Throwable t) {
            if (acceptedByStream) {
                // initiateStream() threw before publication, so no reader can hold these bytes.
                try {
                    streamHandler.onBorrowedStreamAbort(t);
                } catch (Throwable cleanupFailure) {
                    log.error(() -> "Failed to abort borrowed response stream", cleanupFailure);
                }
            } else {
                releaseUnacceptedBuffer(crtBuffer);
            }
            failBorrowedResponse(t);
        }
        // The read window grows only as the reader consumes buffers.
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
                failBorrowedResponse(t);
                return;
            }
        }
        delegate.onFinished(context);
    }

    @Override
    public void onProgress(S3MetaRequestProgress progress) {
        delegate.onProgress(progress);
    }

    // TODO: Forward onTelemetry() to the delegate once S3CrtResponseHandlerAdapter publishes metrics from it.

    private static long initialRangeStart(HttpHeader[] headers) {
        if (headers == null) {
            return 0;
        }
        for (HttpHeader header : headers) {
            if (header != null && "Content-Range".equalsIgnoreCase(header.getName())) {
                Optional<Pair<Long, Long>> range = ContentRangeParser.range(header.getValue());
                if (range.isPresent() && range.get().left() >= 0) {
                    return range.get().left();
                }
                return 0;
            }
        }
        return 0;
    }

    private static void releaseUnacceptedBuffer(S3BorrowedBuffer crtBuffer) {
        if (crtBuffer == null) {
            return;
        }
        try {
            crtBuffer.close();
        } catch (Throwable cleanupFailure) {
            log.error(() -> "Failed to release unaccepted borrowed response buffer", cleanupFailure);
        }
    }

    private void failBorrowedResponse(Throwable failure) {
        try {
            delegate.failResponseHandlerAndFuture(failure);
        } catch (Throwable notificationFailure) {
            log.error(() -> "Failed to report borrowed response failure", notificationFailure);
        }
    }

    private void initiateStream() {
        if (streamInitiated.compareAndSet(false, true)) {
            streamHandler.onBorrowedStreamStart(() -> executeFuture.cancel(true), this::incrementReadWindow);
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

    static final class Builder {
        private CompletableFuture<Void> executeFuture;
        private SdkAsyncHttpResponseHandler responseHandler;
        private PublisherListener<S3MetaRequestProgress> progressListener;
        private CompletableFuture<S3MetaRequestWrapper> metaRequestFuture;
        private S3CrtBorrowedBufferStreamHandler streamHandler;

        Builder executeFuture(CompletableFuture<Void> executeFuture) {
            this.executeFuture = executeFuture;
            return this;
        }

        Builder responseHandler(SdkAsyncHttpResponseHandler responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }

        Builder progressListener(PublisherListener<S3MetaRequestProgress> progressListener) {
            this.progressListener = progressListener;
            return this;
        }

        Builder metaRequestFuture(CompletableFuture<S3MetaRequestWrapper> metaRequestFuture) {
            this.metaRequestFuture = metaRequestFuture;
            return this;
        }

        Builder streamHandler(S3CrtBorrowedBufferStreamHandler streamHandler) {
            this.streamHandler = streamHandler;
            return this;
        }

        S3CrtBorrowedBufferResponseHandlerAdapter build() {
            return new S3CrtBorrowedBufferResponseHandlerAdapter(this);
        }
    }
}
