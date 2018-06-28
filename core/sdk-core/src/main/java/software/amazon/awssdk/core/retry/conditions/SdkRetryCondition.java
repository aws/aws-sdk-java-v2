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

package software.amazon.awssdk.core.retry.conditions;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryUtils;

/**
 * Contains predefined {@link RetryCondition} provided by SDK.
 */
@SdkProtectedApi
public final class SdkRetryCondition {

    public static final RetryCondition DEFAULT = OrRetryCondition.create(
        RetryOnStatusCodeCondition.create(SdkDefaultRetrySetting.RETRYABLE_STATUS_CODES),
        RetryOnExceptionsCondition.create(SdkDefaultRetrySetting.RETRYABLE_EXCEPTIONS),
        c -> RetryUtils.isClockSkewException(c.exception()),
        c -> RetryUtils.isThrottlingException(c.exception()));

    public static final RetryCondition NONE = MaxNumberOfRetriesCondition.create(0);

    private SdkRetryCondition() {
    }
}
