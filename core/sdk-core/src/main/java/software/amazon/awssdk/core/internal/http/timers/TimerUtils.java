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

package software.amazon.awssdk.core.internal.http.timers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkInternalApi
public final class TimerUtils {

    private TimerUtils() {
    }

    /**
     * Schedule a {@link TimeoutTask} and exceptional completes a {@link CompletableFuture} with the provide exception
     * if not otherwise completed before the given timeout.
     *
     * @param completableFuture the completableFuture to be timed
     * @param timeoutExecutor the executor to execute the {@link TimeoutTask}
     * @param exceptionSupplier the exception to thrown after timeout
     * @param timeoutInMills the timeout in milliseconds.
     * @param <T> the type of the {@link CompletableFuture}
     * @return a {@link TimeoutTracker}
     */
    public static <T> TimeoutTracker timeAsyncTaskIfNeeded(CompletableFuture<T> completableFuture,
                                                           ScheduledExecutorService timeoutExecutor,
                                                           Supplier<SdkClientException> exceptionSupplier,
                                                           long timeoutInMills) {
        if (timeoutInMills <= 0) {
            return NoOpTimeoutTracker.INSTANCE;
        }

        TimeoutTask timeoutTask = new AsyncTimeoutTask(completableFuture, exceptionSupplier);

        ScheduledFuture<?> scheduledFuture =
            timeoutExecutor.schedule(timeoutTask,
                                     timeoutInMills,
                                     TimeUnit.MILLISECONDS);
        TimeoutTracker timeoutTracker = new ApiCallTimeoutTracker(timeoutTask, scheduledFuture);

        completableFuture.whenComplete((o, t) -> timeoutTracker.cancel());

        return timeoutTracker;
    }

    /**
     * Schedule a {@link TimeoutTask} that aborts the task if not otherwise completed before the given timeout.
     *
     * @param timeoutExecutor the executor to execute the {@link TimeoutTask}
     * @param timeoutInMills the timeout in milliseconds.
     * @param threadToInterrupt the thread to interrupt
     * @return a {@link TimeoutTracker}
     */
    public static TimeoutTracker timeSyncTaskIfNeeded(ScheduledExecutorService timeoutExecutor,
                                                      long timeoutInMills,
                                                      Thread threadToInterrupt) {
        if (timeoutInMills <= 0) {
            return NoOpTimeoutTracker.INSTANCE;
        }

        SyncTimeoutTask timeoutTask = new SyncTimeoutTask(threadToInterrupt);

        ScheduledFuture<?> scheduledFuture =
            timeoutExecutor.schedule(timeoutTask,
                                     timeoutInMills,
                                     TimeUnit.MILLISECONDS);
        return new ApiCallTimeoutTracker(timeoutTask, scheduledFuture);
    }

    public static long resolveTimeoutInMillis(Supplier<Optional<Duration>> supplier, Duration fallback) {
        return OptionalUtils.firstPresent(supplier.get(), () -> fallback)
                            .map(Duration::toMillis)
                            .orElse(0L);
    }
}
