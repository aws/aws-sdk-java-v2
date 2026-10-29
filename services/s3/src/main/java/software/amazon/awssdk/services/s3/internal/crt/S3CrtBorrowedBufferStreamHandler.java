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

import java.util.concurrent.CompletableFuture;
import java.util.function.LongConsumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Receives the borrowed-buffer body of one {@code GetObject} attempt.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferStreamHandler {
    private final CompletableFuture<ResponseInputStream<GetObjectResponse>> future = new CompletableFuture<>();
    private final CompletableFuture<Runnable> cancellation = new CompletableFuture<>();
    private final S3CrtBorrowedBufferInputStream inputStream =
        new S3CrtBorrowedBufferInputStream(() -> cancellation.thenAccept(Runnable::run),
                                           bytes -> this.readWindowAction.accept(bytes));
    private volatile LongConsumer readWindowAction;
    private volatile GetObjectResponse response;

    S3CrtBorrowedBufferStreamHandler() {
        future.whenComplete((ignored, error) -> {
            if (error != null) {
                inputStream.abort();
            }
        });
    }

    CompletableFuture<ResponseInputStream<GetObjectResponse>> future() {
        return future;
    }

    void onResponse(GetObjectResponse response) {
        this.response = response;
    }

    /**
     * Publishes the response stream when the first buffer is accepted, or after an empty response completes.
     *
     * @param cancellationAction cancels the CRT request when the stream closes before completion
     * @param readWindowAction credits CRT after a fully read buffer is released
     */
    void onBorrowedStreamStart(Runnable cancellationAction, LongConsumer readWindowAction) {
        GetObjectResponse currentResponse = response;
        Validate.validState(currentResponse != null, "onResponse() must be called before borrowed delivery starts");
        this.readWindowAction = readWindowAction;
        cancellation.complete(cancellationAction);
        future.complete(new ResponseInputStream<>(currentResponse, inputStream));
    }

    /**
     * Returns false if the stream has already ended, in which case the buffer is released.
     */
    boolean onBorrowedBuffer(S3BorrowedBuffer buffer) {
        return inputStream.onBuffer(buffer);
    }

    /**
     * Ends the body. Reads return -1 after accepted buffers are drained.
     */
    void onBorrowedStreamComplete() {
        inputStream.onComplete();
    }

    /**
     * Reports an error after the reader drains queued buffers.
     */
    void onBorrowedStreamError(Throwable error) {
        inputStream.onError(error);
    }

    /**
     * Discards queued buffers and closes the stream.
     */
    void onBorrowedStreamAbort() {
        inputStream.abort();
    }

    /**
     * Discards queued buffers and causes later reads to fail with the error.
     */
    void onBorrowedStreamAbort(Throwable error) {
        inputStream.abort(error);
    }
}
