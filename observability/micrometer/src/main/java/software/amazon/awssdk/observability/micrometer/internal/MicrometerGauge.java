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
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.observability.metrics.SdkGauge;

public class MicrometerGauge implements SdkGauge {
    private final AtomicReference<Double> value;
    private final Gauge gauge;

    /**
     * Creates a new MicrometerGauge with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param description The metric description
     * @param tags Common tags to apply to the metric
     */
    public MicrometerGauge(MeterRegistry meterRegistry, String name, String description, Tags tags) {
        this.value = new AtomicReference<>(0.0);
        this.gauge = Gauge.builder(name, value, AtomicReference::get)
                          .description(description)
                          .tags(tags)
                          .register(meterRegistry);
    }

    /**
     * Creates a new MicrometerGauge with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param description The metric description
     */
    public MicrometerGauge(MeterRegistry meterRegistry, String name, String description) {
        this(meterRegistry, name, description, Tags.empty());
    }

    /**
     * Creates a new MicrometerGauge with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param description The metric description
     * @param tagKeys The tag keys
     * @param tagValues The tag values
     */
    public MicrometerGauge(MeterRegistry meterRegistry, String name, String description, String[] tagKeys, String[] tagValues) {
        this(meterRegistry, name, description, createTags(tagKeys, tagValues));
    }

    /**
     * Creates a new MicrometerGauge with the specified parameters.
     * This constructor is for backward compatibility.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param description The metric description
     * @param tags Array of alternating tag keys and values
     */
    public MicrometerGauge(MeterRegistry meterRegistry, String name, String description, String... tags) {
        this(meterRegistry, name, description, Tags.of(tags));
    }

    @Override
    public void set(double value) {
        this.value.set(value);
    }

    @Override
    public double get() {
        return this.value.get();
    }

    /**
     * Creates Tags from separate arrays of keys and values.
     *
     * @param keys The tag keys
     * @param values The tag values
     * @return Tags object
     */
    private static Tags createTags(String[] keys, String[] values) {
        if (keys == null || values == null || keys.length != values.length) {
            return Tags.empty();
        }

        List<Tag> tags = new ArrayList<>(keys.length);
        for (int i = 0; i < keys.length; i++) {
            tags.add(Tag.of(keys[i], values[i]));
        }
        return Tags.of(tags);
    }
}
