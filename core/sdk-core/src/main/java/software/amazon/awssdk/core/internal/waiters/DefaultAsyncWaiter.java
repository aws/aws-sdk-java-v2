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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.waiters.AsyncWaiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;

/**
 * Default implementation of the generic {@link AsyncWaiter}.
 * @param <T> the type of the response expected to return from the polling function
 */
@SdkInternalApi
@ThreadSafe
public final class DefaultAsyncWaiter<T> implements AsyncWaiter<T> {
    private final ScheduledExecutorService executorService;
    private final List<WaiterAcceptor<? super T>> waiterAcceptors;
    private final AsyncWaiterExecutor<T> handler;

    private DefaultAsyncWaiter(DefaultBuilder<T> builder) {
        this.executorService = builder.scheduledExecutorService;
        WaiterConfiguration configuration = new WaiterConfiguration(builder.overrideConfiguration);
        this.waiterAcceptors = Collections.unmodifiableList(builder.waiterAcceptors);
        this.handler = new AsyncWaiterExecutor<>(configuration, waiterAcceptors, executorService);
    }

    @Override
    public CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction) {
        return handler.execute(asyncPollingFunction);
    }

    @Override
    public CompletableFuture<WaiterResponse<T>> runAsync(Supplier<CompletableFuture<T>> asyncPollingFunction,
                                                         WaiterOverrideConfiguration overrideConfig) {
        return new AsyncWaiterExecutor<>(new WaiterConfiguration(overrideConfig), waiterAcceptors, executorService)
            .execute(asyncPollingFunction);
    }

    public static <T> Builder<T> builder() {
        return new DefaultBuilder<>();
    }

    public static final class DefaultBuilder<T> implements Builder<T> {
        private List<WaiterAcceptor<? super T>> waiterAcceptors = new ArrayList<>();
        private ScheduledExecutorService scheduledExecutorService;
        private WaiterOverrideConfiguration overrideConfiguration;

        private DefaultBuilder() {
        }

        @Override
        public Builder<T> scheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            return this;
        }

        @Override
        public Builder<T> acceptors(List<WaiterAcceptor<? super T>> waiterAcceptors) {
            this.waiterAcceptors = new ArrayList<>(waiterAcceptors);
            return this;
        }

        @Override
        public Builder<T> overrideConfiguration(WaiterOverrideConfiguration overrideConfiguration) {
            this.overrideConfiguration = overrideConfiguration;
            return this;
        }

        @Override
        public Builder<T> addAcceptor(WaiterAcceptor<? super T> waiterAcceptor) {
            waiterAcceptors.add(waiterAcceptor);
            return this;
        }

        public DefaultAsyncWaiter<T> build() {
            return new DefaultAsyncWaiter<>(this);
        }
    }
}