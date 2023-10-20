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

import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class FlatteningSubscriberTest {
    private Subscriber<String> mockDelegate;
    private Subscription mockUpstream;
    private FlatteningSubscriber<String> flatteningSubscriber;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() {
        mockDelegate = Mockito.mock(Subscriber.class);
        mockUpstream = Mockito.mock(Subscription.class);
        flatteningSubscriber = new FlatteningSubscriber<>(mockDelegate);
    }

    @Test
    public void requestOne() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);
        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));

        Mockito.verify(mockDelegate).onNext("foo");

        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestTwo() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(2);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestThree() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(3);

        Mockito.verify(mockUpstream, times(1)).request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
        Mockito.reset(mockUpstream, mockDelegate);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verify(mockUpstream).request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
        Mockito.reset(mockUpstream, mockDelegate);

        flatteningSubscriber.onNext(Arrays.asList("baz"));

        Mockito.verify(mockDelegate).onNext("baz");
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestInfinite() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);
        downstream.request(Long.MAX_VALUE);
        downstream.request(Long.MAX_VALUE);
        downstream.request(Long.MAX_VALUE);
        downstream.request(Long.MAX_VALUE);

        Mockito.verify(mockUpstream, times(1)).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));
        flatteningSubscriber.onComplete();

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verify(mockDelegate).onComplete();
        Mockito.verifyNoMoreInteractions(mockDelegate);
    }

    @Test
    public void onCompleteDelayedUntilAllDataDelivered() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));
        flatteningSubscriber.onComplete();

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
        Mockito.reset(mockUpstream, mockDelegate);

        downstream.request(1);
        Mockito.verify(mockDelegate).onNext("bar");
        Mockito.verify(mockDelegate).onComplete();
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void onErrorDropsBufferedData() {
        Throwable t = new Throwable();

        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onNext(Arrays.asList("foo", "bar"));
        flatteningSubscriber.onError(t);

        Mockito.verify(mockDelegate).onNext("foo");
        Mockito.verify(mockDelegate).onError(t);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestsFromDownstreamDoNothingAfterOnComplete() {
        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onComplete();

        Mockito.verify(mockDelegate).onComplete();
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);

        downstream.request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void requestsFromDownstreamDoNothingAfterOnError() {
        Throwable t = new Throwable();

        flatteningSubscriber.onSubscribe(mockUpstream);

        Subscription downstream = getDownstreamFromDelegate();
        downstream.request(1);

        Mockito.verify(mockUpstream).request(1);

        flatteningSubscriber.onError(t);

        Mockito.verify(mockDelegate).onError(t);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);

        downstream.request(1);
        Mockito.verifyNoMoreInteractions(mockUpstream, mockDelegate);
    }

    @Test
    public void stochastic_dataFlushedBeforeOnComplete() {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            for (int i = 0; i < 30_000_000; ++i) {
                Publisher<List<String>> iterablePublisher = subscriber -> subscriber.onSubscribe(new Subscription() {
                    @Override
                    public void request(long l) {
                        exec.submit(() -> {
                            subscriber.onNext(Collections.singletonList("data"));
                            subscriber.onComplete();
                        });
                    }

                    @Override
                    public void cancel() {
                    }
                });

                AtomicInteger seen = new AtomicInteger(0);
                CompletableFuture<Void> finished = new CompletableFuture<>();
                FlatteningSubscriber<String> elementSubscriber = new FlatteningSubscriber<>(new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(String s) {
                        seen.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable e) {
                        finished.completeExceptionally(e);
                    }

                    @Override
                    public void onComplete() {
                        if (seen.get() != 1) {
                            finished.completeExceptionally(
                                new RuntimeException("Should have gotten 1 element before onComplete"));
                        } else {
                            finished.complete(null);
                        }
                    }
                });

                iterablePublisher.subscribe(elementSubscriber);

                finished.join();
            }
        } finally {
            exec.shutdown();
        }
    }

    private Subscription getDownstreamFromDelegate() {
        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        Mockito.verify(mockDelegate).onSubscribe(subscriptionCaptor.capture());
        return subscriptionCaptor.getValue();
    }

}
