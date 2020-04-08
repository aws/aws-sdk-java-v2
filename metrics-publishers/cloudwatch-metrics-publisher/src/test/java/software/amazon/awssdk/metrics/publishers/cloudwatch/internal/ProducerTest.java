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

package software.amazon.awssdk.metrics.publishers.cloudwatch.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.metrics.registry.DefaultMetricRegistry;
import software.amazon.awssdk.metrics.registry.MetricBuilderParams;
import software.amazon.awssdk.metrics.registry.MetricRegistry;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;

@RunWith(MockitoJUnitRunner.class)
public class ProducerTest {
    private static final int CAPACITY = 3;

    @Mock
    private MetricRegistry metricRegistry;

    private BlockingQueue<MetricDatum> queue;
    private MetricProducer producer;

    @Before
    public void setup() {
        queue = new LinkedBlockingDeque<>(CAPACITY);
        producer = MetricProducer.builder()
                                 .queue(queue)
                                 .build();

        metricRegistry = DefaultMetricRegistry.create();
    }

    @Test
    public void addMetrics_withinQueueCapacity() {
        MetricBuilderParams params = MetricBuilderParams.builder().build();
        IntStream.range(0, CAPACITY - 1).forEach(i -> metricRegistry.counter("m" + i, params).increment());
        producer.addMetrics(metricRegistry);
        assertThat(queue.size()).isEqualTo(CAPACITY - 1);
    }

    @Test
    public void addMetrics_onlyAddMetrics_upToQueueCapacity() {
        MetricBuilderParams params = MetricBuilderParams.builder().build();
        IntStream.range(0, CAPACITY + 1).forEach(i -> metricRegistry.counter("m" + i, params).increment());
        producer.addMetrics(metricRegistry);
        assertThat(queue.size()).isEqualTo(CAPACITY);
    }
}
