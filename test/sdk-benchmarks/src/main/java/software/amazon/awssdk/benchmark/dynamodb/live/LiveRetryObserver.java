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

package software.amazon.awssdk.benchmark.dynamodb.live;

import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.benchmark.dynamodb.DynamoDbBenchmarkSystemSetting;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.retry.RetryUtils;

/**
 * Low-impact retry / failure observer for live benchmarks.
 *
 * <p>Uses a public {@link ExecutionInterceptor} and counts HTTP transmissions per API call
 * ({@code beforeTransmission} runs once per attempt). This avoids attaching a
 * {@code MetricPublisher}, which would add publish overhead on the hot path.
 *
 * <p>Enabled by default for live clients. Disable with
 * {@code DYNAMODB_BENCHMARK_LIVE_RETRY_OBSERVE=false} or
 * {@code -Ddynamodb.benchmark.live.retryObserve=false} for an unmodified default client path.
 */
final class LiveRetryObserver implements ExecutionInterceptor {

    private static final ExecutionAttribute<AtomicInteger> ATTEMPTS_THIS_CALL =
        new ExecutionAttribute<>("LiveDynamoDbBenchmarkAttempts");

    private final AtomicInteger completedCalls = new AtomicInteger();
    private final AtomicInteger failedCalls = new AtomicInteger();
    private final AtomicInteger callsWithRetries = new AtomicInteger();
    private final AtomicInteger totalAttempts = new AtomicInteger();
    private final AtomicInteger maxAttemptsOnSingleCall = new AtomicInteger();
    private final AtomicInteger throttlingFailures = new AtomicInteger();

    static boolean isEnabled() {
        return DynamoDbBenchmarkSystemSetting.LIVE_RETRY_OBSERVE.getBooleanValue().orElse(true);
    }

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        AtomicInteger attempts = executionAttributes.getAttribute(ATTEMPTS_THIS_CALL);
        if (attempts == null) {
            attempts = new AtomicInteger();
            executionAttributes.putAttribute(ATTEMPTS_THIS_CALL, attempts);
        }
        int attempt = attempts.incrementAndGet();
        totalAttempts.incrementAndGet();
        maxAttemptsOnSingleCall.accumulateAndGet(attempt, Math::max);
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        completedCalls.incrementAndGet();
        recordRetryIfNeeded(executionAttributes);
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        failedCalls.incrementAndGet();
        recordRetryIfNeeded(executionAttributes);
        Throwable exception = context.exception();
        if (exception instanceof SdkException && RetryUtils.isThrottlingException((SdkException) exception)) {
            throttlingFailures.incrementAndGet();
        }
        if (exception instanceof SdkException) {
            Integer numAttempts = ((SdkException) exception).numAttempts();
            if (numAttempts != null) {
                maxAttemptsOnSingleCall.accumulateAndGet(numAttempts, Math::max);
            }
        }
    }

    private void recordRetryIfNeeded(ExecutionAttributes executionAttributes) {
        AtomicInteger attempts = executionAttributes.getAttribute(ATTEMPTS_THIS_CALL);
        if (attempts != null && attempts.get() > 1) {
            callsWithRetries.incrementAndGet();
        }
    }

    int completedCalls() {
        return completedCalls.get();
    }

    int failedCalls() {
        return failedCalls.get();
    }

    int callsWithRetries() {
        return callsWithRetries.get();
    }

    int totalAttempts() {
        return totalAttempts.get();
    }

    int maxAttemptsOnSingleCall() {
        return maxAttemptsOnSingleCall.get();
    }

    int throttlingFailures() {
        return throttlingFailures.get();
    }

    boolean observedRetries() {
        return callsWithRetries.get() > 0 || maxAttemptsOnSingleCall.get() > 1;
    }

    String summary() {
        return "completedCalls=" + completedCalls.get()
               + ", failedCalls=" + failedCalls.get()
               + ", callsWithRetries=" + callsWithRetries.get()
               + ", totalAttempts=" + totalAttempts.get()
               + ", maxAttemptsOnSingleCall=" + maxAttemptsOnSingleCall.get()
               + ", throttlingFailures=" + throttlingFailures.get();
    }
}
