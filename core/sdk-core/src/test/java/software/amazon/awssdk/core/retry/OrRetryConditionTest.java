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

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

@RunWith(MockitoJUnitRunner.class)
public class OrRetryConditionTest {

    @Mock
    private RetryCondition conditionOne;

    @Mock
    private RetryCondition conditionTwo;

    private RetryCondition orCondition;

    @Before
    public void setup() {
        this.orCondition = OrRetryCondition.create(conditionOne, conditionTwo);
    }

    @Test
    public void allFalseConditions_ReturnsFalse() {
        assertFalse(orCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void firstConditionIsTrue_ReturnsTrue() {
        when(conditionOne.shouldRetry(any(RetryPolicyContext.class)))
                .thenReturn(true);
        assertTrue(orCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void secondConditionIsTrue_ReturnsTrue() {
        when(conditionTwo.shouldRetry(any(RetryPolicyContext.class)))
                .thenReturn(true);
        assertTrue(orCondition.shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void noConditions_ReturnsFalse() {
        assertFalse(OrRetryCondition.create().shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void singleConditionThatReturnsTrue_ReturnsTrue() {
        when(conditionOne.shouldRetry(RetryPolicyContexts.EMPTY))
                .thenReturn(true);
        assertTrue(OrRetryCondition.create(conditionOne).shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void singleConditionThatReturnsFalse_ReturnsFalse() {
        when(conditionOne.shouldRetry(RetryPolicyContexts.EMPTY))
                .thenReturn(false);
        assertFalse(OrRetryCondition.create(conditionOne).shouldRetry(RetryPolicyContexts.EMPTY));
    }

    @Test
    public void conditionsAreEvaluatedInOrder() {
        int numConditions = 1000;
        int firstTrueCondition = 500;

        RetryCondition[] conditions = new RetryCondition[numConditions];
        for (int i = 0; i < numConditions; ++i) {
            RetryCondition mock = Mockito.mock(RetryCondition.class);
            when(mock.shouldRetry(RetryPolicyContexts.EMPTY)).thenReturn(i == firstTrueCondition);
            conditions[i] = mock;
        }

        assertTrue(OrRetryCondition.create(conditions).shouldRetry(RetryPolicyContexts.EMPTY));

        for (int i = 0; i < numConditions; ++i) {
            int timesExpected = i <= firstTrueCondition ? 1 : 0;
            Mockito.verify(conditions[i], times(timesExpected)).shouldRetry(RetryPolicyContexts.EMPTY);
        }
    }

}
