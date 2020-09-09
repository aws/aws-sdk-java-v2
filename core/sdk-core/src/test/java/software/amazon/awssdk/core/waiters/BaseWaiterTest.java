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

package software.amazon.awssdk.core.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;

public abstract class BaseWaiterTest {

    static final String SUCCESS_STATE_MESSAGE = "helloworld";
    static final String NON_SUCCESS_STATE_MESSAGE = "other";
    static ScheduledExecutorService executorService;

    @BeforeClass
    public static void setUp() {
        executorService = Executors.newScheduledThreadPool(2);
    }

    @AfterClass
    public static void tearDown() {
        executorService.shutdown();
    }

    @Test
    public void missingPollingStrategy_shouldThrowException() {
        assertThatThrownBy(() ->Waiter.builder(String.class)
              .build()).hasMessageContaining("pollingStrategy");
    }

    @Test
    public void successOnResponse_matchSuccessInFirstAttempt_shouldReturnResponse() {
        TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
            .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
            .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)));
        WaiterResponse<String> response = successOnResponseWaiterOperation().apply(1, waiterConfig);
        assertThat(response.responseOrException().response()).contains(SUCCESS_STATE_MESSAGE);
        assertThat(response.attemptsExecuted()).isEqualTo(1);
    }

    @Test
    public void successOnResponse_matchError_shouldThrowException() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.errorOnResponseAcceptor(s -> s.equals(NON_SUCCESS_STATE_MESSAGE)));

        assertThatThrownBy(() -> successOnResponseWaiterOperation().apply(2, waiterConfig)).hasMessageContaining("transitioned the waiter to failure state");
    }

    @Test
    public void successOnResponse_matchSuccessInSecondAttempt_shouldReturnResponse() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true));

        WaiterResponse<String> response = successOnResponseWaiterOperation().apply(2, waiterConfig);
        assertThat(response.responseOrException().response()).contains(SUCCESS_STATE_MESSAGE);
        assertThat(response.attemptsExecuted()).isEqualTo(2);
    }

    @Test
    public void successOnResponse_noMatch_shouldReturnResponse() {
        TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
            .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
            .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)));

        assertThatThrownBy(() -> successOnResponseWaiterOperation().apply(2, waiterConfig)).hasMessageContaining("No acceptor was matched for the response");
    }

    @Test
    public void successOnResponse_noMatchExceedsMaxAttempts_shouldRetryThreeTimesAndThrowException() {
        TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
            .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
            .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
            .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true));

        assertThatThrownBy(() -> successOnResponseWaiterOperation().apply(4, waiterConfig)).hasMessageContaining("max retry attempts");
    }

    @Test
    public void successOnResponse_multipleMatchingAcceptors_firstTakesPrecedence() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.errorOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)));

        assertThatThrownBy(() -> successOnResponseWaiterOperation().apply(1, waiterConfig)).hasMessageContaining("transitioned the waiter to failure state");
    }

    @Test
    public void successOnResponse_fixedBackOffStrategy() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(5).backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1))))
                                      .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true));

        long start = System.currentTimeMillis();
        WaiterResponse<String> response = successOnResponseWaiterOperation().apply(5, waiterConfig);
        long end = System.currentTimeMillis();
        assertThat((end - start)).isBetween(4000L, 5000L);
        assertThat(response.attemptsExecuted()).isEqualTo(5);
    }

    @Test
    public void successOnResponse_maxWaitTime() {
        TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
            .pollingStrategy(p -> p.maxAttempts(5)
                                   .maxWaitTime(Duration.ofSeconds(2))
                                   .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1))))
            .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
            .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> successOnResponseWaiterOperation().apply(5, waiterConfig)).hasMessageContaining("has exceeded the max wait time");
        long end = System.currentTimeMillis();
        assertThat((end - start)).isBetween(1000L, 3000L);
    }

    @Test
    public void successOnException_matchSuccessInFirstAttempt_shouldReturnException() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnExceptionAcceptor(s -> s.getMessage().contains(SUCCESS_STATE_MESSAGE)));

        WaiterResponse<String> response = successOnExceptionWaiterOperation().apply(1, waiterConfig);
        assertThat(response.responseOrException().exception().get()).hasMessageContaining(SUCCESS_STATE_MESSAGE);
        assertThat(response.attemptsExecuted()).isEqualTo(1);
    }

    @Test
    public void successOnException_hasRetryAcceptorMatchSuccessInSecondAttempt_shouldReturnException() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnExceptionAcceptor(s -> s.getMessage().contains(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.retryOnExceptionAcceptor(s -> s.getMessage().contains(NON_SUCCESS_STATE_MESSAGE)));

        WaiterResponse<String> response = successOnExceptionWaiterOperation().apply(2, waiterConfig);
        assertThat(response.responseOrException().exception().get()).hasMessageContaining(SUCCESS_STATE_MESSAGE);
        assertThat(response.attemptsExecuted()).isEqualTo(2);
    }

    @Test
    public void successOnException_unexpectedExceptionAndNoRetryAcceptor_shouldThrowException() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnExceptionAcceptor(s -> s.getMessage().contains(SUCCESS_STATE_MESSAGE)));
        assertThatThrownBy(() -> successOnExceptionWaiterOperation().apply(2, waiterConfig)).hasMessageContaining("did not match");
    }

    @Test
    public void successOnException_matchError_shouldThrowException() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnExceptionAcceptor(s -> s.getMessage().contains(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.errorOnExceptionAcceptor(s -> s.getMessage().contains(NON_SUCCESS_STATE_MESSAGE)));
        assertThatThrownBy(() -> successOnExceptionWaiterOperation().apply(2, waiterConfig)).hasMessageContaining("transitioned the waiter to failure state");
    }

    @Test
    public void successOnException_multipleMatchingAcceptors_firstTakesPrecedence() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnExceptionAcceptor(s -> s.getMessage().contains(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.errorOnExceptionAcceptor(s -> s.getMessage().contains(SUCCESS_STATE_MESSAGE)));

        WaiterResponse<String> response = successOnExceptionWaiterOperation().apply(1, waiterConfig);
        assertThat(response.responseOrException().exception().get()).hasMessageContaining(SUCCESS_STATE_MESSAGE);
        assertThat(response.attemptsExecuted()).isEqualTo(1);
    }

    @Test
    public void successOnException_fixedBackOffStrategy() {
       TestWaiterConfiguration waiterConfig = new TestWaiterConfiguration()
                                      .pollingStrategy(p -> p.maxAttempts(5).backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(1))))
                                      .addAcceptor(WaiterAcceptor.retryOnExceptionAcceptor(s -> s.getMessage().equals(NON_SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.successOnExceptionAcceptor(s -> s.getMessage().equals(SUCCESS_STATE_MESSAGE)));

        long start = System.currentTimeMillis();
        WaiterResponse<String> response = successOnExceptionWaiterOperation().apply(5, waiterConfig);
        long end = System.currentTimeMillis();
        assertThat((end - start)).isBetween(4000L, 5000L);
        assertThat(response.attemptsExecuted()).isEqualTo(5);
    }

    public abstract BiFunction<Integer, TestWaiterConfiguration, WaiterResponse<String>> successOnResponseWaiterOperation();

    public abstract BiFunction<Integer, TestWaiterConfiguration, WaiterResponse<String>> successOnExceptionWaiterOperation();

    class TestWaiterConfiguration implements WaiterBuilder<String, TestWaiterConfiguration> {
        private List<WaiterAcceptor<String>> waiterAcceptors = new ArrayList<>();
        private PollingStrategy pollingStrategy;

        /**
         * @return
         */
        public List<WaiterAcceptor<String>> getWaiterAcceptors() {
            return waiterAcceptors;
        }

        /**
         * @return
         */
        public PollingStrategy getPollingStrategy() {
            return pollingStrategy;
        }

        @Override
        public TestWaiterConfiguration acceptors(List<WaiterAcceptor<String>> waiterAcceptors) {
            this.waiterAcceptors = waiterAcceptors;
            return this;
        }

        @Override
        public TestWaiterConfiguration addAcceptor(WaiterAcceptor<String> waiterAcceptor) {
            waiterAcceptors.add(waiterAcceptor);
            return this;
        }

        @Override
        public TestWaiterConfiguration pollingStrategy(PollingStrategy pollingStrategy) {
            this.pollingStrategy = pollingStrategy;
            return this;
        }
    }
}
