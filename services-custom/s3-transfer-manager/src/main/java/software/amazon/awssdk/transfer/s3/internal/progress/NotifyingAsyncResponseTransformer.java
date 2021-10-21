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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.SdkPublisher;

@SdkInternalApi
public class NotifyingAsyncResponseTransformer<ResponseT, ResultT> implements AsyncResponseTransformer<ResponseT, ResultT> {

    public interface AsyncResponseTransformerListener<ResponseT, ResultT> {
        default void beforeOnResponse(ResponseT response) {
        }

        default void beforeSubscribe(Subscriber<? super ByteBuffer> subscriber) {
        }

        default void beforeOnNext(ByteBuffer byteBuffer) {
        }
    }

    private final AsyncResponseTransformer<ResponseT, ResultT> delegate;
    private final AsyncResponseTransformerListener<ResponseT, ResultT> listener;

    public NotifyingAsyncResponseTransformer(AsyncResponseTransformer<ResponseT, ResultT> delegate,
                                             AsyncResponseTransformerListener<ResponseT, ResultT> listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public CompletableFuture<ResultT> prepare() {
        return delegate.prepare();
    }

    @Override
    public void onResponse(ResponseT response) {
        listener.beforeOnResponse(response);
        delegate.onResponse(response);
    }

    public void onStream(SdkPublisher<ByteBuffer> publisher) {
        delegate.onStream(new NotifyingPublisher<>(publisher, listener));
    }

    @Override
    public void exceptionOccurred(Throwable error) {
        delegate.exceptionOccurred(error);
    }

    @SdkInternalApi
    private static final class NotifyingPublisher<ResponseT, ResultT> implements SdkPublisher<ByteBuffer> {
        private final SdkPublisher<ByteBuffer> delegate;
        private final AsyncResponseTransformerListener<ResponseT, ResultT> listener;

        NotifyingPublisher(SdkPublisher<ByteBuffer> delegate,
                           AsyncResponseTransformerListener<ResponseT, ResultT> listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            listener.beforeSubscribe(s);
            delegate.subscribe(new NotifyingSubscriber<>(s, listener));
        }
    }

    @SdkInternalApi
    private static final class NotifyingSubscriber<ResponseT, ResultT> implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> delegate;
        private final AsyncResponseTransformerListener<ResponseT, ResultT> listener;

        NotifyingSubscriber(Subscriber<? super ByteBuffer> delegate,
                            AsyncResponseTransformerListener<ResponseT, ResultT> listener) {
            this.delegate = delegate;
            this.listener = listener;
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            listener.beforeOnNext(byteBuffer);
            delegate.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }
}
