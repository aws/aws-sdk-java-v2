/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;

public final class FunctionalUtils {

    private FunctionalUtils() {
    }

    /**
     * Runs a given {@link UnsafeRunnable} and logs an error without throwing.
     *
     * @param errorMsg Message to log with exception thrown.
     * @param runnable Action to perform.
     */
    public static void runAndLogError(Logger log, String errorMsg, UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error(errorMsg, e);
        }
    }

    /**
     * A wrapper around a Consumer that throws a checked exception.
     *
     * @param unsafeConsumer - something that acts like a consumer but throws an exception
     * @return a consumer that is wrapped in a try-catch converting the checked exception into a runtime exception
     */
    public static <I> Consumer<I> safeConsumer(UnsafeConsumer<I> unsafeConsumer) {
        return (input) -> {
            try {
                unsafeConsumer.accept(input);
            } catch (Exception e) {
                throw asRuntimeException(e);
            }
        };
    }

    /**
     * Takes a functional interface that throws an exception and returns a {@link Function} that deals with that exception by
     * wrapping in a runtime exception. Useful for APIs that use the standard Java functional interfaces that don't throw checked
     * exceptions.
     *
     * @param unsafeFunction Functional interface that throws checked exception.
     * @param <T>            Input
     * @param <R>            Output
     * @return New {@link Function} that handles checked exception.
     */
    public static <T, R> Function<T, R> safeFunction(UnsafeFunction<T, R> unsafeFunction) {
        return t -> {
            try {
                return unsafeFunction.apply(t);
            } catch (Exception e) {
                throw asRuntimeException(e);
            }
        };
    }

    /**
     * A wrapper around a BiConsumer that throws a checked exception.
     *
     * @param unsafeSupplier - something that acts like a BiConsumer but throws an exception
     * @return a consumer that is wrapped in a try-catch converting the checked exception into a runtime exception
     */
    public static <T> Supplier<T> safeSupplier(UnsafeSupplier<T> unsafeSupplier) {
        return () -> {
            try {
                return unsafeSupplier.get();
            } catch (Exception e) {
                throw asRuntimeException(e);
            }
        };
    }

    /**
     * A wrapper around a Runnable that throws a checked exception.
     *
     * @param unsafeRunnable Something that acts like a Runnable but throws an exception
     * @return A Runnable that is wrapped in a try-catch converting the checked exception into a runtime exception
     */
    public static Runnable safeRunnable(UnsafeRunnable unsafeRunnable) {
        return () -> {
            try {
                unsafeRunnable.run();
            } catch (Exception e) {
                throw asRuntimeException(e);
            }
        };
    }

    public static <I, O> Function<I, O> toFunction(Supplier<O> supplier) {
        return ignore -> supplier.get();
    }

    public static <T> T invokeSafely(UnsafeSupplier<T> unsafeSupplier) {
        return safeSupplier(unsafeSupplier).get();
    }

    public static void invokeSafely(UnsafeRunnable unsafeRunnable) {
        safeRunnable(unsafeRunnable).run();
    }

    /**
     * Equivalent of {@link Consumer} that throws a checked exception.
     */
    @FunctionalInterface
    public interface UnsafeConsumer<I> {
        void accept(I input) throws Exception;
    }

    /**
     * Equivalent of {@link Function} that throws a checked exception.
     */
    @FunctionalInterface
    public interface UnsafeFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Equivalent of {@link Supplier} that throws a checked exception.
     */
    @FunctionalInterface
    public interface UnsafeSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Equivalent of {@link Runnable} that throws a checked exception.
     */
    @FunctionalInterface
    public interface UnsafeRunnable {
        void run() throws Exception;
    }

    private static RuntimeException asRuntimeException(Exception exception) {
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        if (exception instanceof IOException) {
            return new UncheckedIOException((IOException) exception);
        }
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return new RuntimeException(exception);
    }
}
