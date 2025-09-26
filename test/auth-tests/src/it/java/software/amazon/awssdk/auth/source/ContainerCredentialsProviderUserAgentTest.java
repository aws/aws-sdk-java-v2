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
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
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
 * Test class to verify that ContainerCredentialsProvider correctly includes
 * business metrics in the User-Agent header. This test focuses specifically on the
 * CREDENTIALS_HTTP ("z") business metric feature ID.
 */
class ContainerCredentialsProviderUserAgentTest {
    private static final String CONTAINER_CREDENTIALS_PATH = "/v2/credentials/test-role-arn";
    private static final String CONTAINER_SERVICE_ENDPOINT = "http://localhost:";

    private MockSyncHttpClient mockHttpClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    @BeforeEach
    public void setup() {

        System.setProperty(SdkSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.property(), 
                          CONTAINER_SERVICE_ENDPOINT + wireMockServer.getPort());
        System.setProperty(SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.property(), 
                          CONTAINER_CREDENTIALS_PATH);

        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockStsResponse());

        stubContainerCredentialsResponses();
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_CONTAINER_SERVICE_ENDPOINT.property());
        System.clearProperty(SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI.property());
        System.clearProperty(SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN.property());
    }

    private static HttpExecuteResponse mockStsResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    private void stubContainerCredentialsResponses() {
        String credentialsResponse = createCredentialsResponse("ACCESS_KEY_ID", "SECRET_ACCESS_KEY", null);
        wireMockServer.stubFor(get(urlPathEqualTo(CONTAINER_CREDENTIALS_PATH))
                                   .willReturn(aResponse().withBody(credentialsResponse)));
    }

    private void stubContainerCredentialsResponsesWithSessionToken() {
        String credentialsResponse = createCredentialsResponse("ACCESS_KEY_ID", "SECRET_ACCESS_KEY", "SESSION_TOKEN");
        wireMockServer.stubFor(get(urlPathEqualTo(CONTAINER_CREDENTIALS_PATH))
                                   .willReturn(aResponse().withBody(credentialsResponse)));
    }

    private void stubContainerCredentialsResponsesWithAuthToken() {
        System.setProperty(SdkSystemSetting.AWS_CONTAINER_AUTHORIZATION_TOKEN.property(), "test-auth-token");

        String credentialsResponse = createCredentialsResponse("ACCESS_KEY_ID", "SECRET_ACCESS_KEY", null);
        wireMockServer.stubFor(get(urlPathEqualTo(CONTAINER_CREDENTIALS_PATH))
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
        response.append("\"Expiration\":\"").append(DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofHours(1)))).append("\"");
        response.append("}");
        return response.toString();
    }

    @ParameterizedTest
    @MethodSource("containerCredentialProviders")
    void userAgentString_containsContainerBusinessMetric_WhenUsingContainerCredentials(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> containerCredentialProviders() {
        return Stream.of(
            Arguments.of(ContainerCredentialsProvider.create(), "m/D,z"),

            Arguments.of(ContainerCredentialsProvider.builder()
                            .endpoint(CONTAINER_SERVICE_ENDPOINT + wireMockServer.getPort())
                            .build(), "m/D,z")
        );
    }

    @ParameterizedTest
    @MethodSource("containerCredentialProvidersWithSessionToken")
    void userAgentString_containsContainerBusinessMetric_WhenUsingContainerCredentialsWithSessionToken(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        stubContainerCredentialsResponsesWithSessionToken();
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> containerCredentialProvidersWithSessionToken() {
        return Stream.of(
            Arguments.of(ContainerCredentialsProvider.create(), "m/D,z")
        );
    }

    @ParameterizedTest
    @MethodSource("containerCredentialProvidersWithAuthToken")
    void userAgentString_containsContainerBusinessMetric_WhenUsingContainerCredentialsWithAuthToken(
            IdentityProvider<? extends AwsCredentialsIdentity> provider, String expected) throws Exception {

        stubContainerCredentialsResponsesWithAuthToken();
        
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        assertThat(userAgentHeaders.get(0)).contains(expected);
    }

    private static Stream<Arguments> containerCredentialProvidersWithAuthToken() {
        return Stream.of(
            Arguments.of(ContainerCredentialsProvider.create(), "m/D,z")
        );
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }
}
