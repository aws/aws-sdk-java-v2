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

package software.amazon.awssdk.core.internal.waiters;

import java.time.Duration;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.retries.api.BackoffStrategy;

@SdkInternalApi
public class LegacyToNonLegacyAdapter implements BackoffStrategy {
    private final software.amazon.awssdk.core.retry.backoff.BackoffStrategy adaptee;

    public LegacyToNonLegacyAdapter(software.amazon.awssdk.core.retry.backoff.BackoffStrategy adaptee) {
        this.adaptee = Objects.requireNonNull(adaptee);
    }

    @Override
    public Duration computeDelay(int attempt) {
        return adaptee.computeDelayBeforeNextRetry(RetryPolicyContext.builder()
                                                                     .retriesAttempted(attempt - 2)
                                                                     .build());
    }

    public software.amazon.awssdk.core.retry.backoff.BackoffStrategy adaptee() {
        return adaptee;
    }
}
