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


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.internal.waiters.DefaultAsyncWaiter;

/**
 * Waiter utility class that waits for a resource to transition to the desired state asynchronously
 *
 * @param <T> the type of the resource returned from the polling function
 */
@SdkPublicApi
public interface AsyncWaiter<T> {

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction) {
        throw new UnsupportedOperationException();
    }

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     *  @param overrideConfig per request override configuration
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                          WaiterOverrideConfiguration overrideConfig) {
        throw new UnsupportedOperationException();
    }

    /**
     * Runs the provided polling function. It completes successfully when the resource enters into a desired state or
     * exceptionally when it is determined that the resource will never enter into the desired state.
     *
     * @param asyncPollingFunction the polling function to trigger
     * @param overrideConfig The consumer that will configure the per request override configuration for waiters
     * @return A {@link CompletableFuture} containing the {@link WaiterResponse}
     */
    default CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                          Consumer<WaiterOverrideConfiguration.Builder> overrideConfig) {
        return runAsync(asyncPollingFunction, WaiterOverrideConfiguration.builder().applyMutation(overrideConfig).build());
    }

    /**
     * Creates a newly initialized builder for the waiter object.
     *
     * @param responseClass the response class
     * @param <T> the type of the response
     * @return a Waiter builder
     */
    static <T> Builder<T> builder(Class<? extends T> responseClass) {
        return DefaultAsyncWaiter.builder();
    }

    /**
     * The Waiter Builder
     * @param <T> the type of the resource
     */
    interface Builder<T> extends WaiterBuilder<T, Builder<T>> {

        /**
         * Defines the {@link ScheduledExecutorService} used to schedule async polling attempts.
         *
         * @param scheduledExecutorService the schedule executor service
         * @return a reference to this object so that method calls can be chained together.
         */
        Builder<T> scheduledExecutorService(ScheduledExecutorService scheduledExecutorService);

        /**
         * An immutable object that is created from the properties that have been set on the builder.
         * @return a reference to this object so that method calls can be chained together.
         */
        AsyncWaiter<T> build();
    }
}