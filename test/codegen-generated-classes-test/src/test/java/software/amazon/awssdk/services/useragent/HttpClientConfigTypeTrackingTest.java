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
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigAsyncClient;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Tests that the HTTP client configuration type metadata (md/hc#d or md/hc#e) is correctly
 * included in the User-Agent header based on whether the HTTP client was auto-detected or
 * explicitly configured.
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

    // --- Default HTTP client tests (no httpClient() call, auto-detected from classpath) ---

    @Test
    public void syncClient_defaultHttpClient_containsHcDefault() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
            .build();
        client.allTypes(r -> {});
        assertThat(lastWireMockUserAgent()).contains("md/hc#d");
    }

    @Test
    public void asyncClient_defaultHttpClient_containsHcDefault() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .endpointOverride(URI.create("http://localhost:" + wireMock.port()))
            .build();
        client.allTypes(r -> {}).join();
        assertThat(lastWireMockUserAgent()).contains("md/hc#d");
    }

    // --- Explicit HTTP client tests (mock HTTP clients) ---

    @Test
    public void syncClient_explicitHttpClient_containsHcExplicit() {
        ProtocolRestJsonWithConfigClient client = ProtocolRestJsonWithConfigClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockSyncHttpClient)
            .build();
        client.allTypes(r -> {});
        assertThat(syncUserAgent()).contains("md/hc#e");
    }

    @Test
    public void asyncClient_explicitHttpClient_containsHcExplicit() {
        ProtocolRestJsonWithConfigAsyncClient client = ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(mockAsyncHttpClient)
            .build();
        client.allTypes(r -> {}).join();
        assertThat(asyncUserAgent()).contains("md/hc#e");
    }

    // --- Persistence tests ---

    @Test
    public void syncClient_persistsAcrossRequests() {
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
        assertThat(firstUserAgent).contains("md/hc#e");
        assertThat(secondUserAgent).contains("md/hc#e");
    }

    @Test
    public void asyncClient_persistsAcrossRequests() {
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
        assertThat(firstUserAgent).contains("md/hc#e");
        assertThat(secondUserAgent).contains("md/hc#e");
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
}
