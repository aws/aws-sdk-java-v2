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

package software.amazon.awssdk.utils.async;

import java.nio.ByteBuffer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Decorator subscriber that limits the number of bytes sent to the wrapped subscriber to at most {@code contentLength}. Once
 * the given content length is reached, the upstream subscription is cancelled, and the wrapped subscriber is completed.
 */
@SdkProtectedApi
public final class ContentLengthAwareSubscriber implements Subscriber<ByteBuffer> {
    private final Subscriber<? super ByteBuffer> subscriber;
    private Subscription subscription;
    private boolean subscriptionCancelled;
    private long remaining;

    public ContentLengthAwareSubscriber(Subscriber<? super ByteBuffer> subscriber, long contentLength) {
        this.subscriber = subscriber;
        this.remaining = contentLength;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (subscription == null) {
            throw new NullPointerException("subscription must not be null");
        }
        this.subscription = subscription;
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(ByteBuffer byteBuffer) {
        if (remaining > 0) {
            long bytesToRead = Math.min(remaining, byteBuffer.remaining());
            // cast is safe, min of long and int is <= max_int
            byteBuffer.limit(byteBuffer.position() + (int) bytesToRead);
            remaining -= bytesToRead;
            subscriber.onNext(byteBuffer);
        }

        if (remaining == 0 && !subscriptionCancelled) {
            subscriptionCancelled = true;
            subscription.cancel();
            onComplete();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException("throwable cannot be null");
        }
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
