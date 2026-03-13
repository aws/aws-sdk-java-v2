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

package software.amazon.awssdk.services;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.core.useragent.BusinessMetricCollection.METRIC_SEARCH_PATTERN;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.multiauth.MultiauthClient;
import software.amazon.awssdk.services.multiauth.auth.scheme.MultiauthAuthSchemeProvider;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.sigv4aauth.Sigv4AauthAsyncClient;
import software.amazon.awssdk.services.sigv4aauth.Sigv4AauthClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.StringInputStream;
import java.util.Arrays;

/**
 * Test class to verify that SIGV4A_SIGNING business metric is correctly included
 * in the User-Agent header when operation called using Sigv4A signing.
 */
class Sigv4aBusinessMetricUserAgentTest {
    private static final String USER_AGENT_HEADER_NAME = "User-Agent";
    private static final StaticCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    private MockSyncHttpClient mockHttpClient;
    private MockAsyncHttpClient mockAsyncHttpClient;

    @BeforeEach
    public void setup() {
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockResponse());

        mockAsyncHttpClient = new MockAsyncHttpClient();
        mockAsyncHttpClient.stubNextResponse(mockResponse());
    }

    @Test
    void when_sigv4aServiceIsUsed_correctMetricIsAdded() {
        Sigv4AauthClient client = Sigv4AauthClient.builder()
                                                  .region(Region.US_WEST_2)
                                                  .credentialsProvider(CREDENTIALS_PROVIDER)
                                                  .httpClient(mockHttpClient)
                                                  .build();

        client.simpleOperationWithNoEndpointParams(r -> r.stringMember("test"));

        String userAgent = getUserAgentFromLastRequest();
        assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply("S"));
    }

    @Test
    void when_sigv4aServiceIsUsedAsync_correctMetricIsAdded() {
        Sigv4AauthAsyncClient asyncClient = Sigv4AauthAsyncClient.builder()
                                                                 .region(Region.US_WEST_2)
                                                                 .credentialsProvider(CREDENTIALS_PROVIDER)
                                                                 .httpClient(mockAsyncHttpClient)
                                                                 .build();

        asyncClient.simpleOperationWithNoEndpointParams(r -> r.stringMember("test")).join();

        String userAgent = getUserAgentFromLastAsyncRequest();
        assertThat(userAgent).matches(METRIC_SEARCH_PATTERN.apply("S"));
    }

    @Test
    void when_regularServiceIsUsed_sigv4aMetricIsNotAdded() {
        ProtocolRestJsonClient client = ProtocolRestJsonClient.builder()
                                                              .region(Region.US_WEST_2)
                                                              .credentialsProvider(CREDENTIALS_PROVIDER)
                                                              .httpClient(mockHttpClient)
                                                              .build();

        client.allTypes(r -> {});

        String userAgent = getUserAgentFromLastRequest();
        assertThat(userAgent).doesNotMatch(METRIC_SEARCH_PATTERN.apply("S"));
    }

    @Test
    void when_regularServiceIsUsedAsync_sigv4aMetricIsNotAdded() {
        ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                                             .region(Region.US_WEST_2)
                                                                             .credentialsProvider(CREDENTIALS_PROVIDER)
                                                                             .httpClient(mockAsyncHttpClient)
                                                                             .build();

        asyncClient.allTypes(r -> {}).join();

        String userAgent = getUserAgentFromLastAsyncRequest();
        assertThat(userAgent).doesNotMatch(METRIC_SEARCH_PATTERN.apply("S"));
    }

    @Test
    void when_signerIsOverridden_sigv4aMetricIsNotAdded() {
        MultiauthClient client = MultiauthClient.builder()
                                                 .region(Region.US_WEST_2)
                                                 .credentialsProvider(CREDENTIALS_PROVIDER)
                                                 .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                        .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                                                           Aws4Signer.create())
                                                                        .build())
                                                 .httpClient(mockHttpClient)
                                                 .build();

        client.multiAuthWithOnlySigv4aAndSigv4(r -> r.stringMember("test"));
        String userAgent = getUserAgentFromLastRequest();

        assertThat(userAgent).doesNotMatch(METRIC_SEARCH_PATTERN.apply("S"));
    }

    @Test
    void when_authSchemeProviderOverridesSigv4aOrder_sigv4IsSelected() {
        MultiauthClient client = MultiauthClient.builder()
                                                  .region(Region.US_WEST_2)
                                                  .credentialsProvider(CREDENTIALS_PROVIDER)
                                                  .authSchemeProvider(MultiauthAuthSchemeProvider.
                                                                          defaultProvider(
                                                      Arrays.asList("sigv4","sigv4a")))
                                                  .httpClient(mockHttpClient)
                                                  .build();

        client.multiAuthWithOnlySigv4aAndSigv4(r -> r.stringMember("test"));
        String userAgent = getUserAgentFromLastRequest();

        assertThat(userAgent).doesNotMatch(METRIC_SEARCH_PATTERN.apply("S"));
    }

    private String getUserAgentFromLastRequest() {
        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get(USER_AGENT_HEADER_NAME);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        return userAgentHeaders.get(0);
    }

    private String getUserAgentFromLastAsyncRequest() {
        SdkHttpRequest lastRequest = mockAsyncHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get(USER_AGENT_HEADER_NAME);
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        return userAgentHeaders.get(0);
    }

    private static HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("{}")))
                                  .build();
    }
}
