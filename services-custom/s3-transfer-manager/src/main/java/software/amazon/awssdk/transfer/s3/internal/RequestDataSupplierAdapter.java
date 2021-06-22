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

package software.amazon.awssdk.transfer.s3.internal;

import com.amazonaws.s3.RequestDataSupplier;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;

/**
 * Adapts an SDK {@link software.amazon.awssdk.core.async.AsyncRequestBody} to CRT's {@link RequestDataSupplier}.
 */
@SdkInternalApi
public final class RequestDataSupplierAdapter implements RequestDataSupplier {
    static final long DEFAULT_REQUEST_SIZE = 8;
    private static final Logger LOG = Logger.loggerFor(RequestDataSupplierAdapter.class);

    private final AtomicReference<SubscriptionStatus> subscriptionStatus =
        new AtomicReference<>(SubscriptionStatus.NOT_SUBSCRIBED);
    private final BlockingQueue<Subscription> subscriptionQueue = new LinkedBlockingQueue<>(1);
    private final BlockingDeque<Event> eventBuffer = new LinkedBlockingDeque<>();

    private final Publisher<ByteBuffer> bodyPublisher;

    // Not volatile, we synchronize on the subscriptionQueue
    private Subscription subscription;

    // TODO: not volatile since it's read and written only by CRT thread(s). Need to
    // ensure that CRT actually ensures consistency across their threads...
    private Subscriber<? super ByteBuffer> subscriber;
    private long pending = 0;
    private final ResponseHeadersHandler headersHandler;

    public RequestDataSupplierAdapter(Publisher<ByteBuffer> bodyPublisher) {
        this.bodyPublisher = bodyPublisher;
        this.subscriber = createSubscriber();
        this.headersHandler = new ResponseHeadersHandler();
    }

    public CompletableFuture<SdkHttpResponse> sdkHttpResponseFuture() {
        return headersHandler.sdkHttpResponseFuture();
    }

    @Override
    public void onResponseHeaders(final int statusCode, final HttpHeader[] headers) {
        headersHandler.onResponseHeaders(statusCode, headers);
    }

    @Override
    public boolean getRequestBytes(ByteBuffer outBuffer) {
        LOG.trace(() -> "Getting data to fill buffer of size " + outBuffer.remaining());

        // Per the spec, onSubscribe is always called before any other
        // signal, so we expect a subscription to always be provided; we just
        // wait for that to happen
        waitForSubscription();

        // The "event loop". Per the spec, the sequence of events is "onSubscribe onNext* (onError | onComplete)?".
        // We don't handle onSubscribe as a discrete event; instead we only enter this loop once we have a
        // subscription.
        //
        // This works by requesting and consuming DATA events until we fill the buffer. We return from the method if
        // we encounter either of the terminal events, COMPLETE or ERROR.
        while (true) {
            // The supplier API requires that we fill the buffer entirely.
            if (!outBuffer.hasRemaining()) {
                break;
            }

            if (eventBuffer.isEmpty() && pending == 0) {
                pending = DEFAULT_REQUEST_SIZE;
                subscription.request(pending);
            }

            Event ev = takeFirstEvent();

            // Discard the event if it's not for the current subscriber
            if (!ev.subscriber().equals(subscriber)) {
                LOG.debug(() -> "Received an event for a previous publisher. Discarding. Event was: " + ev);
                continue;
            }

            switch (ev.type()) {
                case DATA:
                    ByteBuffer srcBuffer = ((DataEvent) ev).data();

                    ByteBuffer bufferToWrite = srcBuffer.duplicate();
                    int nBytesToWrite = Math.min(outBuffer.remaining(), srcBuffer.remaining());

                    // src is larger, create a resized view to prevent
                    // buffer overflow in the subsequent put() call
                    if (bufferToWrite.remaining() > nBytesToWrite) {
                        bufferToWrite.limit(bufferToWrite.position() + nBytesToWrite);
                    }

                    outBuffer.put(bufferToWrite);
                    srcBuffer.position(bufferToWrite.limit());

                    if (!srcBuffer.hasRemaining()) {
                        --pending;
                    } else {
                        eventBuffer.push(ev);
                    }

                    break;

                case COMPLETE:
                    // Leave this event in the queue so that if getRequestData
                    // gets call after the stream is already done, we pop it off again.
                    eventBuffer.push(ev);
                    pending = 0;
                    return true;

                case ERROR:
                    // Leave this event in the queue so that if getRequestData
                    // gets call after the stream is already done, we pop it off again.
                    eventBuffer.push(ev);
                    Throwable t = ((ErrorEvent) ev).error();
                    if (t instanceof RuntimeException) {
                        throw (RuntimeException) t;
                    }
                    throw new RuntimeException(t);

                default:
                    // In case new event types are introduced that this loop doesn't account for
                    throw new IllegalStateException("Unknown event type: " + ev.type());
            }
        }

        return false;
    }

