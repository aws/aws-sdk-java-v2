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

package software.amazon.awssdk.metrics.meter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Type of {@link Metric} to record the timing information. This is used to record start time, end time of tasks
 * and compute the latency.
 */
@SdkPublicApi
public interface Timer extends Metric {

    /**
     * Adds the given duration to the recorded timing info.
     *
     * Calling this method will update the result of {@link #endTime()} and {@link #totalTime()} methods.
     *
     * @param duration Duration of the event to be added to the timer
     * @param timeUnit Time unit of the duration
     */
    void record(long duration, TimeUnit timeUnit);

    /**
     * Runs the given runnable task and records the duration.
     * @param task the task the run
     */
    void record(Runnable task);

    /**
     * Runs the given callable task and records the duration.
     *
     * @param task The task to call
     * @param <T> Return type of the Callable result
     * @return Result of the Callable task
     */
    <T> T record(Callable<T> task) throws Exception;

    /**
     * Record the current time to indicate start of an action. This affects the {@link #startTime()} in the timer.
     */
    void start();

    /**
     * Record the current time to indicate the end of a previously started action.
     * This affects the {@link #endTime()} ()} in the timer.
     */
    void end();

    /**
     * @return the start time recorded in the Timer
     */
    Instant startTime();

    /**
     * @return the end time recorded in the Timer
     */
    Instant endTime();

    /**
     * @return the latency stored in the Timer. This is the difference between {@link #endTime()} and {@link #startTime()}.
     */
    Duration totalTime();
}
