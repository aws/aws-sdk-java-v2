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

package software.amazon.awssdk.s3benchmarks.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryMetricPublisherTest {

    private InMemoryMetricPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new InMemoryMetricPublisher();
    }

    @Test
    void avgDurationMs_throwsOnZeroSamples() {
        assertThatThrownBy(() -> publisher.avgDurationMs("PutObject"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PutObject");
    }

    @Test
    void avgReadThroughput_throwsOnZeroSamples() {
        assertThatThrownBy(() -> publisher.avgReadThroughput("GetObject"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("GetObject");
    }

    @Test
    void avgDurationMs_computesAverage() {
        publisher.publish(metricCollection("PutObject", Duration.ofMillis(10), null));
        publisher.publish(metricCollection("PutObject", Duration.ofMillis(20), null));

        assertThat(publisher.avgDurationMs("PutObject")).isCloseTo(15.0, within(0.01));
    }

    @Test
    void avgReadThroughput_computesAverage() {
        publisher.publish(metricCollection("GetObject", Duration.ofMillis(5), 1000.0));
        publisher.publish(metricCollection("GetObject", Duration.ofMillis(5), 2000.0));

        assertThat(publisher.avgReadThroughput("GetObject")).isCloseTo(1500.0, within(0.01));
    }

    @Test
    void reset_clearsSamples() {
        publisher.publish(metricCollection("PutObject", Duration.ofMillis(10), null));
        publisher.reset();

        assertThatThrownBy(() -> publisher.avgDurationMs("PutObject"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void separatesOperations() {
        publisher.publish(metricCollection("PutObject", Duration.ofMillis(10), null));
        publisher.publish(metricCollection("GetObject", Duration.ofMillis(20), null));

        assertThat(publisher.avgDurationMs("PutObject")).isCloseTo(10.0, within(0.01));
        assertThat(publisher.avgDurationMs("GetObject")).isCloseTo(20.0, within(0.01));
    }

    private static MetricCollection metricCollection(String operationName, Duration duration, Double readThroughput) {
        MetricCollector collector = MetricCollector.create("ApiCall");
        collector.reportMetric(CoreMetric.OPERATION_NAME, operationName);
        collector.reportMetric(CoreMetric.API_CALL_DURATION, duration);

        if (readThroughput != null) {
            MetricCollector attempt = collector.createChild("ApiCallAttempt");
            attempt.reportMetric(CoreMetric.READ_THROUGHPUT, readThroughput);
        }

        return collector.collect();
    }
}
