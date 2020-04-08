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

package software.amazon.awssdk.metrics.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.meter.ConstantGauge;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.DefaultGauge;
import software.amazon.awssdk.metrics.meter.DefaultTimer;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.LongCounter;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.Timer;

public class DefaultMetricRegistryTest {

    private static MetricRegistry registry;
    private static MetricBuilderParams params;

    @BeforeClass
    public static void setup() {
        registry = DefaultMetricRegistry.create();
        params = MetricBuilderParams.builder().addCategory(MetricCategory.DEFAULT).build();
    }

    @Before
    public void clear() {
        registry.clear();
        assertThat(registry.metrics().size()).isEqualTo(0);
    }

    @Test
    public void register_method_storesAllGivenMetrics() {
        String metricOne = "foo", metricTwo = "bar";

        registry.register(metricOne, LongCounter.create());
        registry.register(metricTwo, ConstantGauge.create(5));

        Map<String, Metric> metrics = registry.metrics();
        assertThat(metrics.size()).isEqualTo(2);
        assertThat(metrics.keySet()).containsExactlyInAnyOrder(metricOne, metricTwo);
        assertThat(metrics.get(metricOne)).isInstanceOf(LongCounter.class);
        assertThat(metrics.get(metricTwo)).isInstanceOf(ConstantGauge.class);
    }

    @Test
    public void test_apiCallAttemptMetrics() {
        registry.registerApiCallAttemptMetrics();
        MetricRegistry mr = registry.registerApiCallAttemptMetrics();
        mr.register("foo", LongCounter.create());

        assertThat(registry.metrics()).isEmpty();

        List<MetricRegistry> apiCallAttemptMetrics = registry.apiCallAttemptMetrics();
        assertThat(apiCallAttemptMetrics.size()).isEqualTo(2);
        assertThat(apiCallAttemptMetrics.get(0).metrics()).isEmpty();
        assertThat(apiCallAttemptMetrics.get(1).metrics().size()).isEqualTo(1);
        assertThat(apiCallAttemptMetrics.get(1).metrics().keySet()).containsExactly("foo");
    }

    @Test
    public void test_nonExisting_metric() {
        assertThat(registry.metric("foo")).isNotPresent();
    }

    @Test
    public void test_remove_metric() {
        String metric = "foo";
        registry.register(metric , LongCounter.create());

        assertThat(registry.remove(metric)).isTrue();
        assertThat(registry.remove("bar")).isFalse();
    }

    @Test (expected = IllegalArgumentException.class)
    public void register_throwsException_ifMetricAlreadyExists() {
        String metric = "foo";
        registry.register(metric , LongCounter.create());
        registry.register(metric , ConstantGauge.create(5));
    }

    @Test
    public void testCounter() {
        String metric = "foo";
        Counter counter = registry.counter(metric, params);

        assertThat(counter).isInstanceOf(LongCounter.class);
        assertThat(registry.metrics().size()).isEqualTo(1);
    }

    @Test
    public void counter_returns_existingInstance_IfNameIsSame() {
        String metric = "foo";
        Counter counter1 = registry.counter(metric, params);
        Counter counter2 = registry.counter(metric, params);

        assertThat(counter1).isEqualTo(counter2);
        assertThat(registry.metrics().size()).isEqualTo(1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void counter_throwsError_IfNameIsRegisteredWithAnotherMetric() {
        String metric = "foo";
        registry.timer(metric, params);
        registry.counter(metric, params);
    }

    @Test
    public void testMetricRegistryMethods() {
        registry.register("counter", LongCounter.create());
        registry.counter("counter", params);

        Timer timer = registry.timer("timer", params);
        assertThat(timer).isInstanceOf(DefaultTimer.class);

        Gauge<String> gauge = registry.gauge("gauge", "foobar", params);
        assertThat(gauge).isInstanceOf(DefaultGauge.class);

        assertThat(registry.metrics().size()).isEqualTo(3);
        assertThat(registry.metrics().keySet()).containsExactlyInAnyOrder("counter", "timer", "gauge");
    }
}
