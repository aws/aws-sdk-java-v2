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
import software.amazon.awssdk.metrics.MetricEvent;
import software.amazon.awssdk.metrics.MetricEventRecord;

/**
 * Tests for {@link DefaultMetricEventRecord}.
 */
public class DefaultMetricEventRecordTest {
    @Test
    public void testGetters() {
        MetricEvent<Integer> event = MetricEvent.of("foo", Integer.class, MetricCategory.DEFAULT);

        MetricEventRecord<Integer> record = new DefaultMetricEventRecord<>(event, 2);

        assertThat(record.getEvent()).isEqualTo(event);
        assertThat(record.getData()).isEqualTo(2);
    }
}
