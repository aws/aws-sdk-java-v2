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

package software.amazon.awssdk.core.retry.backoff;

import static software.amazon.awssdk.utils.Validate.isNotNegative;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

/**
 * Simple backoff strategy that always uses a fixed delay for the delay before the next retry attempt.
 */
@SdkPublicApi
public final class FixedDelayBackoffStrategy implements BackoffStrategy {

    private final Duration fixedBackoff;

    private FixedDelayBackoffStrategy(Duration fixedBackoff) {
        this.fixedBackoff = isNotNegative(fixedBackoff, "fixedBackoff");
    }

    @Override
    public Duration computeDelayBeforeNextRetry(RetryPolicyContext context) {
        return fixedBackoff;
    }

    public static FixedDelayBackoffStrategy create(Duration fixedBackoff) {
        return new FixedDelayBackoffStrategy(fixedBackoff);
    }
}
