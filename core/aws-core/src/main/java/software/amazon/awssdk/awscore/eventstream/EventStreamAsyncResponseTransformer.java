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

package software.amazon.awssdk.awscore.eventstream;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;
import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.internal.util.ThrowableUtils;
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
     * Unmarshalls exception events.
     */
    private final HttpResponseHandler<? extends Throwable> exceptionUnmarshaller;
    private final CompletableFuture<Void> future;

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

    private final String serviceName;

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
     * Request Id for the streaming request. The value is populated when the initial response is received from the service.
     * As request id is not sent in event messages (including exceptions), this can be returned by the SDK along with
     * received exception details.
     */
    private String requestId = null;

    /**
     * Extended Request Id for the streaming request. The value is populated when the initial response is received from the
     * service. As request id is not sent in event messages (including exceptions), this can be returned by the SDK along with
     * received exception details.
     */
    private String extendedRequestId = null;

    /**
     * @param eventStreamResponseTransformer Response transformer provided by customer.
     * @param initialResponseUnmarshaller Unmarshaller for the initial-response event stream message.
     * @param eventUnmarshaller Unmarshaller for the various event types.
     */
    public EventStreamAsyncResponseTransformer(
        EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseTransformer,
        HttpResponseHandler<? extends ResponseT> initialResponseUnmarshaller,
        HttpResponseHandler<? extends EventT> eventUnmarshaller,
        HttpResponseHandler<? extends Throwable> exceptionUnmarshaller) {
        this(eventStreamResponseTransformer, initialResponseUnmarshaller, eventUnmarshaller, exceptionUnmarshaller,
              new CompletableFuture<>(), "");
    }

    private EventStreamAsyncResponseTransformer(
        EventStreamResponseHandler<ResponseT, EventT> eventStreamResponseHandler,
        HttpResponseHandler<? extends ResponseT> initialResponseHandler,
        HttpResponseHandler<? extends EventT> eventResponseHandler,
        HttpResponseHandler<? extends Throwable> exceptionResponseHandler,
        CompletableFuture<Void> future,
        String serviceName) {

        this.eventStreamResponseTransformer = eventStreamResponseHandler;
        this.initialResponseUnmarshaller = initialResponseHandler;
        this.eventUnmarshaller = eventResponseHandler;
        this.exceptionUnmarshaller = exceptionResponseHandler;
        this.future = future;
        this.serviceName = serviceName;
    }

    @Override
    public void responseReceived(SdkResponse response) {
        // We use a void unmarshaller and unmarshall the actual response in the message
        // decoder when we receive the initial-response frame. TODO not clear
        // how we would handle REST protocol which would unmarshall the response from the HTTP headers
        if (response != null && response.sdkHttpResponse() != null) {
            this.requestId = response.sdkHttpResponse()
                                     .firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                     .orElse(null);
            this.extendedRequestId = response.sdkHttpResponse()
                                             .firstMatchingHeader(X_AMZ_ID_2_HEADER)
                                             .orElse(null);
        }
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
            try {
                eventStreamResponseTransformer.onEventStream(eventPublisher);
            } catch (Throwable t) {
                dataSubscription.cancel();
                exceptionOccurred(t);
            }
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
                future.complete(null);
                eventStreamResponseTransformer.complete();
                return null;
            } else {
                // Need to propagate the failure up so the future is completed exceptionally. This should only happen
                // when there is a frame level exception that the upper layers don't know about.
                throw ThrowableUtils.failure(error.get());
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
                // TODO: Can we move all of the dispatching to a single unmarshaller?
                if (isEvent(m)) {
                    if (m.getHeaders().get(":event-type").getString().equals("initial-response")) {
                        eventStreamResponseTransformer.responseReceived(
                            initialResponseUnmarshaller.handle(adaptMessageToResponse(m, false),
                                                               EMPTY_EXECUTION_ATTRIBUTES));
                    } else {
                        remainingDemand.decrementAndGet();
                        subscriberRef.get().onNext(eventUnmarshaller.handle(adaptMessageToResponse(m, false),
                                                                            EMPTY_EXECUTION_ATTRIBUTES));
                    }
                } else if (isError(m) || isException(m)) {
                    HttpResponse errorResponse = adaptMessageToResponse(m, true);
                    Throwable exception = exceptionUnmarshaller.handle(
                        errorResponse, new ExecutionAttributes().putAttribute(SdkExecutionAttribute.SERVICE_NAME, serviceName));
                    runAndLogError(log, "Error thrown from exceptionOccurred, ignoring.", () -> exceptionOccurred(exception));
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
     * @param m Message frame.
     * @return True if frame is an exception frame, false if not.
     */
    private boolean isException(Message m) {
        return "exception".equals(m.getHeaders().get(":message-type").getString());
    }

    /**
     * Transforms an event stream message into a {@link HttpResponse} so we can reuse our existing generated unmarshallers.
     *
     * @param m Message to transform.
     */
    private HttpResponse adaptMessageToResponse(Message m, boolean isException) {
        HttpResponse response = new HttpResponse(null);
        response.setContent(new ByteArrayInputStream(m.getPayload()));
        m.getHeaders().forEach((k, v) -> response.addHeader(k, v.getString()));

        if (requestId != null) {
            response.addHeader(X_AMZN_REQUEST_ID_HEADER, requestId);
        }
        if (extendedRequestId != null) {
            response.addHeader(X_AMZ_ID_2_HEADER, extendedRequestId);
        }

        //TODO: fix the hard-coded status code
        response.setStatusCode(isException ? 500 : 200);

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
                                                             future,
                                                             serviceName);
        }
    }
}

