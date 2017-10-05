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

import static software.amazon.awssdk.util.ValidationUtils.assertNotNull;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.retry.v2.RetryPolicyContext;

/**
 * Adapts a legacy {@link RetryPolicy} to the new {@link software.amazon.awssdk.retry.v2.RetryPolicy}. This class is
 * intended for internal use by the SDK.
 */
@SdkInternalApi
@ReviewBeforeRelease("Should be removed in retry policy refactor.")
public class RetryPolicyAdapter implements software.amazon.awssdk.retry.v2.RetryPolicy {

    private final RetryPolicy legacyRetryPolicy;

    public RetryPolicyAdapter(RetryPolicy legacyRetryPolicy) {
        this.legacyRetryPolicy = assertNotNull(legacyRetryPolicy, "legacyRetryPolicy");
    }

    @Override
    public long computeDelayBeforeNextRetry(RetryPolicyContext context) {
        return legacyRetryPolicy.getBackoffStrategy().delayBeforeNextRetry(
                (AmazonWebServiceRequest) context.originalRequest(),
                tryConvertException(context.exception()),
                context.retriesAttempted());
    }

    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        if (context.retriesAttempted() >= getMaxErrorRetry()) {
            return false;
        }
        return legacyRetryPolicy.getRetryCondition().shouldRetry(
                (AmazonWebServiceRequest) context.originalRequest(),
                tryConvertException(context.exception()),
                context.retriesAttempted());
    }

    private AmazonClientException tryConvertException(Exception e) {
        if (e instanceof AmazonClientException) {
            return (AmazonClientException) e;
        }
        return null;
    }


    public RetryPolicy getLegacyRetryPolicy() {
        return this.legacyRetryPolicy;
    }

    private int getMaxErrorRetry() {
        return legacyRetryPolicy.getMaxErrorRetry();
    }

}
