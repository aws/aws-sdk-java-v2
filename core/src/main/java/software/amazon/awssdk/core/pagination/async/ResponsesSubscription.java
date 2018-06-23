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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.pagination.async.PaginationSubscription;

/**
 * An implementation of the {@link Subscription} interface that can be used to signal and cancel demand for
 * paginated response pages.
 *
 * @param <ResponseT> The type of a single response page
 */
@SdkProtectedApi
public class ResponsesSubscription<ResponseT> extends PaginationSubscription<ResponseT> {

    public ResponsesSubscription(Subscriber subscriber, AsyncPageFetcher<ResponseT> nextPageFetcher) {
        super(subscriber, nextPageFetcher);
    }

    protected void handleRequests() {
        if (!hasNextPage()) {
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
            outstandingRequests.getAndDecrement();
            nextPageFetcher.nextPage(currentPage)
                           .whenComplete(((response, error) -> {
                               if (response != null) {
                                   currentPage = response;
                                   subscriber.onNext(response);
                                   handleRequests();
                               }
                               if (error != null) {
                                   subscriber.onError(error);
                                   cleanup();
                               }
                           }));
        }
    }
}
