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
import org.junit.Test;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.MetricRecord;

/**
 * Tests for {@link DefaultMetricRecord}.
 */
public class DefaultSdkMetricRecordTest {
    @Test
    public void testGetters() {
        SdkMetric<Integer> event = SdkMetric.create("foo", Integer.class, MetricLevel.INFO, MetricCategory.CORE);

        MetricRecord<Integer> record = new DefaultMetricRecord<>(event, 2);

        assertThat(record.metric()).isEqualTo(event);
        assertThat(record.value()).isEqualTo(2);
    }
}
