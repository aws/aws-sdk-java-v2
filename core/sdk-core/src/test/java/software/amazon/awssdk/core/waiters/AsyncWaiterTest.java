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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.utils.CompletableFutureUtils;

public class AsyncWaiterTest extends BaseWaiterTest {

    @Override
    public BiFunction<Integer, TestWaiterConfiguration, WaiterResponse<String>> successOnResponseWaiterOperation() {
        return (count, waiterConfiguration) -> AsyncWaiter.builder(String.class)
                                                          .overrideConfiguration(waiterConfiguration.getPollingStrategy())
                                                          .acceptors(waiterConfiguration.getWaiterAcceptors())
                                                          .scheduledExecutorService(executorService)
                                                          .build()
                                                          .runAsync(new ReturnResponseResource(count)).join();
    }

    @Override
    public BiFunction<Integer, TestWaiterConfiguration, WaiterResponse<String>> successOnExceptionWaiterOperation() {
        return (count, waiterConfiguration) -> AsyncWaiter.builder(String.class)
                                                          .overrideConfiguration(waiterConfiguration.getPollingStrategy())
                                                          .acceptors(waiterConfiguration.getWaiterAcceptors())
                                                          .scheduledExecutorService(executorService)
                                                          .build()
                                                          .runAsync(new ThrowExceptionResource(count)).join();
    }

    @Test
    public void missingScheduledExecutor_shouldThrowException() {
        assertThatThrownBy(() -> AsyncWaiter.builder(String.class)
                                            .overrideConfiguration(p -> p.maxAttempts(3).backoffStrategy(BackoffStrategy.none()))
                                            .build()
                                            .runAsync(() -> null))
            .hasMessageContaining("executorService");
    }

    @Test
    public void concurrentWaiterOperations_shouldBeThreadSafe() {
        AsyncWaiter<String> waiter = AsyncWaiter.builder(String.class)
                                                .overrideConfiguration(p -> p.maxAttempts(4).backoffStrategy(BackoffStrategy.none()))
                                                .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                                .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true))
                                                .scheduledExecutorService(executorService)
                                                .build();

        CompletableFuture<WaiterResponse<String>> waiterResponse1 =
            waiter.runAsync(new ReturnResponseResource(2));
        CompletableFuture<WaiterResponse<String>> waiterResponse2 =
            waiter.runAsync(new ReturnResponseResource(3));

        CompletableFuture.allOf(waiterResponse1, waiterResponse2).join();

        assertThat(waiterResponse1.join().attemptsExecuted()).isEqualTo(2);
        assertThat(waiterResponse2.join().attemptsExecuted()).isEqualTo(3);
    }

    @Test
    public void requestOverrideConfig_shouldTakePrecedence() {
        AsyncWaiter<String> waiter = AsyncWaiter.builder(String.class)
                                                .overrideConfiguration(p -> p.maxAttempts(4).backoffStrategy(BackoffStrategy.none()))
                                                .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                                .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true))
                                                .scheduledExecutorService(executorService)
                                                .build();

        assertThatThrownBy(() ->
                               waiter.runAsync(new ReturnResponseResource(2), o -> o.maxAttempts(1))
                           .join()).hasMessageContaining("exceeded the max retry attempts: 1");
    }

    private static final class ReturnResponseResource implements Supplier<CompletableFuture<String>> {
        private final int successAttemptIndex;
        private int count;

        public ReturnResponseResource(int successAttemptIndex) {
            this.successAttemptIndex = successAttemptIndex;
        }

        @Override
        public CompletableFuture<String> get() {
            if (++count < successAttemptIndex) {
                return CompletableFuture.completedFuture(NON_SUCCESS_STATE_MESSAGE);
            }

            return CompletableFuture.completedFuture(SUCCESS_STATE_MESSAGE);
        }
    }

    private static final class ThrowExceptionResource implements Supplier<CompletableFuture<String>> {
        private final int successAttemptIndex;
        private int count;

        public ThrowExceptionResource(int successAttemptIndex) {
            this.successAttemptIndex = successAttemptIndex;
        }

        @Override
        public CompletableFuture<String> get() {
            if (++count < successAttemptIndex) {
                return CompletableFutureUtils.failedFuture(new RuntimeException(NON_SUCCESS_STATE_MESSAGE));
            }

            return CompletableFutureUtils.failedFuture(new RuntimeException(SUCCESS_STATE_MESSAGE));
        }

    }
}
