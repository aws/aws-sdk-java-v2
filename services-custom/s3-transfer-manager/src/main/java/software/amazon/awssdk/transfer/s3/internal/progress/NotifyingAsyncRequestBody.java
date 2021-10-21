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
import java.util.Optional;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.AsyncRequestBody;

@SdkInternalApi
public class NotifyingAsyncRequestBody implements AsyncRequestBody {

    public interface AsyncRequestBodyListener {
        default void beforeSubscribe(Subscriber<? super ByteBuffer> subscriber) {
        }

        default void beforeOnNext(ByteBuffer byteBuffer) {
        }
    }

    private final AsyncRequestBody delegate;
    private final AsyncRequestBodyListener listener;

    public NotifyingAsyncRequestBody(AsyncRequestBody delegate,
                                     AsyncRequestBodyListener listener) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public Optional<Long> contentLength() {
        return delegate.contentLength();
    }

    @Override
    public String contentType() {
        return delegate.contentType();
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        listener.beforeSubscribe(subscriber);
        delegate.subscribe(new NotifyingSubscriber(subscriber, listener));
    }

    @SdkInternalApi
    private static final class NotifyingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> delegate;
        private final AsyncRequestBodyListener listener;

        NotifyingSubscriber(Subscriber<? super ByteBuffer> delegate,
                            AsyncRequestBodyListener listener) {
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
