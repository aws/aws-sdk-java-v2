/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.metrics.registry.MetricRegistry;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;

@RunWith(MockitoJUnitRunner.class)
public class ProducerTest {
    private static final int CAPACITY = 3;

    @Mock
    private MetricTransformer transformer;

    @Mock
    private MetricRegistry metricRegistry;

    private BlockingQueue<MetricDatum> queue;
    private MetricProducer producer;

    @Before
    public void setup() {
        queue = new LinkedBlockingDeque<>(CAPACITY);
        producer = MetricProducer.builder()
                                 .queue(queue)
                                 .metricTransformer(transformer)
                                 .build();

        when(metricRegistry.apiCallAttemptMetrics()).thenReturn(new ArrayList<>());
    }

    @Test
    public void addMetrics_withinQueueCapacity() {
        List<MetricDatum> list = new ArrayList<>();
        IntStream.range(0, CAPACITY - 1).forEach(i -> list.add(createDatum()));
        when(transformer.transform(any())).thenReturn(list);

        producer.addMetrics(metricRegistry);
        assertThat(queue.size()).isEqualTo(CAPACITY - 1);
    }

    @Test
    public void addMetrics_onlyAddMetrics_upToQueueCapacity() {
        List<MetricDatum> list = new ArrayList<>();
        IntStream.range(0, CAPACITY + 1).forEach(i -> list.add(createDatum()));
        when(transformer.transform(any())).thenReturn(list);

        producer.addMetrics(metricRegistry);
        assertThat(queue.size()).isEqualTo(CAPACITY);
    }

    private MetricDatum createDatum() {
        return MetricDatum.builder().build();
    }
}
