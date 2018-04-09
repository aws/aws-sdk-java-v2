/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.pagination.async;

import java.util.Iterator;
import java.util.function.Function;
import org.reactivestreams.Subscriber;

/**
 * A publisher to request for a stream of paginated items. The class can be used to request data for paginated items
 * across multiple pages.
 *
 * @param <ResponseT> The type of a single response page
 * @param <ItemT> The type of paginated member in a response page
 */
public class PaginatedItemsPublisher<ResponseT, ItemT> implements SdkPublisher<ItemT> {

    private final AsyncPageFetcher<ResponseT> nextPageFetcher;

    private final Function<ResponseT, Iterator<ItemT>> getIteratorFunction;

    private final boolean isLastPage;

    public PaginatedItemsPublisher(AsyncPageFetcher<ResponseT> nextPageFetcher,
                                   Function<ResponseT, Iterator<ItemT>> getIteratorFunction,
                                   boolean isLastPage) {
        this.nextPageFetcher = nextPageFetcher;
        this.getIteratorFunction = getIteratorFunction;
        this.isLastPage = isLastPage;
    }

    @Override
    public void subscribe(Subscriber<? super ItemT> subscriber) {
        subscriber.onSubscribe(isLastPage ? new EmptySubscription(subscriber)
                                          : new ItemsSubscription(subscriber, nextPageFetcher, getIteratorFunction));
    }
}
