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

package software.amazon.awssdk.regions.util;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Custom retry policy that retrieve information from a local endpoint in EC2 host.
 *
 * Internal use only.
 */
@SdkProtectedApi
public interface ResourcesEndpointRetryPolicy {
    ResourcesEndpointRetryPolicy NO_RETRY = (retriesAttempted, retryParams) -> false;

    /**
     * Returns whether a failed request should be retried.
     *
     * @param retriesAttempted
     *            The number of times the current request has been
     *            attempted.
     *
     * @return True if the failed request should be retried.
     */
    boolean shouldRetry(int retriesAttempted, ResourcesEndpointRetryParameters retryParams);
}
