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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
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

    @Mock
    private BlockingQueue<MetricDatum> queue;

    private Consumer consumer;

    @Before
    public void setup() {
        consumer = Consumer.builder()
                           .queue(queue)
                           .cloudWatchClient(client)
                           .namespace(NAMESPACE)
                           .build();
    }

    @Test
    public void returnedFuture_isCompleted_IfQueueIsEmpty() throws Exception {
        when(queue.poll()).thenReturn(null);
        CompletableFuture<PutMetricDataResponse> future = consumer.call();

        assertThat(future).isDone();
        assertThat(future.get()).isNull();
    }

    @Test
    public void futureWithPutMetricDataResponse_isReturned_IfQueueIsNotEmpty() throws Exception {
        CompletableFuture<PutMetricDataResponse> future = mock(CompletableFuture.class);
        when(queue.poll()).thenReturn(MetricDatum.builder().build());
        when(client.putMetricData(any(PutMetricDataRequest.class))).thenReturn(future);

        assertThat(consumer.call()).isEqualTo(future);
    }
}
