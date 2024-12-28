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

package software.amazon.awssdk.metrics.publishers.emf.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;

public class MetricEmfConverterTest {

    private MetricEmfConverter metricEmfConverterDefault;
    private MetricEmfConverter metricEmfConverterCustom;
    private final EmfMetricConfiguration testConfigDefault = new EmfMetricConfiguration.Builder().build();
    private final EmfMetricConfiguration testConfigCustom = new EmfMetricConfiguration.Builder()
                                                                                        .logGroupName("my_log_group_name")
                                                                                        .dimensions(Stream.of(HttpMetric.HTTP_CLIENT_NAME)
                                                                                        .collect(Collectors.toSet()))
                                                                                        .metricCategories(Stream.of(MetricCategory.HTTP_CLIENT).collect(Collectors.toSet()))
                                                                                        .metricLevel(MetricLevel.TRACE)
                                                                                        .build();
    private final Clock fixedClock = Clock.fixed(
        Instant.ofEpochMilli(12345678),
        ZoneOffset.UTC
    );

    @BeforeEach
    void setUp() {
        metricEmfConverterDefault = new MetricEmfConverter(testConfigDefault,fixedClock);
        metricEmfConverterCustom = new MetricEmfConverter(testConfigCustom, fixedClock);
    }

    @Test
    void ConvertMetricCollectionToEMF_EmptyCollection() {
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(MetricCollector.create("test").collect());
        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[]}]}}");
        }
    }

    @Test
    void ConvertMetricCollectionToEMF_MultipleMetrics(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\",\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":100.0}");

        }
    }


    @Test
    void ConvertMetricCollectionToEMF_Dimensions(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\","
                                         + "\"Dimensions\":[[\"HttpClientName\"]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},\"AvailableConcurrency\":5,"
                                         + "\"HttpClientName\":\"apache-http-client\"}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_metricCategory(){
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = metricEmfConverterCustom.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\",\"CloudWatchMetrics\":[{\"Namespace\":"
                                         + "\"AwsSdk/Test/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},\"AvailableConcurrency\":5}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_metricLevel(){
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
        List<String> emfLogs = metricEmfConverterCustom.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\""
                                         + ",\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"HttpStatusCode\"}]}]},\"AvailableConcurrency\":5,\"HttpStatusCode\":404}");

        }
    }


    @Test
    void ConvertMetricCollectionToEMF_ChildCollections(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector = metricCollector.createChild("child");
        childMetricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\","
                                         + "\"Dimensions\":[[\"HttpClientName\"]],\"Metrics\":"
                                         + "[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},"
                                         + "\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":100.0,\"HttpClientName\":\"apache-http-client\"}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_MultiChildCollections(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector1 = metricCollector.createChild("child1");
        childMetricCollector1.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        MetricCollector childMetricCollector2 = metricCollector.createChild("child2");
        childMetricCollector2.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(200));
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345.678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\""
                                         + ":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\",\"Dimensions\":[[\"HttpClientName\"]],"
                                         + "\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},"
                                         + "{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":"
                                         + "[100.0,200.0],\"HttpClientName\":\"apache-http-client\"}");

        }
    }


}