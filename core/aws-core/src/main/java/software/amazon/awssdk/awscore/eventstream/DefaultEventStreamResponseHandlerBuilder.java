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

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.async.SequentialSubscriber;

/**
 * Base class for event stream response handler builders.
 *
 * @param <ResponseT> Type of POJO response.
 * @param <EventT> Type of event being published.
 * @param <SubBuilderT> Subtype of builder class for method chaining.
 */
@SdkProtectedApi
public abstract class DefaultEventStreamResponseHandlerBuilder<ResponseT, EventT, SubBuilderT>
    implements EventStreamResponseHandler.Builder<ResponseT, EventT, SubBuilderT> {

    private Consumer<ResponseT> onResponse;
    private Consumer<Throwable> onError;
    private Runnable onComplete;
    private Supplier<Subscriber<EventT>> subscriber;
    private Consumer<SdkPublisher<EventT>> onSubscribe;
    private Function<SdkPublisher<EventT>, SdkPublisher<EventT>> publisherTransformer;

    protected DefaultEventStreamResponseHandlerBuilder() {
    }

    @Override
    public SubBuilderT onResponse(Consumer<ResponseT> responseConsumer) {
        this.onResponse = responseConsumer;
        return subclass();
    }

    Consumer<ResponseT> onResponse() {
        return onResponse;
    }

    @Override
    public SubBuilderT onError(Consumer<Throwable> consumer) {
        this.onError = consumer;
        return subclass();
    }

    Consumer<Throwable> onError() {
        return onError;
    }

    @Override
    public SubBuilderT onComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return subclass();
    }

    Runnable onComplete() {
        return onComplete;
    }

    @Override
    public SubBuilderT subscriber(Supplier<Subscriber<EventT>> eventSubscriber) {
        this.subscriber = eventSubscriber;
        return subclass();
    }

    @Override
    public SubBuilderT subscriber(Consumer<EventT> eventConsumer) {
        this.subscriber = () -> new SequentialSubscriber<>(eventConsumer, new CompletableFuture<>());
        return subclass();
    }

    Supplier<Subscriber<EventT>> subscriber() {
        return subscriber;
    }

    @Override
    public SubBuilderT onEventStream(Consumer<SdkPublisher<EventT>> onSubscribe) {
        this.onSubscribe = onSubscribe;
        return subclass();
    }

    Consumer<SdkPublisher<EventT>> onEventStream() {
        return onSubscribe;
    }

    @Override
    public SubBuilderT publisherTransformer(Function<SdkPublisher<EventT>, SdkPublisher<EventT>> publisherTransformer) {
        this.publisherTransformer = publisherTransformer;
        return subclass();
    }

    Function<SdkPublisher<EventT>, SdkPublisher<EventT>> publisherTransformer() {
        return publisherTransformer;
    }

    private SubBuilderT subclass() {
        return (SubBuilderT) this;
    }

}
