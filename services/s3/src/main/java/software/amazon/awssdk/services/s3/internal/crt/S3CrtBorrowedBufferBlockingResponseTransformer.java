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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Transforms a {@link GetObjectResponse} and its borrowed-buffer body on the CRT client into a {@link ResponseInputStream}.
 *
 * <p>Body bytes arrive through the borrowed-buffer callbacks from {@link S3CrtBorrowedBufferResponseHandlerAdapter}. The
 * future returned by {@link #prepare()} completes when the response stream is initialized. For a body response, this
 * happens before the first borrowed buffer is delivered. For an empty response, it happens before the stream completes.
 *
 * <p>A cross-region redirect calls {@link #prepare()} again for the new request. The response stream is created when it
 * is published, so only a successful response creates one.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferBlockingResponseTransformer
    implements AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> {

    // Lets abort() take effect when it runs before onBorrowedStreamStart() creates the stream.
    private volatile boolean aborted;
    private volatile CompletableFuture<ResponseInputStream<GetObjectResponse>> future;
    private volatile GetObjectResponse response;
    private volatile S3CrtBorrowedBufferInputStream inputStream;

    @Override
    public CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
        CompletableFuture<ResponseInputStream<GetObjectResponse>> result = new CompletableFuture<>();
        future = result;
        return result;
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        // Body bytes arrive through the borrowed-buffer callbacks, so this publisher is empty.
        // It still needs a subscriber, because the execution future completes only after the publisher's completion is delivered.
        publisher.subscribe(new EmptyBodySubscriber(this::exceptionOccurred));
    }

    void abort() {
        aborted = true;
        if (inputStream != null) {
            inputStream.abort();
        }
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        if (future != null) {
            future.completeExceptionally(error);
        }
        onBorrowedStreamError(error);
    }

    @Override
    public String name() {
        return TransformerType.DIRECT_BUFFER_POOL_STREAM.getName();
    }

    /**
     * Creates and publishes the response stream, before the first buffer is delivered or after an empty response completes.
     *
     * @param cancellationAction cancels the CRT request when the stream closes before completion
     * @param readWindowAction credits CRT after a fully read buffer is released
     */
    void onBorrowedStreamStart(Runnable cancellationAction, LongConsumer readWindowAction) {
        Validate.validState(response != null, "onResponse() must be called before borrowed delivery starts");
        Validate.validState(inputStream == null, "The borrowed response stream has already started");
        S3CrtBorrowedBufferInputStream stream = new S3CrtBorrowedBufferInputStream(cancellationAction, readWindowAction);
        inputStream = stream;
        future.whenComplete((ignored, error) -> {
            if (error != null) {
                stream.abort();
            }
        });
        if (aborted) {
            stream.abort();
        }
        future.complete(new ResponseInputStream<>(response, stream));
    }

    /**
     * Returns false if the stream has already ended, in which case the buffer is released.
     */
    boolean onBorrowedBuffer(S3BorrowedBuffer buffer) {
        return stream().onNextBuffer(buffer);
    }

    /**
     * Ends the body. Reads return -1 after accepted buffers are drained.
     */
    void onBorrowedStreamComplete() {
        stream().onComplete();
    }

    /**
     * Reports an error after the reader drains queued buffers.
     */
    void onBorrowedStreamError(Throwable error) {
        if (inputStream != null) {
            inputStream.onError(error);
        }
    }

    /**
     * Subscribes to the empty response publisher without requesting data. Body bytes or an error from the publisher fail the
     * request.
     */
    private static final class EmptyBodySubscriber implements Subscriber<ByteBuffer> {
        private final Consumer<Throwable> onError;
        private Subscription subscription;

        private EmptyBodySubscriber(Consumer<Throwable> onError) {
            this.onError = onError;
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (subscription != null) {
                s.cancel();
                return;
            }
            subscription = s;
            // Don't request data, since the body never goes through this publisher.
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            subscription.cancel();
            onError.accept(new IllegalStateException("The borrowed response body was delivered through the response publisher"));
        }

        @Override
        public void onError(Throwable t) {
            onError.accept(t);
        }

        @Override
        public void onComplete() {
        }
    }

    private S3CrtBorrowedBufferInputStream stream() {
        Validate.validState(inputStream != null, "onBorrowedStreamStart() must be called before buffers are delivered");
        return inputStream;
    }
}
