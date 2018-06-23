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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Interface to deal with async paginated responses.
 * @param <ResponseT> Type of Response
 */
@SdkProtectedApi
public interface AsyncPageFetcher<ResponseT> {

    /**
     * Returns a boolean value indicating if a next page is available.
     *
     * @param oldPage last page sent by service in a paginated operation
     * @return True if there is a next page available. Otherwise false.
     */
    boolean hasNextPage(ResponseT oldPage);

    /**
     * Method that uses the information in #oldPage and returns a
     * completable future for the next page. This method makes service calls.
     *
     * @param oldPage last page sent by service in a paginated operation
     * @return A CompletableFuture that can be used to get the next response page
     */
    CompletableFuture<ResponseT> nextPage(ResponseT oldPage);
}
