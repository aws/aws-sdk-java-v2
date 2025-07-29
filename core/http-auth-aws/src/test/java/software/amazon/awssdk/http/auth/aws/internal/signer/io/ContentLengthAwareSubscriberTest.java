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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.auth.aws.PublisherUtils.randomPublisherOfLength;

import io.reactivex.subscribers.TestSubscriber;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ContentLengthAwareSubscriberTest {
    @Test
    void subscribe_upstreamExceedsContentLength_correctlyTruncates() {
        long contentLength = 64;
        Publisher<ByteBuffer> upstream = randomPublisherOfLength(8192, 8, 16);

        TestSubscriber<ByteBuffer> subscriber = new TestSubscriber<>();

        ContentLengthAwareSubscriber lengthAwareSubscriber = new ContentLengthAwareSubscriber(subscriber, contentLength);
        upstream.subscribe(lengthAwareSubscriber);

        assertThat(totalRemaining(subscriber.values())).isEqualTo(contentLength);
    }

    @Test
    void subscribe_upstreamHasExactlyContentLength_signalsComplete() {
        long contentLength = 8192;
        Publisher<ByteBuffer> upstream = randomPublisherOfLength((int) contentLength, 8, 16);

        TestSubscriber<ByteBuffer> subscriber = new TestSubscriber<>();
        ContentLengthAwareSubscriber lengthAwareSubscriber = new ContentLengthAwareSubscriber(subscriber, contentLength);
        upstream.subscribe(lengthAwareSubscriber);

        subscriber.assertComplete();
        assertThat(totalRemaining(subscriber.values())).isEqualTo(contentLength);
    }

    @Test
    void subscribe_upstreamExceedsContentLength_request1BufferAtATime_correctlyTruncates() throws Exception {
        long contentLength = 8192;

        Publisher<ByteBuffer> upstream = randomPublisherOfLength((int) contentLength * 2, 8, 16);

        CompletableFuture<Void> subscriberFinished = new CompletableFuture<>();
        List<ByteBuffer> buffers = new ArrayList<>();

        Subscriber<ByteBuffer> testSubscriber = new Subscriber<ByteBuffer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                this.subscription.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                buffers.add(byteBuffer);
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                subscriberFinished.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                subscriberFinished.complete(null);
            }
        };

        testSubscriber = Mockito.spy(testSubscriber);

        ContentLengthAwareSubscriber lengthAwareSubscriber = new ContentLengthAwareSubscriber(testSubscriber, contentLength);
        upstream.subscribe(lengthAwareSubscriber);

        subscriberFinished.get(1, TimeUnit.MINUTES);
        Mockito.verify(testSubscriber, Mockito.times(1)).onComplete();
        assertThat(totalRemaining(buffers)).isEqualTo(contentLength);
    }

    @Test
    void subscribe_upstreamExceedsContentLength_upstreamSubscriptionCancelledAfterContentLengthReached() {
        long contentLength = 64;
        Publisher<ByteBuffer> upstream = randomPublisherOfLength((int) contentLength * 4, 8, 16);

        TestSubscriber<ByteBuffer> testSubscriber = new TestSubscriber<>();
        ContentLengthAwareSubscriber lengthAwareSubscriber = new ContentLengthAwareSubscriber(testSubscriber, contentLength);
        SubscriptionWrappingSubscriber subscriptionWrappingSubscriber = new SubscriptionWrappingSubscriber(lengthAwareSubscriber);
        upstream.subscribe(subscriptionWrappingSubscriber);

        testSubscriber.assertComplete();
        assertThat(subscriptionWrappingSubscriber.wrappedSubscription.cancelInvocations.get()).isEqualTo(1L);
        assertThat(totalRemaining(testSubscriber.values())).isEqualTo(contentLength);
    }

    @Test
    void subscribe_upstreamHasContentAndContentLength0_signalsComplete() {
        Publisher<ByteBuffer> upstream = randomPublisherOfLength(128, 8, 16);

        TestSubscriber<ByteBuffer> testSubscriber = new TestSubscriber<>();
        ContentLengthAwareSubscriber lengthAwareSubscriber = new ContentLengthAwareSubscriber(testSubscriber, 0L);
        upstream.subscribe(lengthAwareSubscriber);

        testSubscriber.awaitTerminalEvent(5, TimeUnit.SECONDS);
        testSubscriber.assertComplete();
        assertThat(testSubscriber.values()).isEmpty();
    }

    private static class TestSubscription implements Subscription {
        private final Subscription wrapped;
        private final AtomicLong cancelInvocations = new AtomicLong();

        private TestSubscription(Subscription wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void request(long l) {
            this.wrapped.request(l);
        }

        @Override
        public void cancel() {
            cancelInvocations.incrementAndGet();
            this.wrapped.cancel();
        }
    }

    private long totalRemaining(List<ByteBuffer> buffers) {
        return buffers.stream().mapToLong(ByteBuffer::remaining).sum();
    }

    private static class SubscriptionWrappingSubscriber implements Subscriber<ByteBuffer> {
        private final Subscriber<ByteBuffer> wrapped;
        private TestSubscription wrappedSubscription;

        private SubscriptionWrappingSubscriber(Subscriber<ByteBuffer> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.wrappedSubscription = new TestSubscription(subscription);
            this.wrapped.onSubscribe(this.wrappedSubscription);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            this.wrapped.onNext(byteBuffer);
        }

        @Override
        public void onError(Throwable throwable) {
            this.wrapped.onError(throwable);
        }

        @Override
        public void onComplete() {
            this.wrapped.onComplete();
        }
    }
}
