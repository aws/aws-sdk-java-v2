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
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(
                                                   onEventStream(p -> p.subscribe(requestOneSubscriber)))
                                               .eventResponseHandler((r, e) -> new Object())
                                               .executor(Executors.newSingleThreadExecutor())
                                               .future(new CompletableFuture<>())
                                               .build();
        transformer.onStream(SdkPublisher.adapt(bytePublisher));
        latch.await();
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

    private void verifyExceptionThrown(Map<String, HeaderValue> headers) {
        SdkServiceException exception = SdkServiceException.builder().build();

        Message exceptionMessage = new Message(headers, new byte[0]);

        Flowable<ByteBuffer> bytePublisher = Flowable.just(exceptionMessage.toByteBuffer());

        AsyncResponseTransformer<SdkResponse, Void> transformer =
            EventStreamAsyncResponseTransformer.builder()
                                               .eventStreamResponseHandler(new SubscribingResponseHandler())
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
                    throw ((SdkServiceException) e.getCause());
                }
            }
        }).isSameAs(exception);
    }

    private static class SubscribingResponseHandler implements EventStreamResponseHandler<Object, Object> {

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
