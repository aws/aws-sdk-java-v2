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

package software.amazon.awssdk.metrics.publishers.emf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.testutils.LogCaptor;


public class EmfMetricPublisherTest extends LogCaptor.LogCaptorTestBase{

    private EmfMetricPublisher.Builder publisherBuilder;


    @BeforeEach
    void setUp() {
        publisherBuilder = EmfMetricPublisher.builder();
    }

    @Test
    void Publish_multipleMetrics() {
        EmfMetricPublisher publisher = EmfMetricPublisher.create();
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        publisher.publish(metricCollector.collect());
        assertThat(loggedEvents()).hasSize(2);
    }

    @Test
    void Publish_nullMetrics() {
        EmfMetricPublisher publisher = EmfMetricPublisher.create();
        publisher.publish(null);
        assertThat(loggedEvents()).hasSize(1);
    }

    @Test
    void Publish_EmptyMetrics() {
        EmfMetricPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
                                                       .logGroupName("/aws/lambda/emfMetricTest")
                                                       .namespace("ExampleSDKV2MetricsEmf")
                                                       .metricLevel(MetricLevel.INFO)
                                                       .metricCategories(MetricCategory.HTTP_CLIENT)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector1 = metricCollector.createChild("child1");
        childMetricCollector1.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        MetricCollector childMetricCollector2 = metricCollector.createChild("child2");
        childMetricCollector2.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(200));
        publisher.publish(metricCollector.collect());
        assertThat(loggedEvents()).hasSize(4);
    }

}
