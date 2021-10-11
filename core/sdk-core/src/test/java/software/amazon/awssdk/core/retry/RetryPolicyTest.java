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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;

@RunWith(MockitoJUnitRunner.class)
public class RetryPolicyTest {

    @Mock
    private RetryCondition retryCondition;

    @Mock
    private BackoffStrategy backoffStrategy;

    @Mock
    private BackoffStrategy throttlingBackoffStrategy;

    @Test
    public void nullConditionProvided_useDefault() {
        RetryPolicy policy = RetryPolicy.builder().build();
        RetryPolicy defaultRetryPolicy = RetryPolicy.defaultRetryPolicy();

        assertThat(policy).isEqualTo(defaultRetryPolicy);
        assertThat(policy.retryCondition()).isEqualTo(defaultRetryPolicy.retryCondition());
        assertThat(policy.backoffStrategy()).isEqualTo(BackoffStrategy.defaultStrategy());
        assertThat(policy.throttlingBackoffStrategy()).isEqualTo(BackoffStrategy.defaultThrottlingStrategy());
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

    @Test
    public void throttlingDelay_delegatesToThrottlingBackoffStrategy() {
        RetryPolicy policy = RetryPolicy.builder().throttlingBackoffStrategy(throttlingBackoffStrategy).build();
        policy.throttlingBackoffStrategy().computeDelayBeforeNextRetry(RetryPolicyContexts.EMPTY);
        verify(throttlingBackoffStrategy).computeDelayBeforeNextRetry(RetryPolicyContexts.EMPTY);
    }

    @Test
    public void nonRetryPolicy_shouldUseNullCondition() {
        RetryPolicy noneRetry = RetryPolicy.none();

        assertThat(noneRetry.retryCondition().shouldRetry(RetryPolicyContext.builder().build())).isFalse();
        assertThat(noneRetry.numRetries()).isZero();
        assertThat(noneRetry.backoffStrategy()).isEqualTo(BackoffStrategy.none());
        assertThat(noneRetry.throttlingBackoffStrategy()).isEqualTo(BackoffStrategy.none());
    }

    @Test
    public void nonRetryMode_shouldUseDefaultRetryMode() {
        RetryPolicy policy = RetryPolicy.builder().build();
        assertThat(policy.retryMode().toString()).isEqualTo("LEGACY");
    }

    @Test
    public void nullRetryMode_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> RetryPolicy.builder(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("retry mode cannot be set as null");
    }

    @Test
    public void maxRetriesFromRetryModeIsCorrect() {
        assertThat(RetryPolicy.forRetryMode(RetryMode.LEGACY).numRetries()).isEqualTo(3);
        assertThat(RetryPolicy.forRetryMode(RetryMode.STANDARD).numRetries()).isEqualTo(2);
    }

    @Test
    public void maxRetriesFromDefaultRetryModeIsCorrect() {
        switch (RetryMode.defaultRetryMode()) {
            case LEGACY:
                assertThat(RetryPolicy.defaultRetryPolicy().numRetries()).isEqualTo(3);
                assertThat(RetryPolicy.builder().build().numRetries()).isEqualTo(3);
                break;
            case STANDARD:
                assertThat(RetryPolicy.defaultRetryPolicy().numRetries()).isEqualTo(2);
                assertThat(RetryPolicy.builder().build().numRetries()).isEqualTo(2);
                break;
            default:
                Assert.fail();
        }
    }

    @Test
    public void legacyRetryMode_shouldUseFullJitterAndEqualJitter() {
        RetryPolicy legacyRetryPolicy = RetryPolicy.forRetryMode(RetryMode.LEGACY);

        assertThat(legacyRetryPolicy.backoffStrategy()).isInstanceOf(FullJitterBackoffStrategy.class);
        FullJitterBackoffStrategy backoffStrategy = (FullJitterBackoffStrategy) legacyRetryPolicy.backoffStrategy();
        assertThat(backoffStrategy.toBuilder().baseDelay()).isEqualTo(Duration.ofMillis(100));
        assertThat(backoffStrategy.toBuilder().maxBackoffTime()).isEqualTo(Duration.ofSeconds(20));

        assertThat(legacyRetryPolicy.throttlingBackoffStrategy()).isInstanceOf(EqualJitterBackoffStrategy.class);
        EqualJitterBackoffStrategy throttlingBackoffStrategy =
            (EqualJitterBackoffStrategy) legacyRetryPolicy.throttlingBackoffStrategy();
        assertThat(throttlingBackoffStrategy.toBuilder().baseDelay()).isEqualTo(Duration.ofMillis(500));
        assertThat(throttlingBackoffStrategy.toBuilder().maxBackoffTime()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    public void standardRetryMode_shouldUseFullJitterOnly() {
        RetryPolicy standardRetryPolicy = RetryPolicy.forRetryMode(RetryMode.STANDARD);

        assertThat(standardRetryPolicy.backoffStrategy()).isInstanceOf(FullJitterBackoffStrategy.class);
        FullJitterBackoffStrategy backoffStrategy = (FullJitterBackoffStrategy) standardRetryPolicy.backoffStrategy();
        assertThat(backoffStrategy.toBuilder().baseDelay()).isEqualTo(Duration.ofMillis(100));
        assertThat(backoffStrategy.toBuilder().maxBackoffTime()).isEqualTo(Duration.ofSeconds(20));

        assertThat(standardRetryPolicy.throttlingBackoffStrategy()).isInstanceOf(FullJitterBackoffStrategy.class);
        FullJitterBackoffStrategy throttlingBackoffStrategy =
            (FullJitterBackoffStrategy) standardRetryPolicy.throttlingBackoffStrategy();
        assertThat(throttlingBackoffStrategy.toBuilder().baseDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(throttlingBackoffStrategy.toBuilder().maxBackoffTime()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    public void adaptiveRetryMode_shouldUseFullJitterOnly() {
        RetryPolicy standardRetryPolicy = RetryPolicy.forRetryMode(RetryMode.ADAPTIVE);

        assertThat(standardRetryPolicy.backoffStrategy()).isInstanceOf(FullJitterBackoffStrategy.class);
        FullJitterBackoffStrategy backoffStrategy = (FullJitterBackoffStrategy) standardRetryPolicy.backoffStrategy();
        assertThat(backoffStrategy.toBuilder().baseDelay()).isEqualTo(Duration.ofMillis(100));
        assertThat(backoffStrategy.toBuilder().maxBackoffTime()).isEqualTo(Duration.ofSeconds(20));

        assertThat(standardRetryPolicy.throttlingBackoffStrategy()).isInstanceOf(FullJitterBackoffStrategy.class);
        FullJitterBackoffStrategy throttlingBackoffStrategy =
            (FullJitterBackoffStrategy) standardRetryPolicy.throttlingBackoffStrategy();
        assertThat(throttlingBackoffStrategy.toBuilder().baseDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(throttlingBackoffStrategy.toBuilder().maxBackoffTime()).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    public void fastFailRateLimitingConfigured_retryModeNotAdaptive_throws() {
        assertThatThrownBy(() -> RetryPolicy.builder(RetryMode.STANDARD).fastFailRateLimiting(true).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("only valid for the ADAPTIVE retry mode");
    }

    @Test
    public void fastFailRateLimitingConfigured_retryModeAdaptive_doesNotThrow() {
        RetryPolicy.builder(RetryMode.ADAPTIVE).fastFailRateLimiting(true).build();
    }

    @Test
    public void hashCodeDoesNotThrow() {
        RetryPolicy.defaultRetryPolicy().hashCode();
    }
}
