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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RecordSuccessRequest;
import software.amazon.awssdk.retries.api.RecordSuccessResponse;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenResponse;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.api.internal.AcquireInitialTokenRequestImpl;
import software.amazon.awssdk.retries.internal.circuitbreaker.TokenBucketStore;

/**
 * Test cases common that all retries strategies should satisfy.
 */
class RetryStrategyCommonTest {

    static final int TEST_BUCKET_CAPACITY = 100;
    static final int TEST_EXCEPTION_COST = 5;
    static final IllegalArgumentException IAE = new IllegalArgumentException();
    static final RuntimeException RTE = new RuntimeException();

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
    }

    static Stream<TestCase> parameters() {
        return Stream.concat(
            Stream.concat(buildCases(TestCaseForLegacy::new),
                          buildCases(TestCaseForStandard::new)),
            buildCases(TestCaseForAdaptive::new));
    }

    static Stream<TestCase> buildCases(Function<String, TestCase> defaultTestCaseSupplier) {
        // Configure with well-known values to be able to assert on these without relaying any configured defaults.
        Function<String, TestCase> testCaseSupplier =
            defaultTestCaseSupplier.andThen(t -> t.configureTokenBucketExceptionCost(TEST_EXCEPTION_COST)
                                                  .configureTokenBucketMaxCapacity(TEST_BUCKET_CAPACITY));
        return Stream.of(
            testCaseSupplier.apply("Succeeds when no exceptions are thrown")
                            .configure(b -> b.maxAttempts(3))
                            .configure(b -> b.retryOnException(IllegalArgumentException.class))
                            .expectCapacity(TEST_BUCKET_CAPACITY)
                            .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                            .expectSuccess()
            , testCaseSupplier.apply("Succeeds when 1 exception is thrown out max of 3")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .givenExceptions(IAE)
                              // Acquire cost and then return cost
                              .expectCapacity(TEST_BUCKET_CAPACITY)
                              .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                              .expectSuccess()
            , testCaseSupplier.apply("Succeeds when 2 exceptions are thrown out max of 3")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .givenExceptions(IAE, IAE)
                              // Acquire (cost * 2) and then return cost
                              .expectCapacity(TEST_BUCKET_CAPACITY - TEST_EXCEPTION_COST)
                              .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                              .expectSuccess()
            , testCaseSupplier.apply("Fails when 3 exceptions are thrown out max of 3")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .givenExceptions(IAE, IAE, IAE)
                              // Acquire (cost * 2)
                              .expectCapacity(TEST_BUCKET_CAPACITY - (TEST_EXCEPTION_COST * 2))
                              .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                              .expectThrows()
            , testCaseSupplier.apply("Fails when 4 exceptions are thrown out max of 3")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .givenExceptions(IAE, IAE, IAE, IAE)
                              // Acquire (cost * 2)
                              .expectCapacity(TEST_BUCKET_CAPACITY - (TEST_EXCEPTION_COST * 2))
                              .expectState(DefaultRetryToken.TokenState.MAX_RETRIES_REACHED)
                              .expectThrows()
            , testCaseSupplier.apply("Fails when non-retryable exception throw in the 1st attempt")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .givenExceptions(RTE)
                              // Acquire (cost * 1)
                              .expectCapacity(TEST_BUCKET_CAPACITY)
                              .expectState(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                              .expectThrows()
            , testCaseSupplier.apply("Fails when non-retryable exception throw in the 2nd attempt")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .givenExceptions(IAE, RTE)
                              // Acquire (cost * 1)
                              .expectCapacity(TEST_BUCKET_CAPACITY - TEST_EXCEPTION_COST)
                              .expectState(DefaultRetryToken.TokenState.NON_RETRYABLE_EXCEPTION)
                              .expectThrows()
            , testCaseSupplier.apply("Exhausts the token bucket.")
                              .configure(b -> b.maxAttempts(5))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .configureTokenBucketMaxCapacity(10)
                              .givenExceptions(IAE, IAE, IAE)
                              .expectCapacity(0)
                              .expectState(DefaultRetryToken.TokenState.TOKEN_ACQUISITION_FAILED)
                              .expectThrows()
            , testCaseSupplier.apply("Succeeds when 2 exceptions are thrown out max of 3")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              // Setting exception cost to ZERO disables the circuit-breaker
                              .configureTokenBucketExceptionCost(0)
                              .givenExceptions(IAE, IAE)
                              // Acquired zero, capacity must be unchanged.
                              .expectCapacity(TEST_BUCKET_CAPACITY)
                              .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                              .expectSuccess()
            , testCaseSupplier.apply("Succeeds when 2 exceptions are thrown out max of 3")
                              .configure(b -> b.maxAttempts(3))
                              .configure(b -> b.retryOnException(IllegalArgumentException.class))
                              .configureCircuitBreakerEnabled(false)
                              .givenExceptions(IAE, IAE)
                              // Acquired zero, capacity must be unchanged.
                              .expectCapacity(TEST_BUCKET_CAPACITY)
                              .expectState(DefaultRetryToken.TokenState.SUCCEEDED)
                              .expectSuccess()
        );
    }


    abstract static class TestCase {
        final String name;
        int attempts = 0;
        String scope = "none";
        List<Exception> exceptions = new ArrayList<>();
        RetryStrategy.Builder<?, ?> builder;
        Throwable thrown;
        boolean shouldSucceed = false;
        boolean succeeded;
        Integer expectedCapacity;
        DefaultRetryToken.TokenState expectedState;
        Duration expectedLastRecordedDelay;
        DefaultRetryToken token;
        Duration lastRecordedDelay;

        TestCase(String name, RetryStrategy.Builder<?, ?> builder) {
            this.name = name;
            this.builder = builder;
        }


        public TestCase configure(Function<RetryStrategy.Builder<?, ?>,
            RetryStrategy.Builder<?, ?>> configurator) {
            this.builder = configurator.apply(this.builder);
            return this;
        }

        public abstract TestCase configureTokenBucketMaxCapacity(int maxCapacity);

        public abstract TestCase configureTokenBucketExceptionCost(int exceptionCost);

        public abstract TestCase configureCircuitBreakerEnabled(boolean enabled);

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

        public TestCase expectLastRecordedDelay(Duration delay) {
            this.expectedLastRecordedDelay = delay;
            return this;
        }

        public void run() {
            RetryStrategy<?, ?> strategy = builder.build();
            runTestCase(this, strategy);
        }

        public static void runTestCase(TestCase testCase, RetryStrategy<?, ?> strategy) {
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
                        testCase.lastRecordedDelay = refreshResponse.delay();
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

    static class TestCaseForStandard extends TestCase {

        TestCaseForStandard(String name) {
            super("TestCaseForStandard:: " + name, DefaultRetryStrategy.standardStrategyBuilder());
        }

        @Override
        public TestCase configureTokenBucketMaxCapacity(int maxCapacity) {
            ((DefaultStandardRetryStrategy2.Builder) builder).tokenBucketStore(
                TokenBucketStore
                    .builder()
                    .tokenBucketMaxCapacity(maxCapacity)
                    .build());
            return this;
        }

        @Override
        public TestCase configureTokenBucketExceptionCost(int exceptionCost) {
            ((DefaultStandardRetryStrategy2.Builder) builder).tokenBucketExceptionCost(exceptionCost);
            return this;
        }

        @Override
        public TestCase configureCircuitBreakerEnabled(boolean enabled) {
            ((DefaultStandardRetryStrategy2.Builder) builder).circuitBreakerEnabled(enabled);
            return this;
        }
    }

    static class TestCaseForLegacy extends TestCase {
        TestCaseForLegacy(String name) {
            super("TestCaseForLegacy:: " + name,
                  DefaultRetryStrategy.legacyStrategyBuilder()
                                      .treatAsThrottling(t -> false));
        }

        @Override
        public TestCase configureTokenBucketMaxCapacity(int maxCapacity) {
            ((DefaultLegacyRetryStrategy2.Builder) builder).tokenBucketStore(
                TokenBucketStore
                    .builder()
                    .tokenBucketMaxCapacity(maxCapacity)
                    .build());
            return this;
        }

        @Override
        public TestCase configureTokenBucketExceptionCost(int exceptionCost) {
            ((DefaultLegacyRetryStrategy2.Builder) builder).tokenBucketExceptionCost(exceptionCost);
            return this;
        }

        @Override
        public TestCase configureCircuitBreakerEnabled(boolean enabled) {
            ((DefaultLegacyRetryStrategy2.Builder) builder).circuitBreakerEnabled(enabled);
            return this;
        }

        public TestCaseForLegacy configureTreatAsThrottling(Predicate<Throwable> isThrottling) {
            ((DefaultLegacyRetryStrategy2.Builder) builder).treatAsThrottling(isThrottling);
            return this;
        }

        public TestCaseForLegacy configureThrottlingBackoffStrategy(BackoffStrategy backoffStrategy) {
            ((DefaultLegacyRetryStrategy2.Builder) builder).throttlingBackoffStrategy(backoffStrategy);
            return this;
        }

        public TestCaseForLegacy configureBackoffStrategy(BackoffStrategy backoffStrategy) {
            ((DefaultLegacyRetryStrategy2.Builder) builder).backoffStrategy(backoffStrategy);
            return this;
        }
    }

    static class TestCaseForAdaptive extends TestCase {

        TestCaseForAdaptive(String name) {
            super("TestCaseForAdaptive:: " + name,
                  DefaultRetryStrategy.adaptiveStrategyBuilder()
                                      .treatAsThrottling(t -> false));
        }

        @Override
        public TestCase configureTokenBucketMaxCapacity(int maxCapacity) {
            ((DefaultAdaptiveRetryStrategy2.Builder) builder).tokenBucketStore(
                TokenBucketStore
                    .builder()
                    .tokenBucketMaxCapacity(maxCapacity)
                    .build());
            return this;
        }

        @Override
        public TestCase configureTokenBucketExceptionCost(int exceptionCost) {
            ((DefaultAdaptiveRetryStrategy2.Builder) builder).tokenBucketExceptionCost(exceptionCost);
            return this;
        }

        @Override
        public TestCase configureCircuitBreakerEnabled(boolean enabled) {
            ((DefaultAdaptiveRetryStrategy2.Builder) builder).circuitBreakerEnabled(enabled);
            return this;
        }

        public TestCase configureTreatAsThrottling(Predicate<Throwable> isThrottling) {
            ((DefaultAdaptiveRetryStrategy2.Builder) builder).treatAsThrottling(isThrottling);
            return this;
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
