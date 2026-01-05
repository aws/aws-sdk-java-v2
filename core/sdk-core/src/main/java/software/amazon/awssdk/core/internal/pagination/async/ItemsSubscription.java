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

package software.amazon.awssdk.core.internal.pagination.async;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.pagination.async.PaginationSubscription;

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
    private final AtomicBoolean handlingRequests = new AtomicBoolean();
    private volatile boolean awaitingNewPage = false;

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
        // Prevent recursion if we already invoked handleRequests
        if (!handlingRequests.compareAndSet(false, true)) {
            return;
        }

        try {
            while (true) {
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

                if (isTerminated()) {
                    return;
                }

                if (shouldFetchNextPage()) {
                    awaitingNewPage = true;
                    fetchNextPage().whenComplete((r, e) -> {
                        if (e == null) {
                            awaitingNewPage = false;
                            handleRequests();
                        }
                        // note: signaling onError if e != null is taken care of by fetchNextPage(). No need to do it here.
                    });
                } else if (hasMoreItems()) {
                    synchronized (this) {
                        if (outstandingRequests.get() <= 0) {
                            continue;
                        }

                        subscriber.onNext(singlePageItemsIterator.next());
                        outstandingRequests.getAndDecrement();
                    }
                } else {
                    // Outstanding demand AND no items in current page AND waiting for next page. Just return for now, and
                    // we'll handle demand when the new page arrives.
                    return;
                }
            }
        } finally {
            handlingRequests.set(false);
        }
    }

    private CompletableFuture<ResponseT> fetchNextPage() {
        return nextPageFetcher.nextPage(currentPage)
                              .whenComplete((response, error) -> {
                                  if (response != null) {
                                      currentPage = response;
                                      singlePageItemsIterator = getIteratorFunction.apply(response);
                                  } else if (error != null) {
                                      subscriber.onError(error);
                                      cleanup();
                                  }
                              });
    }

    // Conditions when to fetch the next page:
    //  - We're NOT already waiting for a new page AND either
    //    - We still need to fetch the first page OR
    //    - We've exhausted the current page AND there is a next page available
    private boolean shouldFetchNextPage() {
        if (awaitingNewPage) {
            return false;
        }

        // Current page is null only the first time the method is called.
        // Once initialized, current page will never be null.
        return currentPage == null || (!hasMoreItems() && hasNextPage());
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
