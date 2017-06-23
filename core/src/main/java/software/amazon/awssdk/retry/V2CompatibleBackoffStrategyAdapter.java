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

package software.amazon.awssdk.retry;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.retry.v2.RetryPolicyContext;

/**
 * Adapts the legacy backoff strategy to the new v2 backoff strategy. Strategies that extend this can be used in both legacy and
 * v2 retry policies.
 */
@SdkInternalApi
abstract class V2CompatibleBackoffStrategyAdapter implements V2CompatibleBackoffStrategy {

    @Override
    public long delayBeforeNextRetry(AmazonWebServiceRequest originalRequest,
                                     AmazonClientException exception,
                                     int retriesAttempted) {
        return computeDelayBeforeNextRetry(RetryPolicyContext.builder()
                                                             .originalRequest(originalRequest)
                                                             .exception(exception)
                                                             .retriesAttempted(retriesAttempted)
                                                             .build());
    }
}
