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

import java.util.List;
import software.amazon.awssdk.metrics.MetricLevel;


public class EmfMetricPublisherTest {

    private EmfMetricPublisher.Builder publisherBuilder;

    @BeforeEach
    void setUp() {
        publisherBuilder = EmfMetricPublisher.builder();
    }

    @Test
    void ConvertMetricCollectionToEMF_EmptyCollection(){
        EmfMetricPublisher publisher = publisherBuilder.logGroupName("my-log-group-name")
                                                       .build();

        List<String> emfLogs = publisher.convertMetricCollectionToEmf(MetricCollector.create("test").collect());
        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my-log-group-name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[]}]}}");
        }
    }

    @Test
    void ConvertMetricCollectionToEMF_SingleMetric(){
        EmfMetricPublisher publisher = publisherBuilder.build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},\"AvailableConcurrency\":5}");
        }

    }

    @Test
    void ConvertMetricCollectionToEMF_MultipleMetrics(){
        EmfMetricPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
             assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\""
                                          + ":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}"
                                          + ",{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":100.0}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_Dimensions(){
        EmfMetricPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\","
                                         + "\"Dimensions\":[[\"HttpClientName\"]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},\"AvailableConcurrency\":5,"
                                         + "\"HttpClientName\":\"apache-http-client\"}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_nonExistDimensions(){
        EmfMetricPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());
        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\","
                                         + "\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},"
                                         + "\"AvailableConcurrency\":5}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_extraDimensions(){
        EmfMetricPublisher publisher = publisherBuilder.build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "ServiceId1234");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());
        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\","
                                         + "\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},"
                                         + "\"AvailableConcurrency\":5}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_metricCategory(){
        EmfMetricPublisher publisher = publisherBuilder.metricCategories(MetricCategory.HTTP_CLIENT)
                                                       .build();
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\","
                                         + "\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},"
                                         + "\"AvailableConcurrency\":5}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_metricLevel(){
        EmfMetricPublisher publisher = publisherBuilder.metricLevel(MetricLevel.TRACE)
                                                       .build();
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\""
                                         + ",\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"HttpStatusCode\"}]}]},\"AvailableConcurrency\":5,\"HttpStatusCode\":404}");

        }
    }


    @Test
    void ConvertMetricCollectionToEMF_ChildCollections(){
        EmfMetricPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector = metricCollector.createChild("child");
        childMetricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[\"HttpClientName\"]],\"Metrics\":"
                                         + "[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},"
                                         + "\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":100.0,\"HttpClientName\":\"apache-http-client\"}");

        }
    }

    @Test
    void ConvertMetricCollectionToEMF_MultiChildCollections(){
        EmfMetricPublisher publisher = publisherBuilder.dimensions(HttpMetric.HTTP_CLIENT_NAME)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector1 = metricCollector.createChild("child1");
        childMetricCollector1.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        MetricCollector childMetricCollector2 = metricCollector.createChild("child2");
        childMetricCollector2.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(200));
        List<String> emfLogs = publisher.convertMetricCollectionToEmf(metricCollector.collect());

        for (String emfLog : emfLogs) {
            assertThat(emfLog).isEqualTo("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"/aws/emf/metrics\",\"CloudWatchMetrics\""
                                         + ":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[\"HttpClientName\"]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},"
                                         + "{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":"
                                         + "[100.0,200.0],\"HttpClientName\":\"apache-http-client\"}");

        }
    }


}
