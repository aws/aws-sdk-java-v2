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
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpRequestBodyStream;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber;

/**
 * Adapts an SDK {@link software.amazon.awssdk.core.async.AsyncRequestBody} to CRT's {@link HttpRequestBodyStream}.
 */
@SdkInternalApi
public final class S3CrtRequestBodyStreamAdapter implements HttpRequestBodyStream {
    private static final long MINIMUM_BYTES_BUFFERED = 16 * 1024 * 1024L;
    private final SdkHttpContentPublisher bodyPublisher;
    private final ByteBufferStoringSubscriber requestBodySubscriber;
    private final CompletableFuture<Void> executeFuture;

    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    public S3CrtRequestBodyStreamAdapter(SdkHttpContentPublisher bodyPublisher, CompletableFuture<Void> executeFuture) {
        this.bodyPublisher = bodyPublisher;
        this.requestBodySubscriber = new ByteBufferStoringSubscriber(MINIMUM_BYTES_BUFFERED);
        this.executeFuture = executeFuture;
    }

    @Override
    public boolean sendRequestBody(ByteBuffer outBuffer) {
        if (subscribed.compareAndSet(false, true)) {
            bodyPublisher.subscribe(requestBodySubscriber);
        }

        try {
            return requestBodySubscriber.transferTo(outBuffer) == ByteBufferStoringSubscriber.TransferResult.END_OF_STREAM;
        } catch (RuntimeException e) {
            // This is a native JNI upcall: an escaping exception is printed to stderr and discarded by CRT (#6715).
            // Fail the operation with the original publisher error instead; completing executeFuture cancels the meta request.
            executeFuture.completeExceptionally(e);
            return false;
        }
    }

    @Override
    public long getLength() {
        return bodyPublisher.contentLength().orElse(0L);
    }
}
