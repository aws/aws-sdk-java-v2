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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.meter.DefaultTimer;
import software.amazon.awssdk.metrics.meter.Metric;
import software.amazon.awssdk.metrics.meter.NoOpCounter;
import software.amazon.awssdk.metrics.meter.NoOpGauge;
import software.amazon.awssdk.metrics.meter.NoOpTimer;
import software.amazon.awssdk.metrics.meter.Timer;

public class MetricCategoryAwareRegistryTest {

    private static MetricRegistry delegate;
    private static MetricRegistry registry;
    private static MetricBuilderParams nonWhitelistParams;
    private static final Set<MetricCategory> whitelisted = new HashSet<>();

    @BeforeClass
    public static void setup() {
        delegate = DefaultMetricRegistry.create();
        whitelisted.add(MetricCategory.DEFAULT);
        whitelisted.add(MetricCategory.HTTP_CLIENT);

        registry = MetricCategoryAwareRegistry.builder()
                                              .metricRegistry(delegate)
                                              .categories(whitelisted)
                                              .build();

        nonWhitelistParams = MetricBuilderParams.builder()
                                                .addCategory(MetricCategory.STREAMING)
                                                .build();
    }

    @Before
    public void clear() {
        registry.clear();
        assertThat(registry.metrics().size()).isEqualTo(0);
    }

    @Test
    public void register_method_onlyStoresMetrics_InWhitelistedCategories() {
        String metricOne = "foo", metricTwo = "bar";

        registry.register(metricOne, timer(MetricCategory.HTTP_CLIENT));
        registry.register(metricTwo, timer(MetricCategory.STREAMING));

        Map<String, Metric> metrics = registry.metrics();
        assertThat(metrics.size()).isEqualTo(1);
        assertThat(metrics.keySet()).containsExactlyInAnyOrder(metricOne);
        assertThat(metrics.get(metricOne)).isInstanceOf(DefaultTimer.class);

        assertThat(registry.metrics()).isEqualTo(delegate.metrics());
    }

    @Test
    public void noopInstances_are_returned_ifCategoryIsNotInWhitelisted() {
        String metric = "foo";
        assertThat(registry.counter(metric, nonWhitelistParams)).isEqualTo(NoOpCounter.instance());
        assertThat(registry.timer(metric, nonWhitelistParams)).isEqualTo(NoOpTimer.instance());
        assertThat(registry.gauge(metric, "foo", nonWhitelistParams)).isEqualTo(NoOpGauge.instance());

        assertThat(registry.metrics()).isEmpty();
    }

    private Timer timer(MetricCategory metricCategory) {
        return DefaultTimer.builder()
                           .addCategory(metricCategory)
                           .build();
    }
}
