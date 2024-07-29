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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.util.ProgressUpdaterInvoker;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

/**
 * Publisher that tracks how many bytes are published from the wrapped publisher to the downstream subscriber.
 * If request contains Progress Listeners attached, the callbacks invoke methods to update and track request status
 * by invoking progress updater methods with the bytes being transacted
 */
@SdkInternalApi
public final class BytesReadTrackingPublisher implements SdkHttpContentPublisher {
    private final Publisher<ByteBuffer> upstream;
    private final AtomicLong bytesRead;
    private final ProgressUpdaterInvoker progressUpdaterInvoker;

    public BytesReadTrackingPublisher(Publisher<ByteBuffer> upstream, AtomicLong bytesRead,
                                      ProgressUpdaterInvoker progressUpdaterInvoker) {
        this.upstream = upstream;
        this.bytesRead = bytesRead;
        this.progressUpdaterInvoker = progressUpdaterInvoker;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        upstream.subscribe(new BytesReadTracker(subscriber, bytesRead, progressUpdaterInvoker));
    }

    public long bytesRead() {
        return bytesRead.get();
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.empty();
    }

    private static final class BytesReadTracker implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> downstream;
        private final AtomicLong bytesRead;
        private final ProgressUpdaterInvoker progressUpdaterInvoker;

        private BytesReadTracker(Subscriber<? super ByteBuffer> downstream,
                                 AtomicLong bytesRead, ProgressUpdaterInvoker progressUpdaterInvoker) {
            this.downstream = downstream;
            this.bytesRead = bytesRead;
            this.progressUpdaterInvoker = progressUpdaterInvoker;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(subscription);
            if (progressUpdaterInvoker.progressUpdater() != null) {
                progressUpdaterInvoker.resetBytes();
            }
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            long byteBufferSize = byteBuffer.remaining();
            bytesRead.addAndGet(byteBufferSize);
            downstream.onNext(byteBuffer);
            if (progressUpdaterInvoker != null) {
                progressUpdaterInvoker.incrementBytesTransferred(byteBufferSize);
            }
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
