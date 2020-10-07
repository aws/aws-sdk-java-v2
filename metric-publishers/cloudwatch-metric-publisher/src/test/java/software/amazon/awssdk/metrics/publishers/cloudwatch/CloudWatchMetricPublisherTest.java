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

package software.amazon.awssdk.metrics.publishers.cloudwatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform.MetricCollectionAggregator;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;

public class CloudWatchMetricPublisherTest {
    private CloudWatchAsyncClient cloudWatch;

    private CloudWatchMetricPublisher.Builder publisherBuilder;

    @Before
    public void setup() {
        cloudWatch = Mockito.mock(CloudWatchAsyncClient.class);
        publisherBuilder = CloudWatchMetricPublisher.builder()
                                                    .cloudWatchClient(cloudWatch)
                                                    .uploadFrequency(Duration.ofMinutes(60));

        Mockito.when(cloudWatch.putMetricData(any(PutMetricDataRequest.class)))
               .thenReturn(CompletableFuture.completedFuture(PutMetricDataResponse.builder().build()));
    }

    @Test
    public void noMetricsNoCalls() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.build()) {
            publisher.publish(MetricCollector.create("test").collect());
        }
        assertNoPutMetricCalls();
    }

    @Test
    public void interruptedShutdownStillTerminates() {
        CloudWatchMetricPublisher publisher = publisherBuilder.build();
        Thread.currentThread().interrupt();
        publisher.close();
        assertThat(publisher.isShutdown()).isTrue();
        assertThat(Thread.interrupted()).isTrue(); // Clear interrupt flag
    }

    @Test
    public void closeDoesNotCloseConfiguredClient() {
        CloudWatchMetricPublisher.builder().cloudWatchClient(cloudWatch).build().close();
        Mockito.verify(cloudWatch, never()).close();
    }

    @Test(timeout = 10_000)
    public void closeWaitsForUploadToComplete() throws InterruptedException {
        CountDownLatch cloudwatchPutCalledLatch = new CountDownLatch(1);
        CompletableFuture<PutMetricDataResponse> result = new CompletableFuture<>();

        CloudWatchAsyncClient cloudWatch = Mockito.mock(CloudWatchAsyncClient.class);
        try (CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                            .cloudWatchClient(cloudWatch)
                                                                            .uploadFrequency(Duration.ofMinutes(60))
                                                                            .build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));

            Mockito.when(cloudWatch.putMetricData(any(PutMetricDataRequest.class))).thenAnswer(x -> {
                cloudwatchPutCalledLatch.countDown();
                return result;
            });

            publisher.publish(MetricCollector.create("test").collect());

            Thread closeThread = new Thread(publisher::close);

            assertThat(publisher.isShutdown()).isFalse();

            closeThread.start();

            // Wait until cloudwatch is called
            cloudwatchPutCalledLatch.await();

            // Wait to make sure the close thread seems to be waiting for the cloudwatch call to complete
            Thread.sleep(1_000);

            assertThat(closeThread.isAlive()).isTrue();

            // Complete the cloudwatch call
            result.complete(null);

            // Make sure the close thread finishes
            closeThread.join(5_000);
            assertThat(closeThread.isAlive()).isFalse();
        }
    }

    @Test
    public void defaultNamespaceIsCorrect() {
        try (CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                            .cloudWatchClient(cloudWatch)
                                                                            .build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));
        }

        PutMetricDataRequest call = getPutMetricCall();
        assertThat(call.namespace()).isEqualTo("AwsSdk/JavaSdk2");
    }

    @Test
    public void defaultDimensionsIsCorrect() {
        try (CloudWatchMetricPublisher publisher = CloudWatchMetricPublisher.builder()
                                                                            .cloudWatchClient(cloudWatch)
                                                                            .build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId");
            collector.reportMetric(CoreMetric.OPERATION_NAME, "OperationName");
            collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));
        }

        PutMetricDataRequest call = getPutMetricCall();
        assertThat(call.metricData().get(0).dimensions())
            .containsExactlyInAnyOrder(Dimension.builder()
                                                .name(CoreMetric.SERVICE_ID.name())
                                                .value("ServiceId")
                                                .build(),
                                       Dimension.builder()
                                                .name(CoreMetric.OPERATION_NAME.name())
                                                .value("OperationName")
                                                .build());
    }

    @Test
    public void namespaceSettingIsHonored() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.namespace("namespace").build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));
        }

        assertThat(getPutMetricCall().namespace()).isEqualTo("namespace");
    }

    @Test
    public void dimensionsSettingIsHonored() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.dimensions(CoreMetric.SERVICE_ID).build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId");
            collector.reportMetric(CoreMetric.OPERATION_NAME, "OperationName");
            collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));
        }

        PutMetricDataRequest call = getPutMetricCall();
        assertThat(call.metricData().get(0).dimensions()).containsExactly(Dimension.builder()
                                                                                   .name(CoreMetric.SERVICE_ID.name())
                                                                                   .value("ServiceId")
                                                                                   .build());
    }

    @Test
    public void metricCategoriesSettingIsHonored() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.metricCategories(MetricCategory.HTTP_CLIENT).build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId");
            collector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
            collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));
        }

        PutMetricDataRequest call = getPutMetricCall();
        MetricDatum metric = call.metricData().get(0);
        assertThat(call.metricData()).hasSize(1);
        assertThat(metric.dimensions()).containsExactly(Dimension.builder()
                                                                 .name(CoreMetric.SERVICE_ID.name())
                                                                 .value("ServiceId")
                                                                 .build());
        assertThat(metric.metricName()).isEqualTo(HttpMetric.AVAILABLE_CONCURRENCY.name());
    }

    @Test
    public void metricLevelSettingIsHonored() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.metricLevel(MetricLevel.INFO).build()) {
            MetricCollector collector = newCollector();
            collector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId");
            collector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
            collector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
            publisher.publish(new FixedTimeMetricCollection(collector.collect()));
        }

        PutMetricDataRequest call = getPutMetricCall();
        MetricDatum metric = call.metricData().get(0);
        assertThat(call.metricData()).hasSize(1);
        assertThat(metric.dimensions()).containsExactly(Dimension.builder()
                                                                 .name(CoreMetric.SERVICE_ID.name())
                                                                 .value("ServiceId")
                                                                 .build());
        assertThat(metric.metricName()).isEqualTo(CoreMetric.API_CALL_SUCCESSFUL.name());
    }

    @Test
    public void maximumCallsPerPublishSettingIsHonored() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.maximumCallsPerUpload(1)
                                                                   .detailedMetrics(HttpMetric.AVAILABLE_CONCURRENCY)
                                                                   .build()) {
            for (int i = 0; i < MetricCollectionAggregator.MAX_VALUES_PER_REQUEST + 1; ++i) {
                MetricCollector collector = newCollector();
                collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, i);
                publisher.publish(new FixedTimeMetricCollection(collector.collect()));
            }
        }

        assertThat(getPutMetricCalls()).hasSize(1);
    }

    @Test
    public void detailedMetricsSettingIsHonored() {
        try (CloudWatchMetricPublisher publisher = publisherBuilder.detailedMetrics(HttpMetric.AVAILABLE_CONCURRENCY).build()) {
            for (int i = 0; i < 10; ++i) {
                MetricCollector collector = newCollector();
                collector.reportMetric(HttpMetric.MAX_CONCURRENCY, 10);
                collector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, i);
                publisher.publish(new FixedTimeMetricCollection(collector.collect()));
            }
        }

        PutMetricDataRequest call = getPutMetricCall();
        MetricDatum concurrencyMetric = getDatum(call, HttpMetric.MAX_CONCURRENCY);
        MetricDatum availableConcurrency = getDatum(call, HttpMetric.AVAILABLE_CONCURRENCY);

        assertThat(concurrencyMetric.values()).isEmpty();
        assertThat(concurrencyMetric.counts()).isEmpty();
        assertThat(concurrencyMetric.statisticValues()).isNotNull();

        assertThat(availableConcurrency.values()).isNotEmpty();
        assertThat(availableConcurrency.counts()).isNotEmpty();
        assertThat(availableConcurrency.statisticValues()).isNull();
    }

    private MetricDatum getDatum(PutMetricDataRequest call, SdkMetric<?> metric) {
        return call.metricData().stream().filter(m -> m.metricName().equals(metric.name())).findAny().get();
    }

    private PutMetricDataRequest getPutMetricCall() {
        List<PutMetricDataRequest> calls = getPutMetricCalls();
        assertThat(calls).hasSize(1);
        return calls.get(0);
    }

    private List<PutMetricDataRequest> getPutMetricCalls() {
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        Mockito.verify(cloudWatch).putMetricData(captor.capture());
        return captor.getAllValues();
    }

    private void assertNoPutMetricCalls() {
        Mockito.verify(cloudWatch, never()).putMetricData(any(PutMetricDataRequest.class));
    }

    private MetricCollector newCollector() {
        return MetricCollector.create("test");
    }
}