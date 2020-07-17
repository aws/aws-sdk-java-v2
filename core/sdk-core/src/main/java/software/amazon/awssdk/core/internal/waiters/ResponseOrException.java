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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Represents a value that can be either a response or a Throwable
 *
 * @param <R> response type
 */
@SdkProtectedApi
public final class ResponseOrException<R> {

    private final Optional<R> response;
    private final Optional<Throwable> exception;

    private ResponseOrException(Optional<R> l, Optional<Throwable> r) {
        response = l;
        exception = r;
    }

    /**
     * Maps the Either to a type and returns the resolved value (which may be from the left or the right value).
     *
     * @param lFunc Function that maps the left value if present.
     * @param rFunc Function that maps the right value if present.
     * @param <T>   Type that both the left and right should be mapped to.
     * @return Mapped value from either lFunc or rFunc depending on which value is present.
     */
    public <T> T map(
            Function<? super R, ? extends T> lFunc,
            Function<? super Throwable, ? extends T> rFunc) {
        return response.<T>map(lFunc).orElseGet(() -> exception.map(rFunc).get());
    }

    public Optional<R> response() {
        return response;
    }

    public Optional<Throwable> exception() {
        return exception;
    }

    /**
     * Apply the consumers to the left or the right value depending on which is present.
     *
     * @param lFunc Consumer of left value, invoked if left value is present.
     * @param rFunc Consumer of right value, invoked if right value is present.
     */
    public void apply(Consumer<? super R> lFunc, Consumer<? super Throwable> rFunc) {
        response.ifPresent(lFunc);
        exception.ifPresent(rFunc);
    }

    /**
     * Create a new Either with the left type.
     *
     * @param value Left value
     * @param <R>   Left type
     */
    public static <R> ResponseOrException<R> response(R value) {
        return new ResponseOrException<>(Optional.of(value), Optional.empty());
    }

    /**
     * Create a new Either with the right type.
     *
     * @param value Right value
     * @param <R>   Left type
     */
    public static <R> ResponseOrException<R> exception(Throwable value) {
        return new ResponseOrException<>(Optional.empty(), Optional.of(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResponseOrException)) {
            return false;
        }

        ResponseOrException<?> either = (ResponseOrException<?>) o;

        return response.equals(either.response) && exception.equals(either.exception);
    }

    @Override
    public int hashCode() {
        return 31 * response.hashCode() + exception.hashCode();
    }
}

