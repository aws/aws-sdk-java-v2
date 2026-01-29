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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;

public class BytesWrittenTrackingPublisherTest {

    @Test
    public void onNext_updatesCounter() {
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        Publisher<ByteBuffer> upstream = AsyncRequestBody.fromString("hello");
        BytesWrittenTrackingPublisher publisher = new BytesWrittenTrackingPublisher(upstream, metrics);

        publisher.subscribe(new TestSubscriber());

        assertThat(metrics.bytesWritten().get()).isEqualTo(5);
    }

    @Test
    public void onNext_recordsFirstByteTime() {
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        Publisher<ByteBuffer> upstream = AsyncRequestBody.fromString("hello");
        BytesWrittenTrackingPublisher publisher = new BytesWrittenTrackingPublisher(upstream, metrics);

        assertThat(metrics.firstByteWrittenNanoTime().get()).isEqualTo(0);

        publisher.subscribe(new TestSubscriber());

        assertThat(metrics.firstByteWrittenNanoTime().get()).isGreaterThan(0);
    }

    @Test
    public void onNext_updatesLastByteTime() {
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        Publisher<ByteBuffer> upstream = AsyncRequestBody.fromString("hello");
        BytesWrittenTrackingPublisher publisher = new BytesWrittenTrackingPublisher(upstream, metrics);

        publisher.subscribe(new TestSubscriber());

        assertThat(metrics.lastByteWrittenNanoTime().get()).isGreaterThan(0);
        assertThat(metrics.lastByteWrittenNanoTime().get()).isGreaterThanOrEqualTo(metrics.firstByteWrittenNanoTime().get());
    }

    @Test
    public void emptyBuffer_doesNotUpdateCounters() {
        RequestBodyMetrics metrics = new RequestBodyMetrics();
        Publisher<ByteBuffer> upstream = AsyncRequestBody.fromString("");
        BytesWrittenTrackingPublisher publisher = new BytesWrittenTrackingPublisher(upstream, metrics);

        publisher.subscribe(new TestSubscriber());

        assertThat(metrics.bytesWritten().get()).isEqualTo(0);
        assertThat(metrics.firstByteWrittenNanoTime().get()).isEqualTo(0);
        assertThat(metrics.lastByteWrittenNanoTime().get()).isEqualTo(0);
    }

    private static class TestSubscriber implements Subscriber<ByteBuffer> {
        @Override
        public void onSubscribe(Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            // consume
        }

        @Override
        public void onError(Throwable throwable) {
        }

        @Override
        public void onComplete() {
        }
    }
}
