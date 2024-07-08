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

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.internal.RetryStrategyCommonTest.TestCase;

/**
 * Tests that the configured circuit breaker for each of the strategies remembers
 * state across requests.
 */
class RetryStrategyCircuitBreakerRemembersStateTest {
    static final int TEST_EXCEPTION_COST = 5;
    static final int TEST_MAX = 50;
    static final IllegalArgumentException IAE = new IllegalArgumentException();

    @ParameterizedTest
    @MethodSource("parameters")
    void circuitBreakerRemembersState(Function<String, TestCase> defaultTestCaseSupplier) {
        BackoffStrategy backoff = BackoffStrategy.exponentialDelay(Duration.ofMillis(10), Duration.ofSeconds(25));
        TestCase testCase = defaultTestCaseSupplier.apply("circuit breaker remembers state")
                                                   .configure(b -> b.maxAttempts(3))
                                                   .configure(b -> b.retryOnException(IllegalArgumentException.class))
                                                   .configureTokenBucketExceptionCost(TEST_EXCEPTION_COST)
                                                   .configureTokenBucketMaxCapacity(TEST_MAX)
                                                   .givenExceptions(IAE, IAE);

        // The test case will throw twice and then succeed, so each run will withdraw 2 * TEST_EXCEPTION_COST and deposit back
        // TEST_EXCEPTION_COST.
        RetryStrategy strategy = testCase.builder.build();
        int total = TEST_MAX;
        for (int idx = 0; idx < 9; idx++) {
            String name = testCase.name + " round " + idx;
            TestCase.runTestCase(testCase, strategy);
            assertThat(testCase.thrown).as(name).isNull();
            assertThat(testCase.succeeded).as(name).isTrue();
            assertThat(testCase.token.capacityRemaining()).as(name).isEqualTo(total - TEST_EXCEPTION_COST);
            assertThat(testCase.token.state()).as(name).isEqualTo(DefaultRetryToken.TokenState.SUCCEEDED);
            total -= TEST_EXCEPTION_COST;
        }
        // The tokens have been exhausted, assert that the next call will fail.
        String name = testCase.name + " no more tokens available";
        TestCase.runTestCase(testCase, strategy);
        assertThat(testCase.thrown).as(name).isNotNull();
        assertThat(testCase.succeeded).as(name).isFalse();
        assertThat(testCase.token.capacityRemaining()).as(name).isZero();
        assertThat(testCase.token.state()).as(name).isEqualTo(DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED);
    }

    static List<Function<String, TestCase>> parameters() {
        return Arrays.asList(
            RetryStrategyCommonTest.TestCaseForLegacy::new,
            RetryStrategyCommonTest.TestCaseForStandard::new,
            RetryStrategyCommonTest.TestCaseForAdaptive::new);
    }
}
