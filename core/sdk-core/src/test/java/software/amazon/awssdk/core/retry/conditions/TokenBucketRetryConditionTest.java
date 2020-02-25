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

package software.amazon.awssdk.core.retry.conditions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.retry.RetryPolicyContext;

public class TokenBucketRetryConditionTest {
    private static final SdkException EXCEPTION = SdkClientException.create("");
    private static final SdkException EXCEPTION_2 = SdkClientException.create("");

    @Test
    public void maximumTokensCannotBeExceeded() {
        TokenBucketRetryCondition condition = create(3, e -> 1);
        for (int i = 1; i < 10; ++i) {
            condition.requestSucceeded(context(null));
            assertThat(condition.tokensAvailable()).isEqualTo(3);
        }
    }

    @Test
    public void releasingMoreCapacityThanAvailableSetsCapacityToMax() {
        ExecutionAttributes attributes = new ExecutionAttributes();

        TokenBucketRetryCondition condition = create(11, e -> e == EXCEPTION ? 1 : 3);
        assertThat(condition.shouldRetry(context(EXCEPTION, attributes))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(10);
        assertThat(condition.shouldRetry(context(EXCEPTION_2, attributes))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(7);
        condition.requestSucceeded(context(EXCEPTION_2, attributes));
        assertThat(condition.tokensAvailable()).isEqualTo(10);
        condition.requestSucceeded(context(EXCEPTION_2, attributes));
        assertThat(condition.tokensAvailable()).isEqualTo(11);
    }

    @Test
    public void nonFirstAttemptsAreNotFree() {
        TokenBucketRetryCondition condition = create(2, e -> 1);

        assertThat(condition.shouldRetry(context(EXCEPTION))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(1);

        assertThat(condition.shouldRetry(context(EXCEPTION))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(0);

        assertThat(condition.shouldRetry(context(EXCEPTION))).isFalse();
        assertThat(condition.tokensAvailable()).isEqualTo(0);
    }

    @Test
    public void exceptionCostIsHonored() {
        // EXCEPTION costs 1, anything else costs 10
        TokenBucketRetryCondition condition = create(20, e -> e == EXCEPTION ? 1 : 10);

        assertThat(condition.shouldRetry(context(EXCEPTION))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(19);

        assertThat(condition.shouldRetry(context(EXCEPTION_2))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(9);

        assertThat(condition.shouldRetry(context(EXCEPTION_2))).isFalse();
        assertThat(condition.tokensAvailable()).isEqualTo(9);

        assertThat(condition.shouldRetry(context(EXCEPTION))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(8);
    }

    @Test
    public void successReleasesAcquiredCost() {
        ExecutionAttributes attributes = new ExecutionAttributes();

        TokenBucketRetryCondition condition = create(20, e -> 10);

        assertThat(condition.shouldRetry(context(EXCEPTION, attributes))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(10);

        condition.requestSucceeded(context(EXCEPTION, attributes));
        assertThat(condition.tokensAvailable()).isEqualTo(20);
    }

    @Test
    public void firstRequestSuccessReleasesOne() {
        TokenBucketRetryCondition condition = create(20, e -> 10);

        assertThat(condition.shouldRetry(context(null))).isTrue();
        assertThat(condition.tokensAvailable()).isEqualTo(10);

        condition.requestSucceeded(context(null));
        assertThat(condition.tokensAvailable()).isEqualTo(11);

        condition.requestSucceeded(context(null));
        assertThat(condition.tokensAvailable()).isEqualTo(12);
    }

    @Test
    public void conditionSeemsToBeThreadSafe() throws InterruptedException {
        int bucketSize = 5;
        TokenBucketRetryCondition condition = create(bucketSize, e -> 1);

        AtomicInteger concurrentCalls = new AtomicInteger(0);
        AtomicBoolean failure = new AtomicBoolean(false);
        int parallelism = bucketSize * 2;
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        for (int i = 0; i < parallelism; ++i) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 1000; ++j) {
                        ExecutionAttributes attributes = new ExecutionAttributes();
                        if (condition.shouldRetry(context(EXCEPTION, attributes))) {
                            int calls = concurrentCalls.addAndGet(1);
                            if (calls > bucketSize) {
                                failure.set(true);
                            }
                            Thread.sleep(1);
                            concurrentCalls.addAndGet(-1);
                            condition.requestSucceeded(context(EXCEPTION, attributes));
                        }
                        else {
                            Thread.sleep(1);
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    failure.set(true);
                }
            });

            // Stagger the threads a bit.
            Thread.sleep(1);
        }

        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
            Assert.fail();
        }

        assertThat(failure.get()).isFalse();
    }

    private RetryPolicyContext context(SdkException lastException) {
        return RetryPolicyContext.builder()
                                 .executionAttributes(new ExecutionAttributes())
                                 .exception(lastException)
                                 .build();
    }

    private RetryPolicyContext context(SdkException lastException, ExecutionAttributes attributes) {
        return RetryPolicyContext.builder()
                                 .executionAttributes(attributes)
                                 .exception(lastException)
                                 .build();
    }

    private TokenBucketRetryCondition create(int size, TokenBucketExceptionCostFunction function) {
        return TokenBucketRetryCondition.builder()
                                        .tokenBucketSize(size)
                                        .exceptionCostFunction(function)
                                        .build();
    }

}