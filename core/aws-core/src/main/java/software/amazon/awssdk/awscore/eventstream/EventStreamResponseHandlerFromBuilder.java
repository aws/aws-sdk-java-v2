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

    private final Consumer<ResponseT> responseConsumer;
    private final Consumer<Throwable> errorConsumer;
    private final Runnable onComplete;
    private final Consumer<SdkPublisher<EventT>> onEventStream;
    private final Function<SdkPublisher<EventT>, SdkPublisher<EventT>> publisherTransformer;

    protected EventStreamResponseHandlerFromBuilder(DefaultEventStreamResponseHandlerBuilder<ResponseT, EventT, ?> builder) {
        mutuallyExclusive("onEventStream and subscriber are mutually exclusive, set only one on the Builder",
                          builder.onEventStream(), builder.subscriber());
        Supplier<Subscriber<EventT>> subscriber = builder.subscriber();
        this.onEventStream = subscriber != null ? p -> p.subscribe(subscriber.get()) : builder.onEventStream();
        if (this.onEventStream == null) {
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
    public void onEventStream(SdkPublisher<EventT> p) {
        onEventStream.accept(publisherTransformer.apply(p));
    }

    @Override
    public void exceptionOccurred(Throwable throwable) {
        errorConsumer.accept(throwable);
    }

    @Override
    public void complete() {
        this.onComplete.run();
    }
}
