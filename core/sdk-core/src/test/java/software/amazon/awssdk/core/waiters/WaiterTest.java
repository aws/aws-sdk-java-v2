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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;

public class WaiterTest extends BaseWaiterTest {

    private static final String SUCCESS_STATE_MESSAGE = "helloworld";
    private static final String NON_SUCCESS_STATE_MESSAGE = "other";
    private BackoffStrategy backoffStrategy;

    @Before
    public void setup() {
        backoffStrategy = FixedDelayBackoffStrategy.create(Duration.ofMillis(10));
    }

    @Override
    public BiFunction<Integer, TestWaiterConfiguration, WaiterResponse<String>> successOnResponseWaiterOperation() {
        return (count, waiterConfiguration) -> Waiter.builder(String.class)
                                                     .overrideConfiguration(waiterConfiguration.getPollingStrategy())
                                                     .acceptors(waiterConfiguration.getWaiterAcceptors()).build().run(new ReturnResponseResource(count));
    }

    @Override
    public BiFunction<Integer, TestWaiterConfiguration, WaiterResponse<String>> successOnExceptionWaiterOperation() {
        return (count, waiterConfiguration) -> Waiter.builder(String.class)
                                                     .overrideConfiguration(waiterConfiguration.getPollingStrategy())
                                                     .acceptors(waiterConfiguration.getWaiterAcceptors()).build().run(new ThrowExceptionResource(count));
    }

    @Test
    public void concurrentWaiterOperations_shouldBeThreadSafe() {
        Waiter<String> waiter = Waiter.builder(String.class)
                                      .overrideConfiguration(p -> p.maxAttempts(4).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true))
                                      .build();

        CompletableFuture<WaiterResponse<String>> waiterResponse1 =
            CompletableFuture.supplyAsync(() -> waiter.run(new ReturnResponseResource(2)), executorService);
        CompletableFuture<WaiterResponse<String>> waiterResponse2 =
            CompletableFuture.supplyAsync(() -> waiter.run(new ReturnResponseResource(3)), executorService);

        CompletableFuture.allOf(waiterResponse1, waiterResponse2).join();

        assertThat(waiterResponse1.join().attemptsExecuted()).isEqualTo(2);
        assertThat(waiterResponse2.join().attemptsExecuted()).isEqualTo(3);
    }

    @Test
    public void requestOverrideConfig_shouldTakePrecedence() {
        Waiter<String> waiter = Waiter.builder(String.class)
                                      .overrideConfiguration(p -> p.maxAttempts(4).backoffStrategy(BackoffStrategy.none()))
                                      .addAcceptor(WaiterAcceptor.successOnResponseAcceptor(s -> s.equals(SUCCESS_STATE_MESSAGE)))
                                      .addAcceptor(WaiterAcceptor.retryOnResponseAcceptor(i -> true))
                                      .build();

        assertThatThrownBy(() -> waiter.run(new ReturnResponseResource(2), o -> o.maxAttempts(1)))
            .hasMessageContaining("exceeded the max retry attempts: 1");
    }

    private static final class ReturnResponseResource implements Supplier<String> {
        private final int successAttemptIndex;
        private int count;

        public ReturnResponseResource(int successAttemptIndex) {
            this.successAttemptIndex = successAttemptIndex;
        }


        @Override
        public String get() {
            if (++count < successAttemptIndex) {
                return NON_SUCCESS_STATE_MESSAGE;
            }

            return SUCCESS_STATE_MESSAGE;
        }
    }

    private static final class ThrowExceptionResource implements Supplier<String> {
        private final int successAttemptIndex;
        private int count;

        public ThrowExceptionResource(int successAttemptIndex) {
            this.successAttemptIndex = successAttemptIndex;
        }


        @Override
        public String get() {
            if (++count < successAttemptIndex) {
                throw new RuntimeException(NON_SUCCESS_STATE_MESSAGE);
            }

            throw new RuntimeException(SUCCESS_STATE_MESSAGE);
        }

        public int count() {
            return count;
        }
    }
}
