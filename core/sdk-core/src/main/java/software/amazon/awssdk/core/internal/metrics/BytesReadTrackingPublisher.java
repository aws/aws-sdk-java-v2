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

package software.amazon.awssdk.core.internal.metrics;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Publisher that tracks how many bytes are published from the wrapped publisher to the downstream subscriber.
 */
@SdkInternalApi
public final class BytesReadTrackingPublisher implements Publisher<ByteBuffer> {
    private final Publisher<ByteBuffer> upstream;
    private final AtomicLong bytesRead;

    public BytesReadTrackingPublisher(Publisher<ByteBuffer> upstream, AtomicLong bytesRead) {
        this.upstream = upstream;
        this.bytesRead = bytesRead;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        upstream.subscribe(new BytesReadTracker(subscriber, bytesRead));
    }

    public long bytesRead() {
        return bytesRead.get();
    }

    private static final class BytesReadTracker implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> downstream;
        private final AtomicLong bytesRead;

        private BytesReadTracker(Subscriber<? super ByteBuffer> downstream, AtomicLong bytesRead) {
            this.downstream = downstream;
            this.bytesRead = bytesRead;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            bytesRead.addAndGet(byteBuffer.remaining());
            downstream.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable throwable) {
            downstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            downstream.onComplete();
        }
    }
}
