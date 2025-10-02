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

package software.amazon.awssdk.auth.source;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Test class to verify that InstanceProfileCredentialsProvider (IMDS) correctly includes
 * business metrics in the User-Agent header.
 */
class ImdsUserAgentProviderTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String TEST_ROLE_NAME = "test-role";
    private static final String TOKEN_STUB = "test-token";

    private MockSyncHttpClient mockHttpClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    @BeforeEach
    public void setup() {

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), 
                          "http://localhost:" + wireMockServer.getPort());

        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());

        stubImdsResponses();
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    private static HttpExecuteResponse mockStsResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    private void stubImdsResponses() {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                   .willReturn(aResponse().withBody(TOKEN_STUB)));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                   .willReturn(aResponse().withBody(TEST_ROLE_NAME)));

        String credentialsResponse = createCredentialsResponse("ACCESS_KEY_ID", "SECRET_ACCESS_KEY",
                                                               null);
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + TEST_ROLE_NAME))
                                   .willReturn(aResponse().withBody(credentialsResponse)));
    }

    private void stubImdsResponsesWithSessionToken() {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                   .willReturn(aResponse().withBody(TOKEN_STUB)));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                   .willReturn(aResponse().withBody(TEST_ROLE_NAME)));

        String credentialsResponse = createCredentialsResponse("ACCESS_KEY_ID", "SECRET_ACCESS_KEY",
                                                               "SESSION_TOKEN");
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + TEST_ROLE_NAME))
                                   .willReturn(aResponse().withBody(credentialsResponse)));
    }

    private String createCredentialsResponse(String accessKeyId, String secretAccessKey, String sessionToken) {
        StringBuilder response = new StringBuilder();
        response.append("{");
        response.append("\"AccessKeyId\":\"").append(accessKeyId).append("\",");
        response.append("\"SecretAccessKey\":\"").append(secretAccessKey).append("\",");
        if (sessionToken != null) {
            response.append("\"Token\":\"").append(sessionToken).append("\",");
        }
        response.append("\"Expiration\":\"").append(DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofHours(1))))
                .append("\"");
        response.append("}");
        return response.toString();
    }

    @ParameterizedTest
    @MethodSource("imdsCredentialProviders")
    void userAgentString_containsImdsBusinessMetric_WhenUsingInstanceProfileCredentials(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> imdsCredentialProviders() {
        return Stream.of(
            Arguments.of(InstanceProfileCredentialsProvider.create(), "m/D,0"),

            Arguments.of(InstanceProfileCredentialsProvider.builder()
                            .endpoint("http://localhost:" + wireMockServer.getPort())
                            .build(), "m/D,0")
        );
    }

    @ParameterizedTest
    @MethodSource("imdsCredentialProvidersWithSessionToken")
    void userAgentString_containsImdsBusinessMetric_WhenUsingInstanceProfileCredentialsWithSessionToken(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        stubImdsResponsesWithSessionToken();
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> imdsCredentialProvidersWithSessionToken() {
        return Stream.of(
            Arguments.of(InstanceProfileCredentialsProvider.create(), "m/D,0")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
