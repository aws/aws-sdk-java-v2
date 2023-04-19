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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.api.internal.AcquireInitialTokenRequestImpl;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;

public class StandardRetryStrategyTest {
    static final int TEST_BUCKET_CAPACITY = 100;
    static final int TEST_EXCEPTION_COST = 5;
    static final IllegalArgumentException IAE = new IllegalArgumentException();
    static final RuntimeException RTE = new RuntimeException();

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCase(TestCase testCase) {
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

    }

    public static Collection<TestCase> parameters() {
        BackoffStrategy backoff = BackoffStrategy.exponentialDelay(Duration.ofMillis(10), Duration.ofSeconds(25));
        return Arrays.asList(
            new TestCase("Succeeds when no exceptions are thrown")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .expectCapacity(TEST_BUCKET_CAPACITY)
                .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                .expectSuccess()
            , new TestCase("Succeeds when 1 exception is thrown out max of 3")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .givenExceptions(IAE)
                // Acquire cost and then return cost
                .expectCapacity(TEST_BUCKET_CAPACITY)
                .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                .expectSuccess()
            , new TestCase("Succeeds when 2 exceptions are thrown out max of 3")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .givenExceptions(IAE, IAE)
                // Acquire (cost * 2) and then return cost
                .expectCapacity(TEST_BUCKET_CAPACITY - TEST_EXCEPTION_COST)
                .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                .expectSuccess()
            , new TestCase("Fails when 3 exceptions are thrown out max of 3")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .givenExceptions(IAE, IAE, IAE)
                // Acquire (cost * 3) and then return zero
                .expectCapacity(TEST_BUCKET_CAPACITY - (TEST_EXCEPTION_COST * 3))
                .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                .expectThrows()
            , new TestCase("Fails when 4 exceptions are thrown out max of 3")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .givenExceptions(IAE, IAE, IAE, IAE)
                // Acquire (cost * 3) and then return zero
                .expectCapacity(TEST_BUCKET_CAPACITY - (TEST_EXCEPTION_COST * 3))
                .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                .expectThrows()
            , new TestCase("Fails when non-retryable exception throw in the 1st attempt")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .givenExceptions(RTE)
                // Acquire (cost * 1) and then return zero
                .expectCapacity(TEST_BUCKET_CAPACITY - TEST_EXCEPTION_COST)
                .expectState(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                .expectThrows()
            , new TestCase("Fails when non-retryable exception throw in the 2nd attempt")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .givenExceptions(IAE, RTE)
                // Acquire (cost * 1) and then return zero
                .expectCapacity(TEST_BUCKET_CAPACITY - (TEST_EXCEPTION_COST * 2))
                .expectState(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                .expectThrows()
            , new TestCase("Exhausts the token bucket.")
                .configure(b -> b.maxAttempts(5))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                .fineTune(b -> b.tokenBucketStore(TokenBucketStore
                                                      .builder()
                                                      .tokenBucketMaxCapacity(10)
                                                      .build()))
                .givenExceptions(IAE, IAE, IAE)
                .expectCapacity(0)
                .expectState(DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED)
                .expectThrows()
            , new TestCase("Succeeds when 2 exceptions are thrown out max of 3")
                .configure(b -> b.maxAttempts(3))
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .configure(b -> b.backoffStrategy(backoff))
                // Setting exception cost to ZERO disables the circuit-breaker
                .fineTune(b -> b.tokenBucketExceptionCost(0))
                .givenExceptions(IAE, IAE)
                // Acquired zero, capacity must be unchanged.
                .expectCapacity(TEST_BUCKET_CAPACITY)
                .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                .expectSuccess()
        );
    }


    static class TestCase {
        final String name;
        int attempts = 0;
        String scope = "none";
        List<Exception> exceptions = new ArrayList<>();
        StandardRetryStrategyImpl.Builder builder =
            (StandardRetryStrategyImpl.Builder)
                DefaultRetryStrategy.standardStrategyBuilder();
        Throwable thrown;
        boolean shouldSucceed = false;
        boolean succeeded;
        Integer expectedCapacity;
        DefaultRetryToken.TokenState expectedState;
        DefaultRetryToken token;

        TestCase(String name) {
            this.name = name;
            builder = builder.tokenBucketExceptionCost(TEST_EXCEPTION_COST)
                             .tokenBucketStore(TokenBucketStore
                                                   .builder()
                                                   .tokenBucketMaxCapacity(TEST_BUCKET_CAPACITY)
                                                   .build());
        }

        public TestCase fineTune(Function<StandardRetryStrategyImpl.Builder,
            StandardRetryStrategyImpl.Builder> configurator) {
            this.builder = configurator.apply(this.builder);
            return this;
        }

        public TestCase configure(Function<StandardRetryStrategy.Builder,
            StandardRetryStrategy.Builder> configurator) {
            this.builder = (StandardRetryStrategyImpl.Builder) configurator.apply(this.builder);
            return this;
        }

        public TestCase givenExceptions(Exception... exceptions) {
            Collections.addAll(this.exceptions, exceptions);
            return this;
        }

        public TestCase expectSuccess() {
            this.shouldSucceed = true;
            return this;
        }

        public TestCase expectThrows() {
            this.shouldSucceed = false;
            return this;
        }

        public TestCase expectCapacity(Integer expectedCapacity) {
            this.expectedCapacity = expectedCapacity;
            return this;
        }

        public TestCase expectState(DefaultRetryToken.TokenState expectedState) {
            this.expectedState = expectedState;
            return this;
        }

        public void run() {
            StandardRetryStrategy strategy = builder.build();
            runTestCase(this, strategy);
        }

        public static void runTestCase(TestCase testCase, StandardRetryStrategy strategy) {
            AcquireInitialTokenResponse res = strategy.acquireInitialToken(AcquireInitialTokenRequestImpl.create(testCase.scope));
            RetryToken token = res.token();
            testCase.succeeded = false;
            BusinessLogic logic = new BusinessLogic(testCase.exceptions);
            try {
                while (!testCase.succeeded) {
                    try {
                        logic.call();
                        testCase.succeeded = true;
                        RecordSuccessResponse response = strategy.recordSuccess(RecordSuccessRequest.create(token));
                        token = response.token();
                        testCase.token = (DefaultRetryToken) token;
                    } catch (Exception e) {
                        RefreshRetryTokenResponse refreshResponse =
                            strategy.refreshRetryToken(RefreshRetryTokenRequest.builder()
                                                                               .token(token)
                                                                               .failure(e)
                                                                               .build());
                        token = refreshResponse.token();
                    }
                }
            } catch (TokenAcquisitionFailedException e) {
                testCase.thrown = e;
                testCase.succeeded = false;
                testCase.token = (DefaultRetryToken) e.token();
            }
        }
    }

    static class BusinessLogic implements Callable<Integer> {
        List<Exception> exceptions;
        int invocation = 0;

        BusinessLogic(List<Exception> exceptions) {
            this.exceptions = exceptions;
        }

        @Override
        public Integer call() throws Exception {
            if (invocation < exceptions.size()) {
                throw exceptions.get(invocation++);
            }
            invocation++;
            return invocation;
        }
    }
}
