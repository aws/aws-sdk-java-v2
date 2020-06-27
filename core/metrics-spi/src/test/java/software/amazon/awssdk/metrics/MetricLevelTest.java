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

package software.amazon.awssdk.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MetricLevelTest {
    @Test
    public void allLevelsAreCorrect() {
        assertThat(MetricLevel.TRACE.includesLevel(MetricLevel.TRACE)).isTrue();
        assertThat(MetricLevel.TRACE.includesLevel(MetricLevel.INFO)).isTrue();
        assertThat(MetricLevel.TRACE.includesLevel(MetricLevel.ERROR)).isTrue();
    }

    @Test
    public void infoLevelsAreCorrect() {
        assertThat(MetricLevel.INFO.includesLevel(MetricLevel.TRACE)).isFalse();
        assertThat(MetricLevel.INFO.includesLevel(MetricLevel.INFO)).isTrue();
        assertThat(MetricLevel.INFO.includesLevel(MetricLevel.ERROR)).isTrue();
    }

    @Test
    public void errorLevelsAreCorrect() {
        assertThat(MetricLevel.ERROR.includesLevel(MetricLevel.TRACE)).isFalse();
        assertThat(MetricLevel.ERROR.includesLevel(MetricLevel.INFO)).isFalse();
        assertThat(MetricLevel.ERROR.includesLevel(MetricLevel.ERROR)).isTrue();
    }
}