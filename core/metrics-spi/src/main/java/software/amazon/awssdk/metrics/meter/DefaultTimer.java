/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A default implementation of {@link Timer} metric interface.
 */
@SdkPublicApi
public final class DefaultTimer implements Timer, ToCopyableBuilder<DefaultTimer.Builder, DefaultTimer> {

    private final Clock clock;
    private Instant startTime;
    private Instant endTime;

    private DefaultTimer(Builder builder) {
        this.clock = Validate.notNull(builder.clock, "Clock");
        this.startTime = clock.instant();
        this.endTime = null;
    }

    @Override
    public void record(long duration, TimeUnit timeUnit) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        this.endTime.plusNanos(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
    }

    @Override
    public void record(Runnable task) {
        startTime = clock.instant();
        try {
            task.run();
        } finally {
            endTime = clock.instant();
        }
    }

    @Override
    public <T> T record(Callable<T> task) {
        startTime = clock.instant();
        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            endTime = clock.instant();
        }
    }

    @Override
    public Instant startTime() {
        return startTime;
    }

    @Override
    public Instant endTime() {
        validateEndTime();
        return endTime;
    }

    @Override
    public Duration totalTime() {
        validateEndTime();
        return Duration.between(startTime, endTime);
    }

    private void validateEndTime() {
        if (endTime == null) {
            throw new IllegalStateException("End time was never updated. You need to call one of the "
                                            + "record() methods to set the end time");
        }
    }

    /**
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().clock(clock);
    }

    /**
     * Builder to configure the params for construction
     */
    public static final class Builder implements CopyableBuilder<Builder, DefaultTimer> {

        private Clock clock;

        private Builder() {
        }

        /**
         * Sets the {@link Clock} implementation the timer should use
         *
         * @param clock the {@link Clock} implementation used by the time
         * @return This object for method chaining
         */
        Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public DefaultTimer build() {
            return new DefaultTimer(this);
        }
    }
}
