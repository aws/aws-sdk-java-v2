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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.s3.internal.crt.BorrowedBufferTestUtils.borrowedBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.crt.s3.S3BorrowedBuffer;
import software.amazon.awssdk.services.s3.crt.S3CrtResponseTransformerFactory;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3CrtBorrowedBufferBlockingResponseTransformerTest {
    private static final LongConsumer NO_CREDIT = ignored -> { };

    @Test
    void borrowedStream_whenConsumed_shouldReturnResponseAndReleaseWithCredit() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3BorrowedBuffer buffer = borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                   releases::incrementAndGet);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        GetObjectResponse response = GetObjectResponse.builder().contentLength(3L).build();
        transformer.onResponse(response);
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();

        handler.onBorrowedStreamStart(() -> {
        }, credit::addAndGet);
        handler.onBorrowedBuffer(buffer);
        handler.onBorrowedStreamComplete();

        try (ResponseInputStream<GetObjectResponse> stream = future.join()) {
            byte[] bytes = new byte[3];
            assertThat(stream.response()).isSameAs(response);
            assertThat(stream.read(bytes)).isEqualTo(3);
            assertThat(bytes).isEqualTo("abc".getBytes(UTF_8));
            assertThat(stream.read()).isEqualTo(-1);
        }
        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(3);
    }

    @Test
    void abort_shouldCancelCurrentAndLaterAttempts() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler firstAttempt = transformer.currentAttempt();

        transformer.abort();
        transformer.exceptionOccurred(new IOException("redirect"));
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler secondAttempt = transformer.currentAttempt();

        firstAttempt.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);
        secondAttempt.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);

        assertThat(cancellations).hasValue(2);
    }

    @Test
    void timeout_shouldDiscardWithoutCreditAndRemainVisibleToReader() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3BorrowedBuffer buffer = borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                   releases::incrementAndGet);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedBuffer(buffer);
        handler.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);
        ResponseInputStream<GetObjectResponse> stream = future.join();
        ApiCallTimeoutException timeout = ApiCallTimeoutException.create(1);

        handler.onBorrowedStreamAbort(timeout);

        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
        assertThat(cancellations).hasValue(1);
        assertThatThrownBy(stream::read).isSameAs(timeout);
    }

    @Test
    void futureCancelledBeforeStreamStart_shouldRunCancellationWhenStarted() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();

        future.cancel(true);
        handler.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);

        assertThat(cancellations).hasValue(1);
    }

    @Test
    void exceptionalFutureBeforeStreamStart_shouldReleaseAcceptedBufferWithoutCredit() {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                 releases::incrementAndGet));

        future.completeExceptionally(new RuntimeException("request failed"));

        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
    }

    @Test
    void failedFutureThenLateStreamStart_shouldKeepFutureFailedAndDiscardLateBuffers() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();

        future.completeExceptionally(new RuntimeException("request failed"));
        handler.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);
        assertThat(handler.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                            releases::incrementAndGet))).isFalse();

        assertThat(future).isCompletedExceptionally();
        assertThat(cancellations).hasValue(1);
        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
    }

    @Test
    void exceptionOccurredAfterPublication_shouldRemainVisibleToReader() throws Exception {
        RuntimeException failure = new RuntimeException("request failed");
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        transformer.currentAttempt().onBorrowedStreamStart(() -> { }, NO_CREDIT);

        try (ResponseInputStream<GetObjectResponse> stream = future.join()) {
            transformer.exceptionOccurred(failure);

            assertThatThrownBy(stream::read).isInstanceOf(IOException.class).hasCause(failure);
        }
    }

    @Test
    void remoteErrorAfterPublication_shouldLeaveAcceptedBytesReadableBeforeFailing() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        RuntimeException failure = new RuntimeException("request failed");
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedStreamStart(() -> { }, credit::addAndGet);
        handler.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                 releases::incrementAndGet));
        handler.onBorrowedStreamError(failure);

        try (ResponseInputStream<GetObjectResponse> stream = future.join()) {
            byte[] bytes = new byte[3];
            assertThat(stream.read(bytes)).isEqualTo(3);
            assertThat(bytes).isEqualTo("abc".getBytes(UTF_8));
            assertThatThrownBy(stream::read).isInstanceOf(IOException.class).hasCause(failure);
        }
        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(3);
    }

    @Test
    void factory_shouldReturnDirectBufferPoolMarker() {
        AsyncResponseTransformer<GetObjectResponse, ?> transformer =
            S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool();

        assertThat(transformer).isInstanceOf(S3CrtDirectBufferPoolStreamMarker.class);
        assertThat(transformer.name())
            .isEqualTo(AsyncResponseTransformer.TransformerType.DIRECT_BUFFER_POOL_STREAM.getName());
    }

    @Test
    void prepare_afterCurrentAttemptFailed_shouldStartNewAttempt() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        transformer.prepare();
        S3CrtBorrowedBufferStreamHandler firstAttempt = transformer.currentAttempt();
        transformer.exceptionOccurred(new RuntimeException("redirect"));

        CompletableFuture<ResponseInputStream<GetObjectResponse>> secondFuture = transformer.prepare();

        assertThat(secondFuture).isNotDone();
        assertThat(transformer.currentAttempt()).isNotSameAs(firstAttempt);
    }

    private static S3CrtBorrowedBufferBlockingResponseTransformer newTransformer() {
        return new S3CrtBorrowedBufferBlockingResponseTransformer();
    }
}
