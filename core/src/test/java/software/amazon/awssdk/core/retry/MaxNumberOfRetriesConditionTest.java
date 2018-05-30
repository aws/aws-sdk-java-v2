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

package software.amazon.awssdk.core.retry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.core.retry.conditions.MaxNumberOfRetriesCondition;

public class MaxNumberOfRetriesConditionTest {

    @Test
    public void positiveMaxRetries_OneMoreAttemptToMax_ReturnsTrue() {
        assertTrue(MaxNumberOfRetriesCondition.create(3).shouldRetry(RetryPolicyContexts.withRetriesAttempted(2)));
    }

    @Test
    public void positiveMaxRetries_AtMaxAttempts_ReturnsFalse() {
        assertFalse(MaxNumberOfRetriesCondition.create(3).shouldRetry(RetryPolicyContexts.withRetriesAttempted(3)));
    }

    @Test
    public void positiveMaxRetries_PastMaxAttempts_ReturnsFalse() {
        assertFalse(MaxNumberOfRetriesCondition.create(3).shouldRetry(RetryPolicyContexts.withRetriesAttempted(4)));
    }
}
