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
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Represents a value that can be either a response or a Throwable
 *
 * @param <R> response type
 */
@SdkPublicApi
public final class ResponseOrException<R> {

    private final Optional<R> response;
    private final Optional<Throwable> exception;

    private ResponseOrException(Optional<R> response, Optional<Throwable> exception) {
        this.response = response;
        this.exception = exception;
    }

    /**
     * @return the optional response that has matched with the waiter success condition
     */
    public Optional<R> response() {
        return response;
    }

    /**
     * @return the optional exception that has matched with the waiter success condition
     */
    public Optional<Throwable> exception() {
        return exception;
    }

    /**
     * Create a new ResponseOrException with the response
     *
     * @param value response
     * @param <R> Response type
     */
    public static <R> ResponseOrException<R> response(R value) {
        return new ResponseOrException<>(Optional.of(value), Optional.empty());
    }

    /**
     * Create a new ResponseOrException with the exception
     *
     * @param value exception
     * @param <R> Response type
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

