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

import java.util.Optional;
import java.util.function.Predicate;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Inspects the response or errors returned from the operation and determines whether an expected state is met and returns the
 * next {@link WaiterState} that the waiter should be transitioned to.
 */
@SdkPublicApi
public interface WaiterAcceptor<T> {

    /**
     * @return the next {@link WaiterState} that the waiter should be transitioned to
     */
    WaiterState waiterState();

    /**
     * Check to see if the response matches with the expected state defined by this acceptor
     *
     * @param response the response to inspect
     * @return whether it accepts the response
     */
    default boolean matches(T response) {
        return false;
    }

    /**
     * Check to see if the exception matches the expected state defined by this acceptor
     *
     * @param throwable the exception to inspect
     * @return whether it accepts the throwable
     */
    default boolean matches(Throwable throwable) {
        return false;
    }

    /**
     * Optional message to provide pertaining to the next WaiterState
     *
     * @return the optional message
     */
    default Optional<String> message() {
        return Optional.empty();
    }

    /**
     * Creates a success waiter acceptor which determines if the exception should transition the waiter to success state
     *
     * @param responsePredicate the predicate of the response
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> successOnResponseAcceptor(Predicate<T> responsePredicate) {
        Validate.paramNotNull(responsePredicate, "responsePredicate");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.SUCCESS;
            }

            @Override
            public boolean matches(T response) {
                return responsePredicate.test(response);
            }
        };
    }

    /**
     * Creates an error waiter acceptor which determines if the exception should transition the waiter to success state
     *
     * @param errorPredicate the {@link Throwable} predicate
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> successOnExceptionAcceptor(Predicate<Throwable> errorPredicate) {
        Validate.paramNotNull(errorPredicate, "errorPredicate");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.SUCCESS;
            }

            @Override
            public boolean matches(Throwable t) {
                return errorPredicate.test(t);
            }
        };
    }

    /**
     * Creates an error waiter acceptor which determines if the exception should transition the waiter to failure state
     *
     * @param errorPredicate the {@link Throwable} predicate
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> errorOnExceptionAcceptor(Predicate<Throwable> errorPredicate) {
        Validate.paramNotNull(errorPredicate, "errorPredicate");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.FAILURE;
            }

            @Override
            public boolean matches(Throwable t) {
                return errorPredicate.test(t);
            }
        };
    }

    /**
     * Creates a success waiter acceptor which determines if the exception should transition the waiter to success state
     *
     * @param responsePredicate the predicate of the response
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> errorOnResponseAcceptor(Predicate<T> responsePredicate) {
        Validate.paramNotNull(responsePredicate, "responsePredicate");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.FAILURE;
            }

            @Override
            public boolean matches(T response) {
                return responsePredicate.test(response);
            }
        };
    }

    /**
     * Creates a success waiter acceptor which determines if the exception should transition the waiter to success state
     *
     * @param responsePredicate the predicate of the response
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> errorOnResponseAcceptor(Predicate<T> responsePredicate, String message) {
        Validate.paramNotNull(responsePredicate, "responsePredicate");
        Validate.paramNotNull(message, "message");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.FAILURE;
            }

            @Override
            public boolean matches(T response) {
                return responsePredicate.test(response);
            }

            @Override
            public Optional<String> message() {
                return Optional.of(message);
            }
        };
    }

    /**
     * Creates a retry on exception waiter acceptor which determines if the exception should transition the waiter to retry state
     *
     * @param errorPredicate the {@link Throwable} predicate
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> retryOnExceptionAcceptor(Predicate<Throwable> errorPredicate) {
        Validate.paramNotNull(errorPredicate, "errorPredicate");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.RETRY;
            }

            @Override
            public boolean matches(Throwable t) {
                return errorPredicate.test(t);
            }
        };
    }

    /**
     * Creates a retry on exception waiter acceptor which determines if the exception should transition the waiter to retry state
     *
     * @param responsePredicate the {@link Throwable} predicate
     * @param <T> the response type
     * @return a {@link WaiterAcceptor}
     */
    static <T> WaiterAcceptor<T> retryOnResponseAcceptor(Predicate<T> responsePredicate) {
        Validate.paramNotNull(responsePredicate, "responsePredicate");
        return new WaiterAcceptor<T>() {
            @Override
            public WaiterState waiterState() {
                return WaiterState.RETRY;
            }

            @Override
            public boolean matches(T t) {
                return responsePredicate.test(t);
            }
        };
    }
}
