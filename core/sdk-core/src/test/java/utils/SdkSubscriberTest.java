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

package utils;

import io.reactivex.Flowable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.utils.async.LimitingSubscriber;
import software.amazon.awssdk.utils.internal.async.EmptySubscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SdkSubscriberTest {

    public static final Function<Integer, Iterator<Integer>> SAMPLE_ITERATOR = response -> Arrays.asList(1, 2, 3, 4, 5, 6).listIterator();
    public static final Function<Integer, Iterator<Integer>> EMPTY_ITERATOR = response -> new ArrayList<Integer>().listIterator();

    @Mock
    Subscriber<Integer> mockSubscriber;

    private SdkPublisher<Integer> sdkPublisher;

    @Before
    public void setUp() {
        sdkPublisher = SdkPublisher.adapt(Flowable.just(1, 2, 3, 4, 5, 6));
    }

    @Test
    public void limitingSubscriber_with_different_limits() throws InterruptedException, ExecutionException, TimeoutException {
        List<Integer> belowLimit = new ArrayList<>();
        sdkPublisher.limit(3).subscribe(belowLimit::add).get(5, TimeUnit.SECONDS);
        assertThat(belowLimit).containsExactly(1, 2, 3);

        List<Integer> beyondLimit = new ArrayList<>();
        sdkPublisher.limit(33).subscribe(beyondLimit::add).get(5, TimeUnit.SECONDS);
        assertThat(beyondLimit).containsExactly(1, 2, 3, 4, 5, 6);

        List<Integer> zeroLimit = new ArrayList<>();
        sdkPublisher.limit(0).subscribe(zeroLimit::add).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimit).isEmpty();
    }

    @Test
    public void filteringSubscriber_with_different_filters() throws InterruptedException, ExecutionException, TimeoutException {

        List<Integer> filteredSomeList = new ArrayList<>();
        sdkPublisher.filter(i -> i % 2 == 0).subscribe(filteredSomeList::add).get(5, TimeUnit.SECONDS);
        assertThat(filteredSomeList).containsExactly(2, 4, 6);

        List<Integer> filteredAllList = new ArrayList<>();
        sdkPublisher.filter(i -> i % 10 == 0).subscribe(filteredAllList::add).get(5, TimeUnit.SECONDS);
        assertThat(filteredAllList).isEmpty();

        List<Integer> filteredNone = new ArrayList<>();
        sdkPublisher.filter(i -> i % 1 == 0).subscribe(filteredNone::add).get(5, TimeUnit.SECONDS);
        assertThat(filteredNone).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    public void limit_and_filter_subscriber_chained_with_different_conditions() throws InterruptedException, ExecutionException, TimeoutException {

        List<Integer> belowLimitWithFiltering = new ArrayList<>();
        sdkPublisher.limit(4).filter(i -> i % 2 == 0).subscribe(belowLimitWithFiltering::add).get(5, TimeUnit.SECONDS);
        assertThat(belowLimitWithFiltering).containsExactly(2, 4);

        List<Integer> beyondLimitWithAllFiltering = new ArrayList<>();
        sdkPublisher.limit(33).filter(i -> i % 10 == 0).subscribe(beyondLimitWithAllFiltering::add).get(5, TimeUnit.SECONDS);
        assertThat(beyondLimitWithAllFiltering).isEmpty();

        List<Integer> zeroLimitAndNoFilter = new ArrayList<>();
        sdkPublisher.limit(0).filter(i -> i % 1 == 0).subscribe(zeroLimitAndNoFilter::add).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimitAndNoFilter).isEmpty();

        List<Integer> filteringbelowLimitWith = new ArrayList<>();
        sdkPublisher.filter(i -> i % 2 == 0).limit(2).subscribe(filteringbelowLimitWith::add).get(5, TimeUnit.SECONDS);
        assertThat(filteringbelowLimitWith).containsExactly(2, 4);

        List<Integer> filteringAndOutsideLimit = new ArrayList<>();
        sdkPublisher.filter(i -> i % 10 == 0).limit(33).subscribe(filteringAndOutsideLimit::add).get(5, TimeUnit.SECONDS);
        assertThat(filteringAndOutsideLimit).isEmpty();
    }

    @Test
    public void limit__subscriber_with_empty_input_and_zero_limit() throws Exception {
        sdkPublisher = SdkPublisher.adapt(Flowable.empty());

        List<Integer> zeroLimit = new ArrayList<>();
        sdkPublisher.limit(0).subscribe(zeroLimit::add).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimit).isEmpty();

        List<Integer> nonZeroLimit = new ArrayList<>();
        sdkPublisher.limit(10).subscribe(nonZeroLimit::add).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimit).isEmpty();
    }


    @Test
    public void limiting_subscriber_with_multiple_thread_publishers() throws InterruptedException {
        final int limitFactor = 5;
        LimitingSubscriber<Integer> limitingSubscriber = new LimitingSubscriber<>(mockSubscriber, limitFactor);
        limitingSubscriber.onSubscribe(new EmptySubscription(mockSubscriber));
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            Integer boxed = i;
            executorService.submit(() -> limitingSubscriber.onNext(boxed));
        }
        executorService.awaitTermination(300, TimeUnit.MILLISECONDS);
        Mockito.verify(mockSubscriber, times(limitFactor)).onNext(anyInt());
        Mockito.verify(mockSubscriber).onComplete();
        Mockito.verify(mockSubscriber).onSubscribe(any());
        Mockito.verify(mockSubscriber, never()).onError(any());
    }
}
