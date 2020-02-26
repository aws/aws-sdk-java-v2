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

package software.amazon.awssdk.core.retry.backoff;

import org.junit.Test;
import org.mockito.Mock;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

import java.time.Duration;
import java.util.Random;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

public class EqualJitterBackoffStrategyTest {

    private static final int RANDOM_RESULT = 12345;
    private static final Duration FIVE_DAYS = Duration.ofDays(5);
    private static final Duration MAX_DURATION = Duration.ofSeconds(Long.MAX_VALUE);
    private static final Duration ONE_SECOND = Duration.ofSeconds(1);
    private static final Duration ONE_NANO_SECOND = Duration.ofNanos(1);
    private static final int NANO_IN_MILLISECONDS = 1_000_000;
    private static final Duration NEGATIVE_ONE_SECOND = Duration.ofSeconds(-1);

    @Mock
    private Random mockRandom = mock(Random.class);

    @Test
    public void GIVEN_exponentialDelayAboveCeiling_THEN_return_JitteredCeiling() {
        test(FIVE_DAYS, MAX_DURATION, 3, Integer.MAX_VALUE);
    }

    @Test
    public void GIVEN_maxBaseDelay_THEN_return_JitteredCeiling() {
        test(MAX_DURATION, MAX_DURATION, 1, Integer.MAX_VALUE);
    }

    @Test
    public void GIVEN_oneSecondMaxBackoff_THEN_return_JitteredOneSecond() {
        test(MAX_DURATION, ONE_SECOND, 1, (int) ONE_SECOND.toMillis());
    }

    @Test
    public void GIVEN_exponentialDelayBelowCeiling_THEN_return_CorrectJitteredExponentialDelay() {
        test(ONE_SECOND, MAX_DURATION, 10, (1 << 10) * (int) ONE_SECOND.toMillis());
    }

    @Test
    public void GIVEN_oneNanoSecondMaxBackoff_THEN_return_JitteredZero() {
        test(MAX_DURATION, ONE_NANO_SECOND, 10, 0);
    }

    @Test
    public void GIVEN_oneNanoSecondBaseDelay_THEN_return_CorrectJitteredExponentialDelay() {
        test(ONE_NANO_SECOND, MAX_DURATION, 30,  (int) (1L << 30) / NANO_IN_MILLISECONDS);
    }

    @Test
    public void GIVEN_tooManyRetries_THEN_return_CorrectResultForCappedRetries() {
        test(ONE_NANO_SECOND, MAX_DURATION, 100,
                (int) (1L << 30) / NANO_IN_MILLISECONDS); // RETRIES_ATTEMPTED_CEILING == 30
    }

    @Test
    public void GIVEN_tooManyRetriesWithMaxBaseDelay() {
        test(MAX_DURATION, MAX_DURATION, 100, Integer.MAX_VALUE);
    }

    @Test
    public void GIVEN_oneNanoSecondExponentialDelay_THEN_returnJitteredZero() {
        test(ONE_NANO_SECOND, MAX_DURATION, 0, 0);
    }

    @Test
    public void GIVEN_negativeBaseDelay_Then_throw() {
        assertThrows(IllegalArgumentException.class, () ->
                test(NEGATIVE_ONE_SECOND, MAX_DURATION, 1, 0));
    }

    @Test
    public void GIVEN_negativeMaxBackoff_Then_throw() {
        assertThrows(IllegalArgumentException.class, () ->
                test(ONE_SECOND, NEGATIVE_ONE_SECOND, 1, 0));
    }

    private void test(final Duration baseDelay, final Duration maxBackoffTime,
                      final int retriesAttempted, final int expectedCeilingMillis) {
        final BackoffStrategy backoffStrategy = new EqualJitterBackoffStrategy(baseDelay, maxBackoffTime, mockRandom);

        when(mockRandom.nextInt(expectedCeilingMillis /2 + 1)).thenReturn(RANDOM_RESULT);

        assertThat(backoffStrategy.computeDelayBeforeNextRetry(toRetryContext(retriesAttempted)),
                is(Duration.ofMillis(expectedCeilingMillis / 2 + RANDOM_RESULT)));
    }

    private static RetryPolicyContext toRetryContext(final int retriesAttempted) {
        return RetryPolicyContext.builder().retriesAttempted(retriesAttempted).build();
    }

}