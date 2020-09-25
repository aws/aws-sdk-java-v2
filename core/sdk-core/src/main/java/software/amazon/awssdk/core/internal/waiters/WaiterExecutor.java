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

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.core.waiters.WaiterState;
import software.amazon.awssdk.utils.Either;
import software.amazon.awssdk.utils.Validate;

/**
 * Executes sync waiter operations
 *
 * @param <T> the type of the response
 */
@SdkInternalApi
@ThreadSafe
public final class WaiterExecutor<T> {
    private final WaiterExecutorHelper<T> executorHelper;

    public WaiterExecutor(WaiterConfiguration configuration,
                          List<WaiterAcceptor<? super T>> waiterAcceptors) {
        Validate.paramNotNull(configuration, "configuration");
        Validate.paramNotNull(waiterAcceptors, "waiterAcceptors");
        this.executorHelper = new WaiterExecutorHelper<>(waiterAcceptors, configuration);
    }

    WaiterResponse<T> execute(Supplier<T> pollingFunction) {
        return doExecute(pollingFunction, 0, System.currentTimeMillis());
    }

    WaiterResponse<T> doExecute(Supplier<T> pollingFunction, int attemptNumber, long startTime) {
        attemptNumber++;
        T response;
        try {
            response = pollingFunction.get();
        } catch (Exception exception) {
            return evaluate(pollingFunction, Either.right(exception), attemptNumber, startTime);
        }

        return evaluate(pollingFunction, Either.left(response), attemptNumber, startTime);
    }

    private WaiterResponse<T> evaluate(Supplier<T> pollingFunction,
                                       Either<T, Throwable> responseOrException,
                                       int attemptNumber,
                                       long startTime) {
        Optional<WaiterAcceptor<? super T>> waiterAcceptor = executorHelper.firstWaiterAcceptorIfMatched(responseOrException);

        if (waiterAcceptor.isPresent()) {
            WaiterState state = waiterAcceptor.get().waiterState();
            switch (state) {
                case SUCCESS:
                    return executorHelper.createWaiterResponse(responseOrException, attemptNumber);
                case RETRY:
                    return maybeRetry(pollingFunction, attemptNumber, startTime);
                case FAILURE:
                    throw executorHelper.waiterFailureException(waiterAcceptor.get());
                default:
                    throw new UnsupportedOperationException();
            }
        }

        throw executorHelper.noneMatchException(responseOrException);
    }

    private WaiterResponse<T> maybeRetry(Supplier<T> pollingFunction, int attemptNumber, long startTime) {
        Either<Long, SdkClientException> nextDelayOrUnretryableException =
            executorHelper.nextDelayOrUnretryableException(attemptNumber, startTime);

        if (nextDelayOrUnretryableException.right().isPresent()) {
            throw nextDelayOrUnretryableException.right().get();
        }

        try {
            Thread.sleep(nextDelayOrUnretryableException.left().get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SdkClientException.create("The thread got interrupted", e);
        }
        return doExecute(pollingFunction, attemptNumber, startTime);
    }
}
