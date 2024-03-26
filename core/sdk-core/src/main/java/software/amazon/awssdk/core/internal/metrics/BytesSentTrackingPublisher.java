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
import software.amazon.awssdk.core.internal.progress.listener.ProgressUpdater;
import software.amazon.awssdk.http.async.SdkHttpContentPublisher;

@SdkInternalApi
public class BytesSentTrackingPublisher implements SdkHttpContentPublisher {

    private final Publisher<ByteBuffer> upstream;
    private final AtomicLong bytesSent;
    private ProgressUpdater progressUpdater;
    private Long contentLength;

    public BytesSentTrackingPublisher(Publisher<ByteBuffer> upstream,
                                      ProgressUpdater progressUpdater,
                                      Optional<Long> contentLength) {
        this.upstream = upstream;
        this.bytesSent = new AtomicLong(0L);
        this.progressUpdater = progressUpdater;
        contentLength.ifPresent(value -> {
            this.contentLength = value;
        });
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        upstream.subscribe(new BytesSentTrackingPublisher.BytesSentTracker(subscriber,
                                                                           bytesSent,
                                                                           progressUpdater));
    }

    public long bytesSent() {
        return bytesSent.get();
    }

    @Override
    public Optional<Long> contentLength() {
        return Optional.ofNullable(contentLength);
    }

    private static final class BytesSentTracker implements Subscriber<ByteBuffer> {
        private final Subscriber<? super ByteBuffer> downstream;
        private final AtomicLong bytesSent;
        private final ProgressUpdater progressUpdater;

        private BytesSentTracker(Subscriber<? super ByteBuffer> downstream, AtomicLong bytesSent,
                                 ProgressUpdater progressUpdater) {
            this.downstream = downstream;
            this.bytesSent = bytesSent;
            this.progressUpdater = progressUpdater;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            downstream.onSubscribe(subscription);
            if (progressUpdater != null) {
                progressUpdater.resetBytesSent();
            }
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            downstream.onNext(byteBuffer);
            bytesSent.addAndGet(byteBuffer.remaining());
            if (progressUpdater != null) {
                progressUpdater.incrementBytesSent(bytesSent.get());
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
