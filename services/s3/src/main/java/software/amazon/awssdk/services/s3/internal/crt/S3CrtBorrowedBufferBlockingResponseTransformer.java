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
    S3CrtBorrowedBufferStreamHandler {

    private volatile CompletableFuture<ResponseInputStream<GetObjectResponse>> future;
    private volatile GetObjectResponse response;
    private volatile S3CrtBorrowedBufferInputStream inputStream;
    private volatile DeferredCancellation cancellation;

    @Override
    public CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
        CompletableFuture<ResponseInputStream<GetObjectResponse>> result = new CompletableFuture<>();
        DeferredCancellation deferredCancellation = new DeferredCancellation();
        S3CrtBorrowedBufferInputStream stream =
            new S3CrtBorrowedBufferInputStream(deferredCancellation::cancel);
        result.whenComplete((ignored, error) -> {
            if (result.isCancelled()) {
                stream.abort();
            }
        });
        this.response = null;
        this.cancellation = deferredCancellation;
        this.inputStream = stream;
        this.future = result;
        return result;
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        this.response = response;
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        publisher.subscribe(new DrainingSubscriber<>());
    }

    @Override
    public void onBorrowedStreamStart(Runnable cancellationAction) {
        CompletableFuture<ResponseInputStream<GetObjectResponse>> currentFuture = future;
        S3CrtBorrowedBufferInputStream currentStream = inputStream;
        DeferredCancellation currentCancellation = cancellation;
        GetObjectResponse currentResponse = response;
        Validate.validState(currentFuture != null, "prepare() must be called before borrowed delivery starts");
        Validate.validState(currentStream != null, "prepare() must be called before borrowed delivery starts");
        Validate.validState(currentCancellation != null, "prepare() must be called before borrowed delivery starts");
        Validate.validState(currentResponse != null, "onResponse() must be called before borrowed delivery starts");
        currentCancellation.set(cancellationAction);
        currentFuture.complete(new ResponseInputStream<>(currentResponse, currentStream));
    }

    @Override
    public void onBorrowedBuffer(S3CrtBorrowedBuffer buffer) {
        S3CrtBorrowedBufferInputStream currentStream = inputStream;
        Validate.validState(currentStream != null, "prepare() must be called before borrowed buffers are delivered");
        currentStream.onBuffer(buffer);
    }

    @Override
    public void onBorrowedStreamComplete() {
        S3CrtBorrowedBufferInputStream currentStream = inputStream;
        Validate.validState(currentStream != null, "prepare() must be called before borrowed delivery completes");
        currentStream.onComplete();
    }

    @Override
    public void onBorrowedStreamError(Throwable error) {
        S3CrtBorrowedBufferInputStream currentStream = inputStream;
        if (currentStream != null) {
            currentStream.onError(error);
        }
    }

    @Override
    public void onBorrowedStreamAbort() {
        S3CrtBorrowedBufferInputStream currentStream = inputStream;
        if (currentStream != null) {
            currentStream.abort();
        }
    }

    @Override
    public void onBorrowedStreamAbort(Throwable error) {
        S3CrtBorrowedBufferInputStream currentStream = inputStream;
        if (currentStream != null) {
            currentStream.abort(error);
        }
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        CompletableFuture<ResponseInputStream<GetObjectResponse>> currentFuture = future;
        if (currentFuture != null) {
            currentFuture.completeExceptionally(error);
        }
        onBorrowedStreamError(error);
    }

    @Override
    public String name() {
        return TransformerType.STREAM.getName();
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
