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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
 * Executes async waiter operations
 *
 * @param <T> the type of the response
 */
@SdkInternalApi
@ThreadSafe
public final class AsyncWaiterExecutor<T> {
    private final ScheduledExecutorService executorService;
    private final WaiterExecutorHelper<T> executorHelper;

    public AsyncWaiterExecutor(WaiterConfiguration configuration,
                               List<WaiterAcceptor<? super T>> waiterAcceptors,
                               ScheduledExecutorService executorService) {
        Validate.paramNotNull(waiterAcceptors, "waiterAcceptors");
        this.executorService = Validate.paramNotNull(executorService, "executorService");
        this.executorHelper = new WaiterExecutorHelper<>(waiterAcceptors, configuration);
    }

    /**
     * Execute the provided async polling function
     */
    CompletableFuture<WaiterResponse<T>> execute(Supplier<CompletableFuture<T>> asyncPollingFunction) {
        CompletableFuture<WaiterResponse<T>> future = new CompletableFuture<>();
        doExecute(asyncPollingFunction, future, 0, System.currentTimeMillis());
        return future;
    }

    private void doExecute(Supplier<CompletableFuture<T>> asyncPollingFunction,
                           CompletableFuture<WaiterResponse<T>> future,
                           int attemptNumber,
                           long startTime) {
        runAsyncPollingFunction(asyncPollingFunction, future, ++attemptNumber, startTime);
    }

    private void runAsyncPollingFunction(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                         CompletableFuture<WaiterResponse<T>> future,
                                         int attemptNumber,
                                         long startTime) {
        asyncPollingFunction.get().whenComplete((response, exception) -> {
            try {
                Either<T, Throwable> responseOrException = exception == null ? Either.left(response) : Either.right(exception);

                Optional<WaiterAcceptor<? super T>> optionalWaiterAcceptor =
                    executorHelper.firstWaiterAcceptorIfMatched(responseOrException);

                if (optionalWaiterAcceptor.isPresent()) {
                    WaiterAcceptor<? super T> acceptor = optionalWaiterAcceptor.get();
                    WaiterState state = acceptor.waiterState();
                    switch (state) {
                        case SUCCESS:
                            future.complete(executorHelper.createWaiterResponse(responseOrException, attemptNumber));
                            break;
                        case RETRY:
                            maybeRetry(asyncPollingFunction, future, attemptNumber, startTime);
                            break;
                        case FAILURE:
                            future.completeExceptionally(executorHelper.waiterFailureException(acceptor));
                            break;
                        default:
                            future.completeExceptionally(new UnsupportedOperationException());
                    }
                } else {
                    future.completeExceptionally(executorHelper.noneMatchException(responseOrException));
                }
            } catch (Throwable t) {
                future.completeExceptionally(SdkClientException.create("Encountered unexpected exception.", t));
            }
        });
    }

    private void maybeRetry(Supplier<CompletableFuture<T>> asyncPollingFunction,
                            CompletableFuture<WaiterResponse<T>> future,
                            int attemptNumber,
                            long startTime) {
        Either<Long, SdkClientException> nextDelayOrUnretryableException =
            executorHelper.nextDelayOrUnretryableException(attemptNumber, startTime);

        nextDelayOrUnretryableException.apply(
            nextDelay -> executorService.schedule(() -> doExecute(asyncPollingFunction, future, attemptNumber, startTime),
                                                  nextDelay,
                                                  TimeUnit.MILLISECONDS),
            future::completeExceptionally);

    }
}
