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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.testutils.LogCaptor;
import software.amazon.awssdk.utils.internal.SystemSettingUtilsTestBackdoor;


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
        SystemSettingUtilsTestBackdoor.addEnvironmentVariableOverride("AWS_LAMBDA_LOG_GROUP_NAME", "/aws/lambda/testFunction");
        EmfMetricLoggingPublisher publisher = publisherBuilder.build();
        MetricCollector metricCollector = MetricCollector.create("test");
        publisher.publish(metricCollector.collect());
        assertThat(loggedEvents()).hasSize(2);
        assertThat(loggedEvents().get(1).toString()).contains("/aws/lambda/testFunction");
        SystemSettingUtilsTestBackdoor.clearEnvironmentVariableOverrides();
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

    @Test
    void publish_propertiesSupplierThrowsException_publishesWithoutCustomProperties() {
        EmfMetricLoggingPublisher publisher = publisherBuilder
            .logGroupName("/aws/lambda/emfMetricTest")
            .propertiesSupplier(() -> { throw new RuntimeException("supplier failed"); })
            .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        publisher.publish(metricCollector.collect());

        // Should have: 1 warning about supplier + 1 EMF info log
        boolean hasWarning = loggedEvents().stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getMessage().getFormattedMessage().contains("Properties supplier threw an exception"));
        assertThat(hasWarning).isTrue();

        boolean hasEmfOutput = loggedEvents().stream()
            .anyMatch(e -> e.getLevel() == Level.INFO
                && e.getMessage().getFormattedMessage().contains("\"_aws\":{"));
        assertThat(hasEmfOutput).isTrue();

        // EMF output should not contain any custom properties
        String emfLog = loggedEvents().stream()
            .filter(e -> e.getLevel() == Level.INFO
                && e.getMessage().getFormattedMessage().contains("\"_aws\":{"))
            .findFirst().get().getMessage().getFormattedMessage();
        assertThat(emfLog).contains("\"AvailableConcurrency\":5");
    }

    @Test
    void publish_propertiesSupplierReturnsNull_publishesWithoutCustomProperties() {
        EmfMetricLoggingPublisher publisher = publisherBuilder
            .logGroupName("/aws/lambda/emfMetricTest")
            .propertiesSupplier(() -> null)
            .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        publisher.publish(metricCollector.collect());

        // Should have EMF output without custom properties
        boolean hasEmfOutput = loggedEvents().stream()
            .anyMatch(e -> e.getLevel() == Level.INFO
                && e.getMessage().getFormattedMessage().contains("\"_aws\":{"));
        assertThat(hasEmfOutput).isTrue();

        String emfLog = loggedEvents().stream()
            .filter(e -> e.getLevel() == Level.INFO
                && e.getMessage().getFormattedMessage().contains("\"_aws\":{"))
            .findFirst().get().getMessage().getFormattedMessage();
        assertThat(emfLog).contains("\"AvailableConcurrency\":5");
        // No warning should be logged for null return
        boolean hasWarning = loggedEvents().stream()
            .anyMatch(e -> e.getLevel() == Level.WARN);
        assertThat(hasWarning).isFalse();
    }

    @Test
    void publish_statefulSupplier_eachPublishUsesCurrentMap() {
        AtomicInteger counter = new AtomicInteger(0);
        EmfMetricLoggingPublisher publisher = publisherBuilder
            .logGroupName("/aws/lambda/emfMetricTest")
            .propertiesSupplier(() -> {
                int count = counter.incrementAndGet();
                Map<String, String> map = new HashMap<String, String>();
                map.put("InvocationCount", String.valueOf(count));
                return map;
            })
            .build();

        // First publish
        MetricCollector mc1 = MetricCollector.create("test1");
        mc1.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        publisher.publish(mc1.collect());

        // Second publish
        MetricCollector mc2 = MetricCollector.create("test2");
        mc2.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 10);
        publisher.publish(mc2.collect());

        // Collect all EMF info logs
        List<String> emfLogs = loggedEvents().stream()
            .filter(e -> e.getLevel() == Level.INFO
                && e.getMessage().getFormattedMessage().contains("\"_aws\":{"))
            .map(e -> e.getMessage().getFormattedMessage())
            .collect(java.util.stream.Collectors.toList());

        assertThat(emfLogs).hasSize(2);
        assertThat(emfLogs.get(0)).contains("\"InvocationCount\":\"1\"");
        assertThat(emfLogs.get(1)).contains("\"InvocationCount\":\"2\"");
    }
}
