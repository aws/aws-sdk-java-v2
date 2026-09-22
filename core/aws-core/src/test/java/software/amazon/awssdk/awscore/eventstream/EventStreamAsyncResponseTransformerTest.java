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

package software.amazon.awssdk.awscore.eventstream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.NonRetryableException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.utils.ImmutableMap;
import software.amazon.eventstream.HeaderValue;
import software.amazon.eventstream.Message;

public class EventStreamAsyncResponseTransformerTest {

    @Test
    public void multipleEventsInChunk_OnlyDeliversOneEvent() throws InterruptedException {

        Message eventMessage = new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("event"),
                                                           ":event-type", HeaderValue.fromString("foo")),
                                           new byte[0]);

        CountDownLatch latch = new CountDownLatch(1);
        Flowable<ByteBuffer> bytePublisher = Flowable.just(eventMessage.toByteBuffer(), eventMessage.toByteBuffer())
                                                     .doOnCancel(latch::countDown);
        AtomicInteger numEvents = new AtomicInteger(0);

        // Request one event then cancel
        Subscriber<Object> requestOneSubscriber = new Subscriber<Object>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(Object o) {
                numEvents.incrementAndGet();
                subscription.cancel();
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        };
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(
                                                   onEventStream(p -> p.subscribe(requestOneSubscriber)))
                                               .eventResponseHandler((r, e) -> new Object())
                                               .executor(Executors.newSingleThreadExecutor())
                                               .future(operationFuture)
                                               .build();
        CompletableFuture<Void> transformFuture = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));
        latch.await();
        transformFuture.join();
        operationFuture.join();
        assertThat(numEvents)
            .as("Expected only one event to be delivered")
            .hasValue(1);
    }

    @Test
    public void devilSubscriber_requestDataAfterComplete() throws InterruptedException {

        Message eventMessage = new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("event"),
                                                           ":event-type", HeaderValue.fromString("foo")),
                                           "helloworld".getBytes());

        CountDownLatch latch = new CountDownLatch(1);
        Flowable<ByteBuffer> bytePublisher = Flowable.just(eventMessage.toByteBuffer(), eventMessage.toByteBuffer());
        AtomicInteger numEvents = new AtomicInteger(0);

        Subscriber<Object> requestAfterCompleteSubscriber = new Subscriber<Object>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(Object o) {
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                // Should never ever do this in production!
                subscription.request(1);
                latch.countDown();
            }
        };
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(
                                                   onEventStream(p -> p.subscribe(requestAfterCompleteSubscriber)))
                                               .eventResponseHandler((r, e) -> numEvents.incrementAndGet())
                                               .executor(Executors.newFixedThreadPool(2))
                                               .future(new CompletableFuture<>())
                                               .build();
        transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));
        latch.await();
        assertThat(numEvents)
            .as("Expected only one event to be delivered")
            .hasValue(2);
    }

    @Test(timeout = 5000)
    public void builderConsumerThrowsRuntimeException_attemptFailsBeforeCancellation() throws InterruptedException {
        RuntimeException failure = new RuntimeException("boom");
        verifyBuilderConsumerFailure(event -> {
            throw failure;
        }, failure);
    }

    @Test(timeout = 5000)
    public void builderConsumerThrowsError_attemptFailsBeforeCancellation() throws InterruptedException {
        AssertionError failure = new AssertionError("boom");
        verifyBuilderConsumerFailure(event -> {
            throw failure;
        }, failure);
    }

    @Test(timeout = 5000)
    public void builderConsumerThrowsWithPublisherTransformer_attemptFailsBeforeCancellation()
        throws InterruptedException {
        RuntimeException failure = new RuntimeException("boom");
        Object transformedEvent = new Object();
        TestResponseHandlerBuilder builder =
            new TestResponseHandlerBuilder().publisherTransformer(publisher -> publisher.map(event -> transformedEvent));
        verifyBuilderConsumerFailure(builder, event -> {
            assertThat(event).isSameAs(transformedEvent);
            throw failure;
        }, failure);
    }

    @Test
    public void builderConsumerCompletesNormally_operationCompletesNormally() {
        Message eventMessage = eventMessage();
        AtomicInteger numEvents = new AtomicInteger();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        TestResponseHandler handler = new TestResponseHandlerBuilder()
            .subscriber(event -> numEvents.incrementAndGet())
            .build();
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(handler)
                                               .eventResponseHandler((r, e) -> new Object())
                                               .future(operationFuture)
                                               .build();

        CompletableFuture<Void> transformFuture = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(Flowable.just(eventMessage.toByteBuffer())));

        transformFuture.join();
        operationFuture.join();
        assertThat(numEvents).hasValue(1);
    }

    @Test
    public void builderConsumer_validEventsDoNotCompleteOperation() {
        Message eventMessage = eventMessage();
        PublishProcessor<ByteBuffer> bytePublisher = PublishProcessor.create();
        AtomicInteger numEvents = new AtomicInteger();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        TestResponseHandler handler = new TestResponseHandlerBuilder()
            .subscriber(event -> numEvents.incrementAndGet())
            .build();
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(handler)
                                               .eventResponseHandler((r, e) -> new Object())
                                               .future(operationFuture)
                                               .build();

        CompletableFuture<Void> transformFuture = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));
        bytePublisher.onNext(eventMessage.toByteBuffer());

        assertThat(numEvents).hasValue(1);
        assertThat(transformFuture).isNotDone();
        assertThat(operationFuture).isNotDone();

        bytePublisher.onNext(eventMessage.toByteBuffer());
        bytePublisher.onComplete();

        transformFuture.join();
        operationFuture.join();
        assertThat(numEvents).hasValue(2);
    }

    @Test(timeout = 5000)
    public void staleAttemptCallbackFailureDoesNotCompleteCurrentAttempt() {
        RuntimeException firstAttemptFailure = new RuntimeException("attempt failed");
        RuntimeException lateCallbackFailure = new RuntimeException("late callback failed");
        PublishProcessor<ByteBuffer> firstPublisher = PublishProcessor.create();
        PublishProcessor<ByteBuffer> secondPublisher = PublishProcessor.create();
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        AtomicInteger callbackInvocations = new AtomicInteger();
        TestResponseHandler handler = new TestResponseHandlerBuilder()
            .subscriber(event -> {
                callbackInvocations.incrementAndGet();
                throw lateCallbackFailure;
            })
            .build();
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(handler)
                                               .eventResponseHandler((r, e) -> new Object())
                                               .future(operationFuture)
                                               .build();

        CompletableFuture<Void> firstAttempt = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(firstPublisher));
        transformer.exceptionOccurred(firstAttemptFailure);
        assertThatThrownBy(firstAttempt::join).hasCause(firstAttemptFailure);

        CompletableFuture<Void> secondAttempt = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(secondPublisher));
        firstPublisher.onNext(eventMessage().toByteBuffer());

        assertThat(callbackInvocations).hasValue(1);
        assertThat(operationFuture).isNotDone();
        assertThat(secondAttempt).isNotDone();

        secondPublisher.onComplete();
        secondAttempt.join();
        operationFuture.join();
    }

    @Test(timeout = 5000)
    public void responseHandlerFromBuilderOverride_originalPublisher_callbackFailurePropagates()
        throws InterruptedException {
        RuntimeException failure = new RuntimeException("boom");
        AtomicBoolean invoked = new AtomicBoolean();
        AtomicInteger numEvents = new AtomicInteger();
        TestResponseHandlerBuilder builder = new TestResponseHandlerBuilder().subscriber(event -> {
            numEvents.incrementAndGet();
            throw failure;
        });
        EventStreamResponseHandlerFromBuilder<Object, Object> handler =
            new EventStreamResponseHandlerFromBuilder<Object, Object>(builder) {
                @Override
                public void onEventStream(SdkPublisher<Object> publisher) {
                    invoked.set(true);
                    super.onEventStream(publisher);
                }
            };

        verifyBuilderConsumerFailure(handler, failure, numEvents);
        assertThat(invoked).isTrue();
    }

    @Test(timeout = 5000)
    public void responseHandlerFromBuilderOverride_wrappedPublisher_callbackFailurePropagates()
        throws InterruptedException {
        RuntimeException failure = new RuntimeException("boom");
        AtomicInteger numEvents = new AtomicInteger();
        TestResponseHandlerBuilder builder = new TestResponseHandlerBuilder().subscriber(event -> {
            numEvents.incrementAndGet();
            throw failure;
        });
        EventStreamResponseHandlerFromBuilder<Object, Object> handler =
            new EventStreamResponseHandlerFromBuilder<Object, Object>(builder) {
                @Override
                public void onEventStream(SdkPublisher<Object> publisher) {
                    super.onEventStream(publisher.map(event -> event));
                }
            };

        verifyBuilderConsumerFailure(handler, failure, numEvents);
    }

    @Test(timeout = 5000)
    public void responseHandlerFromBuilderOverride_deferredPublisher_callbackFailurePropagates()
        throws InterruptedException {
        RuntimeException failure = new RuntimeException("boom");
        AtomicInteger numEvents = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            TestResponseHandlerBuilder builder = new TestResponseHandlerBuilder().subscriber(event -> {
                numEvents.incrementAndGet();
                throw failure;
            });
            EventStreamResponseHandlerFromBuilder<Object, Object> handler =
                new EventStreamResponseHandlerFromBuilder<Object, Object>(builder) {
                    @Override
                    public void onEventStream(SdkPublisher<Object> publisher) {
                        executor.execute(() -> super.onEventStream(publisher));
                    }
                };

            verifyBuilderConsumerFailure(handler, failure, numEvents);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void unknownExceptionEventsThrowException() {
        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":message-type", HeaderValue.fromString("exception"));
        headers.put(":exception-type", HeaderValue.fromString("modeledException"));
        headers.put(":content-type", HeaderValue.fromString("application/json"));

        verifyExceptionThrown(headers);
    }

    @Test
    public void errorEventsThrowException() {
        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":message-type", HeaderValue.fromString("error"));

        verifyExceptionThrown(headers);
    }

    @Test
    public void prepareReturnsNewFuture() {
        AsyncResponseTransformer<SdkResponse, Void> transformer =
                EventStreamAsyncResponseTransformer.builder()
                        .eventStreamResponseHandler(
                                onEventStream(p -> {}))
                        .eventResponseHandler((r, e) -> null)
                        .executor(Executors.newFixedThreadPool(2))
                        .future(new CompletableFuture<>())
                        .build();

        CompletableFuture<?> cf1 = transformer.prepare();

        transformer.exceptionOccurred(new RuntimeException("Boom!"));

        assertThat(cf1.isCompletedExceptionally()).isTrue();
        assertThat(transformer.prepare()).isNotEqualTo(cf1);
    }

    @Test(timeout = 2000)
    public void prepareResetsSubscriberRef() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);

        AsyncResponseTransformer<SdkResponse, Void> transformer =
                EventStreamAsyncResponseTransformer.builder()
                        .eventStreamResponseHandler(
                                onEventStream(p -> {
                                    try {
                                        p.subscribe(e -> {});
                                    } catch (Throwable t) {
                                        exceptionThrown.set(true);
                                    } finally {
                                        latch.countDown();
                                    }
                                }))
                        .eventResponseHandler((r, e) -> null)
                        .executor(Executors.newFixedThreadPool(2))
                        .future(new CompletableFuture<>())
                        .build();

        Flowable<ByteBuffer> bytePublisher = Flowable.empty();

        CompletableFuture<Void> transformFuture = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));
        transformFuture.join();

        transformFuture = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));
        transformFuture.join();

        latch.await();
        assertThat(exceptionThrown).isFalse();
    }

    @Test
    public void erroneousExtraExceptionOccurredDoesNotSurfaceException() {
        AtomicLong numExceptions = new AtomicLong(0);
        AsyncResponseTransformer<SdkResponse, Void> transformer =
                EventStreamAsyncResponseTransformer.builder()
                        .eventStreamResponseHandler(new EventStreamResponseHandler<Object, Object>() {
                            @Override
                            public void responseReceived(Object response) {
                            }

                            @Override
                            public void onEventStream(SdkPublisher<Object> publisher) {
                            }

                            @Override
                            public void exceptionOccurred(Throwable throwable) {
                                numExceptions.incrementAndGet();
                            }

                            @Override
                            public void complete() {
                            }
                        })
                        .eventResponseHandler((r, e) -> null)
                        .executor(Executors.newFixedThreadPool(2))
                        .future(new CompletableFuture<>())
                        .build();

        transformer.prepare();
        transformer.exceptionOccurred(new RuntimeException("Boom!"));
        transformer.exceptionOccurred(new RuntimeException("Boom again!"));

        assertThat(numExceptions).hasValue(1);
    }

    // Test that the class guards against signalling exceptionOccurred if the stream is already complete.
    @Test
    public void erroneousExceptionOccurredAfterCompleteDoesNotSurfaceException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Subscriber<Object> subscriber = new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(Object o) {
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        };

        AtomicLong numExceptionOccurredCalls = new AtomicLong(0);
        AsyncResponseTransformer<SdkResponse, Void> transformer =
                EventStreamAsyncResponseTransformer.builder()
                        .eventStreamResponseHandler(new EventStreamResponseHandler<Object, Object>() {
                            @Override
                            public void responseReceived(Object response) {
                            }

                            @Override
                            public void onEventStream(SdkPublisher<Object> publisher) {
                                publisher.subscribe(subscriber);
                            }

                            @Override
                            public void exceptionOccurred(Throwable throwable) {
                                numExceptionOccurredCalls.incrementAndGet();
                            }

                            @Override
                            public void complete() {
                                latch.countDown();
                            }
                        })
                        .eventResponseHandler((r, e) -> null)
                        .executor(Executors.newFixedThreadPool(2))
                        .future(new CompletableFuture<>())
                        .build();

        Flowable<ByteBuffer> bytePublisher = Flowable.empty();

        transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));

        latch.await();

        transformer.exceptionOccurred(new RuntimeException("Uh-oh"));

        assertThat(numExceptionOccurredCalls)
                .as("Expected only one event to be delivered")
                .hasValue(0);
    }

    private void verifyBuilderConsumerFailure(Consumer<Object> consumer, Throwable expectedFailure)
        throws InterruptedException {
        verifyBuilderConsumerFailure(new TestResponseHandlerBuilder(), consumer, expectedFailure);
    }

    private void verifyBuilderConsumerFailure(TestResponseHandlerBuilder builder,
                                              Consumer<Object> consumer,
                                              Throwable expectedFailure) throws InterruptedException {
        AtomicInteger numEvents = new AtomicInteger();
        EventStreamResponseHandler<Object, Object> handler = builder
            .subscriber(event -> {
                numEvents.incrementAndGet();
                consumer.accept(event);
            })
            .build();
        verifyBuilderConsumerFailure(handler, expectedFailure, numEvents);
    }

    private void verifyBuilderConsumerFailure(EventStreamResponseHandler<Object, Object> handler,
                                              Throwable expectedFailure,
                                              AtomicInteger numEvents) throws InterruptedException {
        Message eventMessage = eventMessage();
        CountDownLatch cancelled = new CountDownLatch(1);
        Flowable<ByteBuffer> bytePublisher = Flowable.just(eventMessage.toByteBuffer(), eventMessage.toByteBuffer())
                                                     .doOnCancel(cancelled::countDown);
        CompletableFuture<Void> operationFuture = new CompletableFuture<>();
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(handler)
                                               .eventResponseHandler((r, e) -> new Object())
                                               .future(operationFuture)
                                               .build();

        CompletableFuture<Void> transformFuture = transformer.prepare();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));

        CompletionException completionException = null;
        try {
            transformFuture.join();
        } catch (CompletionException e) {
            completionException = e;
        }
        assertThat(completionException).isNotNull();
        assertThat(completionException.getCause()).isInstanceOf(NonRetryableException.class);
        assertThat(completionException.getCause().getCause()).isSameAs(expectedFailure);
        assertThat(cancelled.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(operationFuture).isNotDone();
        assertThat(numEvents).hasValue(1);
    }

    private Message eventMessage() {
        return new Message(ImmutableMap.of(":message-type", HeaderValue.fromString("event"),
                                           ":event-type", HeaderValue.fromString("foo")),
                           new byte[0]);
    }

    private void verifyExceptionThrown(Map<String, HeaderValue> headers) {
        SdkServiceException exception = SdkServiceException.builder().build();

        Message exceptionMessage = new Message(headers, new byte[0]);

        Flowable<ByteBuffer> bytePublisher = Flowable.just(exceptionMessage.toByteBuffer());

        SubscribingResponseHandler handler = new SubscribingResponseHandler();
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(handler)
                                               .exceptionResponseHandler((response, executionAttributes) -> exception)
                                               .executor(Executors.newSingleThreadExecutor())
                                               .future(new CompletableFuture<>())
                                               .build();
        CompletableFuture<Void> cf = transformer.prepare();
        transformer.onResponse(null);
        transformer.onStream(SdkPublisher.adapt(bytePublisher));

        assertThatThrownBy(() -> {
            try {
                cf.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof SdkServiceException) {
                    throw e.getCause();
                }
            }
        }).isSameAs(exception);

        assertThat(handler.exceptionOccurredCalled).isTrue();
    }

    private static final class TestResponseHandlerBuilder
        extends DefaultEventStreamResponseHandlerBuilder<Object, Object, TestResponseHandlerBuilder> {

        private TestResponseHandler build() {
            return new TestResponseHandler(this);
        }
    }

    private static final class TestResponseHandler extends EventStreamResponseHandlerFromBuilder<Object, Object> {
        private TestResponseHandler(TestResponseHandlerBuilder builder) {
            super(builder);
        }
    }

    private static class SubscribingResponseHandler implements EventStreamResponseHandler<Object, Object> {
        private volatile boolean exceptionOccurredCalled = false;

        @Override
        public void responseReceived(Object response) {
        }

        @Override
        public void onEventStream(SdkPublisher<Object> publisher) {
            publisher.subscribe(e -> {
            });
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            exceptionOccurredCalled = true;
        }

        @Override
        public void complete() {
        }
    }

    public EventStreamResponseHandler<Object, Object> onEventStream(Consumer<SdkPublisher<Object>> onEventStream) {
        return new EventStreamResponseHandler<Object, Object>() {

            @Override
            public void responseReceived(Object response) {
            }

            @Override
            public void onEventStream(SdkPublisher<Object> publisher) {
                onEventStream.accept(publisher);
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
            }

            @Override
            public void complete() {
            }
        };
    }
}
