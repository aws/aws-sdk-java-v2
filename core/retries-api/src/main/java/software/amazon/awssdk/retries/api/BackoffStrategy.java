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

package software.amazon.awssdk.retries.api;

import java.time.Duration;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Determines how long to wait before each execution attempt.
 */
@SdkPublicApi
@ThreadSafe
public interface BackoffStrategy {

    /**
     * Compute the amount of time to wait before the provided attempt number is executed.
     *
     * @param attempt The attempt to compute the delay for, starting at one.
     * @throws IllegalArgumentException If the given attempt is less or equal to zero.
     */
    Duration computeDelay(int attempt);
}
