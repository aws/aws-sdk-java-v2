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

package software.amazon.awssdk.core.internal.async;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reactivex.Flowable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedTestVectors;
import software.amazon.awssdk.core.internal.chunked.AwsChunkedTestVectors.Vector;

/**
 * Tests for {@link AwsChunkedResponseDecodingPublisher}: shared vectors plus Reactive Streams behavior.
 */
class AwsChunkedResponseDecodingPublisherTest {

    // Shared vectors

    @ParameterizedTest(name = "{0}, split={1}")
    @MethodSource("software.amazon.awssdk.core.internal.chunked.AwsChunkedTestVectors#successCases")
    void subscribe_successVector_decodesBodyAndTrailers(Vector vector, int splitSize) throws Exception {
        AwsChunkedResponseDecodingPublisher publisher = publisher(vector, splitSize);
        RequestOneSubscriber subscriber = new RequestOneSubscriber(true);

        publisher.subscribe(subscriber);

        assertThat(subscriber.error).isNull();
        assertThat(subscriber.completed).isTrue();
        assertThat(subscriber.overDelivered).isFalse();
        assertThat(subscriber.bodyAsBytes()).isEqualTo(vector.expectedBody);
        assertThat(publisher.trailersFuture().get(5, SECONDS)).containsExactlyEntriesOf(vector.expectedTrailers);
    }

