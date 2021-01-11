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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.AfterClass;
import org.junit.Test;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

public class DefaultMetricCollectionTest {
    private static final SdkMetric<Integer> M1 = SdkMetric.create("m1", Integer.class, MetricLevel.INFO, MetricCategory.CORE);

    @AfterClass
    public static void teardown() {
        DefaultSdkMetric.clearDeclaredMetrics();
    }

    @Test
    public void testMetricValues_noValues_returnsEmptyList() {
        DefaultMetricCollection foo = new DefaultMetricCollection("foo", Collections.emptyMap(), Collections.emptyList());
        assertThat(foo.metricValues(M1)).isEmpty();
    }

    @Test
    public void testChildren_noChildren_returnsEmptyList() {
        DefaultMetricCollection foo = new DefaultMetricCollection("foo", Collections.emptyMap(), Collections.emptyList());
        assertThat(foo.children()).isEmpty();
    }

    @Test
    public void testIterator_iteratesOverAllValues() {
        Integer[] values = {1, 2, 3};
        Map<SdkMetric<?>, List<MetricRecord<?>>> recordMap = new HashMap<>();
        List<MetricRecord<?>> records = Stream.of(values).map(v -> new DefaultMetricRecord<>(M1, v)).collect(Collectors.toList());
        recordMap.put(M1, records);

        DefaultMetricCollection collection = new DefaultMetricCollection("foo", recordMap, Collections.emptyList());
        final Set<Integer> iteratorValues = StreamSupport.stream(collection.spliterator(), false)
                .map(MetricRecord::value)
                .map(Integer.class::cast)
                .collect(Collectors.toSet());

        assertThat(iteratorValues).containsExactly(values);
    }
}
