/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.waiters;

import software.amazon.awssdk.SdkRequest;
import software.amazon.awssdk.annotation.SdkProtectedApi;

@SdkProtectedApi
public class PollingStrategyContext {

    /**
     * Represents the original input of the operation.
     */
    private final SdkRequest<?, ?, ?> originalRequest;

    /**
     * Represents the number of retries made so far
     */
    private final int retriesAttempted;

    /**
     * Constructs a new polling strategy context with the given
     * request and retries attempted required for custom polling
     */
    PollingStrategyContext(SdkRequest<?, ?, ?> originalRequest, int retriesAttempted) {
        this.originalRequest = originalRequest;
        this.retriesAttempted = retriesAttempted;
    }

    /**
     * @return Original input of the operation.
     */
    public SdkRequest<?, ?, ?> getOriginalRequest() {
        return originalRequest;
    }

    /**
     * @return Number of retries attempted
     */
    public int getRetriesAttempted() {
        return retriesAttempted;
    }

}
