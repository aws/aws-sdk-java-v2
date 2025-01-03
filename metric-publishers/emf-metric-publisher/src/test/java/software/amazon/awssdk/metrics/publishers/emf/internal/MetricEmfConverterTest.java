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
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;

public class MetricEmfConverterTest {

    private MetricEmfConverter metricEmfConverterDefault;
    private MetricEmfConverter metricEmfConverterCustom;
    private final EmfMetricConfiguration testConfigDefault = new EmfMetricConfiguration.Builder()
                                                                                        .logGroupName("my_log_group_name")
                                                                                        .build();
    private final EmfMetricConfiguration testConfigCustom = new EmfMetricConfiguration.Builder()
                                                                                        .namespace("AwsSdk/Test/JavaSdk2")
                                                                                        .logGroupName("my_log_group_name")
                                                                                        .dimensions(Stream.of(HttpMetric.HTTP_CLIENT_NAME).collect(Collectors.toSet()))
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

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[]}]}}");
    }

    @Test
    void ConvertMetricCollectionToEMF_MultipleMetrics(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100));
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},"
                                         + "{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":100.0}");
    }


    @Test
    void ConvertMetricCollectionToEMF_Dimensions(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(CoreMetric.OPERATION_NAME, "operationName");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\","
                                         + "\"Dimensions\":[[\"OperationName\"]],"
                                         + "\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},"
                                         + "\"AvailableConcurrency\":5,"
                                         + "\"OperationName\":\"operationName\"}");
    }

    @Test
    void ConvertMetricCollectionToEMF_metricCategory(){
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        List<String> emfLogs = metricEmfConverterCustom.convertMetricCollectionToEmf(metricCollector.collect());

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\",\"CloudWatchMetrics\":[{\"Namespace\":"
                                         + "\"AwsSdk/Test/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[{\"Name\":\"AvailableConcurrency\"}]}]},\"AvailableConcurrency\":5}");
    }

    @Test
    void ConvertMetricCollectionToEMF_metricLevel(){
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
        List<String> emfLogs = metricEmfConverterCustom.convertMetricCollectionToEmf(metricCollector.collect());

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/Test/JavaSdk2\""
                                         + ",\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"HttpStatusCode\"}]}]},\"AvailableConcurrency\":5,\"HttpStatusCode\":404}");
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

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\","
                                         + "\"CloudWatchMetrics\""
                                         + ":[{\"Namespace\":\"AwsSdk/JavaSdk2\",\"Dimensions\":[[]],"
                                         + "\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},"
                                         + "{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,\"ConcurrencyAcquireDuration\":"
                                         + "[100.0,200.0]}");
    }

    @Test
    void ConvertMetricCollectionToEMF_OverSizedRecords(){

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.HTTP_CLIENT_NAME, "apache-http-client");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        for (int i = 0; i < 120; i++) {
            MetricCollector childMetricCollector = metricCollector.createChild("child" + i);
            childMetricCollector.reportMetric(HttpMetric.CONCURRENCY_ACQUIRE_DURATION, java.time.Duration.ofMillis(100+i));
        }

        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());

        assertThat(emfLogs).containsOnly("{\"_aws\":{\"Timestamp\":12345678,\"LogGroupName\":\"my_log_group_name\",\"CloudWatchMetrics\":[{\"Namespace\":\"AwsSdk/JavaSdk2\","
                                         + "\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"AvailableConcurrency\"},{\"Name\":\"ConcurrencyAcquireDuration\",\"Unit\":\"Milliseconds\"}]}]},\"AvailableConcurrency\":5,"
                                         + "\"ConcurrencyAcquireDuration\":[100.0,101.0,102.0,103.0,104.0,105.0,106.0,107.0,108.0,109.0,110.0,111.0,112.0,113.0,114.0,115.0,116.0,117.0,118.0,119.0,120.0,121.0,122.0,"
                                         + "123.0,124.0,125.0,126.0,127.0,128.0,129.0,130.0,131.0,132.0,133.0,134.0,135.0,136.0,137.0,138.0,139.0,140.0,141.0,142.0,143.0,144.0,145.0,146.0,147.0,148.0,149.0,150.0,151.0,"
                                         + "152.0,153.0,154.0,155.0,156.0,157.0,158.0,159.0,160.0,161.0,162.0,163.0,164.0,165.0,166.0,167.0,168.0,169.0,170.0,171.0,172.0,173.0,174.0,175.0,176.0,177.0,178.0,179.0,180.0,181.0,"
                                         + "182.0,183.0,184.0,185.0,186.0,187.0,188.0,189.0,190.0,191.0,192.0,193.0,194.0,195.0,196.0,197.0,198.0,199.0]}");

    }

    @Test
    void ConvertMetricCollectionToEMF_LargeCollection(){

        MetricCollector metricCollector = MetricCollector.create("test");
        for (int i = 0; i < 220; i++) {
            metricCollector.reportMetric(SdkMetric.create("metric_" + i, Integer.class, MetricLevel.INFO, MetricCategory.CORE), i);
        }

        List<String> emfLogs = metricEmfConverterDefault.convertMetricCollectionToEmf(metricCollector.collect());
        assertThat(emfLogs).hasSize(3);
    }

}