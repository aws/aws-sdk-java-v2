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

package software.amazon.awssdk.core.pagination;

import java.util.Iterator;
import java.util.NoSuchElementException;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Iterator for all response pages in a paginated operation.
 *
 * This class is used to iterate through all the pages of an operation.
 * SDK makes service calls to retrieve the next page when next() method is called.
 *
 * @param <ResponseT> The type of a single response page
 */
@SdkProtectedApi
public class PaginatedResponsesIterator<ResponseT> implements Iterator<ResponseT> {

    private final SyncPageFetcher<ResponseT> nextPageFetcher;

    // This is null when the object is created. It gets initialized in next() method
    // where SDK make service calls.
    private ResponseT oldResponse;

    public PaginatedResponsesIterator(SyncPageFetcher<ResponseT> nextPageFetcher) {
        this.nextPageFetcher = nextPageFetcher;
    }

    @Override
    public boolean hasNext() {
        return oldResponse == null || nextPageFetcher.hasNextPage(oldResponse);
    }

    @Override
    public ResponseT next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more pages left");
        }

        oldResponse = nextPageFetcher.nextPage(oldResponse);

        return oldResponse;
    }
}
