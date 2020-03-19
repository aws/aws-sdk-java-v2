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

package software.amazon.awssdk.core.pagination.sync;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Iterable for the paginated items. This class can be used through iterate through
 * all the items across multiple pages until there is no more response from the service.
 *
 * @param <ResponseT> The type of a single response page
 * @param <ItemT> The type of paginated member in a response page
 */
@SdkProtectedApi
public final class PaginatedItemsIterable<ResponseT, ItemT> implements SdkIterable<ItemT> {

    private final SdkIterable<ResponseT> pagesIterable;
    private final Function<ResponseT, Iterator<ItemT>> getItemIterator;

    private PaginatedItemsIterable(BuilderImpl<ResponseT, ItemT> builder) {
        this.pagesIterable = builder.pagesIterable;
        this.getItemIterator = builder.itemIteratorFunction;
    }

    public static <R, T> Builder<R, T> builder() {
        return new BuilderImpl<>();
    }

    @Override
    public Iterator<ItemT> iterator() {
        return new ItemsIterator(pagesIterable.iterator());
    }

    private class ItemsIterator implements Iterator<ItemT> {

        private final Iterator<ResponseT> pagesIterator;
        private Iterator<ItemT> singlePageItemsIterator;

        ItemsIterator(final Iterator<ResponseT> pagesIterator) {
            this.pagesIterator = pagesIterator;
            this.singlePageItemsIterator = pagesIterator.hasNext() ? getItemIterator.apply(pagesIterator.next())
                                                                   : Collections.emptyIterator();
        }

        @Override
        public boolean hasNext() {
            while (!hasMoreItems() && pagesIterator.hasNext()) {
                singlePageItemsIterator = getItemIterator.apply(pagesIterator.next());
            }

            if (hasMoreItems()) {
                return true;
            }

            return false;
        }

        @Override
        public ItemT next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements left");
            }

            return singlePageItemsIterator.next();
        }

        private boolean hasMoreItems() {
            return singlePageItemsIterator.hasNext();
        }
    }

    public interface Builder<ResponseT, ItemT> {
        Builder<ResponseT, ItemT> pagesIterable(SdkIterable<ResponseT> sdkIterable);

        Builder<ResponseT, ItemT> itemIteratorFunction(Function<ResponseT, Iterator<ItemT>> itemIteratorFunction);

        PaginatedItemsIterable<ResponseT, ItemT> build();
    }

    private static final class BuilderImpl<ResponseT, ItemT> implements Builder<ResponseT, ItemT> {
        private SdkIterable<ResponseT> pagesIterable;
        private Function<ResponseT, Iterator<ItemT>> itemIteratorFunction;

        private BuilderImpl() {
        }

        @Override
        public Builder<ResponseT, ItemT> pagesIterable(SdkIterable<ResponseT> pagesIterable) {
            this.pagesIterable = pagesIterable;
            return this;
        }

        @Override
        public Builder<ResponseT, ItemT> itemIteratorFunction(Function<ResponseT, Iterator<ItemT>> itemIteratorFunction) {
            this.itemIteratorFunction = itemIteratorFunction;
            return this;
        }

        @Override
        public PaginatedItemsIterable<ResponseT, ItemT> build() {
            return new PaginatedItemsIterable<>(this);
        }
    }
}
