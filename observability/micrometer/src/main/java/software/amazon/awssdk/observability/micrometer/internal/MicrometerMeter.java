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
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import software.amazon.awssdk.observability.attributes.Attributes;
import software.amazon.awssdk.observability.metrics.SdkGauge;
import software.amazon.awssdk.observability.metrics.SdkMeter;
import software.amazon.awssdk.observability.metrics.SdkHistogram;
import software.amazon.awssdk.observability.metrics.SdkMonotonicCounter;
import software.amazon.awssdk.observability.metrics.SdkUpDownCounter;

public class MicrometerMeter implements SdkMeter {
    private final MeterRegistry meterRegistry;
    private final String scope;
    private final Tags commonTags;

    /**
     * Creates a new MicrometerMeter with the specified registry and scope.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param scope The instrumentation scope name, used as metric name prefix
     */
    public MicrometerMeter(MeterRegistry meterRegistry, String scope) {
        this(meterRegistry, scope, Tags.empty());
    }

    /**
     * Creates a new MicrometerMeter with the specified registry, scope, and common tags.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param scope The instrumentation scope name, used as metric name prefix
     * @param commonTags Common tags to apply to all metrics
     */
    public MicrometerMeter(MeterRegistry meterRegistry, String scope, Tags commonTags) {
        this.meterRegistry = meterRegistry;
        this.scope = scope != null && !scope.isEmpty() ? scope : "";
        this.commonTags = commonTags != null ? commonTags : Tags.empty();
    }

    /**
     * Creates a new MicrometerMeter with the specified registry, scope, and attributes.
     *
     * @param meterRegistry The Micrometer registry to use
     * @param scope The instrumentation scope name, used as metric name prefix
     * @param attributes Common attributes to apply to all metrics
     */
    public MicrometerMeter(MeterRegistry meterRegistry, String scope, Attributes attributes) {
        this(meterRegistry, scope, MicrometerAttributeConverter.toMicrometerTags(attributes));
    }

    @Override
    public SdkMonotonicCounter counter(String name, String units, String description) {
        String metricName = getMetricName(name);
        return new MicrometerMonotonicCounter(meterRegistry, metricName, units, description, commonTags);
    }

    @Override
    public SdkMonotonicCounter counter(String name, String units, String description, Attributes attributes) {
        String metricName = getMetricName(name);
        Tags tags = MicrometerAttributeConverter.toMicrometerTags(attributes);
        return new MicrometerMonotonicCounter(
            meterRegistry,
            metricName,
            units,
            description,
            Tags.concat(commonTags, tags)
        );
    }

    @Override
    public SdkUpDownCounter upDownCounter(String name, String units, String description) {
        String metricName = getMetricName(name);
        return new MicrometerUpDownCounter(meterRegistry, metricName, units, description, commonTags);
    }

    @Override
    public SdkUpDownCounter upDownCounter(String name, String units, String description, Attributes attributes) {
        String metricName = getMetricName(name);
        Tags tags = MicrometerAttributeConverter.toMicrometerTags(attributes);
        return new MicrometerUpDownCounter(
            meterRegistry,
            metricName,
            units,
            description,
            Tags.concat(commonTags, tags)
        );
    }

    @Override
    public SdkGauge gauge(String name, String units, String description) {
        String metricName = getMetricName(name);
        return new MicrometerGauge(meterRegistry, metricName, description, commonTags);
    }

    @Override
    public SdkGauge gauge(String name, String units, String description, Attributes attributes) {
        String metricName = getMetricName(name);
        Tags tags = MicrometerAttributeConverter.toMicrometerTags(attributes);
        return new MicrometerGauge(
            meterRegistry,
            metricName,
            description,
            Tags.concat(commonTags, tags)
        );
    }

    @Override
    public SdkHistogram histogram(String name, String units, String description) {
        String metricName = getMetricName(name);
        return new MicrometerHistogram(meterRegistry, metricName, units, description, commonTags);
    }

    @Override
    public SdkHistogram histogram(String name, String units, String description, Attributes attributes) {
        String metricName = getMetricName(name);
        Tags tags = MicrometerAttributeConverter.toMicrometerTags(attributes);
        return new MicrometerHistogram(
            meterRegistry,
            metricName,
            units,
            description,
            Tags.concat(commonTags, tags)
        );
    }

    /**
     * Creates a metric name with the scope as prefix if available.
     *
     * @param name The base metric name
     * @return The full metric name with scope prefix
     */
    private String getMetricName(String name) {
        if (scope == null || scope.isEmpty()) {
            return name;
        }
        return scope + "." + name;
    }
}
