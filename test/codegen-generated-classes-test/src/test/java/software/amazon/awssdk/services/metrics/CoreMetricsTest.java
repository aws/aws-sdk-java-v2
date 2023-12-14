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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.metrics.SdkErrorType;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointParams;
import software.amazon.awssdk.services.protocolrestjson.endpoints.ProtocolRestJsonEndpointProvider;
import software.amazon.awssdk.services.protocolrestjson.model.EmptyModeledException;
import software.amazon.awssdk.services.protocolrestjson.model.SimpleStruct;
import software.amazon.awssdk.services.protocolrestjson.paginators.PaginatedOperationWithResultKeyIterable;
import software.amazon.awssdk.services.testutil.MockIdentityProviderUtil;

@RunWith(MockitoJUnitRunner.class)
public class CoreMetricsTest {
    private static final String SERVICE_ID = "AmazonProtocolRestJson";
    private static final String REQUEST_ID = "req-id";
    private static final String EXTENDED_REQUEST_ID = "extended-id";
    private static final int MAX_RETRIES = 2;
    private static final int MAX_ATTEMPTS = MAX_RETRIES + 1;


    private static ProtocolRestJsonClient client;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private SdkHttpClient mockHttpClient;

    @Mock
    private MetricPublisher mockPublisher;

    @Mock
    private ProtocolRestJsonEndpointProvider mockEndpointProvider;

    @Before
    public void setup() throws IOException {
        client = ProtocolRestJsonClient.builder()
                .httpClient(mockHttpClient)
                .region(Region.US_WEST_2)
                .credentialsProvider(MockIdentityProviderUtil.mockIdentityProvider())
                .overrideConfiguration(c -> c.addMetricPublisher(mockPublisher).retryPolicy(b -> b.numRetries(MAX_RETRIES)))
                .endpointProvider(mockEndpointProvider)
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

        when(mockEndpointProvider.resolveEndpoint(any(ProtocolRestJsonEndpointParams.class))).thenReturn(
            CompletableFuture.completedFuture(Endpoint.builder()
                                                      .url(URI.create("https://protocolrestjson.amazonaws.com"))
                                                      .build()));
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
                .region(Region.US_WEST_2)
                .credentialsProvider(MockIdentityProviderUtil.mockIdentityProvider())
                .httpClient(mockHttpClient)
                .build();

        noPublisher.allTypes();
    }

    @Test
    public void testApiCall_publisherOverriddenOnRequest_requestPublisherTakesPrecedence() {
        MetricPublisher requestMetricPublisher = mock(MetricPublisher.class);

        client.allTypes(r -> r.overrideConfiguration(o -> o.addMetricPublisher(requestMetricPublisher)));

        verify(requestMetricPublisher).publish(any(MetricCollection.class));
        verifyNoMoreInteractions(mockPublisher);
    }