    @ParameterizedTest(name = "{0}, split={1}")
    @MethodSource("software.amazon.awssdk.core.internal.chunked.AwsChunkedTestVectors#errorCases")
    void subscribe_errorVector_signalsSingleErrorAndFailsTrailersFuture(Vector vector, int splitSize) {
        AwsChunkedResponseDecodingPublisher publisher = publisher(vector, splitSize);
        RequestOneSubscriber subscriber = new RequestOneSubscriber(true);

        publisher.subscribe(subscriber);

        assertThat(subscriber.errorCount).isEqualTo(1);
        assertThat(subscriber.completed).isFalse();
        assertThat(subscriber.error)
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining(vector.expectedError);
        assertThatThrownBy(() -> publisher.trailersFuture().get(5, SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseReference(subscriber.error);
    }

    // Backpressure

    @Test
    void subscribe_whenUpstreamBufferSpansMultipleChunks_emitsSingleBufferPerUpstreamBuffer() {
        Flowable<ByteBuffer> upstream = Flowable.just(utf8("5\r\nhello\r\n5\r\nworld\r\n0\r\n\r\n"));
        RequestOneSubscriber subscriber = new RequestOneSubscriber(false);

        new AwsChunkedResponseDecodingPublisher(upstream, -1, null).subscribe(subscriber);

        assertThat(subscriber.overDelivered).isFalse();
        assertThat(subscriber.buffers).hasSize(1);
        assertThat(subscriber.bodyAsString()).isEqualTo("helloworld");
    }

    @Test
    void subscribe_whenLargePayloadSplitRandomly_decodesExactBytesWithOneAtATimeDemand() throws Exception {
        Random random = new Random(42);
        byte[] data = new byte[256 * 1024];
        random.nextBytes(data);
        byte[] encoded = AwsChunkedTestVectors.encodeAwsChunked(data, random, "x-amz-response-checksum-crc32:abc==");

        List<ByteBuffer> upstreamBuffers = new ArrayList<>();
        int pos = 0;
        while (pos < encoded.length) {
            int len = Math.min(1 + random.nextInt(8192), encoded.length - pos);
            upstreamBuffers.add(ByteBuffer.wrap(Arrays.copyOfRange(encoded, pos, pos + len)));
            pos += len;
        }

        AwsChunkedResponseDecodingPublisher publisher =
            new AwsChunkedResponseDecodingPublisher(Flowable.fromIterable(upstreamBuffers), data.length, null);
        RequestOneSubscriber subscriber = new RequestOneSubscriber(true);
        publisher.subscribe(subscriber);

        assertThat(subscriber.error).isNull();
        assertThat(subscriber.overDelivered).isFalse();
        assertThat(subscriber.completed).isTrue();
        assertThat(subscriber.bodyAsBytes()).isEqualTo(data);
        assertThat(publisher.trailersFuture().get(5, SECONDS)).containsEntry("x-amz-response-checksum-crc32", "abc==");
    }

    // Terminal signals

    @Test
    void onNext_whenParseErrorAndUpstreamIgnoresCancel_signalsSingleError() {
        RequestOneSubscriber subscriber = new RequestOneSubscriber(true);
        new AwsChunkedResponseDecodingPublisher(ignoringCancelPublisher("ZZ\r\n"), -1, null).subscribe(subscriber);

        assertThat(subscriber.errorCount).isEqualTo(1);
        assertThat(subscriber.completed).isFalse();
        assertThat(subscriber.error).hasMessageContaining("Invalid character in chunk size");
    }

    @Test
    void onNext_whenParseError_cancelsUpstream() {
        AtomicBoolean cancelled = new AtomicBoolean();
        Publisher<ByteBuffer> upstream = s -> {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
            s.onNext(utf8("ZZ\r\n"));
        };

        new AwsChunkedResponseDecodingPublisher(upstream, -1, null).subscribe(new RequestOneSubscriber(true));

        assertThat(cancelled).isTrue();
    }

    @Test
    void onError_whenUpstreamFails_signalsSameErrorAndFailsTrailersFuture() {
        RuntimeException upstreamError = new RuntimeException("connection reset");
        AwsChunkedResponseDecodingPublisher publisher =
            new AwsChunkedResponseDecodingPublisher(Flowable.error(upstreamError), -1, null);
        RequestOneSubscriber subscriber = new RequestOneSubscriber(true);

        publisher.subscribe(subscriber);

        assertThat(subscriber.errorCount).isEqualTo(1);
        assertThat(subscriber.error).isSameAs(upstreamError);
        assertThatThrownBy(() -> publisher.trailersFuture().get(5, SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseReference(upstreamError);
    }

    @Test
    void onNext_whenChunkExceedsDecodedLength_failsBeforeEmittingData() {
        RequestOneSubscriber subscriber = new RequestOneSubscriber(true);

        new AwsChunkedResponseDecodingPublisher(Flowable.just(utf8("5\r\nhello\r\n0\r\n\r\n")), 3, null)
            .subscribe(subscriber);

        assertThat(subscriber.buffers).isEmpty();
        assertThat(subscriber.error).hasMessageContaining("exceeds decoded content length");
    }

    @Test
    void cancel_whenUpstreamKeepsSignallingAfterCancel_dropsAllLaterSignals() {
        Publisher<ByteBuffer> upstream = s -> {
            s.onSubscribe(NO_OP_SUBSCRIPTION);
            s.onNext(utf8("5\r\nhello\r\n"));
            s.onNext(utf8("5\r\nworld\r\n"));
            s.onComplete(); // incomplete message: would otherwise surface "Unexpected end of stream"
        };
        CancelAfterFirstSubscriber subscriber = new CancelAfterFirstSubscriber();

        new AwsChunkedResponseDecodingPublisher(upstream, -1, null).subscribe(subscriber);

        assertThat(subscriber.received).isEqualTo(1);
        assertThat(subscriber.error).isNull();
        assertThat(subscriber.completed).isFalse();
    }

    @Test
    void trailersFuture_whenTrailersParsedButUpstreamNotComplete_isNotYetComplete() {
        Publisher<ByteBuffer> upstream = s -> {
            s.onSubscribe(NO_OP_SUBSCRIPTION);
            s.onNext(utf8("3\r\nfoo\r\n0\r\ntrailer-key:abc==\r\n\r\n"));
            // no onComplete yet
        };
        AwsChunkedResponseDecodingPublisher publisher = new AwsChunkedResponseDecodingPublisher(upstream, -1, null);

        publisher.subscribe(new RequestOneSubscriber(true));

        assertThat(publisher.trailersFuture()).isNotDone();
    }

    @Test
    void cancel_whenDownstreamCancelsMidStream_failsTrailersFutureAndCancelsUpstream() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        Flowable<ByteBuffer> upstream = Flowable.just(utf8("5\r\nhello\r\n"), utf8("5\r\nworld\r\n"), utf8("0\r\n\r\n"))
                                                .doOnCancel(() -> upstreamCancelled.set(true));
        AwsChunkedResponseDecodingPublisher publisher = new AwsChunkedResponseDecodingPublisher(upstream, -1, null);
        CancelAfterFirstSubscriber subscriber = new CancelAfterFirstSubscriber();

        publisher.subscribe(subscriber);

        assertThat(subscriber.received).isEqualTo(1);
        assertThat(upstreamCancelled).isTrue();
        assertThat(publisher.trailersFuture()).isCompletedExceptionally();
        // get() throws CancellationException unwrapped
        assertThatThrownBy(() -> publisher.trailersFuture().get(5, SECONDS))
            .isInstanceOf(CancellationException.class)
            .hasMessageContaining("cancelled before the response body was fully decoded");
    }

    @Test
    void subscribe_whenCalledTwice_secondSubscriberReceivesError() {
        AwsChunkedResponseDecodingPublisher publisher =
            new AwsChunkedResponseDecodingPublisher(Flowable.just(utf8("0\r\n\r\n")), -1, null);
        publisher.subscribe(new RequestOneSubscriber(true));

        RequestOneSubscriber second = new RequestOneSubscriber(true);
        publisher.subscribe(second);

        assertThat(second.error).isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("supports only one subscriber");
    }

    // Helpers

    private static final Subscription NO_OP_SUBSCRIPTION = new Subscription() {
        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
        }
    };

    private static AwsChunkedResponseDecodingPublisher publisher(Vector vector, int splitSize) {
        List<ByteBuffer> buffers = AwsChunkedTestVectors.split(vector.encoded, splitSize)
                                                        .stream()
                                                        .map(ByteBuffer::wrap)
                                                        .collect(Collectors.toList());
        return new AwsChunkedResponseDecodingPublisher(Flowable.fromIterable(buffers),
                                                       vector.expectedDecodedLength,
                                                       vector.declaredTrailers);
    }

    private static ByteBuffer utf8(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends everything in one buffer, ignoring demand and cancel.
     */
    private static Publisher<ByteBuffer> ignoringCancelPublisher(String encoded) {
        return subscriber -> {
            subscriber.onSubscribe(NO_OP_SUBSCRIPTION);
            subscriber.onNext(utf8(encoded));
            subscriber.onComplete();
        };
    }

    /**
     * Requests one element at a time and cancels after receiving the first.
     */
    private static final class CancelAfterFirstSubscriber implements Subscriber<ByteBuffer> {
        private Subscription subscription;
        private int received;
        private Throwable error;
        private boolean completed;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            received++;
            subscription.cancel();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
        }

        @Override
        public void onComplete() {
            completed = true;
        }
    }

    /**
     * Requests one at a time and flags over-delivery.
     */
    private static final class RequestOneSubscriber implements Subscriber<ByteBuffer> {
        private final boolean requestAfterEachItem;
        private final List<byte[]> buffers = new ArrayList<>();
        private Subscription subscription;
        private long outstanding;
        private boolean overDelivered;
        private boolean completed;
        private Throwable error;
        private int errorCount;

        private RequestOneSubscriber(boolean requestAfterEachItem) {
            this.requestAfterEachItem = requestAfterEachItem;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            outstanding = 1;
            s.request(1);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            if (--outstanding < 0) {
                overDelivered = true;
            }
            byte[] copy = new byte[byteBuffer.remaining()];
            byteBuffer.get(copy);
            buffers.add(copy);
            if (requestAfterEachItem) {
                outstanding++;
                subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable t) {
            errorCount++;
            if (error == null) {
                error = t;
            }
        }

        @Override
        public void onComplete() {
            completed = true;
        }

        byte[] bodyAsBytes() {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            buffers.forEach(b -> out.write(b, 0, b.length));
            return out.toByteArray();
        }

        String bodyAsString() {
            return new String(bodyAsBytes(), StandardCharsets.UTF_8);
        }
    }
}
