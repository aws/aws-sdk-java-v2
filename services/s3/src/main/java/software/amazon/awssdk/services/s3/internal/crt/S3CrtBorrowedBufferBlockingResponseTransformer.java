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

/**
 * Transforms a borrowed-buffer GetObject on the CRT client into a {@link ResponseInputStream}.
 *
 * <p>Body bytes arrive through {@link #currentAttempt()}. The publisher passed to {@link #onStream} is drained and ignored.
 * The future returned by {@link #prepare()} completes when the first body data or the successful completion arrives.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferBlockingResponseTransformer
    implements AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>>,
    S3CrtBorrowedBufferStreamHandlerFactory {

    private final AtomicBoolean aborted = new AtomicBoolean();
    private volatile Attempt currentAttempt;

    @Override
    public CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
        Attempt attempt = new Attempt();
        currentAttempt = attempt;
        if (aborted.get()) {
            attempt.onBorrowedStreamAbort();
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

    void abort() {
        aborted.set(true);
        Attempt attempt = currentAttempt;
        if (attempt != null) {
            attempt.onBorrowedStreamAbort();
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

    /**
     * The stream and borrowed-buffer callbacks for one request attempt. A cross-region redirect starts a second attempt, and
     * binding callbacks to an attempt keeps late callbacks from the first attempt out of the second attempt's stream.
     */
    private static final class Attempt implements S3CrtBorrowedBufferStreamHandler {
        private final CompletableFuture<ResponseInputStream<GetObjectResponse>> future = new CompletableFuture<>();
        private final DeferredCancellation cancellation = new DeferredCancellation();
        private final S3CrtBorrowedBufferInputStream inputStream =
            new S3CrtBorrowedBufferInputStream(cancellation::cancel);
        private volatile GetObjectResponse response;

        private Attempt() {
            future.whenComplete((ignored, error) -> {
                if (error != null) {
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

        @Override
        public void onBorrowedStreamStart(Runnable cancellationAction) {
            GetObjectResponse currentResponse = response;
            Validate.validState(currentResponse != null, "onResponse() must be called before borrowed delivery starts");
            cancellation.set(cancellationAction);
            future.complete(new ResponseInputStream<>(currentResponse, inputStream));
        }

        @Override
        public boolean onBorrowedBuffer(S3CrtBorrowedBufferLease buffer) {
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
