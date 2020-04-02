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

package software.amazon.awssdk.core.waiters;

import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class PollingStrategyContext {

    /**
     * Represents the original input of the operation.
     */
    private final Object originalRequest;

    /**
     * Represents the number of retries made so far
     */
    private final int retriesAttempted;

    /**
     * Constructs a new polling strategy context with the given
     * request and retries attempted required for custom polling
     */
    PollingStrategyContext(Object originalRequest, int retriesAttempted) {
        this.originalRequest = originalRequest;
        this.retriesAttempted = retriesAttempted;
    }

    /**
     * @return Original input of the operation.
     */
    public Object getOriginalRequest() {
        return originalRequest;
    }

    /**
     * @return Number of retries attempted
     */
    public int getRetriesAttempted() {
        return retriesAttempted;
    }

}
