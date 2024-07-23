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

package software.amazon.awssdk.retries.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.retries.internal.RetryStrategyCommonTest.IAE;
import static software.amazon.awssdk.retries.internal.RetryStrategyCommonTest.TEST_BUCKET_CAPACITY;
import static software.amazon.awssdk.retries.internal.RetryStrategyCommonTest.TEST_EXCEPTION_COST;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.internal.RetryStrategyCommonTest.TestCase;
import software.amazon.awssdk.retries.internal.RetryStrategyCommonTest.TestCaseForLegacy;

class LegacyRetryStrategyTest {
    static final ThrottlingException THROTTLING = new ThrottlingException();
    static final long THROTTLING_BACKOFF_BASE = 17;
    static final long BACKOFF_BASE = 23;

    @ParameterizedTest
    @MethodSource("parameters")
    void testCase(TestCase testCase) {
        testCase.run();
        if (testCase.shouldSucceed) {
            assertThat(testCase.thrown)
                .as(testCase.name)
                .isNull();
        } else {
            assertThat(testCase.thrown)
                .as(testCase.name)
                .isNotNull();
        }
        assertThat(testCase.succeeded).as(testCase.name).isEqualTo(testCase.shouldSucceed);
        assertThat(testCase.token.capacityRemaining()).as(testCase.name).isEqualTo(testCase.expectedCapacity);
        assertThat(testCase.token.state()).as(testCase.name).isEqualTo(testCase.expectedState);
        if (testCase.expectedLastRecordedDelay != null) {
            assertThat(testCase.lastRecordedDelay).as(testCase.name).isEqualTo(testCase.expectedLastRecordedDelay);
        }
    }

    static List<TestCase> parameters() {
        return Arrays.asList(
            legacyTestCase("Does not withdraws capacity for throttling exception")
                .givenExceptions(THROTTLING, THROTTLING, THROTTLING)
                .expectCapacity(TEST_BUCKET_CAPACITY)
                .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                .expectLastRecordedDelay(Duration.ofSeconds(THROTTLING_BACKOFF_BASE * 3))
                .expectThrows()
            , legacyTestCase("Only withdraws capacity for non-throttling exception")
                .givenExceptions(THROTTLING, IAE, THROTTLING)
                .expectCapacity(TEST_BUCKET_CAPACITY - TEST_EXCEPTION_COST)
                .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                .expectLastRecordedDelay(Duration.ofSeconds(BACKOFF_BASE * 3))
                .expectThrows()
            , legacyTestCase("Uses throttling backoff strategy for throttling exceptions")
                .givenExceptions(THROTTLING, THROTTLING, THROTTLING)
                .expectCapacity(TEST_BUCKET_CAPACITY)
                .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                .expectLastRecordedDelay(Duration.ofSeconds(THROTTLING_BACKOFF_BASE * 3))
                .expectThrows()
            , legacyTestCase("Uses regular backoff strategy for non-throttling exceptions")
                .givenExceptions(THROTTLING, IAE, THROTTLING)
                .expectCapacity(TEST_BUCKET_CAPACITY - TEST_EXCEPTION_COST)
                .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                .expectLastRecordedDelay(Duration.ofSeconds(BACKOFF_BASE * 3))
                .expectThrows()
        );
    }

    static TestCaseForLegacy legacyTestCase(String name) {
        TestCaseForLegacy testCase = new TestCaseForLegacy(name);
        testCase.configureTreatAsThrottling(t -> t instanceof ThrottlingException)
                .configureThrottlingBackoffStrategy(ofBaseTimesAttempt(THROTTLING_BACKOFF_BASE))
                .configureBackoffStrategy(ofBaseTimesAttempt(BACKOFF_BASE))
                .configureTokenBucketExceptionCost(TEST_EXCEPTION_COST)
                .configureTokenBucketMaxCapacity(TEST_BUCKET_CAPACITY)
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(ThrottlingException.class))
                .configure(b -> b.retryOnException(IllegalArgumentException.class));
        return testCase;
    }

    static BackoffStrategy ofBaseTimesAttempt(long base) {
        return attempt -> Duration.ofSeconds(base * attempt);
    }

    static class ThrottlingException extends Exception {
    }
}
