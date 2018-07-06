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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

/**
 * Retry condition implementation that retries if the exception or the cause of the exception matches the classes defined.
 */
@SdkPublicApi
public final class RetryOnExceptionsCondition implements RetryCondition {

    private final Set<Class<? extends Exception>> exceptionsToRetryOn;

    /**
     * @param exceptionsToRetryOn Exception classes to retry on.
     */
    private RetryOnExceptionsCondition(Set<Class<? extends Exception>> exceptionsToRetryOn) {
        this.exceptionsToRetryOn = new HashSet<>(exceptionsToRetryOn);
    }

    /**
     * @param context Context about the state of the last request and information about the number of requests made.
     * @return True if the exception class matches one of the whitelisted exceptions or if the cause of the exception matches the
     *     whitelisted exception.
     */
    @Override
    public boolean shouldRetry(RetryPolicyContext context) {

        SdkException exception = context.exception();
        if (exception == null) {
            return false;
        }

        //TODO: update equals to isAssignableFrom to match all sub classes of IOException
        Predicate<Class<? extends Exception>> isRetryableException =
            ex -> ex.equals(exception.getClass());

        Predicate<Class<? extends Exception>> hasRetrableCause =
            ex -> exception.getCause() != null && ex.equals(exception.getCause().getClass());

        return exceptionsToRetryOn.stream().anyMatch(isRetryableException.or(hasRetrableCause));
    }

    /**
     * @param exceptionsToRetryOn Exception classes to retry on.
     */
    public static RetryOnExceptionsCondition create(Set<Class<? extends Exception>> exceptionsToRetryOn) {
        return new RetryOnExceptionsCondition(exceptionsToRetryOn);
    }

    /**
     * @param exceptionsToRetryOn Exception classes to retry on.
     */
    public static RetryOnExceptionsCondition create(Class<? extends Exception>... exceptionsToRetryOn) {
        return new RetryOnExceptionsCondition(Arrays.stream(exceptionsToRetryOn).collect(Collectors.toSet()));
    }
}
