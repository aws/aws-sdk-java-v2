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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.LambdaSystemSetting;


public class EmfMetricLoggingPublisherTest extends LogCaptor.LogCaptorTestBase{

    private EmfMetricLoggingPublisher.Builder publisherBuilder;


    @BeforeEach
    void setUp() {
        publisherBuilder = EmfMetricLoggingPublisher.builder();
    }

    @Test
    void Publish_noLogGroupName_throwException() {
        assertThatThrownBy(() -> {
            EmfMetricLoggingPublisher publisher = publisherBuilder.build();
            publisher.publish(null);
        })
            .isInstanceOf(NullPointerException.class)
            .hasMessage("logGroupName must not be null.");
    }

    @Test
    void Publish_noLogGroupNameInLambda_defaultLogGroupName() {
        System.setProperty(LambdaSystemSetting.AWS_LAMBDA_FUNCTION_NAME.property(), "testFunction");
        EmfMetricLoggingPublisher publisher = publisherBuilder.build();
        MetricCollector metricCollector = MetricCollector.create("test");
        publisher.publish(metricCollector.collect());
        assertThat(loggedEvents()).hasSize(2);
        assertThat(loggedEvents().get(1).toString()).contains("/aws/lambda/testFunction");
        System.clearProperty(LambdaSystemSetting.AWS_LAMBDA_FUNCTION_NAME.property());
    }

    @Test
    void Publish_nullMetrics() {
        EmfMetricLoggingPublisher publisher = publisherBuilder.logGroupName("/aws/lambda/emfMetricTest").build();
        publisher.publish(null);
        assertThat(loggedEvents()).hasSize(1);
    }

    @Test
    void Publish_metricCollectionWithChild() {
        EmfMetricLoggingPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
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

    @Test
    void Publish_multipleMetrics() {
        EmfMetricLoggingPublisher publisher = publisherBuilder.logGroupName("/aws/lambda/emfMetricTest").build();
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        publisher.publish(metricCollector.collect());
        assertThat(loggedEvents()).hasSize(2);
    }

}
