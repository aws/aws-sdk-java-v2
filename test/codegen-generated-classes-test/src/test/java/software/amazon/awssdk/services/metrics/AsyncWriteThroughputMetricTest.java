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

package software.amazon.awssdk.services.metrics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.testutil.MockIdentityProviderUtil;

/**
 * Functional tests for WRITE_THROUGHPUT metric for async client using WireMock.
 */
@WireMockTest
public class AsyncWriteThroughputMetricTest {

    private MetricPublisher mockPublisher;
    private ProtocolRestJsonAsyncClient client;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wmRuntimeInfo) {
        mockPublisher = Mockito.mock(MetricPublisher.class);
        client = ProtocolRestJsonAsyncClient.builder()
                                            .region(Region.US_WEST_2)
                                            .endpointOverride(URI.create(wmRuntimeInfo.getHttpBaseUrl()))
                                            .credentialsProvider(MockIdentityProviderUtil.mockIdentityProvider())
                                            .overrideConfiguration(c -> c.addMetricPublisher(mockPublisher))
                                            .build();
    }

    @AfterEach
    public void teardown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void streamingInputOperation_withRequestBody_writeThroughputReported() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));

        client.streamingInputOperation(r -> r.build(), AsyncRequestBody.fromByteBuffers(
            ByteBuffer.wrap("test body content".getBytes(StandardCharsets.UTF_8)),
            ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)))).join();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        List<MetricCollection> attemptMetrics = capturedCollection.children();

        assertThat(attemptMetrics).hasSize(1);
        List<Double> writeThroughputValues = attemptMetrics.get(0).metricValues(CoreMetric.WRITE_THROUGHPUT);
        assertThat(writeThroughputValues).hasSize(1);
        assertThat(writeThroughputValues.get(0)).isFinite();
        assertThat(writeThroughputValues.get(0)).isGreaterThan(0);
    }

    @Test
    public void operationWithNoInputOrOutput_noRequestBody_writeThroughputNotReported() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));

        client.operationWithNoInputOrOutput().join();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        List<MetricCollection> attemptMetrics = capturedCollection.children();

        assertThat(attemptMetrics).hasSize(1);
        List<Double> writeThroughputValues = attemptMetrics.get(0).metricValues(CoreMetric.WRITE_THROUGHPUT);
        assertThat(writeThroughputValues).isEmpty();
    }

    @Test
    public void nonStreamingOperation_withRequestBody_writeThroughputNotReported() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200).withBody("{}")));

        client.allTypes(r -> r.stringMember("test")).join();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        List<MetricCollection> attemptMetrics = capturedCollection.children();

        assertThat(attemptMetrics).hasSize(1);
        List<Double> writeThroughputValues = attemptMetrics.get(0).metricValues(CoreMetric.WRITE_THROUGHPUT);
        assertThat(writeThroughputValues).isEmpty();
    }
}
