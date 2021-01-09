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

import static software.amazon.awssdk.utils.Validate.mutuallyExclusive;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * The default implementation of the {@link WaiterResponse}
 */
@SdkInternalApi
public final class DefaultWaiterResponse<T> implements WaiterResponse<T> {
    private final T result;
    private final Throwable exception;
    private final int attemptsExecuted;
    private final ResponseOrException<T> matched;

    private DefaultWaiterResponse(Builder<T> builder) {
        mutuallyExclusive("response and exception are mutually exclusive, set only one on the Builder",
                          builder.response, builder.exception);
        this.result = builder.response;
        this.exception = builder.exception;
        this.attemptsExecuted = Validate.paramNotNull(builder.attemptsExecuted, "attemptsExecuted");
        Validate.isPositive(builder.attemptsExecuted, "attemptsExecuted");
        matched = result != null ?
                  ResponseOrException.response(result) :
                  ResponseOrException.exception(exception);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public ResponseOrException<T> matched() {
        return matched;
    }

    @Override
    public int attemptsExecuted() {
        return attemptsExecuted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultWaiterResponse<?> that = (DefaultWaiterResponse<?>) o;

        if (attemptsExecuted != that.attemptsExecuted) {
            return false;
        }
        if (!Objects.equals(result, that.result)) {
            return false;
        }
        return Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {
        int result1 = result != null ? result.hashCode() : 0;
        result1 = 31 * result1 + (exception != null ? exception.hashCode() : 0);
        result1 = 31 * result1 + attemptsExecuted;
        return result1;
    }

    @Override
    public String toString() {
        ToString toString = ToString.builder("DefaultWaiterResponse")
                                    .add("attemptsExecuted", attemptsExecuted);

        matched.response().ifPresent(r -> toString.add("response", result));
        matched.exception().ifPresent(r -> toString.add("exception", exception));

        return toString.build();
    }

    public static final class Builder<T> {
        private T response;
        private Throwable exception;
        private Integer attemptsExecuted;

        private Builder() {
        }

        /**
         * Defines the response received that has matched with the waiter success condition.
         *
         * @param response the response
         * @return the chained builder
         */
        public Builder<T> response(T response) {
            this.response = response;
            return this;
        }

        /**
         * Defines the exception thrown from the waiter operation that has matched with the waiter success condition
         *
         * @param exception the exception
         * @return the chained builder
         */
        public Builder<T> exception(Throwable exception) {
            this.exception = exception;
            return this;
        }

        /**
         * Defines the number of attempts executed in the waiter operation
         *
         * @param attemptsExecuted the number of attempts
         * @return the chained builder
         */
        public Builder<T> attemptsExecuted(Integer attemptsExecuted) {
            this.attemptsExecuted = attemptsExecuted;
            return this;
        }

        public WaiterResponse<T> build() {
            return new DefaultWaiterResponse<>(this);
        }
    }
}
