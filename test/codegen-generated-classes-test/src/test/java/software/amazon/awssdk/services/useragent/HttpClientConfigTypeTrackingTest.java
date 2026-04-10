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

package software.amazon.awssdk.services.useragent;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.useragent.BusinessMetricCollection.METRIC_SEARCH_PATTERN;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigAsyncClient;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Tests that the HTTP client selection business metric (AJ, AK, or AL) is correctly
 * included in the m/ section of the User-Agent header based on how the HTTP client was configured:
 * auto-detected (AJ), explicit instance via httpClient() (AK), or explicit factory via httpClientBuilder() (AL).
 */
public class HttpClientConfigTypeTrackingTest {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    private WireMockServer wireMock;
    private MockSyncHttpClient mockSyncHttpClient;
    private MockAsyncHttpClient mockAsyncHttpClient;

    @BeforeEach
    public void setup() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();
        wireMock.stubFor(post(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{}")));

        mockSyncHttpClient = new MockSyncHttpClient();
        mockSyncHttpClient.stubNextResponse(mockResponse());

        mockAsyncHttpClient = new MockAsyncHttpClient();
        mockAsyncHttpClient.stubNextResponse(mockResponse());
    }

    @AfterEach
    public void teardown() {
        wireMock.stop();
    }

    // --- Auto-selected HTTP client tests (no httpClient/httpClientBuilder call) ---

    @Test
    public void syncClient_defaultHttpClient_containsAutoMetric() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
            .build();
        client.allTypes(r -> {});
        assertThat(lastWireMockUserAgent())
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
    }

