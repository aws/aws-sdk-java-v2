/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.spi;

/**
 * A {@link TimingInfo} that is unmodifiable.
 */
final class TimingInfoUnmodifiable extends TimingInfo {
    /**
     * @param startEpochTimeMilli start time since epoch in millisecond; or null if not known
     * @param startTimeNano       start time in nanosecond
     * @param endTimeNano         end time in nanosecond; or null if not known
     * @see TimingInfo#unmodifiableTimingInfo(long, Long)
     * @see TimingInfo#unmodifiableTimingInfo(long, long, Long)
     */
    TimingInfoUnmodifiable(Long startEpochTimeMilli, long startTimeNano, Long endTimeNano) {
        super(startEpochTimeMilli, startTimeNano, endTimeNano);
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    public void setEndTime(long ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    public void setEndTimeNano(long ignored) {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    @Override
    public TimingInfo endTiming() {
        throw new UnsupportedOperationException();
    }
}
