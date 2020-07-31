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

import static java.util.Collections.singletonList;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADERS;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkCancellationException;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;
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
public final class EventStreamAsyncResponseTransformer<ResponseT, EventT>
    implements AsyncResponseTransformer<SdkResponse, Void> {

    private static final Logger log = LoggerFactory.getLogger(EventStreamAsyncResponseTransformer.class);

    private static final Object ON_COMPLETE_EVENT = new Object();

    private static final ExecutionAttributes EMPTY_EXECUTION_ATTRIBUTES = new ExecutionAttributes();

    /**
     * {@link EventStreamResponseHandler} provided by customer.
     */
    private final EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler;

    /**
     * Unmarshalls the initial response.
     */
    private final HttpResponseHandler<? extends ResponseT> initialResponseHandler;

    /**
     * Unmarshalls the event POJO.
     */
    private final HttpResponseHandler<? extends EventT> eventResponseHandler;

    /**
     * Unmarshalls exception events.
     */
    private final HttpResponseHandler<? extends Throwable> exceptionResponseHandler;

    /**
     * Remaining demand (i.e number of unmarshalled events) we need to provide to the customers subscriber.
     */
    private final AtomicLong remainingDemand = new AtomicLong(0);

    /**
     * Reference to customers subscriber to events.
     */
    private final AtomicReference<Subscriber<? super EventT>> subscriberRef = new AtomicReference<>();

    private final AtomicReference<Subscription> dataSubscription = new AtomicReference<>();

    /**
     * Event stream message decoder that decodes the binary data into "frames". These frames are then passed to the
     * unmarshaller to produce the event POJO.
     */
    private final MessageDecoder decoder = new MessageDecoder(this::handleMessage);

    /**
     * Tracks whether we have delivered a terminal notification to the subscriber and response handler
     * (i.e. exception or completion).
     */
    private volatile boolean isDone = false;

    /**
     * Executor to deliver events to the subscriber
     */
    private final Executor executor;

    /**
     * Queue of events to deliver to downstream subscriber. Will contain mostly objects
     * of type EventT, the special {@link #ON_COMPLETE_EVENT} will be added when all events
     * have been added to the queue.
     */
    private final Queue<Object> eventsToDeliver = new LinkedList<>();

    /**
     * Flag to indicate we are currently delivering events to the subscriber.
     */
    private final AtomicBoolean isDelivering = new AtomicBoolean(false);

    /**
     * Flag to indicate we are currently requesting demand from the data publisher.
     */
    private final AtomicBoolean isRequesting = new AtomicBoolean(false);

    /**
     * Future to notify on completion. Note that we do not notify this future in the event of an error, that
     * is handled separately by the generated client. Ultimately we need this due to a disconnect between
     * completion of the request (i.e. finish reading all the data from the wire) and the completion of the event
     * stream (i.e. deliver the last event to the subscriber).
     */
    private final CompletableFuture<Void> future;

    /**
     * The name of the aws service
     */
    private final String serviceName;

    /**
     * Request Id for the streaming request. The value is populated when the initial response is received from the service.
     * As request id is not sent in event messages (including exceptions), this can be returned by the SDK along with
     * received exception details.
     */
    private String requestId = null;

    private volatile CompletableFuture<Void> transformFuture;

    /**
     * Extended Request Id for the streaming request. The value is populated when the initial response is received from the
     * service. As request id is not sent in event messages (including exceptions), this can be returned by the SDK along with
     * received exception details.
     */
    private String extendedRequestId = null;

    private EventStreamAsyncResponseTransformer(
        EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler,
        HttpResponseHandler<? extends ResponseT> initialResponseHandler,
        HttpResponseHandler<? extends EventT> eventResponseHandler,
        HttpResponseHandler<? extends Throwable> exceptionResponseHandler,
        Executor executor,
        CompletableFuture<Void> future,
        String serviceName) {

        this.eventStreamResponseHandler = eventStreamResponseHandler;
        this.initialResponseHandler = initialResponseHandler;
        this.eventResponseHandler = eventResponseHandler;
        this.exceptionResponseHandler = exceptionResponseHandler;
        this.executor = executor;
        this.future = future;
        this.serviceName = serviceName;
    }

    @Override
    public CompletableFuture<Void> prepare() {
        transformFuture = new CompletableFuture<>();
        subscriberRef.set(null);
        isDone = false;
        return transformFuture;
    }

    @Override
    public void onResponse(SdkResponse response) {
        if (response != null && response.sdkHttpResponse() != null) {
            this.requestId = SdkHttpUtils.firstMatchingHeaderFromCollection(response.sdkHttpResponse().headers(),
                                                                            X_AMZN_REQUEST_ID_HEADERS)
                                         .orElse(null);

            this.extendedRequestId = response.sdkHttpResponse()
                                             .firstMatchingHeader(X_AMZ_ID_2_HEADER)
                                             .orElse(null);
        }
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        CompletableFuture<Subscription> dataSubscriptionFuture = new CompletableFuture<>();
        publisher.subscribe(new ByteSubscriber(dataSubscriptionFuture));
        dataSubscriptionFuture.thenAccept(dataSubscription -> {
            SdkPublisher<EventT> eventPublisher = new EventPublisher(dataSubscription);
            try {
                eventStreamResponseHandler.onEventStream(eventPublisher);
            } catch (Throwable t) {
                exceptionOccurred(t);
                dataSubscription.cancel();
            }
        });
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        synchronized (this) {
            if (!isDone) {
                isDone = true;
                // If we have a Subscriber at this point notify it as well
                if (subscriberRef.get() != null && shouldSurfaceErrorToEventSubscriber(throwable)) {
                    runAndLogError(log, "Error thrown from Subscriber#onError, ignoring.",
                        () -> subscriberRef.get().onError(throwable));
                }
                eventStreamResponseHandler.exceptionOccurred(throwable);
                transformFuture.completeExceptionally(throwable);
            }
        }
    }

    /**
     * Called when all events have been delivered to the downstream subscriber.
     */
    private void onEventComplete() {
        synchronized (this) {
            // No op if it's already done
            if (isDone) {
                return;
            }

            isDone = true;
            runAndLogError(log, "Error thrown from Subscriber#onComplete, ignoring.",
                () -> subscriberRef.get().onComplete());
            eventStreamResponseHandler.complete();
            future.complete(null);
        }
    }

    /**
     * Handle the event stream message according to it's type.
     *
     * @param m Decoded message.
     */
    private void handleMessage(Message m) {
        try {
            if (isEvent(m)) {
                if (m.getHeaders().get(":event-type").getString().equals("initial-response")) {
                    eventStreamResponseHandler.responseReceived(
                        initialResponseHandler.handle(adaptMessageToResponse(m, false),
                                                      EMPTY_EXECUTION_ATTRIBUTES));
                } else {
                    // Add to queue to be delivered later by the executor
                    eventsToDeliver.add(eventResponseHandler.handle(adaptMessageToResponse(m, false),
                                                                    EMPTY_EXECUTION_ATTRIBUTES));
                }
            } else if (isError(m) || isException(m)) {
                SdkHttpFullResponse errorResponse = adaptMessageToResponse(m, true);
                Throwable exception = exceptionResponseHandler.handle(
                    errorResponse, new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, serviceName));
                runAndLogError(log, "Error thrown from exceptionOccurred, ignoring.", () -> exceptionOccurred(exception));
            }
        } catch (Exception e) {
            throw SdkClientException.builder().cause(e).build();
        }
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
     * @param m Message frame.
     * @return True if frame is an exception frame, false if not.
     */
    private boolean isException(Message m) {
        return "exception".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * Transforms an event stream message into a {@link SdkHttpFullResponse} so we can reuse our existing generated unmarshallers.
     *
     * @param message Message to transform.
     */
    private SdkHttpFullResponse adaptMessageToResponse(Message message, boolean isException) {

        Map<String, List<String>> headers =
            message.getHeaders()
                   .entrySet()
                   .stream()
                   .collect(HashMap::new, (m, e) -> m.put(e.getKey(), singletonList(e.getValue().getString())), Map::putAll);

        if (requestId != null) {
            headers.put(X_AMZN_REQUEST_ID_HEADER, singletonList(requestId));
        }

        if (extendedRequestId != null) {
            headers.put(X_AMZ_ID_2_HEADER, singletonList(extendedRequestId));
        }

        SdkHttpFullResponse.Builder builder =
            SdkHttpFullResponse.builder()
                               .content(AbortableInputStream.create(new ByteArrayInputStream(message.getPayload())))
                               .headers(headers);

        if (!isException) {
            builder.statusCode(200);
        }

        return builder.build();
    }

    private static boolean shouldSurfaceErrorToEventSubscriber(Throwable t) {
        return !(t instanceof SdkCancellationException);
    }

    /**
     * Subscriber for the raw bytes from the stream. Feeds them to the {@link MessageDecoder} as they arrive
     * and will request as much as needed to fulfill any outstanding demand.
     */
    private class ByteSubscriber implements Subscriber<ByteBuffer> {

        private final CompletableFuture<Subscription> dataSubscriptionFuture;

        /**
         * @param dataSubscriptionFuture Future to notify when the {@link Subscription} object is available.
         */
        private ByteSubscriber(CompletableFuture<Subscription> dataSubscriptionFuture) {
            this.dataSubscriptionFuture = dataSubscriptionFuture;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            dataSubscription.set(subscription);
            dataSubscriptionFuture.complete(subscription);
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            // Bail out if we've already delivered an exception to the downstream subscriber
            if (isDone) {
                return;
            }
            synchronized (eventsToDeliver) {
                decoder.feed(BinaryUtils.copyBytesFrom(buffer));
                // If we have things to deliver, do so.
                if (!eventsToDeliver.isEmpty()) {
                    isRequesting.compareAndSet(true, false);
                    drainEventsIfNotAlready();
                } else {
                    // If we still haven't fulfilled the outstanding demand then keep requesting byte chunks until we do
                    if (remainingDemand.get() > 0) {
                        dataSubscription.get().request(1);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // Notified in response handler exceptionOccurred because we have more context on what we've delivered to
            // the event stream subscriber there.
        }

        @Override
        public void onComplete() {
            // Add the special on complete event to signal drainEvents to complete the subscriber
            synchronized (eventsToDeliver) {
                eventsToDeliver.add(ON_COMPLETE_EVENT);
            }
            drainEventsIfNotAlready();
            transformFuture.complete(null);
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
                        if (isDone) {
                            return;
                        }
                        synchronized (eventsToDeliver) {
                            remainingDemand.addAndGet(l);
                            if (!eventsToDeliver.isEmpty()) {
                                drainEventsIfNotAlready();
                            } else {
                                requestDataIfNotAlready();
                            }
                        }
                    }

                    @Override
                    public void cancel() {
                        dataSubscription.cancel();

                        // Need to complete the futures, otherwise the downstream subscriber will never
                        // get notified
                        future.complete(null);
                        transformFuture.complete(null);
                    }
                });
            } else {
                log.error("Event stream publishers can only be subscribed to once.");
                throw new IllegalStateException("This publisher may only be subscribed to once");
            }
        }
    }

    /**
     * Requests data from the {@link ByteBuffer} {@link Publisher} until we have enough data to fulfill demand. If we are
     * already requesting data this is a no-op.
     */
    private void requestDataIfNotAlready() {
        if (isRequesting.compareAndSet(false, true)) {
            dataSubscription.get().request(1);
        }
    }

    /**
     * Drains events from the queue until the demand is met or all events are delivered. If we are already
     * in the process of delivering events this is a no-op.
     */
    private void drainEventsIfNotAlready() {
        if (isDelivering.compareAndSet(false, true)) {
            drainEvents();
        }
    }

    /**
     * Drains events from the queue until the demand is met or all events are delivered. This differs
     * from {@link #drainEventsIfNotAlready()} in that it assumes it has the {@link #isDelivering} 'lease' already.
     */
    private void drainEvents() {
        // If we've already delivered an exception to the subscriber than bail out
        if (isDone) {
            return;
        }

        if (isCompletedOrDeliverEvent()) {
            onEventComplete();
        }
    }

    /**
     * Checks whether the eventsToDeliver is completed and if it is not completed,
     * deliver more events
     *
     * @return true if the eventsToDeliver is completed, otherwise false.
     */
    private boolean isCompletedOrDeliverEvent() {
        synchronized (eventsToDeliver) {
            if (eventsToDeliver.peek() == ON_COMPLETE_EVENT) {
                return true;
            }

            if (eventsToDeliver.isEmpty() || remainingDemand.get() == 0) {
                isDelivering.compareAndSet(true, false);
                // If we still have demand to fulfill then request more if we aren't already requesting
                if (remainingDemand.get() > 0) {
                    requestDataIfNotAlready();
                }
            } else {
                // Deliver the event and recursively call ourselves after it's delivered
                Object event = eventsToDeliver.remove();
                remainingDemand.decrementAndGet();
                CompletableFuture.runAsync(() -> deliverEvent(event), executor)
                                 .thenRunAsync(this::drainEvents, executor)
                                 .whenComplete((v, t) -> {
                                     if (t != null) {
                                         log.error("Error occurred when delivering an event", t);
                                         throw SdkClientException.create("fail to deliver events", t);
                                     }
                                 });
            }
        }
        return false;
    }

    /**
     * Delivers the event to the downstream subscriber. We already know the type so the cast is safe.
     */
    @SuppressWarnings("unchecked")
    private void deliverEvent(Object event) {
        subscriberRef.get().onNext((EventT) event);
    }

    /**
     * Creates a {@link Builder} used to create {@link EventStreamAsyncResponseTransformer}.
     *
     * @param <ResponseT> Initial response type.
     * @param <EventT> Event type being delivered.
     * @return New {@link Builder} instance.
     */
    public static <ResponseT, EventT> Builder<ResponseT, EventT> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link EventStreamAsyncResponseTransformer}.
     *
     * @param <ResponseT> Initial response type.
     * @param <EventT> Event type being delivered.
     */
    public static final class Builder<ResponseT, EventT> {

        private EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler;
        private HttpResponseHandler<? extends ResponseT> initialResponseHandler;
        private HttpResponseHandler<? extends EventT> eventResponseHandler;
        private HttpResponseHandler<? extends Throwable> exceptionResponseHandler;
        private Executor executor;
        private CompletableFuture<Void> future;
        private String serviceName;

        private Builder() {
        }

        /**
         * @param eventStreamResponseHandler Response handler provided by customer.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> eventStreamResponseHandler(
            EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler) {
            this.eventStreamResponseHandler = eventStreamResponseHandler;
            return this;
        }

        /**
         * @param initialResponseHandler Response handler for the initial-response event stream message.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> initialResponseHandler(
            HttpResponseHandler<? extends ResponseT> initialResponseHandler) {
            this.initialResponseHandler = initialResponseHandler;
            return this;
        }

        /**
         * @param eventResponseHandler Response handler for the various event types.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> eventResponseHandler(HttpResponseHandler<? extends EventT> eventResponseHandler) {
            this.eventResponseHandler = eventResponseHandler;
            return this;
        }

        /**
         * @param exceptionResponseHandler Response handler for error and exception messages.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> exceptionResponseHandler(
            HttpResponseHandler<? extends Throwable> exceptionResponseHandler) {
            this.exceptionResponseHandler = exceptionResponseHandler;
            return this;
        }

        /**
         * @param executor Executor used to deliver events.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * @param future Future to notify when the last event has been delivered.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> future(CompletableFuture<Void> future) {
            this.future = future;
            return this;
        }

        /**
         * @param serviceName Descriptive name for the service to be used in exception unmarshalling.
         * @return This object for method chaining.
         */
        public Builder<ResponseT, EventT> serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public EventStreamAsyncResponseTransformer<ResponseT, EventT> build() {
            return new EventStreamAsyncResponseTransformer<>(eventStreamResponseHandler,
                                                             initialResponseHandler,
                                                             eventResponseHandler,
                                                             exceptionResponseHandler,
                                                             executor,
                                                             future,
                                                             serviceName);
        }
    }

}
