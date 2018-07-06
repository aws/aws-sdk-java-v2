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

package software.amazon.awssdk.awscore.retry;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.internal.AwsErrorCode;
import software.amazon.awssdk.awscore.retry.conditions.RetryOnErrorCodeCondition;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

/**
 * Retry Policy used by clients when communicating with AWS services.
 */
@SdkPublicApi
public final class AwsRetryPolicy {

    private AwsRetryPolicy() {
    }

    public static RetryCondition defaultRetryCondition() {
        return OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                       RetryOnErrorCodeCondition.create(AwsErrorCode.RETRYABLE_ERROR_CODES));
    }

    public static RetryPolicy defaultRetryPolicy() {
        return RetryPolicy.defaultRetryPolicy().toBuilder().retryCondition(defaultRetryCondition()).build();
    }
}
