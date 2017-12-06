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

package utils.retry;

import java.time.Duration;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;

/**
 * Backoff strategy used in tests to pull backoff value from a backing array. Number of retries is
 * limited to size of array.
 */
public final class SimpleArrayBackoffStrategy implements BackoffStrategy {

    private final int[] backoffValues;

    public SimpleArrayBackoffStrategy(int[] backoffValues) {
        this.backoffValues = backoffValues;
    }

    @Override
    public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
        return Duration.ofMillis(backoffValues[context.retriesAttempted()]);
    }
}