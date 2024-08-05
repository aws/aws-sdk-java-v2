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

package software.amazon.awssdk.core.internal.async;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.utils.BinaryUtils;

class ByteBuffersAsyncRequestBodyTest {

    private static class TestSubscriber implements Subscriber<ByteBuffer> {
        private Subscription subscription;
        private boolean onCompleteCalled = false;
        private int callsToComplete = 0;
        private final List<ByteBuffer> publishedResults = Collections.synchronizedList(new ArrayList<>());

        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.subscription = s;
        }

        @Override
        public void onNext(ByteBuffer byteBuffer) {
            publishedResults.add(byteBuffer);
        }

        @Override
        public void onError(Throwable throwable) {
            throw new IllegalStateException(throwable);
        }

        @Override
        public void onComplete() {
            onCompleteCalled = true;
            callsToComplete++;
        }
    }

    @Test
    public void subscriberIsMarkedAsCompleted() {
        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.from("Hello World!".getBytes(StandardCharsets.UTF_8));

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);
        subscriber.request(1);

        assertTrue(subscriber.onCompleteCalled);
        assertEquals(1, subscriber.publishedResults.size());
    }

    @Test
    public void subscriberIsMarkedAsCompletedWhenARequestIsMadeForMoreBuffersThanAreAvailable() {
        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.from("Hello World!".getBytes(StandardCharsets.UTF_8));

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);
        subscriber.request(2);

        assertTrue(subscriber.onCompleteCalled);
        assertEquals(1, subscriber.publishedResults.size());
    }

    @Test
    public void subscriberIsThreadSafeAndMarkedAsCompletedExactlyOnce() throws InterruptedException {
        int numBuffers = 100;
        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.of(IntStream.range(0, numBuffers)
                                                                               .mapToObj(i -> ByteBuffer.wrap(new byte[1]))
                                                                               .toArray(ByteBuffer[]::new));

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);

        int parallelism = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        for (int i = 0; i < parallelism; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < numBuffers; j++) {
                    subscriber.request(2);
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        assertTrue(subscriber.onCompleteCalled);
        assertEquals(1, subscriber.callsToComplete);
        assertEquals(numBuffers, subscriber.publishedResults.size());
    }

    @Test
    public void subscriberIsNotMarkedAsCompletedWhenThereAreRemainingBuffersToPublish() {
        byte[] helloWorld = "Hello World!".getBytes(StandardCharsets.UTF_8);
        byte[] goodbyeWorld = "Goodbye World!".getBytes(StandardCharsets.UTF_8);
        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.of((long) (helloWorld.length + goodbyeWorld.length),
                                                                      ByteBuffer.wrap(helloWorld),
                                                                      ByteBuffer.wrap(goodbyeWorld));

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);
        subscriber.request(1);

        assertFalse(subscriber.onCompleteCalled);
        assertEquals(1, subscriber.publishedResults.size());
    }

    @Test
    public void subscriberReceivesAllBuffers() {
        byte[] helloWorld = "Hello World!".getBytes(StandardCharsets.UTF_8);
        byte[] goodbyeWorld = "Goodbye World!".getBytes(StandardCharsets.UTF_8);

        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.of((long) (helloWorld.length + goodbyeWorld.length),
                                                                      ByteBuffer.wrap(helloWorld),
                                                                      ByteBuffer.wrap(goodbyeWorld));

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);
        subscriber.request(2);

        assertEquals(2, subscriber.publishedResults.size());
        assertTrue(subscriber.onCompleteCalled);
        assertArrayEquals(helloWorld, BinaryUtils.copyAllBytesFrom(subscriber.publishedResults.get(0)));
        assertArrayEquals(goodbyeWorld, BinaryUtils.copyAllBytesFrom(subscriber.publishedResults.get(1)));
    }

    @Test
    public void multipleSubscribersReceiveTheSameResults() {
        ByteBuffer sourceBuffer = ByteBuffer.wrap("Hello World!".getBytes(StandardCharsets.UTF_8));
        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.of(sourceBuffer);

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);
        subscriber.request(1);
        TestSubscriber otherSubscriber = new TestSubscriber();
        requestBody.subscribe(otherSubscriber);
        otherSubscriber.request(1);

        ByteBuffer publishedBuffer = subscriber.publishedResults.get(0);
        ByteBuffer otherPublishedBuffer = otherSubscriber.publishedResults.get(0);

        assertEquals(publishedBuffer, otherPublishedBuffer);
    }

    @Test
    public void canceledSubscriberDoesNotReturnNewResults() {
        AsyncRequestBody requestBody = ByteBuffersAsyncRequestBody.of(ByteBuffer.wrap(new byte[0]));

        TestSubscriber subscriber = new TestSubscriber();
        requestBody.subscribe(subscriber);

        subscriber.subscription.cancel();
        subscriber.request(1);

        assertTrue(subscriber.publishedResults.isEmpty());
    }

    @Test
    public void staticOfByteBufferConstructorSetsLengthBasedOnBufferRemaining() {
        ByteBuffer bb1 = ByteBuffer.allocate(2);
        ByteBuffer bb2 = ByteBuffer.allocate(2);
        bb2.position(1);
        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.of(bb1, bb2);
        assertTrue(body.contentLength().isPresent());
        assertEquals(bb1.remaining() + bb2.remaining(), body.contentLength().get());
    }

    @Test
    public void staticFromBytesConstructorSetsLengthBasedOnArrayLength() {
        byte[] bytes = new byte[2];
        ByteBuffersAsyncRequestBody body = ByteBuffersAsyncRequestBody.from(bytes);
        assertTrue(body.contentLength().isPresent());
        assertEquals(bytes.length, body.contentLength().get());
    }

}
