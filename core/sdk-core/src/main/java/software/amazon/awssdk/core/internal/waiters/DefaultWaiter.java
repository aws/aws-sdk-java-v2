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
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.core.waiters.WaiterAcceptor;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of the generic {@link Waiter}.
 * @param <T> the type of the response expected to return from the polling function
 */
@SdkInternalApi
@ThreadSafe
public final class DefaultWaiter<T> implements Waiter<T> {
    private final WaiterConfiguration waiterConfiguration;
    private final List<WaiterAcceptor<? super T>> waiterAcceptors;
    private final WaiterExecutor<T> waiterExecutor;

    private DefaultWaiter(DefaultBuilder<T> builder) {
        this.waiterConfiguration = new WaiterConfiguration(builder.overrideConfiguration);
        this.waiterAcceptors = Collections.unmodifiableList(builder.waiterAcceptors);
        this.waiterExecutor = new WaiterExecutor<>(waiterConfiguration, waiterAcceptors);
    }

    @Override
    public WaiterResponse<T> run(Supplier<T> pollingFunction) {
        return waiterExecutor.execute(pollingFunction);
    }

    @Override
    public WaiterResponse<T> run(Supplier<T> pollingFunction, WaiterOverrideConfiguration overrideConfiguration) {
        Validate.paramNotNull(overrideConfiguration, "overrideConfiguration");
        return new WaiterExecutor<>(new WaiterConfiguration(overrideConfiguration),
                                    waiterAcceptors).execute(pollingFunction);
    }

    public static <T> Builder<T> builder() {
        return new DefaultBuilder<>();
    }

    public static final class DefaultBuilder<T> implements Builder<T> {
        private List<WaiterAcceptor<? super T>> waiterAcceptors = new ArrayList<>();
        private WaiterOverrideConfiguration overrideConfiguration;

        private DefaultBuilder() {
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

        @Override
        public Waiter<T> build() {
            return new DefaultWaiter<>(this);
        }
    }
}