    @Override
    public boolean resetPosition() {
        subscription.cancel();
        subscription = null;

        this.subscriber = createSubscriber();
        subscriptionStatus.set(SubscriptionStatus.NOT_SUBSCRIBED);

        // NOTE: It's possible that even after this happens, eventBuffer gets
        // residual events from the canceled subscription if the publisher
        // handles cancel asynchronously. That doesn't affect us too much since
        // we always ensure the event is for the current subscriber.
        eventBuffer.clear();
        pending = 0;

        return true;
    }

    private Event takeFirstEvent() {
        try {
            return eventBuffer.takeFirst();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for next event", e);
        }
    }

    public SubscriberImpl createSubscriber() {
        return new SubscriberImpl(this::setSubscription, eventBuffer);
    }

    private void setSubscription(Subscription subscription) {
        if (subscriptionStatus.compareAndSet(SubscriptionStatus.SUBSCRIBING, SubscriptionStatus.SUBSCRIBED)) {
            subscriptionQueue.add(subscription);
        } else {
            LOG.error(() -> "The supplier stopped waiting for the subscription. This is likely because it took " +
                    "longer than the timeout to arrive. Cancelling the subscription");
            subscription.cancel();
        }
    }

    static class SubscriberImpl implements Subscriber<ByteBuffer> {
        private final Consumer<Subscription> subscriptionSetter;
        private final Deque<Event> eventBuffer;
        private boolean subscribed = false;

        SubscriberImpl(Consumer<Subscription> subscriptionSetter, Deque<Event> eventBuffer) {
            this.subscriptionSetter = subscriptionSetter;
            this.eventBuffer = eventBuffer;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (subscription == null) {
                throw new NullPointerException("Subscription must not be null");
            }

            if (subscribed) {
                subscription.cancel();
                return;
            }

            subscriptionSetter.accept(subscription);
            subscribed = true;
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            if (byteBuffer == null) {
                throw new NullPointerException("byteBuffer must not be null");
            }
            LOG.trace(() -> "Received new data of size: " + byteBuffer.remaining());
            eventBuffer.add(new DataEvent(this, byteBuffer));
        }

        @Override
        public void onError(Throwable throwable) {
            eventBuffer.add(new ErrorEvent(this, throwable));
        }

        @Override
        public void onComplete() {
            eventBuffer.add(new CompleteEvent(this));
        }
    }

    private void waitForSubscription() {
        if (!subscriptionStatus.compareAndSet(SubscriptionStatus.NOT_SUBSCRIBED, SubscriptionStatus.SUBSCRIBING)) {
            return;
        }

        bodyPublisher.subscribe(this.subscriber);

        try {
            this.subscription = subscriptionQueue.poll(5, TimeUnit.SECONDS);
            if (subscription == null) {
                if (!subscriptionStatus.compareAndSet(SubscriptionStatus.SUBSCRIBING, SubscriptionStatus.TIMED_OUT)) {
                    subscriptionQueue.take().cancel();
                }

                throw new RuntimeException("Publisher did not respond with a subscription within 5 seconds");
            }
        } catch (InterruptedException e) {
            LOG.error(() -> "Interrupted while waiting for subscription", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for subscription", e);
        }
    }

    private enum EventType {
        DATA,
        COMPLETE,
        ERROR
    }

    interface Event {
        Subscriber<? super ByteBuffer> subscriber();

        EventType type();
    }

    private static final class DataEvent implements Event {
        private final Subscriber<? super ByteBuffer> subscriber;
        private final ByteBuffer data;

        DataEvent(Subscriber<? super ByteBuffer> subscriber, ByteBuffer data) {
            this.subscriber = subscriber;
            this.data = data;
        }

        @Override
        public Subscriber<? super ByteBuffer> subscriber() {
            return subscriber;
        }

        @Override
        public EventType type() {
            return EventType.DATA;
        }

        public ByteBuffer data() {
            return data;
        }
    }

    private static final class CompleteEvent implements Event {
        private final Subscriber<? super ByteBuffer> subscriber;

        CompleteEvent(Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public Subscriber<? super ByteBuffer> subscriber() {
            return subscriber;
        }

        @Override
        public EventType type() {
            return EventType.COMPLETE;
        }
    }

    private static final class ErrorEvent implements Event {
        private final Subscriber<? super ByteBuffer> subscriber;
        private final Throwable error;

        ErrorEvent(Subscriber<? super ByteBuffer> subscriber, Throwable error) {
            this.subscriber = subscriber;
            this.error = error;
        }

        @Override
        public Subscriber<? super ByteBuffer> subscriber() {
            return subscriber;
        }

        @Override
        public EventType type() {
            return EventType.ERROR;
        }

        public Throwable error() {
            return error;
        }
    }

    private enum SubscriptionStatus {
        NOT_SUBSCRIBED,
        SUBSCRIBING,
        SUBSCRIBED,
        TIMED_OUT
    }
}
