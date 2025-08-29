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
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.UnsupportedEncodingException;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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

@WireMockTest
class ImdsCredentialsUserAgentTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String PROFILE_NAME = "some-profile";
    private static final String TOKEN_STUB = "some-token";
    
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";

    private static final AwsCredentials BASIC_IDENTITY = basicCredentialsBuilder().build();

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    private MockSyncHttpClient mockHttpClient;

    @BeforeEach
    public void setup() throws UnsupportedEncodingException {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), 
                          "http://localhost:" + wireMockServer.getPort());

        stubSecureCredentialsResponse();
        
        mockHttpClient = new MockSyncHttpClient();
        mockHttpClient.stubNextResponse(mockResponse());
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    public static HttpExecuteResponse mockResponse() {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(200).build())
                                  .responseBody(AbortableInputStream.create(new StringInputStream("")))
                                  .build();
    }

    @ParameterizedTest
    @MethodSource("credentialProviders")
    void userAgentString_containsCredentialProviderNames_IfPresent(IdentityProvider<? extends AwsCredentialsIdentity> provider,
                                                                   String expected) throws Exception {
        stsClient(provider, mockHttpClient).getCallerIdentity();

        SdkHttpRequest lastRequest = mockHttpClient.getLastRequest();
        assertThat(lastRequest).isNotNull();

        List<String> userAgentHeaders = lastRequest.headers().get("User-Agent");
        assertThat(userAgentHeaders).isNotNull().hasSize(1);
        
        String userAgent = userAgentHeaders.get(0);
        
        if ("m/0".equals(expected)) {
            assertThat(userAgent).matches(".*m/[^\\s]*0[^\\s]*.*");
        } else {
            assertThat(userAgent).contains(expected);
        }
    }

    private static Stream<Arguments> credentialProviders() {
        return Stream.of(
            Arguments.of(createRealImdsCredentialsProvider(), "m/0"),
            Arguments.of(StaticCredentialsProvider.create(BASIC_IDENTITY), "stat")
        );
    }

    /**
     * Creates a InstanceProfileCredentialsProvider that uses mocked IMDS endpoints.
     */
    private static InstanceProfileCredentialsProvider createRealImdsCredentialsProvider() {
        return InstanceProfileCredentialsProvider.builder().build();
    }

    private static void stubSecureCredentialsResponse() {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(TOKEN_STUB)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody(PROFILE_NAME)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME)).willReturn(aResponse().withBody(STUB_CREDENTIALS)));
    }

    private static StsClient stsClient(IdentityProvider<? extends AwsCredentialsIdentity> provider, SdkHttpClient httpClient) {
        return StsClient.builder()
                        .credentialsProvider(provider)
                        .httpClient(httpClient)
                        .build();
    }

    private static AwsBasicCredentials.Builder basicCredentialsBuilder() {
        return AwsBasicCredentials.builder()
                                  .accessKeyId("akid")
                                  .secretAccessKey("secret");
    }
}
