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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.SdkPublisher;

public interface EventStreamResponseHandler<ResponseT, EventT> {

    /**
     * Called when the initial response has been received and the POJO response has
     * been unmarshalled. This is guaranteed to be called before {@link #onEventStream(SdkPublisher)}.
     *
     * <p>In the event of a retryable error, this callback may be called multiple times. It
     * also may never be invoked if the request never succeeds.</p>
     *
     * @param response Unmarshalled POJO containing metadata about the streamed data.
     */
    void responseReceived(ResponseT response);

    /**
     * Called when events are ready to be streamed. Implementations  must subscribe to the {@link Publisher} and request data via
     * a {@link org.reactivestreams.Subscription} as they can handle it.
     *
     * <p>
     * If at any time the subscriber wishes to stop receiving data, it may call {@link Subscription#cancel()}. This
     * will be treated as a failure of the response and the {@link #exceptionOccurred(Throwable)} callback will be invoked.
     * </p>
     *
     * <p>This callback may never be called if the response has no content or if an error occurs.</p>
     *
     * <p>
     * In the event of a retryable error, this callback may be called multiple times with different Publishers.
     * If this method is called more than once, implementation must either reset any state to prepare for another
     * stream of data or must throw an exception indicating they cannot reset. If any exception is thrown then no
     * automatic retry is performed.
     * </p>
     */
    void onEventStream(SdkPublisher<EventT> publisher);

    /**
     * Called when an exception occurs while establishing the connection or streaming the response. Implementations
     * should free up any resources in this method. This method may be called multiple times during the lifecycle
     * of a request if automatic retries are enabled.
     *
     * @param throwable Exception that occurred.
     */
    void exceptionOccurred(Throwable throwable);

    /**
     * Called when all data has been successfully published to the {@link org.reactivestreams.Subscriber}. This will
     * only be called once during the lifecycle of the request. Implementors should free up any resources they have
     * opened.
     */
    void complete();

    /**
     * Base builder for sub-interfaces of {@link EventStreamResponseHandler}.
     */
    interface Builder<ResponseT, EventT, SubBuilderT> {

        /**
         * Callback to invoke when the initial response is received.
         *
         * @param responseConsumer Callback that will process the initial response.
         * @return This builder for method chaining.
         */
        SubBuilderT onResponse(Consumer<ResponseT> responseConsumer);

        /**
         * Callback to invoke in the event on an error.
         *
         * @param consumer Callback that will process any error that occurs.
         * @return This builder for method chaining.
         */
        SubBuilderT onError(Consumer<Throwable> consumer);

        /**
         * Action to invoke when the event stream completes.
         *
         * @param runnable Action to run on the completion of the event stream.
         * @return This builder for method chaining.
         */
        SubBuilderT onComplete(Runnable runnable);

        /**
         * Subscriber that will subscribe to the {@link SdkPublisher} of events. Subscriber
         * must be provided.
         *
         * @param eventSubscriberSupplier Supplier for a subscriber that will be subscribed to the publisher of events.
         * @return This builder for method chaining.
         */
        SubBuilderT subscriber(Supplier<Subscriber<EventT>> eventSubscriberSupplier);

        /**
         * Sets the subscriber to the {@link SdkPublisher} of events. The given consumer will be called for each event received
         * by the publisher. Events are requested sequentially after each event is processed. If you need more control over
         * the backpressure strategy consider using {@link #subscriber(Supplier)} instead.
         *
         * @param eventConsumer Consumer that will process incoming events.
         * @return This builder for method chaining.
         */
        SubBuilderT subscriber(Consumer<EventT> eventConsumer);

        /**
         * Callback to invoke when the {@link SdkPublisher} is available. This callback must subscribe to the given publisher.
         * This method should not be used with {@link #subscriber(Supplier)} or any of it's overloads.
         *
         * @param onSubscribe Callback that will subscribe to the {@link SdkPublisher}.
         * @return This builder for method chaining.
         */
        SubBuilderT onEventStream(Consumer<SdkPublisher<EventT>> onSubscribe);

        /**
         * Allows for optional transformation of the publisher of events before subscribing. This transformation must return
         * a {@link SdkPublisher} of the same type so methods like {@link SdkPublisher#map(Function)} and
         * {@link SdkPublisher#buffer(int)} that change the type cannot be used with this method.
         *
         * @param publisherTransformer Function that returns a new {@link SdkPublisher} with augmented behavior.
         * @return This builder for method chaining.
         */
        SubBuilderT publisherTransformer(Function<SdkPublisher<EventT>, SdkPublisher<EventT>> publisherTransformer);

    }
}
