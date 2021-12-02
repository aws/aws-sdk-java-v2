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

package software.amazon.awssdk.metrics.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

public class DefaultMetricCollectorTest {
    private static final SdkMetric<Integer> M1 = SdkMetric.create("m1", Integer.class, MetricLevel.INFO, MetricCategory.CORE);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @AfterClass
    public static void teardown() {
        DefaultSdkMetric.clearDeclaredMetrics();
    }

    @Test
    public void testName_returnsName() {
        MetricCollector collector = MetricCollector.create("collector");
        assertThat(collector.name()).isEqualTo("collector");
    }

    @Test
    public void testCreateChild_returnsChildWithCorrectName() {
        MetricCollector parent = MetricCollector.create("parent");
        MetricCollector child = parent.createChild("child");

        assertThat(child.name()).isEqualTo("child");
    }

    @Test
    public void testCollect_allReportedMetricsInCollection() {
        MetricCollector collector = MetricCollector.create("collector");
        Integer[] values = {1, 2, 3};
        Stream.of(values).forEach(v -> collector.reportMetric(M1, v));
        MetricCollection collect = collector.collect();
        assertThat(collect.metricValues(M1)).containsExactly(values);
    }

    @Test
    public void testCollect_returnedCollectionContainsAllChildren() {
        MetricCollector parent = MetricCollector.create("parent");
        String[] childNames = {"c1", "c2", "c3" };
        Stream.of(childNames).forEach(parent::createChild);
        MetricCollection collected = parent.collect();
        assertThat(collected.children().stream().map(MetricCollection::name)).containsExactly(childNames);
    }
}
