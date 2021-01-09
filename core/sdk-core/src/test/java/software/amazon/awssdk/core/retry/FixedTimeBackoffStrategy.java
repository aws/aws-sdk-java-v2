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

package software.amazon.awssdk.core.retry;

import java.time.Duration;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;

/**
 * Test implementation of {@link BackoffStrategy} to wait a fixed time between retries
 */
public class FixedTimeBackoffStrategy implements BackoffStrategy {

    private final Duration fixedTimeDelay;

    public FixedTimeBackoffStrategy(Duration fixedTimeDelay) {
        this.fixedTimeDelay = fixedTimeDelay;
    }

    @Override
    public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
        return this.fixedTimeDelay;
    }

}
