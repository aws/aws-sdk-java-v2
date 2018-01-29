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
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;

public class AndRetryConditionTest {
    static private final RetryCondition retryTrue = makeRetryCondition(true);
    static private final RetryCondition retryFalse = makeRetryCondition(false);

    @Test
    public void allFalseConditions_ReturnsFalse() {
        assertFalse(new AndRetryCondition(retryFalse, retryFalse)
                .shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void onlyFirstConditionIsTrue_ReturnsFalse() {
        assertFalse(new AndRetryCondition(retryTrue, retryFalse)
                .shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void onlySecondConditionIsTrue_ReturnsFalse() {
        assertFalse(new AndRetryCondition(retryFalse, retryTrue)
                .shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void bothConditionsAreTrue_ReturnsTrue() {
        assertTrue(new AndRetryCondition(retryTrue, retryTrue)
                .shouldRetry(RetryPolicyContexts.EMPTY));
    }

    /**
     * The expected result for an AND condition with no conditions is a little unclear so we disallow it until there is a use
     * case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void noConditions_ThrowsException() {
        new AndRetryCondition().shouldRetry(RetryPolicyContexts.EMPTY);
    }

    @Test
    public void singleConditionThatReturnsTrue_ReturnsTrue() {
        assertTrue(new AndRetryCondition(retryTrue).shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void singleConditionThatReturnsFalse_ReturnsFalse() {
        assertFalse(new AndRetryCondition(retryFalse).shouldRetry(RetryPolicyContexts.EMPTY));
    }

    private static RetryCondition makeRetryCondition(final boolean b) {
        return new RetryCondition() {
            public boolean shouldRetry(RetryPolicyContext context) {
                return b;
            }
        };
    }
}
