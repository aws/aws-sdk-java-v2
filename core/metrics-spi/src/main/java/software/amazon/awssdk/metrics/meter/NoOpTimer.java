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
 * A NoOp implementation of the {@link Timer} metric.
 * <p>
 * Always returns {@link Duration#ZERO} as the total duration.
 */
@SdkPublicApi
public final class NoOpTimer implements Timer {
    private static final NoOpTimer INSTANCE = new NoOpTimer();

    private NoOpTimer() {
    }

    @Override
    public void record(long duration, TimeUnit timeUnit) {
    }

    @Override
    public void record(Runnable task) {
        task.run();
    }

    @Override
    public <T> T record(Callable<T> task) throws Exception {
        return task.call();
    }

    @Override
    public void start() {
    }

    @Override
    public void end() {
    }

    @Override
    public Instant startTime() {
        return Instant.now();
    }

    @Override
    public Instant endTime() {
        return Instant.now();
    }

    @Override
    public Duration totalTime() {
        return Duration.ZERO;
    }

    public static NoOpTimer instance() {
        return INSTANCE;
    }
}
