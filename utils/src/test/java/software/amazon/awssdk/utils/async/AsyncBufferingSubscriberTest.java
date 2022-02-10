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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class AsyncBufferingSubscriberTest {
    private AsyncBufferingSubscriber<String> subscriber;
    private Function<String, CompletableFuture<?>> consumer;
    private CompletableFuture<Void> returnFuture;
    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private static ScheduledExecutorService scheduledExecutorService;

    @BeforeAll
    public static void setUp() {
        scheduledExecutorService = Executors.newScheduledThreadPool(2);
    }

    @BeforeEach
    public void setUpPerTest() {
        returnFuture = new CompletableFuture<>();
        for (int i = 0; i < 101; i++) {
            futures.add(new CompletableFuture<>());
        }
        Iterator<CompletableFuture<Void>> iterator = futures.iterator();
        consumer = s -> iterator.next();

        futures.forEach(f -> {
            scheduledExecutorService.schedule(() -> {
                f.complete(null);
            }, 1, TimeUnit.SECONDS);
        });
        subscriber = new AsyncBufferingSubscriber<>(consumer, returnFuture,
                                                    5);
    }

    @AfterAll
    public static void cleanUp() {
        scheduledExecutorService.shutdown();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 11, 20, 100})
    void differentNumberOfStrings_shouldCompleteSuccessfully(int numberOfStrings) throws Exception {
        new TestPublisher(numberOfStrings).subscribe(subscriber);
        returnFuture.get(1000, TimeUnit.SECONDS);
        assertThat(returnFuture).isCompleted().isNotCompletedExceptionally();
    }

    @Test
    void onErrorInvoked_shouldCompleteFutureExceptionally() {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {

            }
        });
        RuntimeException exception = new RuntimeException("test");
        subscriber.onError(exception);
        assertThat(returnFuture).isCompletedExceptionally();
    }



    private static final class TestPublisher implements Publisher<String> {
        private final int numberOfStrings;
        private volatile boolean isDone = false;
        private final AtomicInteger requestNumber = new AtomicInteger(0);

        private TestPublisher(int numberOfStrings) {
            this.numberOfStrings = numberOfStrings;
        }

        @Override
        public void subscribe(Subscriber<? super String> subscriber) {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (isDone) {
                        return;
                    }

                    if (requestNumber.incrementAndGet() > numberOfStrings) {
                        isDone = true;
                        subscriber.onComplete();
                        return;
                    }

                    subscriber.onNext("key" + requestNumber.get());
                }

                @Override
                public void cancel() {
                }
            });
        }
    }
}
