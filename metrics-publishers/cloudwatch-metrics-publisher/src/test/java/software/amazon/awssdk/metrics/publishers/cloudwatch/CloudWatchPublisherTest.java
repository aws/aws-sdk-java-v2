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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.metrics.meter.Counter;
import software.amazon.awssdk.metrics.meter.DefaultGauge;
import software.amazon.awssdk.metrics.meter.Gauge;
import software.amazon.awssdk.metrics.meter.LongCounter;
import software.amazon.awssdk.metrics.registry.DefaultMetricEvents;
import software.amazon.awssdk.metrics.MetricEvents;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.testutils.service.AwsTestBase;

@RunWith(MockitoJUnitRunner.class)
public class CloudWatchPublisherTest extends AwsTestBase {
    private static final String NAMESPACE = "SdkTestMetrics";
    private static final int PERIOD = 3;
    private static final Double COUNTER_VALUE = 5.0;
    private static final Double GAUGE_VALUE = 10.0;

    @Mock
    private CloudWatchAsyncClient client;

    private static CloudWatchMetricsPublisher cloudWatchPublisher;

    @Before
    public void setup() {
        cloudWatchPublisher = CloudWatchMetricsPublisher.builder()
                                                        .publishFrequency(Duration.ofSeconds(PERIOD))
                                                        .cloudWatchClient(client)
                                                        .namespace(NAMESPACE)
                                                        .build();
    }

    @Test
    public void registerMethod_callsPublishMethod_whichUploadsToCloudWatch() throws Exception {
        // this triggers the publish call
        register();

        // wait for the metrics to get upload
        Thread.sleep(Duration.ofSeconds(PERIOD * 2).toMillis());

        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        Mockito.verify(client, atLeast(1)).putMetricData(captor.capture());
        assertThat(captor.getAllValues()).isNotEmpty();

        PutMetricDataRequest captured = captor.getAllValues().get(0);
        assertThat(captured.namespace()).isEqualTo(NAMESPACE);

        List<MetricDatum> datums = captured.metricData();
        assertThat(datums).isNotEmpty();
        assertThat(datums.stream().map(m -> m.value()))
            .containsOnly(COUNTER_VALUE, GAUGE_VALUE);
    }

    private void register() {
        IntStream.range(0, 20).forEach(i -> {
            cloudWatchPublisher.registerMetrics(createRegistry());
        });
    }

    private MetricEvents createRegistry() {
        MetricEvents registry = DefaultMetricEvents.create();
        IntStream.range(0, 5).forEach(i -> registry.register("counter" + i, counter()));

        MetricEvents attemptMr = registry.registerApiCallAttemptMetrics();
        IntStream.range(0, 5).forEach(i -> attemptMr.register("gauge" + i, gauge()));

        return registry;
    }

    private Counter counter() {
        LongCounter counter = LongCounter.create();
        counter.increment(COUNTER_VALUE.longValue());
        return counter;
    }

    private Gauge gauge() {
        return DefaultGauge.create(GAUGE_VALUE);
    }

}
