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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.services.s3.crt.S3CrtAsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3CrtBorrowedBufferBlockingResponseTransformerTest {

    @Test
    void borrowedStream_whenConsumed_shouldReturnResponseAndReleaseWithCredit() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferLease buffer = new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                                         releases::incrementAndGet,
                                                                         credit::addAndGet);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        GetObjectResponse response = GetObjectResponse.builder().contentLength(3L).build();
        transformer.onResponse(response);
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();

        handler.onBorrowedStreamStart(() -> {
        });
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
    void lateAttemptCallback_shouldNotAffectCurrentAttempt() throws Exception {
        AtomicInteger lateReleases = new AtomicInteger();
        AtomicLong lateCredit = new AtomicLong();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> firstFuture = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(1L).build());
        S3CrtBorrowedBufferStreamHandler firstAttempt = transformer.currentAttempt();
        transformer.exceptionOccurred(new IOException("redirect"));
        assertThat(firstFuture).isCompletedExceptionally();

        CompletableFuture<ResponseInputStream<GetObjectResponse>> secondFuture = transformer.prepare();
        GetObjectResponse secondResponse = GetObjectResponse.builder().contentLength(3L).build();
        transformer.onResponse(secondResponse);
        S3CrtBorrowedBufferStreamHandler secondAttempt = transformer.currentAttempt();
        secondAttempt.onBorrowedStreamStart(() -> {
        });

        firstAttempt.onBorrowedStreamStart(() -> {
        });
        assertThat(firstAttempt.onBorrowedBuffer(new S3CrtBorrowedBufferLease(ByteBuffer.wrap("x".getBytes(UTF_8)),
                                                                               lateReleases::incrementAndGet,
                                                                               lateCredit::addAndGet))).isFalse();
        firstAttempt.onBorrowedStreamError(new IOException("late attempt failure"));
        secondAttempt.onBorrowedBuffer(new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                                     () -> {
                                                                     },
                                                                     ignored -> {
                                                                     }));
        secondAttempt.onBorrowedStreamComplete();

        try (ResponseInputStream<GetObjectResponse> secondStream = secondFuture.join()) {
            assertThat(secondStream.response()).isSameAs(secondResponse);
            assertThat(secondStream.read()).isEqualTo('a');
            assertThat(secondStream.read()).isEqualTo('b');
            assertThat(secondStream.read()).isEqualTo('c');
            assertThat(secondStream.read()).isEqualTo(-1);
        }
        assertThat(lateReleases).hasValue(1);
        assertThat(lateCredit).hasValue(0);
    }

    @Test
    void abort_shouldCancelCurrentAndLaterAttempts() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler firstAttempt = transformer.currentAttempt();

        transformer.abort();
        transformer.exceptionOccurred(new IOException("redirect"));
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler secondAttempt = transformer.currentAttempt();

        firstAttempt.onBorrowedStreamStart(cancellations::incrementAndGet);
        secondAttempt.onBorrowedStreamStart(cancellations::incrementAndGet);

        assertThat(cancellations).hasValue(2);
    }

    @Test
    void timeout_shouldDiscardWithoutCreditAndRemainVisibleToReader() throws Exception {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferLease buffer = new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                                         releases::incrementAndGet,
                                                                         credit::addAndGet);
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedBuffer(buffer);
        handler.onBorrowedStreamStart(cancellations::incrementAndGet);
        ResponseInputStream<GetObjectResponse> stream = future.join();
        ApiCallTimeoutException timeout = ApiCallTimeoutException.create(1);

        handler.onBorrowedStreamAbort(timeout);

        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
        assertThat(cancellations).hasValue(1);
        assertThatThrownBy(stream::read)
            .isInstanceOf(IOException.class)
            .hasCause(timeout);
    }

    @Test
    void futureCancelledBeforeStreamStart_shouldRunCancellationWhenStarted() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();

        future.cancel(true);
        handler.onBorrowedStreamStart(cancellations::incrementAndGet);

        assertThat(cancellations).hasValue(1);
    }

    @Test
    void exceptionalFutureBeforeStreamStart_shouldReleaseAcceptedBufferWithoutCredit() {
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedBuffer(new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                               releases::incrementAndGet,
                                                               credit::addAndGet));

        future.completeExceptionally(new RuntimeException("request failed"));

        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
    }

    @Test
    void failedFutureThenLateStreamStart_shouldKeepFutureFailedAndDiscardLateBuffers() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();

        future.completeExceptionally(new RuntimeException("request failed"));
        handler.onBorrowedStreamStart(cancellations::incrementAndGet);
        assertThat(handler.onBorrowedBuffer(new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                                          releases::incrementAndGet,
                                                                          credit::addAndGet))).isFalse();

        assertThat(future).isCompletedExceptionally();
        assertThat(cancellations).hasValue(1);
        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
    }

    @Test
    void exceptionOccurredAfterPublication_shouldRemainVisibleToReader() throws Exception {
        RuntimeException failure = new RuntimeException("request failed");
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        transformer.currentAttempt().onBorrowedStreamStart(() -> { });

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
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedStreamStart(() -> { });
        handler.onBorrowedBuffer(new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                               releases::incrementAndGet,
                                                               credit::addAndGet));
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
    void factory_shouldReturnBorrowedStreamTransformer() {
        AsyncResponseTransformer<GetObjectResponse, ?> transformer =
            S3CrtAsyncResponseTransformer.toBlockingInputStreamFromDirectBufferPool();

        assertThat(transformer).isInstanceOf(S3CrtBorrowedBufferBlockingResponseTransformer.class);
        assertThat(transformer.name()).isEqualTo(AsyncResponseTransformer.TransformerType.STREAM.getName());
    }

    @Test
    void prepare_withoutActivation_shouldFailClearlyWithoutMaskingOriginalException() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = new S3CrtBorrowedBufferBlockingResponseTransformer();

        assertThatThrownBy(transformer::prepare)
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("S3 CRT async client");
        assertThatCode(() -> transformer.exceptionOccurred(new RuntimeException("original"))).doesNotThrowAnyException();
    }

    @Test
    void activate_whenAlreadyActivated_shouldFail() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();

        assertThatThrownBy(transformer::activate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been used");
    }

    @Test
    void prepare_afterDone_shouldFail() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        transformer.prepare();
        transformer.done();

        assertThatThrownBy(transformer::prepare)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been used");
        assertThatThrownBy(transformer::activate).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prepare_whileCurrentAttemptPending_shouldFailWithoutReplacingIt() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        transformer.prepare();
        S3CrtBorrowedBufferStreamHandler attempt = transformer.currentAttempt();

        assertThatThrownBy(transformer::prepare)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been used");
        assertThat(transformer.currentAttempt()).isSameAs(attempt);
    }

    @Test
    void prepare_afterCurrentAttemptFailed_shouldStartNewAttempt() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        transformer.prepare();
        S3CrtBorrowedBufferStreamHandler firstAttempt = transformer.currentAttempt();
        transformer.exceptionOccurred(new RuntimeException("redirect"));

        CompletableFuture<ResponseInputStream<GetObjectResponse>> secondFuture = transformer.prepare();

        assertThat(secondFuture).isNotDone();
        assertThat(transformer.currentAttempt()).isNotSameAs(firstAttempt);
    }

    @Test
    void prepare_afterStreamPublished_shouldFailWithoutAffectingPublishedStream() throws Exception {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        S3CrtBorrowedBufferStreamHandler handler = transformer.currentAttempt();
        handler.onBorrowedStreamStart(() -> {
        });

        Throwable rejection = catchThrowable(transformer::prepare);
        assertThat(rejection).isInstanceOf(IllegalStateException.class).hasMessageContaining("already been used");
        transformer.exceptionOccurred(new RuntimeException(rejection));

        handler.onBorrowedBuffer(new S3CrtBorrowedBufferLease(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                               () -> {
                                                               },
                                                               ignored -> {
                                                               }));
        handler.onBorrowedStreamComplete();
        try (ResponseInputStream<GetObjectResponse> stream = future.join()) {
            byte[] bytes = new byte[3];
            assertThat(stream.read(bytes)).isEqualTo(3);
            assertThat(bytes).isEqualTo("abc".getBytes(UTF_8));
            assertThat(stream.read()).isEqualTo(-1);
        }
    }

    @Test
    void rejectedPrepareWhileAttemptPending_shouldNotFailCurrentAttempt() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = activatedTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();

        Throwable rejection = catchThrowable(transformer::prepare);
        transformer.exceptionOccurred(rejection);

        assertThat(rejection).isInstanceOf(IllegalStateException.class);
        assertThat(future).isNotDone();
    }

    private static S3CrtBorrowedBufferBlockingResponseTransformer activatedTransformer() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = new S3CrtBorrowedBufferBlockingResponseTransformer();
        transformer.activate();
        return transformer;
    }
}
