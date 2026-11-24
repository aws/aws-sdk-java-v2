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

package software.amazon.awssdk.core.internal.http.async;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.async.DelegatingSubscriber;

/**
 * Publisher that records whether the wrapped publisher has signaled {@code onComplete}.
 *
 * <p>The flag is set before {@code onComplete} is forwarded, so any code that the downstream subscriber's
 * {@code onComplete} triggers, on this thread or another, already sees it.
 */
@SdkInternalApi
public final class CompletionTrackingPublisher implements Publisher<ByteBuffer> {
    private final Publisher<ByteBuffer> upstream;
    private final AtomicBoolean complete;

    public CompletionTrackingPublisher(Publisher<ByteBuffer> upstream, AtomicBoolean complete) {
        this.upstream = upstream;
        this.complete = complete;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
        Validate.paramNotNull(subscriber, "subscriber");
        upstream.subscribe(new CompletionTracker(subscriber, complete));
    }

    private static final class CompletionTracker extends DelegatingSubscriber<ByteBuffer, ByteBuffer> {
        private final AtomicBoolean complete;

        private CompletionTracker(Subscriber<? super ByteBuffer> downstream, AtomicBoolean complete) {
            super(downstream);
            this.complete = complete;
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            subscriber.onNext(byteBuffer);
        }

        @Override
        public void onComplete() {
            complete.set(true);
            super.onComplete();
        }
    }
}
