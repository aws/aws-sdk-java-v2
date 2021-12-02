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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class S3CrtDataPublisherTest {

    private S3CrtDataPublisher dataPublisher;

    @Before
    public void setup() {
        dataPublisher = new S3CrtDataPublisher();
    }

    @Test
    public void publisherFinishesSuccessfully_shouldInvokeOnComplete() throws InterruptedException {
        AtomicBoolean errorOccurred = new AtomicBoolean(false);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Queue<ByteBuffer> events = new ConcurrentLinkedQueue<>();
        int numOfData = 3;
        dataPublisher.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                events.add(byteBuffer);
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onError(Throwable t) {
                errorOccurred.set(true);
            }

            @Override
            public void onComplete() {
                countDownLatch.countDown();
            }
        });

        for (int i = 0; i < numOfData; i++) {
            dataPublisher.deliverData(ByteBuffer.wrap(RandomUtils.nextBytes(20)));
        }

        dataPublisher.notifyStreamingFinished();

        countDownLatch.await(5, TimeUnit.SECONDS);
        assertThat(errorOccurred).isFalse();
        assertThat(events.size()).isEqualTo(numOfData);
    }

    @Test
    public void publisherHasOneByteBuffer_subscriberRequestOnce_shouldInvokeComplete() throws InterruptedException {
        AtomicBoolean errorOccurred = new AtomicBoolean(false);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Queue<ByteBuffer> events = new ConcurrentLinkedQueue<>();
        int numOfData = 1;
        dataPublisher.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                events.add(byteBuffer);
            }

            @Override
            public void onError(Throwable t) {
                errorOccurred.set(true);
            }

            @Override
            public void onComplete() {
                countDownLatch.countDown();
            }
        });

        for (int i = 0; i < numOfData; i++) {
            dataPublisher.deliverData(ByteBuffer.wrap(RandomUtils.nextBytes(20)));
        }

        dataPublisher.notifyStreamingFinished();

        countDownLatch.await(5, TimeUnit.SECONDS);
        assertThat(countDownLatch.getCount()).isEqualTo(0);
        assertThat(errorOccurred).isFalse();
        assertThat(events.size()).isEqualTo(numOfData);
    }

    @Test
    public void publisherThrowsError_shouldInvokeOnError() throws InterruptedException {
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Queue<ByteBuffer> events = new ConcurrentLinkedQueue<>();
        int numOfData = 3;
        dataPublisher.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                events.add(byteBuffer);
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onError(Throwable t) {
                countDownLatch.countDown();
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }
        });

        for (int i = 0; i < numOfData; i++) {
            dataPublisher.deliverData(ByteBuffer.wrap(RandomUtils.nextBytes(20)));
        }

        dataPublisher.notifyError(new RuntimeException("test"));

        countDownLatch.await(5, TimeUnit.SECONDS);
        assertThat(onCompleteCalled).isFalse();
        assertThat(events.size()).isEqualTo(numOfData);
    }

    @Test
    public void subscriberCancels_shouldNotInvokeTerminalMethods() {
        AtomicBoolean onCompleteCalled = new AtomicBoolean(false);
        AtomicBoolean errorOccurred = new AtomicBoolean(false);

        Queue<ByteBuffer> events = new ConcurrentLinkedQueue<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int numOfData = 3;
        for (int i = 0; i < numOfData; i++) {
            futures.add(
                CompletableFuture.runAsync(() -> dataPublisher.deliverData(ByteBuffer.wrap(RandomUtils.nextBytes(20)))));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((r, t) -> {
            CompletableFuture.runAsync(() -> dataPublisher.notifyStreamingFinished());
        });

        dataPublisher.subscribe(new Subscriber<ByteBuffer>() {
            private Subscription subscription;
            @Override
            public void onSubscribe(Subscription s) {
                this.subscription = s;
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer byteBuffer) {
                events.add(byteBuffer);
                subscription.cancel();
            }

            @Override
            public void onError(Throwable t) {
                errorOccurred.set(true);
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }
        });

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(onCompleteCalled).isFalse();
        assertThat(errorOccurred).isFalse();
        assertThat(events.size()).isEqualTo(1);
    }
}
