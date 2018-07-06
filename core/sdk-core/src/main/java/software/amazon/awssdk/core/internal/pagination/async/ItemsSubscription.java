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

package software.amazon.awssdk.core.internal.pagination.async;

import java.util.Iterator;
import java.util.function.Function;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An implementation of the {@link Subscription} interface that can be used to signal and cancel demand for
 * paginated items across pages
 *
 * @param <ResponseT> The type of a single response page
 * @param <ItemT> The type of paginated member in a response page
 */
@SdkInternalApi
public final class ItemsSubscription<ResponseT, ItemT> extends PaginationSubscription<ResponseT> {
    private final Function<ResponseT, Iterator<ItemT>> getIteratorFunction;
    private volatile Iterator<ItemT> singlePageItemsIterator;

    private ItemsSubscription(BuilderImpl builder) {
        super(builder);
        this.getIteratorFunction = builder.iteratorFunction;
    }

    /**
     * Create a builder for creating a {@link ItemsSubscription}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    protected void handleRequests() {
        if (!hasMoreItems() && !hasNextPage()) {
            completeSubscription();
            return;
        }

        synchronized (this) {
            if (outstandingRequests.get() <= 0) {
                stopTask();
                return;
            }
        }

        if (!isTerminated()) {
            /**
             * Current page is null only the first time the method is called.
             * Once initialized, current page will never be null
             */
            if (currentPage == null || (!hasMoreItems() && hasNextPage())) {
                fetchNextPage();

            } else if (hasMoreItems()) {
                sendNextElement();

            // All valid cases are covered above. Throw an exception if any combination is missed
            } else {
                throw new IllegalStateException("Execution should have not reached here");
            }
        }
    }

    private void fetchNextPage() {
        nextPageFetcher.nextPage(currentPage)
                       .whenComplete(((response, error) -> {
                           if (response != null) {
                               currentPage = response;
                               singlePageItemsIterator = getIteratorFunction.apply(response);
                               sendNextElement();
                           }
                           if (error != null) {
                               subscriber.onError(error);
                               cleanup();
                           }
                       }));
    }

    /**
     * Calls onNext and calls the recursive method.
     */
    private void sendNextElement() {
        if (singlePageItemsIterator.hasNext()) {
            subscriber.onNext(singlePageItemsIterator.next());
            outstandingRequests.getAndDecrement();
        }

        handleRequests();
    }

    private boolean hasMoreItems() {
        return singlePageItemsIterator != null && singlePageItemsIterator.hasNext();
    }


    public interface Builder extends PaginationSubscription.Builder<ItemsSubscription, Builder> {
        Builder iteratorFunction(Function iteratorFunction);

        @Override
        ItemsSubscription build();
    }

    private static final class BuilderImpl extends PaginationSubscription.BuilderImpl<ItemsSubscription, Builder>
        implements Builder {
        private Function iteratorFunction;

        @Override
        public Builder iteratorFunction(Function iteratorFunction) {
            this.iteratorFunction = iteratorFunction;
            return this;
        }

        @Override
        public ItemsSubscription build() {
            return new ItemsSubscription(this);
        }
    }
}
