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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
final class S3CrtBorrowedBufferBlockingResponseTransformer
    implements AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>>,
    S3CrtBorrowedBufferStreamHandler,
    S3CrtBorrowedBufferStreamHandlerFactory {

    private final AtomicBoolean aborted = new AtomicBoolean();
    private volatile Attempt currentAttempt;

    @Override
    public CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
        Attempt previous = currentAttempt;
        Attempt attempt = new Attempt();
        currentAttempt = attempt;
        // A retry or a cross-region redirect calls prepare() again, which displaces the previous attempt. Nothing else
        // ever cleans that attempt up: it is only reached through this field, and its own adapter only notifies it when
        // its execute future completes exceptionally, which does not happen for an HTTP error response. Left alone it
        // keeps accepting buffers and holds any it has already queued, so the leases are only reclaimed later by CRT's
        // GC fallback.
        if (previous != null) {
            previous.abandon();
        }
        if (aborted.get()) {
            attempt.abort();
        }
        return attempt.future();
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        currentAttemptState().onResponse(response);
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        publisher.subscribe(new DrainingSubscriber<>());
    }

    @Override
    public S3CrtBorrowedBufferStreamHandler currentAttempt() {
        return currentAttemptState();
    }

    @Override
    public void onBorrowedStreamStart(Runnable cancellationAction) {
        currentAttemptState().onBorrowedStreamStart(cancellationAction);
    }

    @Override
    public boolean onBorrowedBuffer(S3CrtBorrowedBuffer buffer) {
        return currentAttemptState().onBorrowedBuffer(buffer);
    }

    @Override
    public void onBorrowedStreamComplete() {
        currentAttemptState().onBorrowedStreamComplete();
    }

    @Override
    public void onBorrowedStreamError(Throwable error) {
        Attempt attempt = currentAttempt;
        if (attempt != null) {
            attempt.onBorrowedStreamError(error);
        }
    }

    @Override
    public void onBorrowedStreamAbort() {
        Attempt attempt = currentAttempt;
        if (attempt != null) {
            attempt.onBorrowedStreamAbort();
        }
    }

    @Override
    public void onBorrowedStreamAbort(Throwable error) {
        Attempt attempt = currentAttempt;
        if (attempt != null) {
            attempt.onBorrowedStreamAbort(error);
        }
    }

    void abort() {
        aborted.set(true);
        Attempt attempt = currentAttempt;
        if (attempt != null) {
            attempt.abort();
        }
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        Attempt attempt = currentAttempt;
        if (attempt != null) {
            attempt.future().completeExceptionally(error);
            attempt.onBorrowedStreamError(error);
        }
    }

    @Override
    public String name() {
        return TransformerType.STREAM.getName();
    }

    private Attempt currentAttemptState() {
        return Validate.notNull(currentAttempt, "prepare() must be called before borrowed delivery starts");
    }

    private static final class Attempt implements S3CrtBorrowedBufferStreamHandler {
        private final CompletableFuture<ResponseInputStream<GetObjectResponse>> future = new CompletableFuture<>();
        private final DeferredCancellation cancellation = new DeferredCancellation();
        private final S3CrtBorrowedBufferInputStream inputStream =
            new S3CrtBorrowedBufferInputStream(cancellation::cancel);
        private final AtomicBoolean streamPublished = new AtomicBoolean();
        private volatile GetObjectResponse response;

        private Attempt() {
            future.whenComplete((ignored, error) -> {
                if (future.isCancelled()) {
                    inputStream.abort();
                }
            });
        }

        private CompletableFuture<ResponseInputStream<GetObjectResponse>> future() {
            return future;
        }

        private void onResponse(GetObjectResponse response) {
            this.response = response;
        }

        private void abort() {
            inputStream.abort();
        }

        /**
         * Releases what a displaced attempt is holding, after a retry has replaced it.
         *
         * <p>Deliberately conservative: if this attempt ever tried to hand a stream to the caller, it is left
         * untouched, because the caller may be holding that stream and reading from it. Only an attempt that never
         * published can be certain to have no reader, and closing that one discards buffers nobody can reach.
         */
        private void abandon() {
            if (!streamPublished.get()) {
                inputStream.abort();
            }
        }

        @Override
        public void onBorrowedStreamStart(Runnable cancellationAction) {
            GetObjectResponse currentResponse = response;
            Validate.validState(currentResponse != null, "onResponse() must be called before borrowed delivery starts");
            cancellation.set(cancellationAction);
            // Set before completing, so a prepare() that races this can never mistake a published stream for an
            // abandoned one and close it under its reader.
            streamPublished.set(true);
            future.complete(new ResponseInputStream<>(currentResponse, inputStream));
        }

        @Override
        public boolean onBorrowedBuffer(S3CrtBorrowedBuffer buffer) {
            return inputStream.onBuffer(buffer);
        }

        @Override
        public void onBorrowedStreamComplete() {
            inputStream.onComplete();
        }

        @Override
        public void onBorrowedStreamError(Throwable error) {
            inputStream.onError(error);
        }

        @Override
        public void onBorrowedStreamAbort() {
            inputStream.abort();
        }

        @Override
        public void onBorrowedStreamAbort(Throwable error) {
            inputStream.abort(error);
        }
    }

    private static final class DeferredCancellation {
        private final AtomicReference<Runnable> action = new AtomicReference<>();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean invoked = new AtomicBoolean();

        private void set(Runnable cancellationAction) {
            Validate.paramNotNull(cancellationAction, "cancellationAction");
            Validate.validState(action.compareAndSet(null, cancellationAction),
                                "Borrowed delivery cancellation is already configured");
            invokeIfReady();
        }

        private void cancel() {
            cancelled.set(true);
            invokeIfReady();
        }

        private void invokeIfReady() {
            Runnable cancellationAction = action.get();
            if (cancelled.get() && cancellationAction != null && invoked.compareAndSet(false, true)) {
                cancellationAction.run();
            }
        }
    }
}
