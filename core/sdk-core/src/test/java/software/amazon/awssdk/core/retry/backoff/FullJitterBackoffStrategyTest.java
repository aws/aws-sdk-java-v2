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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

@RunWith(Parameterized.class)
public class FullJitterBackoffStrategyTest {

    @Parameters
    public static Collection<TestCase> parameters() throws Exception {
        return Arrays.asList(
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(0)
                          .expectedMaxDelay(Duration.ofMillis(100))
                          .expectedMedDelay(Duration.ofMillis(50))
                          .expectedMinDelay(Duration.ofMillis(1)),
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(1)
                          .expectedMaxDelay(Duration.ofMillis(200))
                          .expectedMedDelay(Duration.ofMillis(100))
                          .expectedMinDelay(Duration.ofMillis(1)),
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(2)
                          .expectedMaxDelay(Duration.ofMillis(400))
                          .expectedMedDelay(Duration.ofMillis(200))
                          .expectedMinDelay(Duration.ofMillis(1)),
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(3)
                          .expectedMaxDelay(Duration.ofMillis(800))
                          .expectedMedDelay(Duration.ofMillis(400))
                          .expectedMinDelay(Duration.ofMillis(1)),
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(4)
                          .expectedMaxDelay(Duration.ofMillis(1600))
                          .expectedMedDelay(Duration.ofMillis(800))
                          .expectedMinDelay(Duration.ofMillis(1)),
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(5)
                          .expectedMaxDelay(Duration.ofMillis(3200))
                          .expectedMedDelay(Duration.ofMillis(1600))
                          .expectedMinDelay(Duration.ofMillis(1)),
            new TestCase().backoffStrategy(BackoffStrategy.defaultStrategy(RetryMode.STANDARD))
                          .retriesAttempted(100)
                          .expectedMaxDelay(Duration.ofSeconds(20))
                          .expectedMedDelay(Duration.ofSeconds(10))
                          .expectedMinDelay(Duration.ofMillis(1))
        );
    }

    @Parameter
    public TestCase testCase;

    @Mock
    private Random mockRandom = mock(Random.class);

    @Before
    public void setUp() throws Exception {
        testCase.backoffStrategy = injectMockRandom(testCase.backoffStrategy);
    }

    @Test
    public void testMaxDelay() {
        mockMaxRandom();
        test(testCase.backoffStrategy, testCase.retriesAttempted, testCase.expectedMaxDelay);
    }

    @Test
    public void testMedDelay() {
        mockMediumRandom();
        test(testCase.backoffStrategy, testCase.retriesAttempted, testCase.expectedMedDelay);
    }

    @Test
    public void testMinDelay() {
        mockMinRandom();
        test(testCase.backoffStrategy, testCase.retriesAttempted, testCase.expectedMinDelay);
    }

    private static void test(BackoffStrategy backoffStrategy, int retriesAttempted, Duration expectedDelay) {
        RetryPolicyContext context = RetryPolicyContext.builder()
                                                       .retriesAttempted(retriesAttempted)
                                                       .build();
        Duration computedDelay = backoffStrategy.computeDelayBeforeNextRetry(context);
        assertThat(computedDelay).isEqualTo(expectedDelay);
    }

    private FullJitterBackoffStrategy injectMockRandom(BackoffStrategy strategy) {
        FullJitterBackoffStrategy.Builder builder = ((FullJitterBackoffStrategy) strategy).toBuilder();
        return new FullJitterBackoffStrategy(builder.baseDelay(), builder.maxBackoffTime(), mockRandom);
    }

    private void mockMaxRandom() {
        when(mockRandom.nextInt(anyInt())).then((Answer<Integer>) invocationOnMock -> {
            Integer firstArg = (Integer) returnsFirstArg().answer(invocationOnMock);
            return firstArg - 1;
        });
    }

    private void mockMinRandom() {
        when(mockRandom.nextInt(anyInt())).then((Answer<Integer>) invocationOnMock -> {
            return 0;
        });
    }

    private void mockMediumRandom() {
        when(mockRandom.nextInt(anyInt())).then((Answer<Integer>) invocationOnMock -> {
            Integer firstArg = (Integer) returnsFirstArg().answer(invocationOnMock);
            return firstArg / 2 - 1;
        });
    }

    private static class TestCase {
        private BackoffStrategy backoffStrategy;
        private int retriesAttempted;
        private Duration expectedMinDelay;
        private Duration expectedMedDelay;
        private Duration expectedMaxDelay;

        public TestCase backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public TestCase retriesAttempted(int retriesAttempted) {
            this.retriesAttempted = retriesAttempted;
            return this;
        }

        public TestCase expectedMinDelay(Duration expectedMinDelay) {
            this.expectedMinDelay = expectedMinDelay;
            return this;
        }

        public TestCase expectedMedDelay(Duration expectedMedDelay) {
            this.expectedMedDelay = expectedMedDelay;
            return this;
        }

        public TestCase expectedMaxDelay(Duration expectedMaxDelay) {
            this.expectedMaxDelay = expectedMaxDelay;
            return this;
        }
    }
}