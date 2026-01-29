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
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Publisher that tracks how many bytes are published from the wrapped publisher to the downstream subscriber,
 * along with timing information for throughput calculation.
 */
@SdkInternalApi
public final class BytesWrittenTrackingPublisher implements Publisher<ByteBuffer> {
    private final Publisher<ByteBuffer> upstream;
    private final RequestBodyMetrics metrics;

    public BytesWrittenTrackingPublisher(Publisher<ByteBuffer> upstream, RequestBodyMetrics metrics) {
        this.upstream = upstream;
        this.metrics = metrics;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        upstream.subscribe(new BytesWrittenTracker(subscriber, metrics));
    }

    private static final class BytesWrittenTracker implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> downstream;
        private final RequestBodyMetrics metrics;

        private BytesWrittenTracker(Subscriber<? super ByteBuffer> downstream, RequestBodyMetrics metrics) {
            this.downstream = downstream;
            this.metrics = metrics;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            int bytes = byteBuffer.remaining();
            if (bytes > 0) {
                metrics.firstByteWrittenNanoTime().compareAndSet(0, System.nanoTime());
                metrics.bytesWritten().addAndGet(bytes);
                metrics.lastByteWrittenNanoTime().set(System.nanoTime());
            }
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
