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

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public interface SyncPageFetcher<ResponseT> {

    /**
     * Returns a boolean value indicating if a next page is available.
     *
     * @param oldPage last page sent by service in a paginated operation
     * @return True if there is a next page available. Otherwise false.
     */
    boolean hasNextPage(ResponseT oldPage);

    /**
     * Method that uses the information in #oldPage and returns the
     * next page if available by making a service call.
     *
     * @param oldPage last page sent by service in a paginated operation
     * @return the next page if available. Otherwise returns null.
     */
    ResponseT nextPage(ResponseT oldPage);
}
