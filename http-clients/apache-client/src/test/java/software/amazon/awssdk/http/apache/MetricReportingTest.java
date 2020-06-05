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

package software.amazon.awssdk.http.apache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.http.HttpMetric.AVAILABLE_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.HTTP_CLIENT_NAME;
import static software.amazon.awssdk.http.HttpMetric.LEASED_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.MAX_CONCURRENCY;
import static software.amazon.awssdk.http.HttpMetric.PENDING_CONCURRENCY_ACQUIRES;
import java.io.IOException;
import java.time.Duration;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.pool.PoolStats;
import org.apache.http.protocol.HttpContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.utils.AttributeMap;

@RunWith(MockitoJUnitRunner.class)
public class MetricReportingTest {

    @Mock
    public ConnectionManagerAwareHttpClient mockHttpClient;

    @Mock
    public PoolingHttpClientConnectionManager cm;

    @Before
    public void methodSetup() throws IOException {
        when(mockHttpClient.execute(any(HttpUriRequest.class), any(HttpContext.class)))
                .thenReturn(new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK"));
        when(mockHttpClient.getHttpClientConnectionManager()).thenReturn(cm);

        PoolStats stats = new PoolStats(1, 2, 3, 4);
        when(cm.getTotalStats()).thenReturn(stats);
    }

    @Test
    public void prepareRequest_callableCalled_metricsReported() throws IOException {
        ApacheHttpClient client = newClient();
        MetricCollector collector = MetricCollector.create("test");
        HttpExecuteRequest executeRequest = newRequest(collector);

        client.prepareRequest(executeRequest).call();

        MetricCollection collected = collector.collect();

        assertThat(collected.metricValues(HTTP_CLIENT_NAME)).containsExactly("Apache");
        assertThat(collected.metricValues(LEASED_CONCURRENCY)).containsExactly(1);
        assertThat(collected.metricValues(PENDING_CONCURRENCY_ACQUIRES)).containsExactly(2);
        assertThat(collected.metricValues(AVAILABLE_CONCURRENCY)).containsExactly(3);
        assertThat(collected.metricValues(MAX_CONCURRENCY)).containsExactly(4);
    }

    @Test
    public void prepareRequest_connectionManagerNotPooling_callableCalled_metricsReported() throws IOException {
        ApacheHttpClient client = newClient();
        when(mockHttpClient.getHttpClientConnectionManager()).thenReturn(mock(HttpClientConnectionManager.class));
        MetricCollector collector = MetricCollector.create("test");
        HttpExecuteRequest executeRequest = newRequest(collector);

        client.prepareRequest(executeRequest).call();

        MetricCollection collected = collector.collect();

        assertThat(collected.metricValues(HTTP_CLIENT_NAME)).containsExactly("Apache");
        assertThat(collected.metricValues(LEASED_CONCURRENCY)).isEmpty();
        assertThat(collected.metricValues(PENDING_CONCURRENCY_ACQUIRES)).isEmpty();
        assertThat(collected.metricValues(AVAILABLE_CONCURRENCY)).isEmpty();
        assertThat(collected.metricValues(MAX_CONCURRENCY)).isEmpty();
    }

    private ApacheHttpClient newClient() {
        ApacheHttpRequestConfig config = ApacheHttpRequestConfig.builder()
                .connectionAcquireTimeout(Duration.ofDays(1))
                .connectionTimeout(Duration.ofDays(1))
                .socketTimeout(Duration.ofDays(1))
                .proxyConfiguration(ProxyConfiguration.builder().build())
                .build();

        return new ApacheHttpClient(mockHttpClient, config, AttributeMap.empty());
    }

    private HttpExecuteRequest newRequest(MetricCollector collector) {
        final SdkHttpFullRequest sdkRequest = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.HEAD)
                .host("amazonaws.com")
                .protocol("https")
                .build();

        HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
                .request(sdkRequest)
                .metricCollector(collector)
                .build();

        return executeRequest;
    }
}
