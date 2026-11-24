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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class CompletionTrackingPublisherTest {

    @Test
    void subscribe_upstreamCompletes_setsFlagBeforeForwardingOnComplete() {
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicBoolean flagSeenInOnComplete = new AtomicBoolean(false);
        CompletionTrackingPublisher publisher =
            new CompletionTrackingPublisher(Flowable.just(ByteBuffer.wrap(new byte[1])), complete);

        publisher.subscribe(new RequestAllSubscriber() {
            @Override
            public void onComplete() {
                flagSeenInOnComplete.set(complete.get());
            }
        });

        assertThat(flagSeenInOnComplete).isTrue();
        assertThat(complete).isTrue();
    }

    @Test
    void subscribe_nullSubscriber_throwsNullPointerExceptionWithoutSubscribingUpstream() {
        AtomicBoolean upstreamSubscribed = new AtomicBoolean(false);
        Publisher<ByteBuffer> silentUpstream = s -> upstreamSubscribed.set(true);
        CompletionTrackingPublisher publisher = new CompletionTrackingPublisher(silentUpstream, new AtomicBoolean(false));

        assertThatThrownBy(() -> publisher.subscribe(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("subscriber must not be null");
        assertThat(upstreamSubscribed).isFalse();
    }

    private static class RequestAllSubscriber implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
