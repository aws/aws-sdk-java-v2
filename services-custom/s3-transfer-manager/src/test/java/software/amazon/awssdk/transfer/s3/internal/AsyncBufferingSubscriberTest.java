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

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscription;

class AsyncBufferingSubscriberTest {
    private static final int MAX_CONCURRENT_EXECUTIONS = 5;
    private AsyncBufferingSubscriber<String> subscriber;
    private Function<String, CompletableFuture<?>> consumer;
    private CompletableFuture<Void> returnFuture;
    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private static ScheduledExecutorService scheduledExecutorService;

    @BeforeAll
    public static void setUp() {
        scheduledExecutorService = Executors.newScheduledThreadPool(5);
    }

    @BeforeEach
    public void setUpPerTest() {
        returnFuture = new CompletableFuture<>();
        for (int i = 0; i < 101; i++) {
            futures.add(new CompletableFuture<>());
        }
        Iterator<CompletableFuture<Void>> iterator = futures.iterator();
        consumer = s -> {
            CompletableFuture<Void> future = iterator.next();
            scheduledExecutorService.schedule(() -> {
                future.complete(null);
            }, 200, TimeUnit.MILLISECONDS);
            return future;
        };

        subscriber = new AsyncBufferingSubscriber<>(consumer, returnFuture,
                                                    MAX_CONCURRENT_EXECUTIONS);
    }

    @AfterAll
    public static void cleanUp() {
        scheduledExecutorService.shutdown();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 11, 20, 100})
    void differentNumberOfStrings_shouldCompleteSuccessfully(int numberOfStrings) throws Exception {
        Flowable.fromArray(IntStream.range(0, numberOfStrings).mapToObj(String::valueOf).toArray(String[]::new)).subscribe(subscriber);


        List<Integer> numRequestsInFlightSampling = new ArrayList<>();

        Disposable disposable = Observable.interval(100, TimeUnit.MILLISECONDS, Schedulers.newThread())
                                         .map(time -> subscriber.numRequestsInFlight())
                                         .subscribe(numRequestsInFlightSampling::add, t -> {});

        returnFuture.get(1000, TimeUnit.SECONDS);
        assertThat(returnFuture).isCompleted().isNotCompletedExceptionally();
        if (numberOfStrings >= MAX_CONCURRENT_EXECUTIONS) {
            assertThat(numRequestsInFlightSampling).contains(MAX_CONCURRENT_EXECUTIONS);
        }
        disposable.dispose();
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
}
