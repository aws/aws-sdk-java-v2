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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PaginatedItemsPublisher;
import software.amazon.awssdk.utils.async.LimitingSubscriber;
import software.amazon.awssdk.utils.internal.async.EmptySubscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SdkSubscriberTest {

    public static final Function<Integer, Iterator<Integer>> SAMPLE_ITERATOR = response -> Arrays.asList(1, 2, 3, 4, 5, 6).listIterator();
    public static final Function<Integer, Iterator<Integer>> EMPTY_ITERATOR = response -> new ArrayList<Integer>().listIterator();
    @Mock
    AsyncPageFetcher asyncPageFetcher;
    PaginatedItemsPublisher<Integer, Integer> itemsPublisher;

    @Mock
    Subscriber<Integer> mockSubscriber;

    @Before
    public void setUp() {
        doReturn(CompletableFuture.completedFuture(1))
                .when(asyncPageFetcher).nextPage(null);
        doReturn(false)
                .when(asyncPageFetcher).hasNextPage(anyObject());
    }

    @Test
    public void limitingSubscriber_with_different_limits() throws InterruptedException, ExecutionException, TimeoutException {
        itemsPublisher = PaginatedItemsPublisher.builder().nextPageFetcher(asyncPageFetcher)
                .iteratorFunction(SAMPLE_ITERATOR).isLastPage(false).build();

        final List<Integer> belowLimit = new ArrayList<>();
        itemsPublisher.limit(3).subscribe(e -> belowLimit.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(belowLimit).isEqualTo(Arrays.asList(1, 2, 3));

        final List<Integer> beyondLimit = new ArrayList<>();
        itemsPublisher.limit(33).subscribe(e -> beyondLimit.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(beyondLimit).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6));

        final List<Integer> zeroLimit = new ArrayList<>();
        itemsPublisher.limit(0).subscribe(e -> zeroLimit.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimit).isEqualTo(Arrays.asList());
    }

    @Test
    public void filteringSubscriber_with_different_filters() throws InterruptedException, ExecutionException, TimeoutException {
        itemsPublisher = PaginatedItemsPublisher.builder().nextPageFetcher(asyncPageFetcher)
                .iteratorFunction(SAMPLE_ITERATOR).isLastPage(false).build();

        final List<Integer> filteredSomeList = new ArrayList<>();
        itemsPublisher.filter(i -> i % 2 == 0).subscribe(e -> filteredSomeList.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(filteredSomeList).isEqualTo(Arrays.asList(2, 4, 6));

        final List<Integer> filteredAllList = new ArrayList<>();
        itemsPublisher.filter(i -> i % 10 == 0).subscribe(e -> filteredAllList.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(filteredAllList).isEqualTo(Arrays.asList());

        final List<Integer> filteredNone = new ArrayList<>();
        itemsPublisher.filter(i -> i % 1 == 0).subscribe(e -> filteredNone.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(filteredNone).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6));

    }

    @Test
    public void limit_and_filter_subscriber_chained_with_different_conditions() throws InterruptedException, ExecutionException, TimeoutException {
        itemsPublisher = PaginatedItemsPublisher.builder().nextPageFetcher(asyncPageFetcher)
                .iteratorFunction(SAMPLE_ITERATOR).isLastPage(false).build();

        final List<Integer> belowLimitWithFiltering = new ArrayList<>();
        itemsPublisher.limit(4).filter(i -> i % 2 == 0).subscribe(e -> belowLimitWithFiltering.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(belowLimitWithFiltering).isEqualTo(Arrays.asList(2, 4));

        final List<Integer> beyondLimitWithAllFiltering = new ArrayList<>();
        itemsPublisher.limit(33).filter(i -> i % 10 == 0).subscribe(e -> beyondLimitWithAllFiltering.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(beyondLimitWithAllFiltering).isEqualTo(Arrays.asList());

        final List<Integer> zeroLimitAndNoFilter = new ArrayList<>();
        itemsPublisher.limit(0).filter(i -> i % 1 == 0).subscribe(e -> zeroLimitAndNoFilter.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimitAndNoFilter).isEqualTo(Arrays.asList());

        final List<Integer> filteringbelowLimitWith = new ArrayList<>();
        itemsPublisher.filter(i -> i % 2 == 0).limit(2).subscribe(e -> filteringbelowLimitWith.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(filteringbelowLimitWith).isEqualTo(Arrays.asList(2, 4));

        final List<Integer> filteringAndOutsideLimit = new ArrayList<>();
        itemsPublisher.filter(i -> i % 10 == 0).limit(33).subscribe(e -> filteringAndOutsideLimit.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(filteringAndOutsideLimit).isEqualTo(Arrays.asList());
    }

    @Test
    public void limit__subscriber_with_empty_input_and_zero_limit() throws InterruptedException, ExecutionException, TimeoutException {
        itemsPublisher = PaginatedItemsPublisher.builder().nextPageFetcher(asyncPageFetcher)
                .iteratorFunction(EMPTY_ITERATOR).isLastPage(false).build();

        final List<Integer> zeroLimit = new ArrayList<>();
        itemsPublisher.limit(0).subscribe(e -> zeroLimit.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimit).isEqualTo(Arrays.asList());

        final List<Integer> nonZeroLimit = new ArrayList<>();
        itemsPublisher.limit(10).subscribe(e -> nonZeroLimit.add(e)).get(5, TimeUnit.SECONDS);
        assertThat(zeroLimit).isEqualTo(Arrays.asList());
    }


    @Test
    public void limiting_subscriber_with_multiple_thread_publishers() throws InterruptedException {
        final int limitFactor = 5;
        LimitingSubscriber<Integer> limitingSubscriber = new LimitingSubscriber<>(mockSubscriber, limitFactor);
        limitingSubscriber.onSubscribe(new EmptySubscription(mockSubscriber));
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            final Integer integer = Integer.valueOf(i);
            executorService.submit(() -> limitingSubscriber.onNext(new Integer(integer)));
        }
        executorService.awaitTermination(300, TimeUnit.MILLISECONDS);
        Mockito.verify(mockSubscriber, times(limitFactor)).onNext(anyInt());
        Mockito.verify(mockSubscriber).onComplete();
        Mockito.verify(mockSubscriber).onSubscribe(anyObject());
        Mockito.verify(mockSubscriber, never()).onError(anyObject());
    }
}
