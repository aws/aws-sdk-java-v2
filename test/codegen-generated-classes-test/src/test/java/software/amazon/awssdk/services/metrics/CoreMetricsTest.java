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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;

@RunWith(MockitoJUnitRunner.class)
public class CoreMetricsTest {
    private static final String SERVICE_ID = "AmazonProtocolRestJson";
    private static final String REQUEST_ID = "req-id";
    private static final String EXTENDED_REQUEST_ID = "extended-id";
    private static final int MAX_RETRIES = 2;

    private static ProtocolRestJsonClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private SdkHttpClient mockHttpClient;

    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    @Mock
    private MetricPublisher mockPublisher;

    @Before
    public void setup() throws IOException {
        client = ProtocolRestJsonClient.builder()
                .httpClient(mockHttpClient)
                .credentialsProvider(mockCredentialsProvider)
                .overrideConfiguration(c -> c.metricPublisher(mockPublisher).retryPolicy(b -> b.numRetries(MAX_RETRIES)))
                .build();
        AbortableInputStream content = contentStream("{}");
        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                .statusCode(200)
                .putHeader("x-amz-request-id", REQUEST_ID)
                .putHeader("x-amz-id-2", EXTENDED_REQUEST_ID)
                .content(content)
                .build();

        HttpExecuteResponse mockResponse = mockExecuteResponse(httpResponse);

        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);
        when(mockExecuteRequest.call()).thenAnswer(invocation -> {
           try {
               Thread.sleep(100);
           } catch (InterruptedException ie) {
               ie.printStackTrace();
           }
           return mockResponse;
        });

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
                .thenReturn(mockExecuteRequest);

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
    public void testApiCall_noConfiguredPublisher_succeeds() {
        ProtocolRestJsonClient noPublisher = ProtocolRestJsonClient.builder()
                .httpClient(mockHttpClient)
                .build();

        noPublisher.allTypes();
    }

    @Test
    public void testApiCall_publisherOverriddenOnRequest_requestPublisherTakesPrecedence() {
        MetricPublisher requestMetricPublisher = mock(MetricPublisher.class);

        client.allTypes(r -> r.overrideConfiguration(o -> o.metricPublisher(requestMetricPublisher)));

        verify(requestMetricPublisher).publish(any(MetricCollection.class));
        verifyZeroInteractions(mockPublisher);
    }

    @Test
    public void testApiCall_operationSuccessful_addsMetrics() {
        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        assertThat(capturedCollection.name()).isEqualTo("ApiCall");
        assertThat(capturedCollection.metricValues(CoreMetric.SERVICE_ID))
                .containsExactly(SERVICE_ID);
        assertThat(capturedCollection.metricValues(CoreMetric.OPERATION_NAME))
                .containsExactly("AllTypes");
        assertThat(capturedCollection.metricValues(CoreMetric.CREDENTIALS_FETCH_DURATION).get(0))
                .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.SIGNING_DURATION).get(0))
                .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.MARSHALLING_DURATION).get(0))
                .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_DURATION).get(0))
                .isGreaterThan(Duration.ZERO);

        assertThat(capturedCollection.children()).hasSize(1);
        MetricCollection attemptCollection = capturedCollection.children().get(0);

        assertThat(attemptCollection.name()).isEqualTo("ApiCallAttemptMetrics");
        assertThat(attemptCollection.children()).isEmpty();
        assertThat(attemptCollection.metricValues(CoreMetric.HTTP_STATUS_CODE))
                .containsExactly(200);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_REQUEST_ID))
                .containsExactly(REQUEST_ID);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
                .containsExactly(EXTENDED_REQUEST_ID);

        assertThat(attemptCollection.metricValues(CoreMetric.HTTP_REQUEST_ROUND_TRIP_TIME).get(0))
                .isGreaterThanOrEqualTo(Duration.ofMillis(100));
    }

    @Test
    public void testApiCall_serviceReturnsError_errorInfoIncludedInMetrics() throws IOException {
        AbortableInputStream content = contentStream("{}");

        SdkHttpFullResponse httpResponse = SdkHttpFullResponse.builder()
                .statusCode(500)
                .putHeader("x-amz-request-id", REQUEST_ID)
                .putHeader("x-amz-id-2", EXTENDED_REQUEST_ID)
                .putHeader("X-Amzn-Errortype", "EmptyModeledException")
                .content(content)
                .build();

        HttpExecuteResponse response = mockExecuteResponse(httpResponse);

        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);
        when(mockExecuteRequest.call()).thenReturn(response);

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
                .thenReturn(mockExecuteRequest);

        thrown.expect(EmptyModeledException.class);
        try {
            client.allTypes();
        } finally {
            ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
            verify(mockPublisher).publish(collectionCaptor.capture());

            MetricCollection capturedCollection = collectionCaptor.getValue();

            assertThat(capturedCollection.children()).hasSize(MAX_RETRIES + 1);

            for (MetricCollection requestMetrics : capturedCollection.children()) {
                assertThat(requestMetrics.metricValues(CoreMetric.EXCEPTION).get(0))
                        // Note: for some reason we don't throw the same exact
                        // instance as the one unmarshalled by the response
                        // handler (seems we make a copy upstream) so an
                        // isSamAs assertion fails here
                        .isInstanceOf(EmptyModeledException.class);

                // A service exception is still a successful HTTP execution so
                // we should still have HTTP metrics as well.
                assertThat(requestMetrics.metricValues(CoreMetric.HTTP_STATUS_CODE))
                        .containsExactly(500);
                assertThat(requestMetrics.metricValues(CoreMetric.AWS_REQUEST_ID))
                        .containsExactly(REQUEST_ID);
                assertThat(requestMetrics.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
                        .containsExactly(EXTENDED_REQUEST_ID);
                assertThat(requestMetrics.metricValues(CoreMetric.HTTP_REQUEST_ROUND_TRIP_TIME).get(0))
                        .isGreaterThanOrEqualTo(Duration.ZERO);
            }
        }
    }

    @Test
    public void testApiCall_clientSideExceptionThrown_includedInMetrics() {
        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class))).thenThrow(new RuntimeException("oops"));

        thrown.expect(RuntimeException.class);

        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        MetricCollection requestMetrics = capturedCollection.children().get(0);

        assertThat(requestMetrics.metricValues(CoreMetric.EXCEPTION).get(0)).isExactlyInstanceOf(RuntimeException.class);
    }

    private static HttpExecuteResponse mockExecuteResponse(SdkHttpFullResponse httpResponse) {
        HttpExecuteResponse mockResponse = mock(HttpExecuteResponse.class);
        when(mockResponse.httpResponse()).thenReturn(httpResponse);
        when(mockResponse.responseBody()).thenReturn(httpResponse.content());
        return mockResponse;
    }

    private static AbortableInputStream contentStream(String content) {
        ByteArrayInputStream baos = new ByteArrayInputStream(content.getBytes());
        return AbortableInputStream.create(baos);
    }
}
