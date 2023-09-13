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

package software.amazon.awssdk.utils.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

public class InputStreamSubscriberTest {
    private SimplePublisher<ByteBuffer> publisher;
    private InputStreamSubscriber subscriber;

    @BeforeEach
    public void setup() {
        publisher = new SimplePublisher<>();
        subscriber = new InputStreamSubscriber();
    }

    @Test
    public void onComplete_returnsEndOfStream_onRead() {
        publisher.subscribe(subscriber);
        publisher.complete();
        assertThat(subscriber.read()).isEqualTo(-1);
        assertThat(subscriber.read(new byte[1])).isEqualTo(-1);
        assertThat(subscriber.read(new byte[1], 0, 1)).isEqualTo(-1);
    }

    @Test
    public void onError_throws_onRead() {
        IllegalStateException exception = new IllegalStateException();

        publisher.subscribe(subscriber);
        publisher.error(exception);
        assertThatThrownBy(() -> subscriber.read()).isEqualTo(exception);
        assertThatThrownBy(() -> subscriber.read(new byte[1])).isEqualTo(exception);
        assertThatThrownBy(() -> subscriber.read(new byte[1], 0, 1)).isEqualTo(exception);
    }

    @Test
    public void onComplete_afterOnNext_returnsEndOfStream() {
        publisher.subscribe(subscriber);
        publisher.send(byteBufferOfLength(1));
        publisher.complete();
        assertThat(subscriber.read()).isEqualTo(0);
        assertThat(subscriber.read()).isEqualTo(-1);
    }

    @Test
    public void onComplete_afterEmptyOnNext_returnsEndOfStream() {
        publisher.subscribe(subscriber);
        publisher.send(byteBufferOfLength(0));
        publisher.send(byteBufferOfLength(0));
        publisher.send(byteBufferOfLength(0));
        publisher.complete();
        assertThat(subscriber.read()).isEqualTo(-1);
    }

    @Test
    public void read_afterOnNext_returnsData() {
        publisher.subscribe(subscriber);
        publisher.send(byteBufferWithByte(10));
        assertThat(subscriber.read()).isEqualTo(10);
    }

    @Test
    public void readBytes_afterOnNext_returnsData() {
        publisher.subscribe(subscriber);
        publisher.send(byteBufferWithByte(10));
        publisher.send(byteBufferWithByte(20));

        byte[] bytes = new byte[2];
        assertThat(subscriber.read(bytes)).isEqualTo(2);
        assertThat(bytes[0]).isEqualTo((byte) 10);
        assertThat(bytes[1]).isEqualTo((byte) 20);
    }

    @Test
    public void readBytesWithOffset_afterOnNext_returnsData() {
        publisher.subscribe(subscriber);
        publisher.send(byteBufferWithByte(10));
        publisher.send(byteBufferWithByte(20));

        byte[] bytes = new byte[3];
        assertThat(subscriber.read(bytes, 1, 2)).isEqualTo(2);
        assertThat(bytes[1]).isEqualTo((byte) 10);
        assertThat(bytes[2]).isEqualTo((byte) 20);
    }

    @Test
    public void read_afterClose_fails() {
        publisher.subscribe(subscriber);
        subscriber.close();
        assertThatThrownBy(() -> subscriber.read()).isInstanceOf(CancellationException.class);
        assertThatThrownBy(() -> subscriber.read(new byte[1])).isInstanceOf(CancellationException.class);
        assertThatThrownBy(() -> subscriber.read(new byte[1], 0, 1)).isInstanceOf(CancellationException.class);
    }

    @Test
    public void readByteArray_0Len_returns0() {
        publisher.subscribe(subscriber);

        assertThat(subscriber.read(new byte[1], 0, 0)).isEqualTo(0);
    }

    public static List<Arguments> stochastic_methodCallsSeemThreadSafe_parameters() {
        Object[][] inputStreamOperations = {
            { "read();", subscriberRead1() },
            { "read(); close();", subscriberRead1().andThen(subscriberClose()) },
            { "read(byte[]); close();", subscriberReadArray().andThen(subscriberClose()) },
            { "read(byte[]); read(byte[]);", subscriberReadArray().andThen(subscriberReadArray()) }
        };

        Object[][] publisherOperations = {
            { "onNext(...);", subscriberOnNext() },
            { "onNext(...); onComplete();", subscriberOnNext().andThen(subscriberOnComplete()) },
            { "onNext(...); onError(...);", subscriberOnNext().andThen(subscriberOnError()) },
            { "onComplete();", subscriberOnComplete() },
            { "onError(...);", subscriberOnError() }
        };

        List<Arguments> result = new ArrayList<>();
        for (Object[] iso : inputStreamOperations) {
            for (Object[] po : publisherOperations) {
                result.add(Arguments.of(iso[1], po[1], iso[0] + " and " + po[0] + " in parallel"));
            }
        }
        return result;
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stochastic_methodCallsSeemThreadSafe_parameters")
    @Timeout(10)
    public void stochastic_methodCallsSeemThreadSafe(Consumer<InputStreamSubscriber> inputStreamOperation,
                                                     Consumer<InputStreamSubscriber> publisherOperation,
                                                     String testName)
        throws InterruptedException, ExecutionException {
        int numIterations = 100;

        // Read/close aren't mutually thread safe, and onNext/onComplete/onError aren't mutually thread safe, but one
        // group of functions might be executed in parallel with the others. We try to make sure that this is safe.

        ExecutorService executor = Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().daemonThreads(true).build());
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < numIterations; i++) {
                CountDownLatch waitingAtStartLine = new CountDownLatch(2);
                CountDownLatch startLine = new CountDownLatch(1);

                InputStreamSubscriber subscriber = new InputStreamSubscriber();
                subscriber.onSubscribe(mockSubscription(subscriber));

                futures.add(executor.submit(() -> {
                    waitingAtStartLine.countDown();
                    startLine.await();
                    inputStreamOperation.accept(subscriber);
                    return null;
                }));
                futures.add(executor.submit(() -> {
                    waitingAtStartLine.countDown();
                    startLine.await();
                    publisherOperation.accept(subscriber);
                    return null;
                }));

                waitingAtStartLine.await();
                startLine.countDown();
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    public static Consumer<InputStreamSubscriber> subscriberOnNext() {
        return s -> s.onNext(ByteBuffer.allocate(1));
    }

    public static Consumer<InputStreamSubscriber> subscriberOnComplete() {
        return s -> s.onComplete();
    }

    public static Consumer<InputStreamSubscriber> subscriberOnError() {
        return s -> s.onError(new Throwable());
    }

    public static Consumer<InputStreamSubscriber> subscriberRead1() {
        return s -> s.read();
    }

    public static Consumer<InputStreamSubscriber> subscriberReadArray() {
        return s -> s.read(new byte[4]);
    }

    public static Consumer<InputStreamSubscriber> subscriberClose() {
        return s -> s.close();
    }

    private Subscription mockSubscription(Subscriber<ByteBuffer> subscriber) {
        Subscription subscription = mock(Subscription.class);
        doAnswer(new Answer<Void>() {
            boolean done = false;
            @Override
            public Void answer(InvocationOnMock invocation) {
                if (!done) {
                    subscriber.onNext(ByteBuffer.wrap(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 }));
                    subscriber.onComplete();
                    done = true;
                }
                return null;
            }
        }).when(subscription).request(anyLong());
        return subscription;
    }


    private ByteBuffer byteBufferOfLength(int length) {
        return ByteBuffer.allocate(length);
    }

    public ByteBuffer byteBufferWithByte(int b) {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte) b);
        buffer.flip();
        return buffer;
    }
}