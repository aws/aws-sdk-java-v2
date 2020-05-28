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

package software.amazon.awssdk.testutils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * This retries a particular function multiple times until it returns an expected result (or fails with an exception). Certain
 * expected exception types can be ignored.
 */
public final class Waiter<T> {
    private static final Logger log = Logger.loggerFor(Waiter.class);

    private final Supplier<T> thingToTry;
    private Predicate<T> whenToStop = t -> true;
    private Predicate<T> whenToFail = t -> false;
    private Set<Class<? extends Throwable>> whatExceptionsToStopOn = Collections.emptySet();
    private Set<Class<? extends Throwable>> whatExceptionsToIgnore = Collections.emptySet();

    /**
     * @see #run(Supplier)
     */
    private Waiter(Supplier<T> thingToTry) {
        Validate.paramNotNull(thingToTry, "thingToTry");
        this.thingToTry = thingToTry;
    }

    /**
     * Create a waiter that attempts executing the provided function until the condition set with {@link #until(Predicate)} is
     * met or until it throws an exception. Expected exception types can be ignored with {@link #ignoringException(Class[])}.
     */
    public static <T> Waiter<T> run(Supplier<T> thingToTry) {
        return new Waiter<>(thingToTry);
    }

    /**
     * Define the condition on the response under which the thing we are trying is complete.
     *
     * If this isn't set, it will always be true. ie. if the function call succeeds, we stop waiting.
     */
    public Waiter<T> until(Predicate<T> whenToStop) {
        this.whenToStop = whenToStop;
        return this;
    }

    /**
     * Define the condition on the response under which the thing we are trying has already failed and further
     * attempts are pointless.
     *
     * If this isn't set, it will always be false.
     */
    public Waiter<T> failOn(Predicate<T> whenToFail) {
        this.whenToFail = whenToFail;
        return this;
    }

    /**
     * Define the condition on an exception thrown under which the thing we are trying is complete.
     *
     * If this isn't set, it will always be false. ie. never stop on any particular exception.
     */
    @SafeVarargs
    public final Waiter<T> untilException(Class<? extends Throwable>... whenToStopOnException) {
        this.whatExceptionsToStopOn = new HashSet<>(Arrays.asList(whenToStopOnException));
        return this;
    }

    /**
     * Define the exception types that should be ignored if the thing we are trying throws them.
     */
    @SafeVarargs
    public final Waiter<T> ignoringException(Class<? extends Throwable>... whatToIgnore) {
        this.whatExceptionsToIgnore = new HashSet<>(Arrays.asList(whatToIgnore));
        return this;
    }

    /**
     * Execute the function, returning true if the thing we're trying does not succeed after 30 seconds.
     */
    public boolean orReturnFalse() {
        try {
            orFail();
            return true;
        } catch (AssertionError e) {
            return false;
        }
    }

    /**
     * Execute the function, throwing an assertion error if the thing we're trying does not succeed after 30 seconds.
     */
    public T orFail() {
        return orFailAfter(Duration.ofMinutes(1));
    }

    /**
     * Execute the function, throwing an assertion error if the thing we're trying does not succeed after the provided duration.
     */
    public T orFailAfter(Duration howLongToTry) {
        Validate.paramNotNull(howLongToTry, "howLongToTry");

        Instant start = Instant.now();
        int attempt = 0;

        while (Duration.between(start, Instant.now()).compareTo(howLongToTry) < 0) {
            ++attempt;
            try {
                if (attempt > 1) {
                    wait(attempt);
                }

                T result = thingToTry.get();
                if (whenToStop.test(result)) {
                    log.info(() -> "Got expected response: " + result);
                    return result;
                } else if (whenToFail.test(result)) {
                    throw new AssertionError("Received a response that matched the failOn predicate: " + result);
                }
                int unsuccessfulAttempt = attempt;
                log.info(() -> "Attempt " + unsuccessfulAttempt + " failed predicate.");
            } catch (RuntimeException e) {
                Throwable t = e instanceof CompletionException ? e.getCause() : e;

                if (whatExceptionsToStopOn.contains(t.getClass())) {
                    log.info(() -> "Got expected exception: " + t.getClass().getSimpleName());
                    return null;
                }

                if (whatExceptionsToIgnore.contains(t.getClass())) {
                    int unsuccessfulAttempt = attempt;
                    log.info(() -> "Attempt " + unsuccessfulAttempt +
                                   " failed with an expected exception (" + t.getClass() + ")");
                } else {
                    throw e;
                }
            }
        }

        throw new AssertionError("Condition was not met after " + attempt + " attempts (" +
                                 Duration.between(start, Instant.now()).getSeconds() + " seconds)");
    }

    private void wait(int attempt) {
        int howLongToWaitMs = 250 << Math.min(attempt - 1, 4); // Max = 250 * 2^4 = 4_000.

        try {
            Thread.sleep(howLongToWaitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
