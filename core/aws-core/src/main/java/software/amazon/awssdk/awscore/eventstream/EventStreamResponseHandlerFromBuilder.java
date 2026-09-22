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

import static software.amazon.awssdk.utils.Validate.getOrDefault;
import static software.amazon.awssdk.utils.Validate.mutuallyExclusive;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.FunctionalUtils;

/**
 * Base class for creating implementations of an {@link EventStreamResponseHandler} from a builder.
 * See {@link EventStreamResponseHandler.Builder}.
 *
 * @param <ResponseT> Type of initial response object.
 * @param <EventT> Type of event being published.
 */
@SdkProtectedApi
public abstract class EventStreamResponseHandlerFromBuilder<ResponseT, EventT>
    implements EventStreamResponseHandler<ResponseT, EventT> {

    /**
     * Preserves the failure reporter when a synchronous override wraps the invocation publisher before delegating.
     */
    private static final ThreadLocal<Consumer<Throwable>> SCOPED_CALLBACK_FAILURE_REPORTER = new ThreadLocal<>();

    private final Consumer<ResponseT> responseConsumer;
    private final Consumer<Throwable> errorConsumer;
    private final Runnable onComplete;
    private final Supplier<Subscriber<EventT>> subscriber;
    private final Consumer<EventT> eventConsumer;
    private final Consumer<SdkPublisher<EventT>> onEventStream;
    private final Function<SdkPublisher<EventT>, SdkPublisher<EventT>> publisherTransformer;

    protected EventStreamResponseHandlerFromBuilder(DefaultEventStreamResponseHandlerBuilder<ResponseT, EventT, ?> builder) {
        mutuallyExclusive("onEventStream and subscriber are mutually exclusive, set only one on the Builder",
                          builder.onEventStream(), builder.subscriber(), builder.eventConsumer());
        this.subscriber = builder.subscriber();
        this.eventConsumer = builder.eventConsumer();
        this.onEventStream = builder.onEventStream();
        if (this.subscriber == null && this.eventConsumer == null && this.onEventStream == null) {
            throw new IllegalArgumentException("Must provide either a subscriber or set onEventStream "
                                               + "and subscribe to the publisher in the callback method");
        }
        this.responseConsumer = getOrDefault(builder.onResponse(), FunctionalUtils::noOpConsumer);
        this.errorConsumer = getOrDefault(builder.onError(), FunctionalUtils::noOpConsumer);
        this.onComplete = getOrDefault(builder.onComplete(), FunctionalUtils::noOpRunnable);
        this.publisherTransformer = getOrDefault(builder.publisherTransformer(), Function::identity);
    }

    @Override
    public void responseReceived(ResponseT response) {
        responseConsumer.accept(response);
    }

    @Override
    public void onEventStream(SdkPublisher<EventT> publisher) {
        CallbackFailureContextPublisher<EventT> context = CallbackFailureContextPublisher.from(publisher);
        SdkPublisher<EventT> transformedPublisher = publisherTransformer.apply(context.publisher());

        if (eventConsumer != null) {
            transformedPublisher.subscribe(sequentialConsumerSubscriber(eventConsumer, context.failureReporter()));
        } else if (subscriber != null) {
            transformedPublisher.subscribe(subscriber.get());
        } else {
            onEventStream.accept(transformedPublisher);
        }
    }

    static <ResponseT, EventT> void invokeOnEventStream(EventStreamResponseHandler<ResponseT, EventT> responseHandler,
                                                        SdkPublisher<EventT> publisher,
                                                        Consumer<Throwable> failureReporter) {
        Consumer<Throwable> previousReporter = SCOPED_CALLBACK_FAILURE_REPORTER.get();
        SCOPED_CALLBACK_FAILURE_REPORTER.set(failureReporter);
        try {
            responseHandler.onEventStream(new CallbackFailureContextPublisher<>(publisher, failureReporter));
        } finally {
            if (previousReporter == null) {
                SCOPED_CALLBACK_FAILURE_REPORTER.remove();
            } else {
                SCOPED_CALLBACK_FAILURE_REPORTER.set(previousReporter);
            }
        }
    }

    // Keeps the subscriber implementation private while allowing package-level Reactive Streams verification.
    static <T> Subscriber<T> sequentialConsumerSubscriber(Consumer<T> eventConsumer,
                                                           Consumer<Throwable> failureReporter) {
        return new SequentialConsumerSubscriber<>(eventConsumer, failureReporter);
    }

    /**
     * Carries a callback failure reporter through virtual dispatch and resolves the scoped fallback for plain publishers.
     */
    private static final class CallbackFailureContextPublisher<T> implements SdkPublisher<T> {
        private final SdkPublisher<T> publisher;
        private final Consumer<Throwable> carriedFailureReporter;

        private CallbackFailureContextPublisher(SdkPublisher<T> publisher,
                                                Consumer<Throwable> carriedFailureReporter) {
            this.publisher = publisher;
            this.carriedFailureReporter = carriedFailureReporter;
        }

        private static <T> CallbackFailureContextPublisher<T> from(SdkPublisher<T> publisher) {
            if (publisher instanceof CallbackFailureContextPublisher) {
                return (CallbackFailureContextPublisher<T>) publisher;
            }
            return new CallbackFailureContextPublisher<>(publisher, null);
        }

        private SdkPublisher<T> publisher() {
            return publisher;
        }

        private Consumer<Throwable> failureReporter() {
            return carriedFailureReporter != null
                   ? carriedFailureReporter
                   : getOrDefault(SCOPED_CALLBACK_FAILURE_REPORTER.get(), FunctionalUtils::noOpConsumer);
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            publisher.subscribe(subscriber);
        }
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        errorConsumer.accept(throwable);
    }

    @Override
    public void complete() {
        this.onComplete.run();
    }

    /**
     * Reports consumer failures before cancelling upstream so cancellation cannot mask the failure.
     */
    private static final class SequentialConsumerSubscriber<T> implements Subscriber<T> {
        private final Consumer<T> eventConsumer;
        private final Consumer<Throwable> failureReporter;
        private Subscription subscription;
        private boolean terminated;

        private SequentialConsumerSubscriber(Consumer<T> eventConsumer, Consumer<Throwable> failureReporter) {
            this.eventConsumer = eventConsumer;
            this.failureReporter = failureReporter;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            if (subscription == null) {
                throw new NullPointerException("subscription must not be null.");
            }
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }

            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(T event) {
            if (event == null) {
                NullPointerException exception = new NullPointerException("onNext(null) is not allowed.");
                fail(exception);
                throw exception;
            }
            if (terminated) {
                return;
            }

            try {
                eventConsumer.accept(event);
                subscription.request(1);
            } catch (Throwable throwable) {
                fail(throwable);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (throwable == null) {
                throw new NullPointerException("throwable must not be null.");
            }
            terminated = true;
        }

        @Override
        public void onComplete() {
            terminated = true;
        }

        private void fail(Throwable throwable) {
            terminated = true;
            try {
                failureReporter.accept(throwable);
            } finally {
                subscription.cancel();
            }
        }
    }
}
