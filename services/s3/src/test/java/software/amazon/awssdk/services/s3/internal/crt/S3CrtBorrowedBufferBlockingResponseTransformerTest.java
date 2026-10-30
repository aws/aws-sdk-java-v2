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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
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

        transformer.onBorrowedStreamStart(() -> {
        }, credit::addAndGet);
        transformer.onBorrowedBuffer(buffer);
        transformer.onBorrowedStreamComplete();

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
    void responseListener_shouldBeNotifiedBeforeStreamFuture() {
        AtomicReference<GetObjectResponse> notified = new AtomicReference<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer =
            new S3CrtBorrowedBufferBlockingResponseTransformer(notified::set);
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        GetObjectResponse response = GetObjectResponse.builder().contentLength(3L).build();
        transformer.onResponse(response);
        AtomicReference<GetObjectResponse> notifiedWhenStreamReady = new AtomicReference<>();
        future.thenRun(() -> notifiedWhenStreamReady.set(notified.get()));

        transformer.onBorrowedStreamStart(() -> { }, NO_CREDIT);

        assertThat(notified).hasValue(response);
        assertThat(notifiedWhenStreamReady).hasValue(response);
    }

    @Test
    void responseListener_whenStreamStartsAfterFailure_shouldNotBeNotified() {
        AtomicReference<GetObjectResponse> notified = new AtomicReference<>();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer =
            new S3CrtBorrowedBufferBlockingResponseTransformer(notified::set);
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().contentLength(3L).build());
        transformer.exceptionOccurred(ApiCallTimeoutException.create(1));

        transformer.onBorrowedStreamStart(() -> { }, NO_CREDIT);

        assertThat(notified).hasValue(null);
        assertThat(future).isCompletedExceptionally();
    }

    @Test
    void redirect_shouldStreamTheRedirectedRequest() throws Exception {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> firstFuture = transformer.prepare();
        transformer.exceptionOccurred(new RuntimeException("redirect"));

        CompletableFuture<ResponseInputStream<GetObjectResponse>> secondFuture = transformer.prepare();
        GetObjectResponse response = GetObjectResponse.builder().contentLength(3L).build();
        transformer.onResponse(response);
        transformer.onBorrowedStreamStart(() -> { }, NO_CREDIT);
        transformer.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)), () -> { }));
        transformer.onBorrowedStreamComplete();

        assertThat(firstFuture).isCompletedExceptionally();
        try (ResponseInputStream<GetObjectResponse> stream = secondFuture.join()) {
            byte[] bytes = new byte[3];
            assertThat(stream.response()).isSameAs(response);
            assertThat(stream.read(bytes)).isEqualTo(3);
            assertThat(bytes).isEqualTo("abc".getBytes(UTF_8));
            assertThat(stream.read()).isEqualTo(-1);
        }
    }

    @Test
    void abortBeforeRedirect_shouldCancelRedirectedRequestWhenItStarts() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        transformer.prepare();

        transformer.abort();
        transformer.exceptionOccurred(new IOException("redirect"));
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);

        assertThat(cancellations).hasValue(1);
    }

    @Test
    void abortAfterStreamStart_shouldCancelRequest() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);

        transformer.abort();

        assertThat(cancellations).hasValue(1);
    }

    @Test
    void futureCancelledBeforeStreamStart_shouldRunCancellationWhenStarted() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());

        future.cancel(true);
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);

        assertThat(cancellations).hasValue(1);
    }

    @Test
    void failedFutureThenLateStreamStart_shouldKeepFutureFailedAndDiscardLateBuffers() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());

        future.completeExceptionally(new RuntimeException("request failed"));
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet, NO_CREDIT);
        assertThat(transformer.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
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
        transformer.onBorrowedStreamStart(() -> { }, NO_CREDIT);

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
        transformer.onBorrowedStreamStart(() -> { }, credit::addAndGet);
        transformer.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                     releases::incrementAndGet));
        transformer.onBorrowedStreamError(failure);

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
    void exceptionOccurredBeforeStreamStart_shouldKeepFutureFailedAndCloseStreamWhenStarted() {
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        AtomicLong credit = new AtomicLong();
        RuntimeException failure = new RuntimeException("request failed");
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());

        transformer.exceptionOccurred(failure);
        transformer.onBorrowedStreamStart(cancellations::incrementAndGet, credit::addAndGet);

        assertThat(transformer.onBorrowedBuffer(borrowedBuffer(ByteBuffer.wrap("abc".getBytes(UTF_8)),
                                                                releases::incrementAndGet))).isFalse();
        assertThatThrownBy(future::join).hasCause(failure);
        assertThat(cancellations).hasValue(1);
        assertThat(releases).hasValue(1);
        assertThat(credit).hasValue(0);
    }

    @Test
    void streamStart_calledTwice_shouldFail() {
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());
        transformer.onBorrowedStreamStart(() -> { }, NO_CREDIT);

        assertThatThrownBy(() -> transformer.onBorrowedStreamStart(() -> { }, NO_CREDIT))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bodyDeliveredThroughResponsePublisher_shouldCancelSubscriptionAndFailRequest() {
        AtomicInteger cancellations = new AtomicInteger();
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());

        transformer.onStream(subscriber -> {
            subscribe(subscriber, cancellations::incrementAndGet);
            subscriber.onNext(ByteBuffer.allocate(1));
        });

        assertThat(cancellations).hasValue(1);
        assertThatThrownBy(future::join).hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void responsePublisherError_shouldFailRequest() {
        RuntimeException failure = new RuntimeException("publisher failed");
        S3CrtBorrowedBufferBlockingResponseTransformer transformer = newTransformer();
        CompletableFuture<ResponseInputStream<GetObjectResponse>> future = transformer.prepare();
        transformer.onResponse(GetObjectResponse.builder().build());

        transformer.onStream(subscriber -> {
            subscribe(subscriber, () -> { });
            subscriber.onError(failure);
        });

        assertThatThrownBy(future::join).hasCause(failure);
    }

    @Test
    void factory_shouldReturnDirectBufferPoolMarker() {
        AsyncResponseTransformer<GetObjectResponse, ?> transformer =
            S3CrtResponseTransformerFactory.toBlockingInputStreamFromDirectBufferPool();

        assertThat(transformer).isInstanceOf(S3CrtDirectBufferPoolStreamMarker.class);
        assertThat(transformer.name())
            .isEqualTo(AsyncResponseTransformer.TransformerType.DIRECT_BUFFER_POOL_STREAM.getName());
    }

    private static S3CrtBorrowedBufferBlockingResponseTransformer newTransformer() {
        return new S3CrtBorrowedBufferBlockingResponseTransformer();
    }

    private static void subscribe(Subscriber<? super ByteBuffer> subscriber, Runnable onCancel) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
                onCancel.run();
            }
        });
    }
}
