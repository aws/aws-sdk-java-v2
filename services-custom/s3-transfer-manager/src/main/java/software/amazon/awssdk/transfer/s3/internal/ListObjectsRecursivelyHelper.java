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

import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.core.pagination.async.AsyncPageFetcher;
import software.amazon.awssdk.core.pagination.async.PaginatedItemsPublisher;
import software.amazon.awssdk.core.util.PaginatorUtils;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Logger;

/**
 * A helper class that returns all objects within a bucket given a {@link ListObjectsV2Request} recursively.
 */
@SdkInternalApi
public class ListObjectsRecursivelyHelper {
    private static final Logger logger = Logger.loggerFor(ListObjectsRecursivelyHelper.class);
    private final Function<ListObjectsV2Request, CompletableFuture<ListObjectsV2Response>> listObjectsFunction;

    public ListObjectsRecursivelyHelper(Function<ListObjectsV2Request,
                                        CompletableFuture<ListObjectsV2Response>> listObjectsFunction) {

        this.listObjectsFunction = listObjectsFunction;
    }

    public SdkPublisher<S3Object> s3Objects(ListObjectsV2Request firstRequest) {
        Function<ListObjectsV2Response, Iterator<S3Object>> getIterator = response -> {
            if (response != null && !CollectionUtils.isNullOrEmpty(response.contents())) {
                return response.contents().stream().filter(r -> {
                    if (response.prefix().equals(r.key())) {
                        logger.debug(() -> "Skipping download for object (" + r.key() + ") since it is a virtual directory");
                        return false;
                    }

                    return true;
                }).iterator();
            }

            return Collections.emptyIterator();
        };

        return PaginatedItemsPublisher.builder().nextPageFetcher(new ListObjectsV2ResponseFetcher(firstRequest))
                                      .iteratorFunction(getIterator).isLastPage(false).build();
    }

    private final class ListObjectsV2ResponseFetcher implements AsyncPageFetcher<ListObjectsV2Response> {
        private final Deque<String> commonPrefixes = new ConcurrentLinkedDeque<>();
        private volatile ListObjectsV2Request firstRequest;

        private ListObjectsV2ResponseFetcher(ListObjectsV2Request firstRequest) {
            this.firstRequest = firstRequest;
        }

        @Override
        public boolean hasNextPage(ListObjectsV2Response previousPage) {
            return PaginatorUtils.isOutputTokenAvailable(previousPage.nextContinuationToken()) ||
                   !commonPrefixes.isEmpty();
        }

        @Override
        public CompletableFuture<ListObjectsV2Response> nextPage(ListObjectsV2Response previousPage) {
            CompletableFuture<ListObjectsV2Response> future;

            if (previousPage == null) {
                // If this is the first request
                future = listObjectsFunction.apply(firstRequest);
            } else if (PaginatorUtils.isOutputTokenAvailable(previousPage.nextContinuationToken())) {
                // If there is a next page with the same prefix
                future =
                    listObjectsFunction.apply(firstRequest.toBuilder()
                                                          .continuationToken(previousPage.nextContinuationToken())
                                                          .build());
            } else {
                // If there is no next page, we should start with the next common prefix
                String nextPrefix = commonPrefixes.pop();
                firstRequest = firstRequest.toBuilder().prefix(nextPrefix).build();
                future = listObjectsFunction.apply(firstRequest);
            }

            return future.thenApply(t -> {
                t.commonPrefixes().stream().map(CommonPrefix::prefix).forEach(commonPrefixes::push);
                return t;
            });
        }
    }
}
