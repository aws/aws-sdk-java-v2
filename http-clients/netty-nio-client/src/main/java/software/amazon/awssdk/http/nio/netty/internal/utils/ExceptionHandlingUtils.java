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

package software.amazon.awssdk.http.nio.netty.internal.utils;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class ExceptionHandlingUtils {

    private ExceptionHandlingUtils() {
    }

    /**
     * Runs a task within try-catch block. All exceptions thrown from the execution
     * will be sent to errorNotifier.
     *
     * <p>
     * This is useful for single-line executable code to avoid try-catch code clutter.
     *
     * @param executable the task to run
     * @param errorNotifier error notifier
     */
    public static void tryCatch(Runnable executable, Consumer<Throwable> errorNotifier) {
        try {
            executable.run();
        } catch (Throwable throwable) {
            errorNotifier.accept(throwable);
        }
    }

    /**
     *  Runs a task within try-catch-finally block. All exceptions thrown from the execution
     *  will be sent to errorNotifier.
     *
     * <p>
     * This is useful for single-line executable code to avoid try-catch code clutter.
     *
     * @param executable the task to run
     * @param errorNotifier the error notifier
     * @param cleanupExecutable the cleanup executable
     * @param <T> the type of the object to be returned
     * @return the object if succeeds
     */
    public static <T> T tryCatchFinally(Callable<T> executable,
                                        Consumer<Throwable> errorNotifier,
                                        Runnable cleanupExecutable) {
        try {
            return executable.call();
        } catch (Throwable throwable) {
            errorNotifier.accept(throwable);
        } finally {
            cleanupExecutable.run();
        }
        return null;
    }
}
