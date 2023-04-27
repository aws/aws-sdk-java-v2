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

package software.amazon.awssdk.retries.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RetryStrategyBuilderTest {

    public static Collection<TestCase> parameters() {
        return Arrays.asList(
            new TestCase()
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .givenThrowable(new IllegalArgumentException())
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException())
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnException(IllegalArgumentException.class))
                .givenThrowable(new NumberFormatException())
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCause(IllegalArgumentException.class))
                .givenThrowable(new IllegalArgumentException())
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCause(IllegalArgumentException.class))
                .givenThrowable(new NumberFormatException())
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCause(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new IllegalStateException()))
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCause(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new IllegalArgumentException()))
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCause(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new RuntimeException(new IllegalArgumentException())))
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCause(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new RuntimeException(new NumberFormatException())))
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new IllegalArgumentException())
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException())
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new IllegalArgumentException()))
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new NumberFormatException())
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCauseInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new IllegalArgumentException())
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCauseInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException())
                .expectShouldNotRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCauseInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new IllegalArgumentException()))
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCauseInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new NumberFormatException())
                .expectShouldRetry()
            , new TestCase()
                .configure(b -> b.retryOnExceptionOrCauseInstanceOf(IllegalArgumentException.class))
                .givenThrowable(new RuntimeException(new RuntimeException(new NumberFormatException())))
                .expectShouldRetry()
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testCase(TestCase testCase) {
        assertThat(testCase.run()).isEqualTo(testCase.expected());
    }

    static class TestCase {
        private final BuilderToTestDefaults builder = new BuilderToTestDefaults();
        private Throwable testThrowable;
        private boolean expectedTestResult;

        TestCase configure(Function<BuilderToTestDefaults, BuilderToTestDefaults> configure) {
            configure.apply(builder);
            return this;
        }

        TestCase givenThrowable(Throwable testThrowable) {
            this.testThrowable = testThrowable;
            return this;
        }

        TestCase expectShouldRetry() {
            this.expectedTestResult = true;
            return this;
        }

        TestCase expectShouldNotRetry() {
            this.expectedTestResult = false;
            return this;
        }

        boolean run() {
            return builder.shouldRetryCapture().test(testThrowable);
        }

        boolean expected() {
            return expectedTestResult;
        }
    }

    static class BuilderToTestDefaults implements RetryStrategy.Builder<BuilderToTestDefaults, DummyRetryStrategy> {
        Predicate<Throwable> shouldRetryCapture = null;

        Predicate<Throwable> shouldRetryCapture() {
            return shouldRetryCapture;
        }

        @Override
        public BuilderToTestDefaults retryOnException(Predicate<Throwable> shouldRetry) {
            shouldRetryCapture = shouldRetry;
            return this;
        }

        @Override
        public BuilderToTestDefaults maxAttempts(int maxAttempts) {
            return this;
        }

        @Override
        public DummyRetryStrategy build() {
            return null;
        }
    }

    static class DummyRetryStrategy implements RetryStrategy<BuilderToTestDefaults, DummyRetryStrategy> {

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            return null;
        }

        @Override
        public RefreshRetryTokenResponse refreshRetryToken(RefreshRetryTokenRequest request) {
            return null;
        }

        @Override
        public RecordSuccessResponse recordSuccess(RecordSuccessRequest request) {
            return null;
        }

        @Override
        public BuilderToTestDefaults toBuilder() {
            return null;
        }
    }

}