    @Test
    public void asyncClient_defaultHttpClient_containsAutoMetric() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
            .build();
        client.allTypes(r -> {}).join();
        assertThat(lastWireMockUserAgent())
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
    }

    // --- Explicit HTTP client instance tests (httpClient() call) ---

    @Test
    public void syncClient_explicitHttpClient_containsExplicitInstanceMetric() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockSyncHttpClient)
            .build();
        client.allTypes(r -> {});
        assertThat(syncUserAgent())
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
    }

    @Test
    public void asyncClient_explicitHttpClient_containsExplicitInstanceMetric() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockAsyncHttpClient)
            .build();
        client.allTypes(r -> {}).join();
        assertThat(asyncUserAgent())
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
    }

    // --- Explicit HTTP client factory tests (httpClientBuilder() call) ---

    @Test
    public void syncClient_explicitHttpClientBuilder_containsExplicitFactoryMetric() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClientBuilder(new MockSyncHttpClientBuilder(mockSyncHttpClient))
            .build();
        client.allTypes(r -> {});
        assertThat(syncUserAgent())
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
    }

    @Test
    public void asyncClient_explicitHttpClientBuilder_containsExplicitFactoryMetric() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClientBuilder(new MockAsyncHttpClientBuilder(mockAsyncHttpClient))
            .build();
        client.allTypes(r -> {}).join();
        assertThat(asyncUserAgent())
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
    }

    // --- Mutual exclusivity tests ---

    @Test
    public void syncClient_defaultHttpClient_doesNotContainExplicitMetrics() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
            .build();
        client.allTypes(r -> {});
        String ua = lastWireMockUserAgent();
        assertThat(ua).matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
        assertThat(ua).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
        assertThat(ua).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
    }

    @Test
    public void syncClient_explicitInstance_doesNotContainOtherMetrics() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockSyncHttpClient)
            .build();
        client.allTypes(r -> {});
        String ua = syncUserAgent();
        assertThat(ua).matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
        assertThat(ua).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
        assertThat(ua).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
    }

    @Test
    public void syncClient_explicitFactory_doesNotContainOtherMetrics() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClientBuilder(new MockSyncHttpClientBuilder(mockSyncHttpClient))
            .build();
        client.allTypes(r -> {});
        String ua = syncUserAgent();
        assertThat(ua).matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
        assertThat(ua).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
        assertThat(ua).doesNotMatch(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
    }

    // --- Persistence tests ---

    @Test
    public void syncClient_autoMetric_persistsAcrossRequests() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
            .build();
        client.allTypes(r -> {});
        String firstUserAgent = lastWireMockUserAgent();
        client.allTypes(r -> {});
        String secondUserAgent = lastWireMockUserAgent();
        assertThat(firstUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
        assertThat(secondUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_AUTO.value()));
    }

    @Test
    public void syncClient_explicitInstanceMetric_persistsAcrossRequests() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockSyncHttpClient)
            .build();
        client.allTypes(r -> {});
        String firstUserAgent = syncUserAgent();
        mockSyncHttpClient.stubNextResponse(mockResponse());
        client.allTypes(r -> {});
        String secondUserAgent = syncUserAgent();
        assertThat(firstUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
        assertThat(secondUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
    }

    @Test
    public void asyncClient_explicitInstanceMetric_persistsAcrossRequests() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockAsyncHttpClient)
            .build();
        client.allTypes(r -> {}).join();
        String firstUserAgent = asyncUserAgent();
        mockAsyncHttpClient.stubNextResponse(mockResponse());
        client.allTypes(r -> {}).join();
        String secondUserAgent = asyncUserAgent();
        assertThat(firstUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
        assertThat(secondUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_INSTANCE.value()));
    }

    @Test
    public void syncClient_explicitFactoryMetric_persistsAcrossRequests() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClientBuilder(new MockSyncHttpClientBuilder(mockSyncHttpClient))
            .build();
        client.allTypes(r -> {});
        String firstUserAgent = syncUserAgent();
        mockSyncHttpClient.stubNextResponse(mockResponse());
        client.allTypes(r -> {});
        String secondUserAgent = syncUserAgent();
        assertThat(firstUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
        assertThat(secondUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
    }

    @Test
    public void asyncClient_explicitFactoryMetric_persistsAcrossRequests() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClientBuilder(new MockAsyncHttpClientBuilder(mockAsyncHttpClient))
            .build();
        client.allTypes(r -> {}).join();
        String firstUserAgent = asyncUserAgent();
        mockAsyncHttpClient.stubNextResponse(mockResponse());
        client.allTypes(r -> {}).join();
        String secondUserAgent = asyncUserAgent();
        assertThat(firstUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
        assertThat(secondUserAgent)
            .matches(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.HTTP_CLIENT_EXPLICIT_FACTORY.value()));
    }

    // --- Helpers ---

    private String lastWireMockUserAgent() {
        List<LoggedRequest> requests = wireMock.findAll(allRequests());
        assertThat(requests).isNotEmpty();
        return requests.get(requests.size() - 1).getHeader("User-Agent");
    }

    private String syncUserAgent() {
        SdkHttpRequest lastRequest = mockSyncHttpClient.getLastRequest();
        List<String> headers = lastRequest.headers().get("User-Agent");
        assertThat(headers).isNotNull().hasSize(1);
        return headers.get(0);
    }

    private String asyncUserAgent() {
        SdkHttpRequest lastRequest = mockAsyncHttpClient.getLastRequest();
        List<String> headers = lastRequest.headers().get("User-Agent");
        assertThat(headers).isNotNull().hasSize(1);
        return headers.get(0);
    }

    private static HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                  .build();
    }

    /**
     * A minimal SdkHttpClient.Builder that returns a pre-built mock sync HTTP client.
     */
    private static class MockSyncHttpClientBuilder implements SdkHttpClient.Builder<MockSyncHttpClientBuilder> {
        private final SdkHttpClient client;

        MockSyncHttpClientBuilder(SdkHttpClient client) {
            this.client = client;
        }

        @Override
        public SdkHttpClient buildWithDefaults(software.amazon.awssdk.utils.AttributeMap serviceDefaults) {
            return client;
        }
    }

    /**
     * A minimal SdkAsyncHttpClient.Builder that returns a pre-built mock async HTTP client.
     */
    private static class MockAsyncHttpClientBuilder implements SdkAsyncHttpClient.Builder<MockAsyncHttpClientBuilder> {
        private final SdkAsyncHttpClient client;

        MockAsyncHttpClientBuilder(SdkAsyncHttpClient client) {
            this.client = client;
        }

        @Override
        public SdkAsyncHttpClient buildWithDefaults(software.amazon.awssdk.utils.AttributeMap serviceDefaults) {
            return client;
        }
    }
}
