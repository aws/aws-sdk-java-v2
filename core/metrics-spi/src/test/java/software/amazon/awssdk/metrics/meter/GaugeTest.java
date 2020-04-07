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

package software.amazon.awssdk.metrics.meter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.metrics.MetricCategory;

public class GaugeTest {

    @Test
    public void constantGauge_CreateMethod() {
        String value = "foobar";
        Gauge<String> gauge = ConstantGauge.create(value);

        assertThat(gauge.value()).isEqualTo(value);
        assertThat(gauge.categories()).isEmpty();
    }


    @Test
    public void constantGauge_Builder() {
        Gauge<Long> gauge = ConstantGauge.builder()
                                         .value(5L)
                                         .addCategory(MetricCategory.ALL)
                                         .build();

        assertThat(gauge.value()).isEqualTo(5L);
        assertThat(gauge.categories()).containsExactly(MetricCategory.ALL);
    }

    @Test
    public void testDefaultGauge() {
        String value = "foobar";
        String newValue = "barbaz";
        DefaultGauge<String> gauge = DefaultGauge.create(value);

        assertThat(gauge.value()).isEqualTo(value);
        assertThat(gauge.categories()).isEmpty();

        gauge.value(newValue);
        assertThat(gauge.value()).isEqualTo(newValue);
    }
}
