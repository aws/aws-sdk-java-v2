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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.utils.Validate;

/**
 * Retry condition implementation that retries if the HTTP status code matches one of the provided status codes.
 */
@SdkPublicApi
public final class RetryOnStatusCodeCondition implements RetryCondition {

    private final Set<Integer> statusCodesToRetryOn;

    private RetryOnStatusCodeCondition(Set<Integer> statusCodesToRetryOn) {
        this.statusCodesToRetryOn = new HashSet<>(
                Validate.paramNotNull(statusCodesToRetryOn, "statusCodesToRetryOn"));
    }

    /**
     * @param context Context about the state of the last request and information about the number of requests made.
     * @return True if the HTTP status code matches one of the provided status codes. False if it doesn't match or the request
     *     failed for reasons other than an exceptional HTTP response (i.e. IOException).
     */
    @Override
    public boolean shouldRetry(RetryPolicyContext context) {
        return Optional.ofNullable(context.httpStatusCode()).map(s ->
            statusCodesToRetryOn.stream().anyMatch(code -> code.equals(s))).orElse(false);
    }

    public static RetryOnStatusCodeCondition create(Set<Integer> statusCodesToRetryOn) {
        return new RetryOnStatusCodeCondition(statusCodesToRetryOn);
    }

    public static RetryOnStatusCodeCondition create(Integer... statusCodesToRetryOn) {
        return new RetryOnStatusCodeCondition(Arrays.stream(statusCodesToRetryOn).collect(Collectors.toSet()));
    }
}
