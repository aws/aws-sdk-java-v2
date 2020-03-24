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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.retry.conditions.AndRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

@RunWith(MockitoJUnitRunner.class)
public class AndRetryConditionTest {

    @Mock
    private RetryCondition conditionOne;

    @Mock
    private RetryCondition conditionTwo;

    private RetryCondition andCondition;

    @Before
    public void setup() {
        andCondition = AndRetryCondition.create(conditionOne, conditionTwo);
    }

    @Test
    public void allFalseConditions_ReturnsFalse() {
        assertFalse(andCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void onlyFirstConditionIsTrue_ReturnsFalse() {
        when(conditionOne.shouldRetry(any(RetryPolicyContext.class)))
                .thenReturn(true);
        assertFalse(andCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void onlySecondConditionIsTrue_ReturnsFalse() {
        when(conditionTwo.shouldRetry(any(RetryPolicyContext.class)))
                .thenReturn(true);
        assertFalse(andCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void bothConditionsAreTrue_ReturnsTrue() {
        when(conditionOne.shouldRetry(any(RetryPolicyContext.class)))
                .thenReturn(true);
        when(conditionTwo.shouldRetry(any(RetryPolicyContext.class)))
                .thenReturn(true);
        assertTrue(andCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    /**
     * The expected result for an AND condition with no conditions is a little unclear so we disallow it until there is a use
     * case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void noConditions_ThrowsException() {
        AndRetryCondition.create().shouldRetry(RetryPolicyContexts.EMPTY);
    }

    @Test
    public void singleConditionThatReturnsTrue_ReturnsTrue() {
        when(conditionOne.shouldRetry(RetryPolicyContexts.EMPTY))
                .thenReturn(true);
        assertTrue(AndRetryCondition.create(conditionOne).shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void singleConditionThatReturnsFalse_ReturnsFalse() {
        when(conditionOne.shouldRetry(RetryPolicyContexts.EMPTY))
                .thenReturn(false);
        assertFalse(AndRetryCondition.create(conditionOne).shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void conditionsAreEvaluatedInOrder() {
        int numConditions = 1000;
        int firstFalseCondition = 500;

        RetryCondition[] conditions = new RetryCondition[numConditions];
        for (int i = 0; i < numConditions; ++i) {
            RetryCondition mock = Mockito.mock(RetryCondition.class);
            when(mock.shouldRetry(RetryPolicyContexts.EMPTY)).thenReturn(i != firstFalseCondition);
            conditions[i] = mock;
        }

        assertFalse(AndRetryCondition.create(conditions).shouldRetry(RetryPolicyContexts.EMPTY));

        for (int i = 0; i < numConditions; ++i) {
            int timesExpected = i <= firstFalseCondition ? 1 : 0;
            Mockito.verify(conditions[i], times(timesExpected)).shouldRetry(RetryPolicyContexts.EMPTY);
        }
    }
}
