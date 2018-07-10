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
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.internal.pagination.async.ItemsSubscription;

/**
 * A publisher to request for a stream of paginated items. The class can be used to request data for paginated items
 * across multiple pages.
 *
 * @param <ResponseT> The type of a single response page
 * @param <ItemT> The type of paginated member in a response page
 */
@SdkProtectedApi
public final class PaginatedItemsPublisher<ResponseT, ItemT> implements SdkPublisher<ItemT> {

    private final AsyncPageFetcher<ResponseT> nextPageFetcher;

    private final Function<ResponseT, Iterator<ItemT>> getIteratorFunction;

    private final boolean isLastPage;

    private PaginatedItemsPublisher(BuilderImpl builder) {
        this.nextPageFetcher = builder.nextPageFetcher;
        this.getIteratorFunction = builder.iteratorFunction;
        this.isLastPage = builder.isLastPage;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public void subscribe(Subscriber<? super ItemT> subscriber) {
        subscriber.onSubscribe(isLastPage ? new EmptySubscription(subscriber)
                                          : ItemsSubscription.builder()
                                                             .subscriber(subscriber)
                                                             .nextPageFetcher(nextPageFetcher)
                                                             .iteratorFunction(getIteratorFunction)
                                                             .build());
    }

    public interface Builder {
        Builder nextPageFetcher(AsyncPageFetcher nextPageFetcher);

        Builder iteratorFunction(Function iteratorFunction);

        Builder isLastPage(boolean isLastPage);

        PaginatedItemsPublisher build();
    }

    private static final class BuilderImpl implements Builder {
        private AsyncPageFetcher nextPageFetcher;
        private Function iteratorFunction;
        private boolean isLastPage;

        @Override
        public Builder nextPageFetcher(AsyncPageFetcher nextPageFetcher) {
            this.nextPageFetcher = nextPageFetcher;
            return this;
        }

        @Override
        public Builder iteratorFunction(Function iteratorFunction) {
            this.iteratorFunction = iteratorFunction;
            return this;
        }

        @Override
        public Builder isLastPage(boolean isLastPage) {
            this.isLastPage = isLastPage;
            return this;
        }

        @Override
        public PaginatedItemsPublisher build() {
            return new PaginatedItemsPublisher(this);
        }
    }
}
