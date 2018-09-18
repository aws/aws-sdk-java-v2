/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
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
        transformer.responseReceived(null);
        transformer.onStream(SdkPublisher.adapt(bytePublisher));

        assertThatThrownBy(transformer::complete).isSameAs(exception);
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