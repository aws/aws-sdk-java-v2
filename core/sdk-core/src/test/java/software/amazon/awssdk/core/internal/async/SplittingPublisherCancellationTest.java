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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncRequestBodySplitConfiguration;
import software.amazon.awssdk.core.async.CloseableAsyncRequestBody;

/**
 * Tests for how {@link SplittingPublisher} reacts to the downstream cancelling its subscription.
 */
class SplittingPublisherCancellationTest {

    private static Stream<Arguments> modes() {
        return Stream.of(
            Arguments.of(false, true, false),
            Arguments.of(false, false, false),
            Arguments.of(false, true, true),
            Arguments.of(true, true, false),
            Arguments.of(true, false, false),
            Arguments.of(true, true, true));
    }

    @ParameterizedTest(name = "retryable={0}, knownLength={1}, bufferBeforeSend={2}")
    @MethodSource("modes")
    void subscribe_whenDownstreamCancelsInOnSubscribe_shouldNotSubscribeUpstream(boolean retryable,
                                                                                 boolean knownLength,
                                                                                 boolean bufferBeforeSend) {
        SyncUpstream upstream = new SyncUpstream(knownLength ? 16L : null, 16, 4);
        SplittingPublisher publisher = publisher(upstream, 4, 8, retryable, bufferBeforeSend);

        publisher.subscribe(new PartSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
            }
        });

        assertThat(upstream.subscribes.get()).isZero();
        assertThat(upstream.buffersDelivered.get()).isZero();
    }

    @Test
    void cancel_whenHandedOutPartNotSubscribed_shouldCancelUpstream() {
        // Report repro 2: non-retryable, chunk 64 > buffer 16, the only part is never subscribed.
        SyncUpstream upstream = new SyncUpstream(64L, 64, 8);
        SplittingPublisher publisher = publisher(upstream, 64, 16, false, false);

        publisher.subscribe(new PartSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                s.request(1);
            }

            @Override
            public void onNext(CloseableAsyncRequestBody part) {
                subscription.cancel();
            }
        });

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(upstream.buffersDelivered.get()).isZero();
    }

    @Test
    void cancel_whenRetryablePartSubscribedAndClosed_shouldCancelUpstreamAndErrorPart() {
        // Report repro 3: retryable, chunk 32 > buffer 8, part subscribed, then cancel and close.
        SyncUpstream upstream = new SyncUpstream(1024L, 1024, 4);
        SplittingPublisher publisher = publisher(upstream, 32, 8, true, false);
        RecordingByteSubscriber part1 = new RecordingByteSubscriber();

        publisher.subscribe(new PartSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                s.request(1);
            }

            @Override
            public void onNext(CloseableAsyncRequestBody part) {
                part.subscribe(part1);
                subscription.cancel();
                part.close();
            }
        });

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(part1.result).isCompletedExceptionally();
        assertThat(part1.error()).isInstanceOf(CancellationException.class);
    }

    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    void cancel_whenPartInProgress_shouldErrorPartWithCancellationException(boolean retryable) {
        ControlledUpstream upstream = new ControlledUpstream(16L);
        SplittingPublisher publisher = publisher(upstream, 8, 16, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        PartSubscriber downstream = new SubscribingPartSubscriber(parts);
        publisher.subscribe(downstream);

        upstream.emit(4);
        downstream.subscription.cancel();

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).error()).isInstanceOf(CancellationException.class);
        assertThat(parts.get(0).bytes.get()).isEqualTo(4);
        assertThat(downstream.terminal).isNull();
    }

    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    void cancel_whenCompletedPartsAlreadyDelivered_shouldNotErrorThem(boolean retryable) {
        // Known length: part 1 is complete when part 2 is handed out; cancelling on part 2 errors only part 2.
        ControlledUpstream upstream = new ControlledUpstream(16L);
        SplittingPublisher publisher = publisher(upstream, 4, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);
        publisher.subscribe(downstream);

        upstream.emit(4);
        assertThat(parts).hasSize(2);
        downstream.subscription.cancel();

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(parts.get(0).result).isCompleted();
        assertThat(parts.get(0).bytes.get()).isEqualTo(4);
        assertThat(parts.get(1).error()).isInstanceOf(CancellationException.class);
    }

    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    void cancel_whenUnknownLengthPartDelivered_shouldNotErrorDeliveredPart(boolean retryable) {
        // Unknown length: a part is only handed out once complete, so a cancel never errors a delivered part.
        ControlledUpstream upstream = new ControlledUpstream(null);
        SplittingPublisher publisher = publisher(upstream, 4, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);
        publisher.subscribe(downstream);

        upstream.emit(4);
        assertThat(parts).hasSize(1);
        downstream.subscription.cancel();

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(parts.get(0).result).isCompleted();
        assertThat(parts.get(0).bytes.get()).isEqualTo(4);
    }

    @Test
    void cancel_whenRetryablePartsDrainedButUnclosed_shouldCancelUpstreamAndErrorInProgressPart() {
        // Retryable, buffer >= chunk; parts 1-3 drained but never closed, cancel on part 3 (repro3d).
        SyncUpstream upstream = new SyncUpstream(1024L, 1024, 4);
        SplittingPublisher publisher = publisher(upstream, 32, 64, true, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();

        publisher.subscribe(new PartSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                s.request(3);
            }

            @Override
            public void onNext(CloseableAsyncRequestBody part) {
                RecordingByteSubscriber r = new RecordingByteSubscriber();
                parts.add(r);
                part.subscribe(r);
                if (parts.size() == 3) {
                    subscription.cancel();
                }
            }
        });

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(parts).hasSize(3);
        assertThat(parts.get(0).result).isCompleted();
        assertThat(parts.get(1).result).isCompleted();
        assertThat(parts.get(2).error()).isInstanceOf(CancellationException.class);
    }

    @Test
    void subscribe_whenCalledTwice_shouldRejectSecondWithoutResubscribingUpstream() {
        ControlledUpstream upstream = new ControlledUpstream(8L);
        SplittingPublisher publisher = publisher(upstream, 4, 8, false, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber first = new SubscribingPartSubscriber(parts);
        publisher.subscribe(first);
        upstream.emit(4);

        PartSubscriber second = new PartSubscriber();
        publisher.subscribe(second);

        assertThat(upstream.subscribes.get()).isEqualTo(1);
        assertThat(second.terminal).isInstanceOf(IllegalStateException.class);

        upstream.emit(4);
        upstream.complete();
        assertThat(parts).hasSize(2);
        assertThat(parts).allSatisfy(p -> {
            assertThat(p.result).isCompleted();
            assertThat(p.bytes.get()).isEqualTo(4);
        });
        assertThat(first.completed).isTrue();
    }

    @Test
    void completeCurrentBody_whenLengthMismatch_shouldErrorCurrentPart() {
        ControlledUpstream upstream = new ControlledUpstream(8L);
        SplittingPublisher publisher = publisher(upstream, 8, 8, false, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);
        publisher.subscribe(downstream);

        upstream.emit(4);
        upstream.complete();

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(downstream.terminal).isInstanceOf(IllegalStateException.class);
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).error()).isInstanceOf(IllegalStateException.class);
    }

    private static Stream<Arguments> cancelSignallingModes() {
        return Stream.of(
            Arguments.of(false, false),
            Arguments.of(false, true),
            Arguments.of(true, false),
            Arguments.of(true, true));
    }

    @ParameterizedTest(name = "retryable={0}, upstreamErrorsOnCancel={1}")
    @MethodSource("cancelSignallingModes")
    @Timeout(30)
    void cancel_whenUpstreamSignalsInsideCancel_shouldNotSignalDownstreamDuringCancel(boolean retryable,
                                                                                    boolean upstreamErrorsOnCancel)
        throws Exception {
        CancelSignallingUpstream upstream = new CancelSignallingUpstream(16L, upstreamErrorsOnCancel);
        SplittingPublisher publisher = publisher(upstream, 8, 16, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        AtomicBoolean insideCancel = new AtomicBoolean();
        AtomicBoolean signalledInsideCancel = new AtomicBoolean();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts) {
            @Override
            public void onError(Throwable t) {
                signalledInsideCancel.compareAndSet(false, insideCancel.get());
                super.onError(t);
            }

            @Override
            public void onComplete() {
                signalledInsideCancel.compareAndSet(false, insideCancel.get());
                super.onComplete();
            }
        };
        publisher.subscribe(downstream);

        upstream.emit(4);
        insideCancel.set(true);
        downstream.subscription.cancel();
        insideCancel.set(false);

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(signalledInsideCancel.get()).as("terminal signal delivered inside cancel()").isFalse();
        assertThat(downstream.terminal).isNull();
        assertThat(downstream.completed).isFalse();
        assertThat(parts).hasSize(1);
        // An onError the upstream signals because it was cancelled does not replace the cancel as the part's cause.
        Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
        assertThat(partError).isInstanceOf(CancellationException.class);
    }

    /**
     * The in-progress part's {@code onError} is queued behind the data already buffered for it, so a part subscriber with no
     * outstanding demand only sees the error once it requests that data. This matches how an upstream {@code onError}
     * reaches parts.
     */
    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    @Timeout(30)
    void cancel_whenPartSubscriberHasNoDemand_shouldDeliverBufferedDataThenError(boolean retryable) throws Exception {
        ControlledUpstream upstream = new ControlledUpstream(16L);
        SplittingPublisher publisher = publisher(upstream, 16, 16, retryable, false);
        AtomicReference<Subscription> partSubscription = new AtomicReference<>();
        AtomicLong partBytes = new AtomicLong();
        CompletableFuture<Void> partResult = new CompletableFuture<>();
        PartSubscriber downstream = new PartSubscriber() {
            @Override
            public void onSubscribe(Subscription s) {
                super.onSubscribe(s);
                s.request(1);
            }

            @Override
            public void onNext(CloseableAsyncRequestBody part) {
                part.subscribe(new Subscriber<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        partSubscription.set(s);
                        s.request(1);
                    }

                    @Override
                    public void onNext(ByteBuffer byteBuffer) {
                        partBytes.addAndGet(byteBuffer.remaining());
                    }

                    @Override
                    public void onError(Throwable t) {
                        partResult.completeExceptionally(t);
                    }

                    @Override
                    public void onComplete() {
                        partResult.complete(null);
                    }
                });
            }
        };
        publisher.subscribe(downstream);

        upstream.emit(4);
        upstream.emit(4);
        upstream.emit(4);
        downstream.subscription.cancel();

        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(partBytes.get()).isEqualTo(4);
        assertThat(partResult).isNotDone();

        partSubscription.get().request(Long.MAX_VALUE);

        Throwable partError = partResult.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
        assertThat(partError).isInstanceOf(CancellationException.class);
        assertThat(partBytes.get()).isEqualTo(12);
    }

    private static Stream<Arguments> throwingRequestModes() {
        return Stream.of(
            Arguments.of(false, true, 1),
            Arguments.of(false, true, 2),
            Arguments.of(false, false, 1),
            Arguments.of(false, false, 2),
            Arguments.of(true, true, 1),
            Arguments.of(true, true, 2),
            Arguments.of(true, false, 1),
            Arguments.of(true, false, 2));
    }

    @ParameterizedTest(name = "retryable={0}, knownLength={1}, throwOnRequestCall={2}")
    @MethodSource("throwingRequestModes")
    @Timeout(30)
    void request_whenUpstreamRequestThrows_shouldCancelUpstreamAndErrorDownstreamAndPart(boolean retryable,
                                                                                       boolean knownLength,
                                                                                       int throwOnRequestCall)
        throws Exception {
        ThrowingUpstream upstream = new ThrowingUpstream(knownLength ? 16L : null, 16, true, throwOnRequestCall,
                                                         OnCancel.NOTHING);
        SplittingPublisher publisher = publisher(upstream, 8, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);

        publisher.subscribe(downstream);

        assertThat(upstream.cancels.get()).isEqualTo(1);
        assertThat(upstream.requests.get()).isEqualTo(throwOnRequestCall);
        assertRequestFailure(downstream.terminal, upstream.requestFailure);
        if (knownLength) {
            assertThat(parts).hasSize(1);
            Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
            assertThat(partError).isSameAs(downstream.terminal);
            assertThat(parts.get(0).bytes.get()).isEqualTo(4L * (throwOnRequestCall - 1));
        } else {
            assertThat(parts).isEmpty();
        }

        Throwable terminal = downstream.terminal;
        upstream.emit(8);
        upstream.complete();

        assertThat(parts).hasSize(knownLength ? 1 : 0);
        assertThat(downstream.terminal).isSameAs(terminal);
        assertThat(downstream.completed).isFalse();
        assertThat(upstream.requests.get()).isEqualTo(throwOnRequestCall);
    }

    @Test
    @Timeout(30)
    void request_whenPartConsumerReleaseTriggersThrowingRequest_shouldErrorDownstreamAndPart() throws Exception {
        // Non-retryable parts release bytes as they are delivered; the third request(1) is issued on the consumer thread.
        ThrowingUpstream upstream = new ThrowingUpstream(64L, 64, true, 3, OnCancel.NOTHING);
        SplittingPublisher publisher = publisher(upstream, 32, 8, false, false);
        ExecutorService consumer = Executors.newSingleThreadExecutor(r -> new Thread(r, "part-consumer"));
        AtomicReference<Throwable> consumerFailure = new AtomicReference<>();
        CompletableFuture<Void> partResult = new CompletableFuture<>();
        AtomicReference<Subscription> partSubscription = new AtomicReference<>();
        Runnable requestOne = () -> consumer.execute(() -> {
            try {
                partSubscription.get().request(1);
            } catch (Throwable t) {
                consumerFailure.compareAndSet(null, t);
            }
        });
        try {
            PartSubscriber downstream = new PartSubscriber() {
                @Override
                public void onSubscribe(Subscription s) {
                    super.onSubscribe(s);
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(CloseableAsyncRequestBody part) {
                    part.subscribe(new Subscriber<ByteBuffer>() {
                        @Override
                        public void onSubscribe(Subscription s) {
                            partSubscription.set(s);
                        }

                        @Override
                        public void onNext(ByteBuffer byteBuffer) {
                            requestOne.run();
                        }

                        @Override
                        public void onError(Throwable t) {
                            partResult.completeExceptionally(t);
                        }

                        @Override
                        public void onComplete() {
                            partResult.complete(null);
                        }
                    });
                }
            };
            publisher.subscribe(downstream);
            // Two buffers fill the 8 byte budget; the upstream now waits for the consumer to release bytes.
            assertThat(upstream.requests.get()).isEqualTo(2);
            assertThat(downstream.terminal).isNull();

            requestOne.run();

            Throwable partError = partResult.handle((v, t) -> t).get(10, TimeUnit.SECONDS);
            assertRequestFailure(partError, upstream.requestFailure);
            assertThat(downstream.terminal).isSameAs(partError);
            assertThat(upstream.cancels.get()).isEqualTo(1);
            assertThat(upstream.requestFailureThread.get()).isEqualTo("part-consumer");
            assertThat(consumerFailure.get()).isNull();
        } finally {
            consumer.shutdownNow();
        }
    }

    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    @Timeout(30)
    void cancel_whenUpstreamCancelThrows_shouldStillErrorInProgressPart(boolean retryable) throws Exception {
        ThrowingUpstream upstream = new ThrowingUpstream(16L, 4, false, 0, OnCancel.THROW);
        SplittingPublisher publisher = publisher(upstream, 16, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);
        publisher.subscribe(downstream);
        assertThat(parts).hasSize(1);

        downstream.subscription.cancel();

        assertThat(upstream.cancels.get()).isEqualTo(1);
        Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
        assertThat(partError).isInstanceOf(CancellationException.class);
        assertThat(parts.get(0).bytes.get()).isEqualTo(4);
        assertThat(downstream.terminal).isNull();
        assertThat(downstream.completed).isFalse();
    }

    @Test
    @Timeout(30)
    void completeCurrentBody_whenLengthMismatchAndUpstreamCancelThrows_shouldStillErrorDownstreamAndPart()
        throws Exception {
        ThrowingUpstream upstream = new ThrowingUpstream(8L, 4, true, 0, OnCancel.THROW);
        SplittingPublisher publisher = publisher(upstream, 8, 8, false, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);

        publisher.subscribe(downstream);

        assertThat(upstream.cancels.get()).isEqualTo(1);
        assertThat(downstream.terminal).isInstanceOf(IllegalStateException.class)
                                       .hasMessageContaining("mismatches");
        assertThat(parts).hasSize(1);
        Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
        assertThat(partError).isSameAs(downstream.terminal);
    }

    /**
     * The upstream signals {@code onError} from inside the {@code cancel()} that follows its throwing {@code request()}. That
     * error must not replace the rule 3.16 failure.
     */
    @ParameterizedTest(name = "retryable={0}, knownLength={1}, throwOnRequestCall={2}")
    @MethodSource("throwingRequestModes")
    @Timeout(30)
    void request_whenUpstreamRequestThrowsAndErrorsOnCancel_shouldKeepRequestFailure(boolean retryable,
                                                                                    boolean knownLength,
                                                                                    int throwOnRequestCall)
        throws Exception {
        ThrowingUpstream upstream = new ThrowingUpstream(knownLength ? 16L : null, 16, true, throwOnRequestCall,
                                                         OnCancel.SIGNAL_ERROR);
        SplittingPublisher publisher = publisher(upstream, 8, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);

        publisher.subscribe(downstream);

        assertThat(upstream.cancels.get()).isEqualTo(1);
        assertRequestFailure(downstream.terminal, upstream.requestFailure);
        if (knownLength) {
            assertThat(parts).hasSize(1);
            Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
            assertThat(partError).isSameAs(downstream.terminal);
        } else {
            assertThat(parts).isEmpty();
        }
    }

    /**
     * The length-mismatch failure cancels the upstream, which signals {@code onError} from inside {@code cancel()}. That
     * error must not replace the mismatch failure.
     */
    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    @Timeout(30)
    void completeCurrentBody_whenLengthMismatchAndUpstreamErrorsOnCancel_shouldKeepMismatchFailure(boolean retryable)
        throws Exception {
        // request() emits nothing; the test drives the upstream so that cancel() is not deferred behind a request().
        ThrowingUpstream upstream = new ThrowingUpstream(8L, 0, false, 0, OnCancel.SIGNAL_ERROR);
        SplittingPublisher publisher = publisher(upstream, 8, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);
        publisher.subscribe(downstream);

        upstream.emit(4);
        upstream.complete();

        assertThat(upstream.cancels.get()).isEqualTo(1);
        assertThat(downstream.terminal).isInstanceOf(IllegalStateException.class)
                                       .hasMessageContaining("mismatches");
        assertThat(parts).hasSize(1);
        Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
        assertThat(partError).isSameAs(downstream.terminal);
    }

    /**
     * A downstream cancel races a stream failure (a length mismatch, which goes through {@code failStream}) on another
     * thread. Whichever stops the splitter first decides the cause, and the splitter's own {@link CancellationException}
     * is never sent to the downstream that cancelled. The window this guards, between the cancel stopping the splitter and
     * {@link software.amazon.awssdk.utils.async.SimplePublisher} recording it, is narrow, so a regression fails only
     * intermittently.
     */
    @Test
    @Timeout(120)
    void cancel_whenRacingStreamFailure_shouldNotErrorDownstreamWithCancellation() throws Exception {
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 2000; i++) {
                boolean retryable = i % 2 == 0;
                ControlledUpstream upstream = new ControlledUpstream(8L);
                SplittingPublisher publisher = publisher(upstream, 8, 8, retryable, false);
                List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
                SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);
                publisher.subscribe(downstream);
                upstream.emit(4);

                CountDownLatch start = new CountDownLatch(1);
                Future<?> canceller = threads.submit(() -> {
                    start.await();
                    downstream.subscription.cancel();
                    return null;
                });
                // Completing short of the known length fails the stream with an IllegalStateException.
                Future<?> failer = threads.submit(() -> {
                    start.await();
                    upstream.complete();
                    return null;
                });
                start.countDown();
                canceller.get(5, TimeUnit.SECONDS);
                failer.get(5, TimeUnit.SECONDS);

                Throwable terminal = downstream.terminal;
                assertThat(terminal == null || terminal instanceof IllegalStateException)
                    .as("iteration %d (retryable=%s): downstream error %s", i, retryable, terminal)
                    .isTrue();
                assertThat(parts).hasSize(1);
                Throwable partError = parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS);
                assertThat(partError).as("iteration %d", i)
                                     .isInstanceOfAny(CancellationException.class, IllegalStateException.class);
                assertThat(upstream.cancelled.get()).as("iteration %d", i).isTrue();
            }
        } finally {
            threads.shutdownNow();
        }
    }

    private static void assertRequestFailure(Throwable actual, Throwable requestFailure) {
        assertThat(actual).isInstanceOf(IllegalStateException.class)
                          .hasMessageContaining("rule 3.16");
        assertThat(actual.getCause()).isSameAs(requestFailure);
    }

    /**
     * Intended behavior change: {@code limit(n)} cancels after the nth {@code onNext}. With a known content length and no
     * {@code bufferBeforeSend}, the nth part is delivered before its data arrives, so it fails with a
     * {@link CancellationException}. See {@code AsyncRequestBody.splitCloseable}.
     */
    @ParameterizedTest(name = "retryable={0}")
    @ValueSource(booleans = {true, false})
    @Timeout(30)
    void limit_whenKnownLengthPartsDeliveredBeforeFilled_shouldErrorLastLimitedPart(boolean retryable) throws Exception {
        SyncUpstream upstream = new SyncUpstream(16L, 16, 4);
        SplittingPublisher publisher = publisher(upstream, 4, 8, retryable, false);
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts);

        publisher.limit(2).subscribe(downstream);

        assertThat(downstream.completed).isTrue();
        assertThat(downstream.terminal).isNull();
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS)).isNull();
        assertThat(parts.get(0).bytes.get()).isEqualTo(4);
        assertThat(parts.get(1).result.handle((v, t) -> t).get(5, TimeUnit.SECONDS))
            .isInstanceOf(CancellationException.class);
        assertThat(parts.get(1).bytes.get()).isZero();
        assertThat(upstream.cancelled.get()).isTrue();
        assertThat(upstream.buffersDelivered.get()).isEqualTo(1);
    }

    private static Stream<Arguments> completedPartModes() {
        // retryable, knownLength (known length uses bufferBeforeSend so parts are delivered complete)
        return Stream.of(
            Arguments.of(false, false),
            Arguments.of(false, true),
            Arguments.of(true, false),
            Arguments.of(true, true));
    }

    /**
     * Counterpart of {@link #limit_whenKnownLengthPartsDeliveredBeforeFilled_shouldErrorLastLimitedPart}: when parts are
     * only delivered once complete, every part {@code limit(n)} takes completes, and a completed retryable part stays
     * retryable even though the cancel path calls {@code error()} on it.
     */
    @ParameterizedTest(name = "retryable={0}, knownLength={1}")
    @MethodSource("completedPartModes")
    @Timeout(30)
    void limit_whenPartsDeliveredComplete_shouldCompleteAllLimitedParts(boolean retryable, boolean knownLength)
        throws Exception {
        SyncUpstream upstream = new SyncUpstream(knownLength ? 16L : null, 16, 4);
        SplittingPublisher publisher = publisher(upstream, 4, 8, retryable, knownLength);
        List<CloseableAsyncRequestBody> bodies = new CopyOnWriteArrayList<>();
        List<RecordingByteSubscriber> parts = new CopyOnWriteArrayList<>();
        SubscribingPartSubscriber downstream = new SubscribingPartSubscriber(parts) {
            @Override
            public void onNext(CloseableAsyncRequestBody part) {
                bodies.add(part);
                super.onNext(part);
            }
        };

        try {
            publisher.limit(2).subscribe(downstream);

            assertThat(downstream.completed).isTrue();
            assertThat(downstream.terminal).isNull();
            assertThat(parts).hasSize(2);
            for (RecordingByteSubscriber part : parts) {
                assertThat(part.result.handle((v, t) -> t).get(5, TimeUnit.SECONDS)).isNull();
                assertThat(part.bytes.get()).isEqualTo(4);
            }
            assertThat(upstream.cancelled.get()).isTrue();
            assertThat(upstream.buffersDelivered.get()).isEqualTo(2);

            if (retryable) {
                RecordingByteSubscriber retry = new RecordingByteSubscriber();
                bodies.get(1).subscribe(retry);
                assertThat(retry.result.handle((v, t) -> t).get(5, TimeUnit.SECONDS)).isNull();
                assertThat(retry.bytes.get()).isEqualTo(4);
            }
        } finally {
            bodies.forEach(CloseableAsyncRequestBody::close);
        }
    }

    /**
     * Stress variant of {@link #cancel_whenRacingUpstreamDemand_shouldNeverCallSubscriptionConcurrently} with a synchronous
     * upstream: {@code request(n)} emits inline on whichever thread calls it (the subscribing thread, part consumers releasing
     * bytes), racing a cancel from a pool thread. Checks both rule 2.7 (no concurrent calls) and rule 3.3 (no
     * {@code request -> onNext -> request} nesting).
     */
    @Test
    @Timeout(180)
    void cancel_whenRacingSynchronousUpstream_shouldSerializeAndNotNestSubscriptionCalls() throws Exception {
        ExecutorService consumers = Executors.newFixedThreadPool(4);
        int completedStreams = 0;
        try {
            for (int i = 0; i < 500; i++) {
                boolean retryable = i % 2 == 0;
                boolean knownLength = (i / 2) % 2 == 0;
                SyncRacingUpstream upstream = new SyncRacingUpstream(knownLength ? 256L : null, 256);
                SplittingPublisher publisher = publisher(upstream, 16, 32, retryable, false);
                // 16 parts; drawing up to 17 also lets some streams complete, racing late consumer releases.
                int cancelAtPart = ThreadLocalRandom.current().nextInt(1, 18);
                List<CompletableFuture<Void>> partResults = new CopyOnWriteArrayList<>();

                publisher.subscribe(new PartSubscriber() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        super.onSubscribe(s);
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(CloseableAsyncRequestBody part) {
                        CompletableFuture<Void> done = new CompletableFuture<>();
                        partResults.add(done);
                        consumers.execute(() -> part.subscribe(new AsyncDrainingSubscriber(consumers, part, done)));
                        if (partResults.size() == cancelAtPart) {
                            consumers.execute(subscription::cancel);
                        }
                    }
                });

                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                while (!upstream.cancelled.get() && !upstream.completed.get() && System.nanoTime() < deadline) {
                    Thread.sleep(1);
                }
                assertThat(upstream.cancelled.get() || upstream.completed.get())
                    .as("iteration %d (retryable=%s, knownLength=%s): upstream terminated", i, retryable, knownLength)
                    .isTrue();
                for (CompletableFuture<Void> r : partResults) {
                    r.handle((v, t) -> null).get(10, TimeUnit.SECONDS);
                }
                assertThat(upstream.overlap.get())
                    .as("iteration %d: overlapping or nested Subscription calls on %s", i, upstream.overlapThreads)
                    .isFalse();
                if (upstream.completed.get()) {
                    completedStreams++;
                }
            }
        } finally {
            consumers.shutdownNow();
        }
        assertThat(completedStreams).as("iterations whose upstream completed").isPositive();
    }

    @Test
    @Timeout(120)
    void cancel_whenRacingUpstreamDemand_shouldNeverCallSubscriptionConcurrently() throws Exception {
        ExecutorService upstreamThread = Executors.newSingleThreadExecutor();
        ExecutorService consumers = Executors.newFixedThreadPool(4);
        try {
            for (int i = 0; i < 300; i++) {
                boolean retryable = i % 2 == 0;
                AsyncUpstream upstream = new AsyncUpstream(256L, 4, upstreamThread);
                SplittingPublisher publisher = publisher(upstream, 16, 32, retryable, false);
                int cancelAtPart = ThreadLocalRandom.current().nextInt(1, 12);
                List<CompletableFuture<Void>> partResults = new CopyOnWriteArrayList<>();

                publisher.subscribe(new PartSubscriber() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        super.onSubscribe(s);
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(CloseableAsyncRequestBody part) {
                        CompletableFuture<Void> done = new CompletableFuture<>();
                        partResults.add(done);
                        consumers.execute(() -> part.subscribe(new AsyncDrainingSubscriber(consumers, part, done)));
                        if (partResults.size() == cancelAtPart) {
                            consumers.execute(subscription::cancel);
                        }
                    }
                });

                long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
                while (!upstream.cancelled.get() && !upstream.completed.get() && System.nanoTime() < deadline) {
                    Thread.sleep(1);
                }
                assertThat(upstream.cancelled.get() || upstream.completed.get())
                    .as("iteration %d: upstream terminated", i).isTrue();
                for (CompletableFuture<Void> r : partResults) {
                    r.handle((v, t) -> null).get(10, TimeUnit.SECONDS);
                }
                assertThat(upstream.overlap.get()).as("iteration %d: concurrent Subscription calls", i).isFalse();
            }
        } finally {
            upstreamThread.shutdownNow();
            consumers.shutdownNow();
        }
    }

    private static SplittingPublisher publisher(AsyncRequestBody upstream, long chunk, long buffer, boolean retryable,
                                                boolean bufferBeforeSend) {
        return SplittingPublisher.builder()
                                 .asyncRequestBody(upstream)
                                 .splitConfiguration(AsyncRequestBodySplitConfiguration.builder()
                                                                                       .chunkSizeInBytes(chunk)
                                                                                       .bufferSizeInBytes(buffer)
                                                                                       .build())
                                 .retryableSubAsyncRequestBodyEnabled(retryable)
                                 .bufferBeforeSend(bufferBeforeSend)
                                 .build();
    }

    /** Synchronous upstream in the style of the defect report: request(n) emits n buffers inline. */
    private static final class SyncUpstream implements AsyncRequestBody {
        private final Long contentLength;
        private final long total;
        private final int bufferSize;
        private final AtomicInteger subscribes = new AtomicInteger();
        private final AtomicInteger buffersDelivered = new AtomicInteger();
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private SyncUpstream(Long contentLength, long total, int bufferSize) {
            this.contentLength = contentLength;
            this.total = total;
            this.bufferSize = bufferSize;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscribes.incrementAndGet();
            s.onSubscribe(new Subscription() {
                private long sent;
                private boolean done;

                @Override
                public void request(long n) {
                    for (long i = 0; i < n && sent < total && !cancelled.get(); i++) {
                        buffersDelivered.incrementAndGet();
                        sent += bufferSize;
                        s.onNext(ByteBuffer.allocate(bufferSize));
                    }
                    if (sent >= total && !done && !cancelled.get()) {
                        done = true;
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }
    }

    /** Upstream driven explicitly by the test; ignores demand (the splitter tolerates it within its buffer). */
    private static final class ControlledUpstream implements AsyncRequestBody {
        private final Long contentLength;
        private final AtomicInteger subscribes = new AtomicInteger();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile Subscriber<? super ByteBuffer> subscriber;

        private ControlledUpstream(Long contentLength) {
            this.contentLength = contentLength;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscribes.incrementAndGet();
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                    cancelled.set(true);
                }
            });
        }

        void emit(int bytes) {
            subscriber.onNext(ByteBuffer.allocate(bytes));
        }

        void complete() {
            subscriber.onComplete();
        }
    }

    /**
     * Upstream driven explicitly by the test whose {@code cancel()} synchronously signals {@code onComplete} or
     * {@code onError} (allowed by rule 1.8), to check that the splitter never signals the downstream from inside its own
     * {@code cancel()} call.
     */
    private static final class CancelSignallingUpstream implements AsyncRequestBody {
        private final Long contentLength;
        private final boolean errorOnCancel;
        private final RuntimeException cancelFailure = new RuntimeException("upstream failed on cancel");
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile Subscriber<? super ByteBuffer> subscriber;

        private CancelSignallingUpstream(Long contentLength, boolean errorOnCancel) {
            this.contentLength = contentLength;
            this.errorOnCancel = errorOnCancel;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscriber = s;
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                }

                @Override
                public void cancel() {
                    if (!cancelled.compareAndSet(false, true)) {
                        return;
                    }
                    if (errorOnCancel) {
                        s.onError(cancelFailure);
                    } else {
                        s.onComplete();
                    }
                }
            });
        }

        void emit(int bytes) {
            subscriber.onNext(ByteBuffer.allocate(bytes));
        }
    }

    /** What {@link ThrowingUpstream} does when its subscription is cancelled. */
    private enum OnCancel {
        NOTHING,
        /** Throw from {@code cancel()} (a rule 3.15 violation). */
        THROW,
        /** Signal {@code onError} synchronously from inside {@code cancel()} (allowed by rule 1.8). */
        SIGNAL_ERROR
    }

    /**
     * Synchronous upstream (request(n) emits up to n 4-byte buffers inline, up to {@code emitTotal} bytes) that violates
     * rule 3.16 on a chosen {@code request()} call, and reacts to {@code cancel()} as chosen by {@link OnCancel}. The test
     * can push late signals with {@link #emit(int)} and {@link #complete()}.
     */
    private static final class ThrowingUpstream implements AsyncRequestBody {
        private static final int BUFFER_SIZE = 4;
        private final Long contentLength;
        private final long emitTotal;
        private final boolean completeAtEnd;
        private final int throwOnRequestCall;
        private final OnCancel onCancel;
        private final RuntimeException requestFailure = new RuntimeException("request() failed");
        private final RuntimeException cancelFailure = new RuntimeException("upstream failed on cancel");
        private final AtomicInteger requests = new AtomicInteger();
        private final AtomicInteger cancels = new AtomicInteger();
        private final AtomicReference<String> requestFailureThread = new AtomicReference<>();
        private volatile Subscriber<? super ByteBuffer> subscriber;

        /**
         * @param throwOnRequestCall 1-based index of the {@code request()} call that throws; 0 means never.
         */
        private ThrowingUpstream(Long contentLength, long emitTotal, boolean completeAtEnd, int throwOnRequestCall,
                                 OnCancel onCancel) {
            this.contentLength = contentLength;
            this.emitTotal = emitTotal;
            this.completeAtEnd = completeAtEnd;
            this.throwOnRequestCall = throwOnRequestCall;
            this.onCancel = onCancel;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            subscriber = s;
            s.onSubscribe(new Subscription() {
                private long sent;
                private boolean done;

                @Override
                public void request(long n) {
                    if (requests.incrementAndGet() == throwOnRequestCall) {
                        requestFailureThread.set(Thread.currentThread().getName());
                        throw requestFailure;
                    }
                    for (long i = 0; i < n && sent < emitTotal && cancels.get() == 0; i++) {
                        sent += BUFFER_SIZE;
                        s.onNext(ByteBuffer.allocate(BUFFER_SIZE));
                    }
                    if (completeAtEnd && sent >= emitTotal && !done && cancels.get() == 0) {
                        done = true;
                        s.onComplete();
                    }
                }

                @Override
                public void cancel() {
                    if (cancels.incrementAndGet() != 1) {
                        return;
                    }
                    if (onCancel == OnCancel.THROW) {
                        throw new RuntimeException("cancel() failed");
                    }
                    if (onCancel == OnCancel.SIGNAL_ERROR) {
                        s.onError(cancelFailure);
                    }
                }
            });
        }

        void emit(int bytes) {
            subscriber.onNext(ByteBuffer.allocate(bytes));
        }

        void complete() {
            subscriber.onComplete();
        }
    }

    /**
     * Upstream that emits on its own thread and records whether two threads were ever inside request()/cancel() at the
     * same time (Reactive Streams rule 2.7). Each call holds the "inside" marker briefly to widen the race window.
     */
    private static final class AsyncUpstream implements AsyncRequestBody {
        private final long total;
        private final int bufferSize;
        private final ExecutorService executor;
        private final AtomicInteger inside = new AtomicInteger();
        private final AtomicBoolean overlap = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean completed = new AtomicBoolean();
        private final AtomicLong demand = new AtomicLong();
        private long sent;

        private AsyncUpstream(long total, int bufferSize, ExecutorService executor) {
            this.total = total;
            this.bufferSize = bufferSize;
            this.executor = executor;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.of(total);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    enter();
                    try {
                        demand.addAndGet(n);
                        executor.execute(() -> drain(s));
                    } finally {
                        exit();
                    }
                }

                @Override
                public void cancel() {
                    enter();
                    try {
                        cancelled.set(true);
                    } finally {
                        exit();
                    }
                }
            });
        }

        private void drain(Subscriber<? super ByteBuffer> s) {
            while (demand.get() > 0 && !cancelled.get() && sent < total) {
                demand.decrementAndGet();
                sent += bufferSize;
                s.onNext(ByteBuffer.allocate(bufferSize));
            }
            if (sent >= total && !cancelled.get() && completed.compareAndSet(false, true)) {
                s.onComplete();
            }
        }

        private void enter() {
            if (inside.getAndIncrement() != 0) {
                overlap.set(true);
            }
            long end = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(20);
            while (System.nanoTime() < end) {
                Thread.yield();
            }
        }

        private void exit() {
            inside.decrementAndGet();
        }
    }

    /**
     * Synchronous upstream: {@code request(n)} emits up to n 4-byte buffers inline on the calling thread. Records whether a
     * {@code request()}/{@code cancel()} call was ever entered while another one was in progress, which catches both
     * cross-thread overlap (rule 2.7) and same-thread {@code request -> onNext -> request} nesting (rule 3.3).
     */
    private static final class SyncRacingUpstream implements AsyncRequestBody {
        private static final int BUFFER_SIZE = 4;
        private final Long contentLength;
        private final long total;
        private final AtomicInteger inside = new AtomicInteger();
        private final AtomicBoolean overlap = new AtomicBoolean();
        private final List<String> overlapThreads = new CopyOnWriteArrayList<>();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicBoolean completed = new AtomicBoolean();
        private final AtomicLong sent = new AtomicLong();
        private volatile String insideThread;

        private SyncRacingUpstream(Long contentLength, long total) {
            this.contentLength = contentLength;
            this.total = total;
        }

        @Override
        public Optional<Long> contentLength() {
            return Optional.ofNullable(contentLength);
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            s.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    enter();
                    try {
                        for (long i = 0; i < n && !cancelled.get() && sent.get() < total; i++) {
                            sent.addAndGet(BUFFER_SIZE);
                            s.onNext(ByteBuffer.allocate(BUFFER_SIZE));
                        }
                        if (sent.get() >= total && !cancelled.get() && completed.compareAndSet(false, true)) {
                            s.onComplete();
                        }
                    } finally {
                        exit();
                    }
                }

                @Override
                public void cancel() {
                    enter();
                    try {
                        cancelled.set(true);
                    } finally {
                        exit();
                    }
                }
            });
        }

        private void enter() {
            String current = Thread.currentThread().getName();
            if (inside.getAndIncrement() != 0) {
                overlap.set(true);
                overlapThreads.add(insideThread + " -> " + current);
            }
            insideThread = current;
            long end = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(5);
            while (System.nanoTime() < end) {
                Thread.yield();
            }
        }

        private void exit() {
            inside.decrementAndGet();
        }
    }

    /** Subscribes each part on a pool thread one element at a time, closing it after its terminal signal. */
    private static final class AsyncDrainingSubscriber implements Subscriber<ByteBuffer> {
        private final ExecutorService executor;
        private final CloseableAsyncRequestBody part;
        private final CompletableFuture<Void> done;
        private Subscription subscription;

        private AsyncDrainingSubscriber(ExecutorService executor, CloseableAsyncRequestBody part,
                                        CompletableFuture<Void> done) {
            this.executor = executor;
            this.part = part;
            this.done = done;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            executor.execute(() -> s.request(1));
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            executor.execute(() -> subscription.request(1));
        }

        @Override
        public void onError(Throwable t) {
            part.close();
            done.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            part.close();
            done.complete(null);
        }
    }

    private static class PartSubscriber implements Subscriber<CloseableAsyncRequestBody> {
        protected volatile Subscription subscription;
        protected volatile Throwable terminal;
        protected volatile boolean completed;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
        }

        @Override
        public void onNext(CloseableAsyncRequestBody part) {
        }

        @Override
        public void onError(Throwable t) {
            terminal = t;
        }

        @Override
        public void onComplete() {
            completed = true;
        }
    }

    /** Requests every part and subscribes a {@link RecordingByteSubscriber} to each. */
    private static class SubscribingPartSubscriber extends PartSubscriber {
        private final List<RecordingByteSubscriber> parts;

        private SubscribingPartSubscriber(List<RecordingByteSubscriber> parts) {
            this.parts = parts;
        }

        @Override
        public void onSubscribe(Subscription s) {
            super.onSubscribe(s);
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(CloseableAsyncRequestBody part) {
            RecordingByteSubscriber r = new RecordingByteSubscriber();
            parts.add(r);
            part.subscribe(r);
        }
    }

    private static final class RecordingByteSubscriber implements Subscriber<ByteBuffer> {
        private final CompletableFuture<Void> result = new CompletableFuture<>();
        private final AtomicLong bytes = new AtomicLong();
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        @Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            bytes.addAndGet(byteBuffer.remaining());
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            result.completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            result.complete(null);
        }

        Throwable error() {
            return error.get();
        }
    }
}
