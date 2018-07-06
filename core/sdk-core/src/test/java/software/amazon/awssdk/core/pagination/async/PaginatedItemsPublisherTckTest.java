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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * TCK verification test for {@link PaginatedItemsPublisher}.
 */
public class PaginatedItemsPublisherTckTest extends PublisherVerification<Long> {

    public PaginatedItemsPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long l) {
        Function<List<Long>, Iterator<Long>> getIterator = response -> response != null ? response.iterator()
                                                                                        : Collections.emptyIterator();

        return PaginatedItemsPublisher.builder()
                                      .nextPageFetcher(new PageFetcher(l, 5))
                                      .iteratorFunction(getIterator)
                                      .isLastPage(false)
                                      .build();
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        // It's not possible to initialize PaginatedItemsPublisher to a failed
        // state since we can only reach a failed state if we fail to fulfill a
        // request, e.g. because the service returned an error response.

        // return null to skip related tests
        return null;
    }

    /**
     * Simple {@link AsyncPageFetcher} that returns lists of longs as pages.
     */
    private static class PageFetcher implements AsyncPageFetcher<List<Long>> {
        private final long maxVal;
        private final long step;

        private PageFetcher(long maxVal, long step) {
            this.maxVal = maxVal;
            this.step = step;
        }

        @Override
        public boolean hasNextPage(List<Long> oldPage) {
            return (lastElement(oldPage)) < maxVal - 1;
        }

        @Override
        public CompletableFuture<List<Long>> nextPage(List<Long> oldPage) {
            long i = lastElement(oldPage) + 1;
            long j = Math.min(i + step, maxVal);
            List<Long> stream = LongStream.range(i, j).boxed().collect(Collectors.toList());
            return CompletableFuture.completedFuture(stream);
        }

        private long lastElement(List<Long> s) {
            // first page is always null
            if (s == null) return -1;
            return s.get(s.size() - 1);
        }
    }
}
