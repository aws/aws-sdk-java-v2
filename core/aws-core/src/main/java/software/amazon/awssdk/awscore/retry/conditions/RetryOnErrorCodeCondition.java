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

package software.amazon.awssdk.awscore.retry.conditions;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

/**
 * Retry condition implementation that retries if the exception or the cause of the exception matches the error codes defined.
 */
@SdkPublicApi
public final class RetryOnErrorCodeCondition implements RetryCondition {

    private final Set<String> retryableErrorCodes;

    private RetryOnErrorCodeCondition(Set<String> retryableErrorCodes) {
        this.retryableErrorCodes = retryableErrorCodes;
    }

    @Override
    public boolean shouldRetry(RetryPolicyContext context) {

        Exception ex = context.exception();
        if (ex instanceof SdkServiceException) {
            SdkServiceException exception = (SdkServiceException) ex;

            return retryableErrorCodes.contains(exception.errorCode());
        }
        return false;
    }

    public static RetryOnErrorCodeCondition create(String... retryableErrorCodes) {
        return new RetryOnErrorCodeCondition(Arrays.stream(retryableErrorCodes).collect(Collectors.toSet()));
    }

    public static RetryOnErrorCodeCondition create(Set<String> retryableErrorCodes) {
        return new RetryOnErrorCodeCondition(retryableErrorCodes);
    }
}
