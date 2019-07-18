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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.metrics.registry.MetricRegistry;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Helper class to transform the {@link MetricRegistry} instances into a list
 * of CloudWatch {@link MetricDatum} objects.
 */
@SdkInternalApi
public class MetricTransformer {

    private static final MetricTransformer INSTANCE = new MetricTransformer();

    private MetricTransformer() {
    }

    public static MetricTransformer getInstance() {
        return INSTANCE;
    }

    /**
     * Convert the metrics returned by {@link MetricRegistry#metrics()} into list of {@link MetricDatum}s.
     */
    public List<MetricDatum> transform(MetricRegistry metricRegistry) {
        List<MetricDatum> results = new ArrayList<>();

        for (Map.Entry<String, Metric> entry : metricRegistry.metrics().entrySet()) {
            Metric metric = entry.getValue();
            MetricDatum.Builder builder = MetricDatum.builder()
                                                     .metricName(entry.getKey())
                                                     .dimensions(dimensions(metric));

            if (metric instanceof Timer) {
                metricDatum((Timer) metric, builder);
            } else if (metric instanceof Counter) {
                metricDatum((Counter) metric, builder);
            } else if (metric instanceof Gauge) {
                Gauge gauge = (Gauge) metric;
                if (gauge.value() instanceof Number) {
                    updateMetricDatum(builder, (Number) gauge.value());
                } else {
                    continue;
                }
            }

            results.add(builder.build());
        }

        return results;
    }

    // TODO Add service and Operation from the MetricRegistry as dimensions
    private List<Dimension> dimensions(Metric metric) {
        List<Dimension> dimensions = new ArrayList<>();

        metric.categories().stream()
              .map(c -> Dimension.builder()
                                 .name("MetricCategory")
                                 .value(c.name())
                                 .build())
              .forEach(dimensions::add);

        return dimensions;
    }

    private void metricDatum(Timer metric, MetricDatum.Builder builder) {
        builder.value(Double.valueOf(metric.totalTime().toMillis()))
               .unit(StandardUnit.MILLISECONDS);
    }

    private void metricDatum(Counter metric, MetricDatum.Builder builder) {
        updateMetricDatum(builder, metric.count());
    }

    private void updateMetricDatum(MetricDatum.Builder builder, Number number) {
        builder.value(number.doubleValue())
               .unit(StandardUnit.COUNT);
    }
}
