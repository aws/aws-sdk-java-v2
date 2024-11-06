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

package software.amazon.awssdk.core.internal.useragent.businessmetrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.useragent.BusinessMetricCollection;

class BusinessMetricsTest {

    @Test
    void elementsCanBeAdded_inTwoWays() {
        BusinessMetricCollection metrics = new BusinessMetricCollection(10);
        metrics.addMetric("A");
        metrics.addMetric("B");
        metrics.addMetric("C");
        metrics.merge(Arrays.asList("X", "Y", "Z"));
        assertThat(metrics.asBoundedString()).isEqualTo("A,B,C,X,Y");
    }

    @Test
    void stringsAreEqual() {
        BusinessMetricCollection metrics1 = new BusinessMetricCollection(10);
        BusinessMetricCollection metrics2 = new BusinessMetricCollection(10);
        metrics1.addMetric("A");
        metrics1.addMetric("B");
        metrics2.addMetric("A");
        metrics2.addMetric("B");
        assertThat(metrics1.asBoundedString()).isEqualTo(metrics2.asBoundedString());
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("inputValues")
    void businessMetrics_works_asExpected(String description, Collection<String> metricsList, int maxLen, String expected) {
        BusinessMetricCollection metrics = new BusinessMetricCollection(maxLen);
        metrics.merge(metricsList);
        assertThat(metrics.asBoundedString()).isEqualTo(expected);
    }

    private static Stream<Arguments> inputValues() {
        return Stream.of(
            Arguments.of("Null list", null, 10, ""),
            Arguments.of("Empty list", Collections.emptyList(), 10, ""),
            Arguments.of("Single list", Collections.singletonList("A"), 10, "A"),
            Arguments.of("Truncates when element is last", Arrays.asList("A", "B", "C", "D", "E"), 5, "A,B,C"),
            Arguments.of("Truncates and ignores comma", Arrays.asList("A", "B", "C", "D", "E"), 6, "A,B,C"),
            Arguments.of("Truncates and ignores element on boundary", Arrays.asList("Aa", "Bb", "Cc", "D", "E"), 5, "Aa,Bb")
        );
    }
}
