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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.atomic.AtomicLong;
import software.amazon.awssdk.observability.metrics.SdkUpDownCounter;

public class MicrometerUpDownCounter implements SdkUpDownCounter {
    private final AtomicLong counter;
    private final Gauge gauge;

    /**
     * Creates a new MicrometerUpDownCounter with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param units The measurement units
     * @param description The metric description
     * @param tags Common tags to apply to the metric
     */
    public MicrometerUpDownCounter(MeterRegistry meterRegistry, String name, String units, String description, Tags tags) {
        this.counter = new AtomicLong(0);
        this.gauge = Gauge.builder(name, counter, AtomicLong::get)
                          .description(description)
                          .baseUnit(units)
                          .tags(tags)
                          .register(meterRegistry);
    }

    /**
     * Creates a new MicrometerUpDownCounter with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param units The measurement units
     * @param description The metric description
     */
    public MicrometerUpDownCounter(MeterRegistry meterRegistry, String name, String units, String description) {
        this(meterRegistry, name, units, description, Tags.empty());
    }

    @Override
    public void add(long value) {
        counter.set(value);
    }

    @Override
    public long get() {
        return counter.get();
    }
}
