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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.DrainingSubscriber;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Transforms a {@link GetObjectResponse} and its borrowed-buffer body on the CRT client into a {@link ResponseInputStream}.
 *
 * <p>Body bytes arrive through {@link #currentAttempt()}. The future returned by {@link #prepare()} completes when the
 * first body data or the successful completion arrives.
 *
 * <p>A cross-region redirect starts a new attempt after the previous attempt fails. Each attempt keeps its callbacks
 * separate so late callbacks cannot affect the replacement attempt.
 */
@SdkInternalApi
public final class S3CrtBorrowedBufferBlockingResponseTransformer
    implements AsyncResponseTransformer<GetObjectResponse, ResponseInputStream<GetObjectResponse>> {

    private volatile boolean aborted;
    private volatile S3CrtBorrowedBufferStreamHandler currentAttempt;

    @Override
    public synchronized CompletableFuture<ResponseInputStream<GetObjectResponse>> prepare() {
        S3CrtBorrowedBufferStreamHandler previousAttempt = currentAttempt;
        if (previousAttempt != null && !previousAttempt.future().isCompletedExceptionally()) {
            throw new IllegalStateException("The current borrowed-buffer response attempt has not failed.");
        }
        S3CrtBorrowedBufferStreamHandler attempt = new S3CrtBorrowedBufferStreamHandler();
        currentAttempt = attempt;
        if (aborted) {
            attempt.onBorrowedStreamAbort();
        }
        return attempt.future();
    }

    @Override
    public void onResponse(GetObjectResponse response) {
        currentAttempt.onResponse(response);
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        // Body bytes arrive through the borrowed-buffer callbacks, so this publisher is empty.
        // It still needs a subscriber, because the execution future completes only after the publisher's completion is delivered.
        publisher.subscribe(new DrainingSubscriber<>());
    }

    public S3CrtBorrowedBufferStreamHandler currentAttempt() {
        return currentAttempt;
    }

    void abort() {
        aborted = true;
        S3CrtBorrowedBufferStreamHandler attempt = currentAttempt;
        if (attempt != null) {
            attempt.onBorrowedStreamAbort();
        }
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        S3CrtBorrowedBufferStreamHandler attempt = currentAttempt;
        if (attempt != null) {
            attempt.future().completeExceptionally(error);
            attempt.onBorrowedStreamError(error);
        }
    }

    @Override
    public String name() {
        return TransformerType.DIRECT_BUFFER_POOL_STREAM.getName();
    }
}
