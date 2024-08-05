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
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterResponse;
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
        int attemptNumber = 0;
        long startTime = System.currentTimeMillis();

        while (true) {
            attemptNumber++;

            Either<T, Throwable> polledResponse = pollResponse(pollingFunction);
            WaiterAcceptor<? super T> waiterAcceptor = firstWaiterAcceptor(polledResponse);
            switch (waiterAcceptor.waiterState()) {
                case SUCCESS:
                    return executorHelper.createWaiterResponse(polledResponse, attemptNumber);
                case RETRY:
                    waitToRetry(attemptNumber, startTime);
                    break;
                case FAILURE:
                    throw executorHelper.waiterFailureException(waiterAcceptor);
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private Either<T, Throwable> pollResponse(Supplier<T> pollingFunction) {
        try {
            return Either.left(pollingFunction.get());
        } catch (Exception exception) {
            return Either.right(exception);
        }
    }

    private WaiterAcceptor<? super T> firstWaiterAcceptor(Either<T, Throwable> responseOrException) {
        return executorHelper.firstWaiterAcceptorIfMatched(responseOrException)
                             .orElseThrow(() -> executorHelper.noneMatchException(responseOrException));
    }

    private void waitToRetry(int attemptNumber, long startTime) {
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
    }
}
