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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class InMemoryPublisherTest {

    @Test
    public void subscribe_deliversAllData() throws Exception {
        byte[] bytes = "test data".getBytes(StandardCharsets.UTF_8);
        List<ByteBuffer> data = Arrays.asList(ByteBuffer.wrap(bytes));
        InMemoryPublisher publisher = new InMemoryPublisher(data, bytes.length);

        List<ByteBuffer> received = new ArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);

        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                received.add(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }
        });

        assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(1);
    }

    @Test
    public void subscribe_reEntrantRequestFromOnNext_doesNotDeadlock() throws Exception {
        List<ByteBuffer> data = Arrays.asList(
            ByteBuffer.wrap("a".getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap("b".getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap("c".getBytes(StandardCharsets.UTF_8))
        );
        InMemoryPublisher publisher = new InMemoryPublisher(data, 3);

        List<ByteBuffer> received = new ArrayList<>();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean error = new AtomicBoolean(false);

        publisher.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                received.add(byteBuffer);
                // Re-entrant request — this is what ByteBufferStoringSubscriber does
                subscription.request(1);
            }

            @Override
            public void onError(Throwable t) {
                error.set(true);
                completed.countDown();
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }
        });

        assertThat(completed.await(5, TimeUnit.SECONDS))
            .as("Should complete without deadlocking")
            .isTrue();
        assertThat(error.get()).isFalse();
        assertThat(received).hasSize(3);
    }

    @Test
    public void subscribe_secondSubscription_getsError() {
        List<ByteBuffer> data = Arrays.asList(ByteBuffer.wrap("x".getBytes(StandardCharsets.UTF_8)));
        InMemoryPublisher publisher = new InMemoryPublisher(data, 1);

        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Subscription s) { s.request(1); }
            @Override public void onNext(ByteBuffer b) { }
            @Override public void onError(Throwable t) { }
            @Override public void onComplete() { }
        });

        AtomicBoolean gotError = new AtomicBoolean(false);
        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Subscription s) { }
            @Override public void onNext(ByteBuffer b) { }
            @Override public void onError(Throwable t) { gotError.set(true); }
            @Override public void onComplete() { }
        });

        assertThat(gotError.get()).isTrue();
    }

    @Test
    public void subscribe_requestNonPositive_signalsError() throws Exception {
        List<ByteBuffer> data = Arrays.asList(ByteBuffer.wrap("x".getBytes(StandardCharsets.UTF_8)));
        InMemoryPublisher publisher = new InMemoryPublisher(data, 1);

        AtomicBoolean gotError = new AtomicBoolean(false);
        CountDownLatch completed = new CountDownLatch(1);

        publisher.subscribe(new Subscriber<ByteBuffer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(0);
            }

            @Override public void onNext(ByteBuffer b) { }

            @Override
            public void onError(Throwable t) {
                gotError.set(t instanceof IllegalArgumentException);
                completed.countDown();
            }

            @Override public void onComplete() { completed.countDown(); }
        });

        assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(gotError.get()).isTrue();
    }
}
