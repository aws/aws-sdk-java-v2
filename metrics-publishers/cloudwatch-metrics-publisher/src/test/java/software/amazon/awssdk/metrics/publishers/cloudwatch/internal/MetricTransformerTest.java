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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.internal.http.pipeline.stages.utils.MetricUtils;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.Timer;
import software.amazon.awssdk.metrics.metrics.SdkDefaultMetric;
import software.amazon.awssdk.metrics.registry.DefaultMetricEvents;
import software.amazon.awssdk.metrics.MetricEvents;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

@RunWith(MockitoJUnitRunner.class)
public class MetricTransformerTest {

    private static final String SERVICE = "myawesomeservice";
    private static final String OPERATION = "superoperation";

    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Double COUNT = 5.0;
    private static final Double VALUE = 10.0;
    private static final MetricTransformer transformer = MetricTransformer.getInstance();

    @Mock
    private Timer timer;

    @Mock
    private Counter counter;

    @Mock
    private Gauge gauge;

    @Before
    public void setup() {
        when(timer.totalTime()).thenReturn(ONE_HOUR, Duration.ZERO);
        when(counter.count()).thenReturn(COUNT);
        when(gauge.value()).thenReturn(VALUE);
        when(gauge.categories()).thenReturn(Collections.singleton(MetricCategory.DEFAULT));
    }

    @Test
    public void properlyTransformMetrics() {
        String timer1 = "timer1", timer2 = "timer2";
        String counterName = "counter";
        String gaugeName = "gauge";

        MetricEvents registry = DefaultMetricEvents.create();
        addRequiredDimensions(registry);
        registry.register(timer1, timer);
        registry.register(timer2, timer);
        registry.register(counterName, counter);
        registry.register(gaugeName, gauge);

        List<MetricDatum> datums = transformer.transform(registry);
        assertThat(datums.size()).isEqualTo(6); // + 2 because of SERVICE and OPERATION

        List<String> names = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        List<StandardUnit> units = new ArrayList<>();
        List<Dimension> dimensions = new ArrayList<>();

        datums.forEach(m -> {
            names.add(m.metricName());
            values.add(m.value());
            units.add(m.unit());
            dimensions.addAll(m.dimensions());
        });

        assertThat(names).containsExactlyInAnyOrder(timer1, timer2, counterName, gaugeName, SdkDefaultMetric.SERVICE.name(), SdkDefaultMetric.OPERATION.name());
        assertThat(values).containsExactlyInAnyOrder(Double.valueOf(ONE_HOUR.toMillis()), 0.0, COUNT, VALUE, null, null);
        assertThat(units).containsExactlyInAnyOrder(StandardUnit.MILLISECONDS, StandardUnit.MILLISECONDS,
                                                    StandardUnit.COUNT, StandardUnit.COUNT, null, null);

        assertThat(dimensions.stream().map(d -> d.name()))
            .containsOnly("MetricCategory", SdkDefaultMetric.SERVICE.name(), SdkDefaultMetric.OPERATION.name());
        assertThat(dimensions.stream().map(d -> d.value()))
            .containsOnly(MetricCategory.DEFAULT.name(), SERVICE, OPERATION);
    }

    @Test
    public void transform_DoesNot_Convert_AttemptMetrics() {
        MetricEvents registry = DefaultMetricEvents.create();
        addRequiredDimensions(registry);

        MetricEvents attemptMR = registry.registerApiCallAttemptMetrics();
        attemptMR.register("counter", counter);

        List<MetricDatum> datums = transformer.transform(registry).stream()
                .filter(m -> !SdkDefaultMetric.OPERATION.name().equals(m.metricName()))
                .filter(m -> !SdkDefaultMetric.SERVICE.name().equals(m.metricName()))
                .collect(Collectors.toList());

        assertThat(datums).isEmpty();
    }

    private void addRequiredDimensions(MetricEvents registry) {
        MetricUtils.registerConstantGauge(SERVICE, registry, SdkDefaultMetric.SERVICE);
        MetricUtils.registerConstantGauge(OPERATION, registry, SdkDefaultMetric.OPERATION);
    }
}