    @Test
    public void testPaginatingApiCall_publisherOverriddenOnRequest_requestPublisherTakesPrecedence() {
        MetricPublisher requestMetricPublisher = mock(MetricPublisher.class);

        PaginatedOperationWithResultKeyIterable iterable =
            client.paginatedOperationWithResultKeyPaginator(
                r -> r.overrideConfiguration(o -> o.addMetricPublisher(requestMetricPublisher)));

        List<SimpleStruct> resultingItems = iterable.items().stream().collect(Collectors.toList());

        verify(requestMetricPublisher).publish(any(MetricCollection.class));
        verifyNoMoreInteractions(mockPublisher);
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
        assertThat(capturedCollection.metricValues(CoreMetric.ENDPOINT_RESOLVE_DURATION)).isNotEmpty();
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(true);
        assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_DURATION).get(0))
            .isGreaterThan(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.CREDENTIALS_FETCH_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.MARSHALLING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(capturedCollection.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(0);
        assertThat(capturedCollection.metricValues(CoreMetric.SERVICE_ENDPOINT).get(0)).isEqualTo(URI.create(
            "https://protocolrestjson.amazonaws.com"));

        assertThat(capturedCollection.children()).hasSize(1);
        MetricCollection attemptCollection = capturedCollection.children().get(0);

        assertThat(attemptCollection.name()).isEqualTo("ApiCallAttempt");
        assertThat(attemptCollection.metricValues(CoreMetric.BACKOFF_DELAY_DURATION))
            .containsExactly(Duration.ZERO);
        assertThat(attemptCollection.metricValues(HttpMetric.HTTP_STATUS_CODE))
            .containsExactly(200);
        assertThat(attemptCollection.metricValues(CoreMetric.SIGNING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_REQUEST_ID))
            .containsExactly(REQUEST_ID);
        assertThat(attemptCollection.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
            .containsExactly(EXTENDED_REQUEST_ID);
        assertThat(attemptCollection.metricValues(CoreMetric.SERVICE_CALL_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ofMillis(100));
        assertThat(attemptCollection.metricValues(CoreMetric.UNMARSHALLING_DURATION).get(0))
            .isGreaterThanOrEqualTo(Duration.ZERO);
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

            assertThat(capturedCollection.children()).hasSize(MAX_ATTEMPTS);
            assertThat(capturedCollection.metricValues(CoreMetric.RETRY_COUNT)).containsExactly(MAX_RETRIES);
            assertThat(capturedCollection.metricValues(CoreMetric.API_CALL_SUCCESSFUL)).containsExactly(false);

            for (MetricCollection requestMetrics : capturedCollection.children()) {
                // A service exception is still a successful HTTP execution so
                // we should still have HTTP metrics as well.
                assertThat(requestMetrics.metricValues(HttpMetric.HTTP_STATUS_CODE))
                    .containsExactly(500);
                assertThat(requestMetrics.metricValues(CoreMetric.AWS_REQUEST_ID))
                    .containsExactly(REQUEST_ID);
                assertThat(requestMetrics.metricValues(CoreMetric.AWS_EXTENDED_REQUEST_ID))
                    .containsExactly(EXTENDED_REQUEST_ID);
                assertThat(requestMetrics.metricValues(CoreMetric.SERVICE_CALL_DURATION)).hasOnlyOneElementSatisfying(d -> {
                    assertThat(d).isGreaterThanOrEqualTo(Duration.ZERO);
                });
                assertThat(requestMetrics.metricValues(CoreMetric.UNMARSHALLING_DURATION)).hasOnlyOneElementSatisfying(d -> {
                    assertThat(d).isGreaterThanOrEqualTo(Duration.ZERO);
                });
                assertThat(requestMetrics.metricValues(CoreMetric.ERROR_TYPE)).containsExactly(SdkErrorType.SERVER_ERROR.toString());
            }
        }
    }

    @Test
    public void testApiCall_httpClientThrowsNetworkError_errorTypeIncludedInMetrics() throws IOException {
        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);
        when(mockExecuteRequest.call()).thenThrow(new IOException("I/O error"));

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
            .thenReturn(mockExecuteRequest);

        thrown.expect(SdkException.class);
        try {
            client.allTypes();
        } finally {
            ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
            verify(mockPublisher).publish(collectionCaptor.capture());

            MetricCollection capturedCollection = collectionCaptor.getValue();
            assertThat(capturedCollection.children()).isNotEmpty();
            for (MetricCollection requestMetrics : capturedCollection.children()) {
                assertThat(requestMetrics.metricValues(CoreMetric.ERROR_TYPE)).containsExactly(SdkErrorType.IO.toString());
            }
        }
    }

    @Test
    public void testApiCall_endpointProviderAddsPathQueryFragment_notReportedInServiceEndpointMetric() {
        when(mockEndpointProvider.resolveEndpoint(any(ProtocolRestJsonEndpointParams.class)))
            .thenReturn(CompletableFuture.completedFuture(Endpoint.builder()
                                                                  .url(URI.create("https://protocolrestjson.amazonaws.com:8080/foo?bar#baz"))
                                                                  .build()));
        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        URI expectedServiceEndpoint = URI.create("https://protocolrestjson.amazonaws.com:8080");
        assertThat(capturedCollection.metricValues(CoreMetric.SERVICE_ENDPOINT)).containsExactly(expectedServiceEndpoint);
    }

    public void testApiCall_apiAttempt_ttfbSeparateFromResponseBody() throws IOException {
        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);

        Duration delayStep = Duration.ofMillis(50);
        InputStream delayedInputStream = new InputStream() {
            private InputStream contentStream = SdkBytes.fromUtf8String("{}").asInputStream();
            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(delayStep.toMillis());
                } catch (InterruptedException ignored) {
                }
                return contentStream.read();
            }
        };

        SdkHttpFullResponse httpFullResponse = SdkHttpFullResponse.builder()
                                                                  .statusCode(200)
                                                                  .content(AbortableInputStream.create(delayedInputStream))
                                                                  .build();

        HttpExecuteResponse executeResponse = mockExecuteResponse(httpFullResponse);

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
            .thenReturn(mockExecuteRequest);

        when(mockExecuteRequest.call()).thenAnswer(invocationOnMock -> {
            try {
                Thread.sleep(delayStep.toMillis());
            } catch (InterruptedException ignored) {
            }
            return executeResponse;
        });

        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        List<Duration> ttfbValues =
            capturedCollection.children().stream()
                              .flatMap(mc -> mc.metricValues(CoreMetric.TIME_TO_FIRST_BYTE).stream())
                              .collect(Collectors.toList());

        assertThat(ttfbValues).isNotEmpty();
        // TTFB should be between [50, 100). Any higher means we included reading at least part of the response body
        for (Duration ttfb : ttfbValues) {
            assertThat(ttfb).isBetween(delayStep, delayStep.multipliedBy(2).minusMillis(1));
        }
    }

    @Test
    public void testApiCall_apiAttempt_ttlbIncludesReadingFullResponse() throws IOException {
        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);

        Duration delayStep = Duration.ofMillis(50);
        InputStream delayedInputStream = new InputStream() {
            private InputStream contentStream = SdkBytes.fromUtf8String("{}").asInputStream();
            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(delayStep.toMillis());
                } catch (InterruptedException ignored) {
                }
                return contentStream.read();
            }
        };

        SdkHttpFullResponse httpFullResponse = SdkHttpFullResponse.builder()
                                                                  .statusCode(200)
                                                                  .content(AbortableInputStream.create(delayedInputStream))
                                                                  .build();

        HttpExecuteResponse executeResponse = mockExecuteResponse(httpFullResponse);

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
            .thenReturn(mockExecuteRequest);

        when(mockExecuteRequest.call()).thenAnswer(invocationOnMock -> {
            try {
                Thread.sleep(delayStep.toMillis());
            } catch (InterruptedException ignored) {
            }
            return executeResponse;
        });

        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();
        List<Duration> ttlbValues =
            capturedCollection.children().stream()
                              .flatMap(mc -> mc.metricValues(CoreMetric.TIME_TO_LAST_BYTE).stream())
                              .collect(Collectors.toList());

        assertThat(ttlbValues).isNotEmpty();
        // TTFB should be at least 4 * delay step; it should take 3 read() calls to get EOF on the stream, and the mock call()
        // invocation also includes a delay.
        for (Duration ttlb : ttlbValues) {
            assertThat(ttlb).isGreaterThan(delayStep.multipliedBy(4));
        }
    }

    @Test
    public void testApiCall_apiAttempt_throughputCalculatedCorrectly() throws IOException {
        ExecutableHttpRequest mockExecuteRequest = mock(ExecutableHttpRequest.class);

        Duration delayStep = Duration.ofMillis(5);
        InputStream delayedInputStream = new InputStream() {
            private InputStream contentStream = SdkBytes.fromUtf8String("{}").asInputStream();
            @Override
            public int read() throws IOException {
                try {
                    Thread.sleep(delayStep.toMillis());
                } catch (InterruptedException ignored) {
                }
                return contentStream.read();
            }
        };

        SdkHttpFullResponse httpFullResponse = SdkHttpFullResponse.builder()
                                                                  .statusCode(200)
                                                                  .content(AbortableInputStream.create(delayedInputStream))
                                                                  .build();

        HttpExecuteResponse executeResponse = mockExecuteResponse(httpFullResponse);

        when(mockHttpClient.prepareRequest(any(HttpExecuteRequest.class)))
            .thenReturn(mockExecuteRequest);

        when(mockExecuteRequest.call()).thenAnswer(invocationOnMock -> {
            try {
                Thread.sleep(delayStep.toMillis());
            } catch (InterruptedException ignored) {
            }
            return executeResponse;
        });

        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        List<MetricCollection> perAttemptMetrics = capturedCollection.children();
        assertThat(perAttemptMetrics).isNotEmpty();
        for (MetricCollection attemptMetrics : perAttemptMetrics) {
            Duration ttfb = attemptMetrics.metricValues(CoreMetric.TIME_TO_FIRST_BYTE).get(0);
            Duration ttlb = attemptMetrics.metricValues(CoreMetric.TIME_TO_LAST_BYTE).get(0);

            double expectedThroughput = (2.0 / ttlb.minus(ttfb).toNanos()) * Duration.ofSeconds(1).toNanos();
            assertThat(attemptMetrics.metricValues(CoreMetric.READ_THROUGHPUT).get(0)).isEqualTo(expectedThroughput);
        }
    }

    @Test
    public void testApiCall_resolverHasDelay_delayPresentInResolveDuration() {
        Endpoint endpoint = Endpoint.builder().url(URI.create("https://myservice.aws")).build();
        when(mockEndpointProvider.resolveEndpoint(any(ProtocolRestJsonEndpointParams.class))).thenAnswer(i -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            return CompletableFuture.completedFuture(endpoint);
        });

        client.allTypes();

        ArgumentCaptor<MetricCollection> collectionCaptor = ArgumentCaptor.forClass(MetricCollection.class);
        verify(mockPublisher).publish(collectionCaptor.capture());

        MetricCollection capturedCollection = collectionCaptor.getValue();

        Duration resolveDuration = capturedCollection.metricValues(CoreMetric.ENDPOINT_RESOLVE_DURATION).get(0);
        assertThat(resolveDuration).isGreaterThan(Duration.ofMillis(50));
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
