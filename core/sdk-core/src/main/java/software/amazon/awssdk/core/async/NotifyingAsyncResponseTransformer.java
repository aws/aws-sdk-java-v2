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

package software.amazon.awssdk.core.async;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Pair;

// TODO: Resolve potential duplicates w/ EventListeningSubscriber
// TODO: Consider duplicating as internal API for the relevant modules until API is finalized
@SdkProtectedApi
public class NotifyingAsyncResponseTransformer<ResponseT, ResultT> implements AsyncResponseTransformer<ResponseT, ResultT> {

    public interface AsyncResponseTransformerListener<ResponseT> extends PublisherListener {
        /**
         * Invoked before {@link AsyncResponseTransformer#onResponse(Object)}
         */
        default void transformerOnResponse(ResponseT response) {
        }

        /**
         * Invoked before {@link AsyncResponseTransformer#onStream(SdkPublisher)}
         */
        default void transformerOnStream(SdkPublisher<ByteBuffer> publisher) {
        }

        /**
         * Invoked before {@link AsyncResponseTransformer#exceptionOccurred(Throwable)}
         */
        default void transformerExceptionOccurred(Throwable t) {
        }
    }

    public interface PublisherListener extends SubscriberListener {
        /**
         * Invoked before {@link Publisher#subscribe(Subscriber)}
         */
        default void publisherSubscribe(Subscriber<? super ByteBuffer> subscriber) {
        }
    }

    public interface SubscriberListener {
        /**
         * Invoked before {@link Subscriber#onNext(Object)}
         */
        default void subscriberOnNext(ByteBuffer byteBuffer) {
        }

        /**
         * Invoked before {@link Subscriber#onComplete()}
         */
        default void subscriberOnComplete() {
        }

        /**
         * Invoked before {@link Subscriber#onError(Throwable)}
         */
        default void subscriberOnError(Throwable t) {
        }
    }

    private final AsyncResponseTransformer<ResponseT, ResultT> delegate;
    private final AsyncResponseTransformerListener<ResponseT> listener;

    private NotifyingAsyncResponseTransformer(AsyncResponseTransformer<ResponseT, ResultT> delegate,
                                              AsyncResponseTransformerListener<ResponseT> listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    public static <ResponseT, ResultT> AsyncResponseTransformer<ResponseT, ResultT> wrap(
        AsyncResponseTransformer<ResponseT, ResultT> delegate,
        AsyncResponseTransformerListener<ResponseT> listener) {
        return new NotifyingAsyncResponseTransformer<>(delegate, listener);
    }

    @Override
    public CompletableFuture<ResultT> prepare() {
        return delegate.prepare();
    }

    @Override
    public void onResponse(ResponseT response) {
        listener.transformerOnResponse(response);
        delegate.onResponse(response);
    }

    @Override
    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        listener.transformerOnStream(publisher);
        delegate.onStream(new NotifyingPublisher(publisher, listener));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        listener.transformerExceptionOccurred(error);
        delegate.exceptionOccurred(error);
    }

    @SdkInternalApi
    private static final class NotifyingPublisher implements SdkPublisher<ByteBuffer> {
        private final SdkPublisher<ByteBuffer> delegate;
        private final PublisherListener listener;

        NotifyingPublisher(SdkPublisher<ByteBuffer> delegate,
                           PublisherListener listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            listener.publisherSubscribe(s);
            delegate.subscribe(new NotifyingSubscriber(s, listener));
        }
    }

    @SdkInternalApi
    private static final class NotifyingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> delegate;
        private final SubscriberListener listener;

        NotifyingSubscriber(Subscriber<? super ByteBuffer> delegate,
                            SubscriberListener listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            listener.subscriberOnNext(byteBuffer);
            delegate.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            listener.subscriberOnError(t);
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            listener.subscriberOnComplete();
            delegate.onComplete();
        }
    }

    /**
     * Returns a future that is completed upon end-of-stream, regardless of whether the transformer is configured to complete its
     * future upon end-of-response or end-of-stream.
     */
    public static <A, B> Pair<AsyncResponseTransformer<A, B>, CompletableFuture<Void>> wrapWithEndOfStreamFuture(
        AsyncResponseTransformer<A, B> responseTransformer) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AsyncResponseTransformer<A, B> wrapped = wrap(
            responseTransformer,
            new AsyncResponseTransformerListener<A>() {
                @Override
                public void transformerExceptionOccurred(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void subscriberOnError(Throwable t) {
                    future.completeExceptionally(t);
                }

                @Override
                public void subscriberOnComplete() {
                    future.complete(null);
                }
            });
        return Pair.of(wrapped, future);
    }
}
