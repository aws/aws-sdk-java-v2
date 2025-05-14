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

package software.amazon.awssdk.observability.micrometer.internal;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import software.amazon.awssdk.observability.metrics.SdkMonotonicCounter;

public class MicrometerMonotonicCounter implements SdkMonotonicCounter {
    private final Counter counter;

    public MicrometerMonotonicCounter(Counter counter) {
        this.counter = counter;
    }

    /**
     * Creates a new MicrometerMonotonicCounter with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param units The measurement units
     * @param description The metric description
     * @param tags Common tags to apply to the metric
     */
    public MicrometerMonotonicCounter(MeterRegistry meterRegistry, String name, String units, String description, Tags tags) {
        this(Counter.builder(name)
                    .description(description)
                    .baseUnit(units)
                    .tags(tags)
                    .register(meterRegistry));
    }

    @Override
    public void add(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Monotonic counter can only be incremented by non-negative values");
        }
        counter.increment(value);
    }

    public void increment() {
        counter.increment();
    }

    public double count() {
        return counter.count();
    }
}
