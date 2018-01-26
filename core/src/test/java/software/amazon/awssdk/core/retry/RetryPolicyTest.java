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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

@RunWith(MockitoJUnitRunner.class)
public class RetryPolicyTest {

    @Mock
    private RetryCondition retryCondition;

    @Mock
    private BackoffStrategy backoffStrategy;

    public void nullRetryCondition_UsesDefaultRetryCondition() {
        RetryPolicy policy = RetryPolicy.builder().retryCondition(null).backoffStrategy(backoffStrategy).build();

        assertThat(policy.toBuilder().retryCondition()).isEqualToComparingFieldByField(RetryCondition.DEFAULT);
    }

    public void nullBackoffStrategy_UsesDefaultBackoffStrategy() {
        RetryPolicy policy = RetryPolicy.builder().retryCondition(retryCondition).backoffStrategy(backoffStrategy).build();
        assertThat(policy.toBuilder().backoffStrategy()).isEqualToComparingFieldByField(BackoffStrategy.defaultStrategy());
    }

    @Test
    public void shouldRetry_DelegatesToRetryCondition() {
        RetryPolicy policy = RetryPolicy.builder().retryCondition(retryCondition).backoffStrategy(backoffStrategy).build();
        policy.retryCondition().shouldRetry(RetryPolicyContexts.EMPTY);

        verify(retryCondition).shouldRetry(RetryPolicyContexts.EMPTY);
    }

    @Test
    public void delay_DelegatesToBackoffStrategy() {
        RetryPolicy policy = RetryPolicy.builder().retryCondition(retryCondition).backoffStrategy(backoffStrategy).build();
        policy.backoffStrategy().computeDelayBeforeNextRetry(RetryPolicyContexts.EMPTY);

        verify(backoffStrategy).computeDelayBeforeNextRetry(RetryPolicyContexts.EMPTY);

    }

}
