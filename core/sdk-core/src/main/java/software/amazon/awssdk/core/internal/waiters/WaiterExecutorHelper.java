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

package software.amazon.awssdk.core.internal.waiters;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.utils.Either;

/**
 * The waiter executor helper class. Contains the logic shared by {@link WaiterExecutor} and
 * {@link AsyncWaiterExecutor}
 */
@SdkInternalApi
public final class WaiterExecutorHelper<T> {
    private final List<WaiterAcceptor<? super T>> waiterAcceptors;
    private final BackoffStrategy backoffStrategy;
    private final Duration waitTimeout;
    private final int maxAttempts;

    public WaiterExecutorHelper(List<WaiterAcceptor<? super T>> waiterAcceptors,
                                WaiterConfiguration configuration) {
        this.waiterAcceptors = waiterAcceptors;
        this.backoffStrategy = configuration.backoffStrategy();
        this.waitTimeout = configuration.waitTimeout();
        this.maxAttempts = configuration.maxAttempts();
    }

    public WaiterResponse<T> createWaiterResponse(Either<T, Throwable> responseOrException, int attempts) {
        return responseOrException.map(
            r -> DefaultWaiterResponse.<T>builder().response(r).attemptsExecuted(attempts).build(),
            e -> DefaultWaiterResponse.<T>builder().exception(e).attemptsExecuted(attempts).build()
        );
    }

    public Optional<WaiterAcceptor<? super T>> firstWaiterAcceptorIfMatched(Either<T, Throwable> responseOrException) {
        return responseOrException.map(this::responseMatches, this::exceptionMatches);
    }

    public long computeNextDelayInMills(int attemptNumber) {
        return backoffStrategy.computeDelayBeforeNextRetry(RetryPolicyContext.builder()
                                                                             .retriesAttempted(attemptNumber)
                                                                             .build())
                              .toMillis();
    }

    public boolean exceedsMaxWaitTime(long startTime, long nextDelayInMills) {
        if (waitTimeout == null) {
            return false;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime + nextDelayInMills > waitTimeout.toMillis();
    }

    public Either<Long, SdkClientException> nextDelayOrUnretryableException(int attemptNumber, long startTime) {
        if (attemptNumber >= maxAttempts) {
            return Either.right(SdkClientException.create("The waiter has exceeded the max retry attempts: " +
                                                          maxAttempts));
        }

        long nextDelay = computeNextDelayInMills(attemptNumber);
        if (exceedsMaxWaitTime(startTime, nextDelay)) {
            return Either.right(SdkClientException.create("The waiter has exceeded the max wait time or the "
                                                          + "next retry will exceed the max wait time + " +
                                                          waitTimeout));
        }

        return Either.left(nextDelay);
    }

    public SdkClientException noneMatchException(Either<T, Throwable> responseOrException) {
        return responseOrException.map(
            r -> SdkClientException.create("No acceptor was matched for the response"),
            t -> SdkClientException.create("An exception was thrown and did not match any "
                                           + "waiter acceptors", t));
    }

    public SdkClientException waiterFailureException(WaiterAcceptor<? super T> acceptor) {
        return SdkClientException.create(acceptor.message().orElse("A waiter acceptor was matched and transitioned "
                                                                   + "the waiter to failure state"));
    }

    private Optional<WaiterAcceptor<? super T>> responseMatches(T response) {
        return waiterAcceptors.stream()
                              .filter(acceptor -> acceptor.matches(response))
                              .findFirst();
    }

    private Optional<WaiterAcceptor<? super T>> exceptionMatches(Throwable exception) {
        return waiterAcceptors.stream()
                              .filter(acceptor -> acceptor.matches(exception))
                              .findFirst();
    }
}
