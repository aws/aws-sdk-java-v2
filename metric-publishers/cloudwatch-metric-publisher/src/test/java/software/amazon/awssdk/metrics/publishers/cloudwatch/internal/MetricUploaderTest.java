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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;

public class MetricUploaderTest {
    private List<CompletableFuture<PutMetricDataResponse>> putMetricDataResponseFutures = new ArrayList<>();

    private CloudWatchAsyncClient client;

    private MetricUploader uploader;

    @Before
    public void setUp() {
        client = Mockito.mock(CloudWatchAsyncClient.class);
        uploader = new MetricUploader(client);

        Mockito.when(client.putMetricData(any(PutMetricDataRequest.class))).thenAnswer(p -> {
            CompletableFuture<PutMetricDataResponse> result = new CompletableFuture<>();
            putMetricDataResponseFutures.add(result);
            return result;
        });
    }

    @Test
    public void uploadSuccessIsPropagated() {
        CompletableFuture<Void> uploadFuture = uploader.upload(Arrays.asList(PutMetricDataRequest.builder().build(),
                                                                             PutMetricDataRequest.builder().build()));

        assertThat(putMetricDataResponseFutures).hasSize(2);
        assertThat(uploadFuture).isNotCompleted();

        putMetricDataResponseFutures.get(0).complete(PutMetricDataResponse.builder().build());

        assertThat(uploadFuture).isNotCompleted();

        putMetricDataResponseFutures.get(1).complete(PutMetricDataResponse.builder().build());

        assertThat(uploadFuture).isCompleted();
    }

    @Test
    public void uploadFailureIsPropagated() {
        CompletableFuture<Void> uploadFuture = uploader.upload(Arrays.asList(PutMetricDataRequest.builder().build(),
                                                                             PutMetricDataRequest.builder().build()));

        assertThat(putMetricDataResponseFutures).hasSize(2);
        assertThat(uploadFuture).isNotCompleted();

        putMetricDataResponseFutures.get(0).completeExceptionally(new Throwable());
        putMetricDataResponseFutures.get(1).complete(PutMetricDataResponse.builder().build());

        assertThat(uploadFuture).isCompletedExceptionally();
    }

    @Test
    public void closeFalseDoesNotCloseClient() {
        uploader.close(false);
        Mockito.verify(client, never()).close();
    }

    @Test
    public void closeTrueClosesClient() {
        uploader.close(true);
        Mockito.verify(client, times(1)).close();
    }
}