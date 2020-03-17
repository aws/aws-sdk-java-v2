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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnStatusCodeCondition;

public class RetryOnStatusCodeConditionTest {

    private final RetryCondition condition = RetryOnStatusCodeCondition.create(Sets.newHashSet(404, 500, 513));

    @Test
    public void retryableStatusCode_ReturnsTrue() {
        assertTrue(condition.shouldRetry(RetryPolicyContexts.withStatusCode(404)));
    }

    @Test
    public void nonRetryableStatusCode_ReturnsTrue() {
        assertFalse(condition.shouldRetry(RetryPolicyContexts.withStatusCode(400)));
    }

    @Test
    public void noStatusCodeInContext_ReturnFalse() {
        assertFalse(condition.shouldRetry(RetryPolicyContexts.withStatusCode(null)));
    }

    @Test
    public void noStatusCodesInList_ReturnsFalse() {
        final RetryCondition noStatusCodes = RetryOnStatusCodeCondition.create(Collections.emptySet());
        assertFalse(noStatusCodes.shouldRetry(RetryPolicyContexts.withStatusCode(404)));
    }

    @Test(expected = NullPointerException.class)
    public void nullListOfStatusCodes_ThrowsException() {
        Set<Integer> nullSet = null;
        RetryOnStatusCodeCondition.create(nullSet);
    }

}
