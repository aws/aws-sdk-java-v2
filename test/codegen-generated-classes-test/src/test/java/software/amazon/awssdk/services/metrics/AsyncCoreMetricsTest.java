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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.URI;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;

@RunWith(MockitoJUnitRunner.class)
public class AsyncCoreMetricsTest {
    private static final String SERVICE_ID = "AmazonProtocolRestJson";
    private static final String REQUEST_ID = "req-id";
    private static final String EXTENDED_REQUEST_ID = "extended-id";

    private static ProtocolRestJsonAsyncClient client;

    @Rule
    public WireMockRule wireMock = new WireMockRule(0);

    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    @Mock
    private MetricPublisher mockPublisher;

    @Before
    public void setup() throws IOException {
        client = ProtocolRestJsonAsyncClient.builder()
                                            .credentialsProvider(mockCredentialsProvider)
                                            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                            .overrideConfiguration(c -> c.metricPublisher(mockPublisher))
                                            .build();

        when(mockCredentialsProvider.resolveCredentials()).thenAnswer(invocation -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return AwsBasicCredentials.create("foo", "bar");
        });
    }

    @After
    public void teardown() {
        if (client != null) {
            client.close();
        }
        client = null;
    }

    @Test
    public void apiCall_noConfiguredPublisher_succeeds() {
        stubSuccessfulResponse();
        ProtocolRestJsonAsyncClient noPublisher = ProtocolRestJsonAsyncClient.builder()
                                                                             .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
                                                                             .build();

        noPublisher.allTypes().join();
    }

    @Test
    public void apiCall_publisherOverriddenOnRequest_requestPublisherTakesPrecedence() {
        stubSuccessfulResponse();
        MetricPublisher requestMetricPublisher = mock(MetricPublisher.class);

        client.allTypes(r -> r.overrideConfiguration(o -> o.metricPublisher(requestMetricPublisher))).join();

        verify(requestMetricPublisher).publish(any(MetricCollection.class));
        verifyZeroInteractions(mockPublisher);
    }

    @Test
    public void apiCall_operationSuccessful_addsMetrics() {
        stubSuccessfulResponse();
        client.allTypes().join();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        assertThat(capturedCollection.name()).isEqualTo("ApiCall");
        assertThat(capturedCollection.metricValues(CoreMetric.SERVICE_ID))
            .containsExactly(SERVICE_ID);
        assertThat(capturedCollection.metricValues(CoreMetric.OPERATION_NAME))
            .containsExactly("AllTypes");
    }

    @Test
    public void apiCall_operationFailed_addsMetrics() {
        stubErrorResponse();
        assertThatThrownBy(() -> client.allTypes().join()).hasCauseInstanceOf(ProtocolRestJsonException.class);

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        assertThat(capturedCollection.name()).isEqualTo("ApiCall");
        assertThat(capturedCollection.metricValues(CoreMetric.SERVICE_ID))
            .containsExactly(SERVICE_ID);
        assertThat(capturedCollection.metricValues(CoreMetric.OPERATION_NAME))
            .containsExactly("AllTypes");
    }

    private void stubSuccessfulResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(200)
                                           .withBody("{}")));
    }

    private void stubErrorResponse() {
        stubFor(post(anyUrl())
                    .willReturn(aResponse().withStatus(500)
                                           .withBody("{}")));
    }
}
