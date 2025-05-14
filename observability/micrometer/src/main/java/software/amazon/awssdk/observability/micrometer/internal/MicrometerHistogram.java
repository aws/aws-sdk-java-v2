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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import software.amazon.awssdk.observability.metrics.SdkHistogram;

public class MicrometerHistogram implements SdkHistogram {
    private final DistributionSummary summary;

    /**
     * Creates a new MicrometerHistogram with the specified DistributionSummary.
     *
     * @param summary The Micrometer DistributionSummary to use
     */
    public MicrometerHistogram(DistributionSummary summary) {
        this.summary = summary;
    }

    /**
     * Creates a new MicrometerHistogram with the specified parameters.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param name The metric name (already includes scope prefix)
     * @param units The measurement units
     * @param description The metric description
     * @param tags Common tags to apply to the metric
     */
    public MicrometerHistogram(MeterRegistry meterRegistry, String name, String units, String description, Tags tags) {
        this(DistributionSummary.builder(name)
                                .description(description)
                                .baseUnit(units)
                                .tags(tags)
                                .register(meterRegistry));
    }

    @Override
    public void record(double value) {
        summary.record(value);
    }

    public long count() {
        return summary.count();
    }

    public double totalAmount() {
        return summary.totalAmount();
    }

    public double max() {
        return summary.max();
    }

    public double mean() {
        return summary.mean();
    }
}
