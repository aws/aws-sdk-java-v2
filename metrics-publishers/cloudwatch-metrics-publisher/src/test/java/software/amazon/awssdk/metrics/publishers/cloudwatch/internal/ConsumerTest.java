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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;

@RunWith(MockitoJUnitRunner.class)
public class ConsumerTest {
    private static final String NAMESPACE = "javasdk";

    @Mock
    private CloudWatchAsyncClient client;

    private BlockingQueue<MetricDatum> queue = new LinkedBlockingQueue<>();

    private MetricConsumer consumer;

    @Before
    public void setup() {
        queue.clear();

        consumer = MetricConsumer.builder()
                                 .queue(queue)
                                 .cloudWatchClient(client)
                                 .namespace(NAMESPACE)
                                 .build();
    }

    @Test
    public void returnedFuture_isCompleted_IfQueueIsEmpty() {
        List<CompletableFuture<PutMetricDataResponse>> futures = consumer.call();
        assertThat(futures).hasSize(0);
    }

    @Test
    public void futureWithPutMetricDataResponse_isReturned_IfQueueIsNotEmpty() {
        CompletableFuture<PutMetricDataResponse> future = new CompletableFuture<>();
        queue.add(MetricDatum.builder().build());

        when(client.putMetricData(any(PutMetricDataRequest.class))).thenReturn(future);

        assertThat(consumer.call()).containsExactly(future);
    }
}
