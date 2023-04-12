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

package software.amazon.awssdk.retries.backoff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FixedDelayWithJitterTest {
    static final ComputedNextInt MIN_VALUE_RND = new ComputedNextInt(bound -> 0);
    static final ComputedNextInt MID_VALUE_RND = new ComputedNextInt(bound -> bound / 2);
    static final ComputedNextInt MAX_VALUE_RND = new ComputedNextInt(bound -> bound - 1);
    static final Duration BASE_DELAY = Duration.ofMillis(23);

    public static Collection<TestCase> parameters() {
        return Arrays.asList(
            // --- Using random that returns: bound - 1
            new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(1)
                .expectDelayInMs(22)
            , new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(2)
                .expectDelayInMs(22)
            , new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(3)
                .expectDelayInMs(22)
            , new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(5)
                .expectDelayInMs(22)
            , new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(7)
                .expectDelayInMs(22)
            , new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(11)
                .expectDelayInMs(22)
            , new TestCase()
                .configureRandom(MAX_VALUE_RND)
                .givenAttempt(13)
                .expectDelayInMs(22)
            // --- Using random that returns: bound / 2
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(1)
                .expectDelayInMs(11)
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(2)
                .expectDelayInMs(11)
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(3)
                .expectDelayInMs(11)
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(5)
                .expectDelayInMs(11)
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(7)
                .expectDelayInMs(11)
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(11)
                .expectDelayInMs(11)
            , new TestCase()
                .configureRandom(MID_VALUE_RND)
                .givenAttempt(13)
                .expectDelayInMs(11)
            // --- Using random that returns: 0
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(1)
                .expectDelayInMs(0)
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(2)
                .expectDelayInMs(0)
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(3)
                .expectDelayInMs(0)
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(5)
                .expectDelayInMs(0)
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(7)
                .expectDelayInMs(0)
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(11)
                .expectDelayInMs(0)
            , new TestCase()
                .configureRandom(MIN_VALUE_RND)
                .givenAttempt(13)
                .expectDelayInMs(0)
        );
    }


    @ParameterizedTest
    @MethodSource("parameters")
    public void testCase(TestCase testCase) {
        assertThat(testCase.run(), equalTo(testCase.expected()));
    }

    static class TestCase {
        Random random;
        int attempt;
        long expectedDelayMs;

        TestCase configureRandom(Random random) {
            this.random = random;
            return this;
        }

        TestCase givenAttempt(int attempt) {
            this.attempt = attempt;
            return this;
        }

        TestCase expectDelayInMs(long expectedDelayMs) {
            this.expectedDelayMs = expectedDelayMs;
            return this;
        }

        Duration run() {
            return
                new FixedDelayWithJitter(() -> random, BASE_DELAY)
                    .computeDelay(this.attempt);
        }

        Duration expected() {
            return Duration.ofMillis(expectedDelayMs);
        }
    }

    static class ComputedNextInt extends Random {
        final Function<Integer, Integer> compute;

        ComputedNextInt(Function<Integer, Integer> compute) {
            this.compute = compute;
        }

        @Override
        public int nextInt(int bound) {
            return compute.apply(bound);
        }
    }
}
