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

package software.amazon.awssdk.services.s3.internal.s3crt;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.Logger;

/**
 * Publisher of the response data from crt. Tracks outstanding demand and delivers the data to the subscriber
 */
@SdkInternalApi
public final class S3CrtDataPublisher implements SdkPublisher<ByteBuffer> {
    private static final Logger log = Logger.loggerFor(S3CrtDataPublisher.class);
    private static final Event COMPLETE = new CompleteEvent();
    private static final Event CANCEL = new CancelEvent();
    /**
     * Flag to indicate we are currently delivering events to the subscriber.
     */
    private final AtomicBoolean isDelivering = new AtomicBoolean(false);
    private final Queue<Event> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicLong outstandingDemand = new AtomicLong(0);
    private final AtomicReference<Subscriber<? super ByteBuffer>> subscriberRef = new AtomicReference<>(null);

    private volatile boolean isDone;

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        if (subscriberRef.compareAndSet(null, subscriber)) {
            subscriber.onSubscribe(new DataSubscription());

            // Per Reactive-Streams spec 104, if a Publisher fails it MUST signal an onError.
            notifyErrorIfNeeded(subscriber);
        }  else {
            log.error(() -> "DataPublisher can only be subscribed to once.");
            throw new IllegalStateException("DataPublisher may only be subscribed to once");
        }
    }

    public void notifyStreamingFinished() {
        // If the subscription is cancelled, no op
        if (isDone) {
            return;
        }

        buffer.add(COMPLETE);
        flushBuffer();
    }

    public void notifyError(Exception exception) {
        // If the subscription is cancelled, no op
        if (isDone) {
            return;
        }

        isDone = true;
        buffer.clear();
        buffer.add(new ErrorEvent(exception));
        flushBuffer();
    }

    public void deliverData(ByteBuffer byteBuffer) {
        // If the subscription is cancelled, no op
        if (isDone) {
            return;
        }
        buffer.add(new DataEvent(byteBuffer));
        flushBuffer();
    }

    private void notifyErrorIfNeeded(Subscriber<? super ByteBuffer> subscriber) {
        Event event = buffer.peek();
        if (event != null && event.type().equals(EventType.ERROR)) {
            isDone = true;
            subscriber.onError(((ErrorEvent) event).error());
        }
    }

    private boolean isTerminalEvent(Event event) {
        return event.type().equals(EventType.ERROR) ||
               event.type().equals(EventType.COMPLETE) ||
               event.type().equals(EventType.CANCEL);
    }

    private void handleTerminalEvent(Event event) {
        switch (event.type()) {
            case COMPLETE:
                isDone = true;
                subscriberRef.get().onComplete();
                break;
            case ERROR:
                ErrorEvent errorEvent = (ErrorEvent) event;
                subscriberRef.get().onError(errorEvent.error());
                break;
            case CANCEL:
                subscriberRef.set(null);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.type());
        }
    }

    private void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }
        // if it's already draining, no op
        if (subscriberRef.get() != null && isDelivering.compareAndSet(false, true)) {

            // If it's a terminal event, then we don't to check if there's an outstandingDemand
            Event firstEvent = buffer.peek();
            if (firstEvent != null && isTerminalEvent(firstEvent)) {
                buffer.poll();
                handleTerminalEvent(firstEvent);
                isDelivering.set(false);
                return;
            }

            while (!buffer.isEmpty() && outstandingDemand.get() > 0) {
                log.debug(() -> "Publishing data, buffer size: " + buffer.size() + ", demand: " + outstandingDemand.get());
                Event event = buffer.poll();
                // It's possible that the buffer gets cleared in notifyError() or cancel() and the subscriber
                // gets cleared in cancel()
                if (event == null || subscriberRef.get() == null) {
                    break;
                }

                if (isTerminalEvent(event)) {
                    handleTerminalEvent(event);
                    isDelivering.set(false);
                    return;
                }

                DataEvent dataEvent = (DataEvent) event;
                outstandingDemand.decrementAndGet();
                subscriberRef.get().onNext(dataEvent.data());
            }
            isDelivering.set(false);
        }
    }

    private final class DataSubscription implements Subscription {

        @Override
        public void request(long n) {
            if (isDone) {
                return;
            }

            if (n <= 0) {
                subscriberRef.get().onError(new IllegalArgumentException("Request is for <= 0 elements: " + n));
                return;
            }

            addDemand(n);
            log.debug(() -> "Received demand: " + n + ". Total demands: " + outstandingDemand.get());
            flushBuffer();
        }

        @Override
        public void cancel() {
            if (isDone) {
                return;
            }

            log.debug(() -> "The subscription is cancelled");
            isDone = true;
            buffer.clear();
            buffer.add(CANCEL);
            flushBuffer();
        }

        private void addDemand(long n) {

            outstandingDemand.getAndUpdate(initialDemand -> {
                if (Long.MAX_VALUE - initialDemand < n) {
                    return Long.MAX_VALUE;
                } else {
                    return initialDemand + n;
                }
            });
        }
    }

    private enum EventType {
        DATA,
        COMPLETE,
        ERROR,
        CANCEL
    }

    private interface Event {
        EventType type();
    }

    private static final class DataEvent implements Event {
        private final ByteBuffer data;

        DataEvent(ByteBuffer data) {
            this.data = data;
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

        @Override
        public EventType type() {
            return EventType.COMPLETE;
        }
    }

    private static final class CancelEvent implements Event {

        @Override
        public EventType type() {
            return EventType.CANCEL;
        }
    }

    private static class ErrorEvent implements Event {
        private final Throwable error;

        ErrorEvent(Throwable error) {
            this.error = error;
        }

        @Override
        public EventType type() {
            return EventType.ERROR;
        }

        public final Throwable error() {
            return error;
        }
    }
}

