/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.eventstream;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.util.Throwables;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.eventstream.Message;
import software.amazon.eventstream.MessageDecoder;

/**
 * Unmarshalling layer on top of the {@link AsyncResponseTransformer} to decode event stream messages and deliver them to the
 * subscriber.
 *
 * @param <ResponseT> Initial response type of event stream operation.
 * @param <EventT> Base type of event stream message frames.
 */
@SdkProtectedApi
public class EventStreamAsyncResponseTransformer<ResponseT, EventT>
    implements AsyncResponseTransformer<SdkResponse, Void> {

    private static final Logger log = LoggerFactory.getLogger(EventStreamAsyncResponseTransformer.class);

    private static final ExecutionAttributes EMPTY_EXECUTION_ATTRIBUTES = new ExecutionAttributes();

    /**
     * {@link EventStreamResponseHandler} provided by customer.
     */
    private final EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseTransformer;

    /**
     * Unmarshalls the initial response.
     */
    private final HttpResponseHandler<? extends ResponseT> initialResponseUnmarshaller;

    /**
     * Unmarshalls the event POJO.
     */
    private final HttpResponseHandler<? extends EventT> eventUnmarshaller;

    /**
     * Remaining demand (i.e number of unmarshalled events) we need to provide to the customers subscriber.
     */
    private final AtomicLong remainingDemand = new AtomicLong(0);

    /**
     * Reference to customers subscriber to events.
     */
    private final AtomicReference<Subscriber<? super EventT>> subscriberRef = new AtomicReference<>();

    /**
     * Event stream message decoder that decodes the binary data into "frames". These frames are then passed to the
     * unmarshaller to produce the event POJO.
     */
    private final MessageDecoder decoder = createDecoder();

    /**
     * Tracks whether we have delivered a terminal notification to the subscriber and response handler
     * (i.e. exception or completion).
     */
    private volatile boolean isDone = false;

    /**
     * Holds a reference to any exception delivered to exceptionOccurred.
     */
    private final AtomicReference<Throwable> error = new AtomicReference<>();

    /**
     * @param eventStreamResponseTransformer Response transformer provided by customer.
     * @param initialResponseUnmarshaller Unmarshaller for the initial-response event stream message.
     * @param eventUnmarshaller Unmarshaller for the various event types.
     */
    public EventStreamAsyncResponseTransformer(
        EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseTransformer,
        HttpResponseHandler<? extends ResponseT> initialResponseUnmarshaller,
        HttpResponseHandler<? extends EventT> eventUnmarshaller) {

        this.eventStreamResponseTransformer = eventStreamResponseTransformer;
        this.initialResponseUnmarshaller = initialResponseUnmarshaller;
        this.eventUnmarshaller = eventUnmarshaller;
    }

    @Override
    public void responseReceived(SdkResponse response) {
        // We use a void unmarshaller and unmarshall the actual response in the message
        // decoder when we receive the initial-response frame. TODO not clear
        // how we would handle REST protocol which would unmarshall the response from the HTTP headers
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        synchronized (this) {
            // Reset to allow more exceptions to propagate for retries
            isDone = false;
        }
        CompletableFuture<Subscription> dataSubscriptionFuture = new CompletableFuture<>();
        publisher.subscribe(new ByteSubscriber(dataSubscriptionFuture));
        dataSubscriptionFuture.thenAccept(dataSubscription -> {
            SdkPublisher<EventT> eventPublisher = new EventPublisher(dataSubscription);
            eventStreamResponseTransformer.onEventStream(eventPublisher);
        });
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        synchronized (this) {
            if (!isDone) {
                isDone = true;
                error.set(throwable);
                // If we have a Subscriber at this point notify it as well
                if (subscriberRef.get() != null) {
                    runAndLogError(log, "Error thrown from Subscriber#onError, ignoring.",
                        () -> subscriberRef.get().onError(throwable));
                }
                eventStreamResponseTransformer.exceptionOccurred(throwable);
            }
        }
    }

    @Override
    public Void complete() {
        synchronized (this) {
            if (!isDone) {
                isDone = true;
                // If we have a Subscriber at this point notify it as well
                if (subscriberRef.get() != null) {
                    runAndLogError(log, "Error thrown from Subscriber#onComplete, ignoring.",
                        () -> subscriberRef.get().onComplete());
                }
                eventStreamResponseTransformer.complete();
                return null;
            } else {
                // Need to propagate the failure up so the future is completed exceptionally. This should only happen
                // when there is a frame level exception that the upper layers don't know about.
                throw Throwables.failure(error.get());
            }
        }
    }

    /**
     * Create the event stream {@link MessageDecoder} which will decode the raw bytes into {@link Message} frames.
     *
     * @return Decoder.
     */
    private MessageDecoder createDecoder() {
        return new MessageDecoder(m -> {
            try {
                if (isEvent(m)) {
                    if (m.getHeaders().get(":event-type").getString().equals("initial-response")) {
                        eventStreamResponseTransformer.responseReceived(
                            initialResponseUnmarshaller.handle(adaptMessageToResponse(m),
                                                               EMPTY_EXECUTION_ATTRIBUTES));
                    } else {
                        remainingDemand.decrementAndGet();
                        subscriberRef.get().onNext(eventUnmarshaller.handle(adaptMessageToResponse(m),
                                                                            EMPTY_EXECUTION_ATTRIBUTES));
                    }
                } else if (isError(m)) {
                    String message = m.getHeaders().get(":error-message").getString();
                    String errorCode = m.getHeaders().get(":error-code").getString();
                    EventStreamException exception = EventStreamException.builder()
                                                                         .message(message)
                                                                         .errorCode(errorCode)
                                                                         .build();
                    runAndLogError(log, "Error thrown from exceptionOccurred, ignoring.",
                        () -> exceptionOccurred(exception));
                }
            } catch (Exception e) {
                throw SdkClientException.builder().cause(e).build();
            }
        });
    }

    /**
     * @param m Message frame.
     * @return True if frame is an event frame, false if not.
     */
    private boolean isEvent(Message m) {
        return "event".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * @param m Message frame.
     * @return True if frame is an error frame, false if not.
     */
    private boolean isError(Message m) {
        return "error".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * Transforms an event stream message into a {@link HttpResponse} so we can reuse our existing generated unmarshallers.
     *
     * @param m Message to transform.
     */
    private HttpResponse adaptMessageToResponse(Message m) {
        HttpResponse response = new HttpResponse(null);
        response.setContent(new ByteArrayInputStream(m.getPayload()));
        m.getHeaders().forEach((k, v) -> response.addHeader(k, v.getString()));
        return response;
    }

    /**
     * Subscriber for the raw bytes from the stream. Feeds them to the {@link MessageDecoder} as they arrive
     * and will request as much as needed to fulfill any outstanding demand.
     */
    private class ByteSubscriber implements Subscriber<ByteBuffer> {

        private final CompletableFuture<Subscription> dataSubscriptionFuture;

        private Subscription subscription;

        /**
         * @param dataSubscriptionFuture Future to notify when the {@link Subscription} object is available.
         */
        private ByteSubscriber(CompletableFuture<Subscription> dataSubscriptionFuture) {
            this.dataSubscriptionFuture = dataSubscriptionFuture;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            dataSubscriptionFuture.complete(subscription);
            this.subscription = subscription;
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            decoder.feed(BinaryUtils.copyBytesFrom(buffer));
            // If we still haven't fulfilled the outstanding demand then keep requesting byte chunks until we do
            if (remainingDemand.get() > 0) {
                this.subscription.request(1);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // Notified in response handler exceptionOccurred because we have more context on what we've delivered to
            // the event stream subscriber there.
        }

        @Override
        public void onComplete() {
            // Notified in response handler complete method because we have more context on what we've delivered to
            // the event stream subscriber there.
        }
    }

    /**
     * Publisher of event stream events. Tracks outstanding demand and requests raw data from the stream until that demand is
     * fulfilled.
     */
    private class EventPublisher implements SdkPublisher<EventT> {

        private final Subscription dataSubscription;

        private EventPublisher(Subscription dataSubscription) {
            this.dataSubscription = dataSubscription;
        }

        @Override
        public void subscribe(Subscriber<? super EventT> subscriber) {
            if (subscriberRef.compareAndSet(null, subscriber)) {
                subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long l) {
                        // Kick off the first request to the byte buffer publisher which will keep requesting
                        // bytes until we can fulfill the demand of the event publisher.
                        dataSubscription.request(1);
                        remainingDemand.addAndGet(l);
                    }

                    @Override
                    public void cancel() {
                        dataSubscription.cancel();
                    }
                });
            } else {
                log.error("Event stream publishers can only be subscribed to once.");
                throw new IllegalStateException("This publisher may only be subscribed to once");
            }
        }
    }

}
