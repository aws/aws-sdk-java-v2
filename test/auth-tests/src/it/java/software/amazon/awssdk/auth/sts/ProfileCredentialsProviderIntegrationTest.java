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

package software.amazon.awssdk.auth.sts;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileCredentialsProviderIntegrationTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String USER_AGENT = SdkUserAgent.create().userAgent();
    private static final String PROFILE_NAME = "some-profile";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String TOKEN_STUB = "some-token";

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    private void stubSecureCredentialsResponse(ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(TOKEN_STUB)));
        stubCredentialsResponse(responseDefinitionBuilder);
    }

    private void stubTokenFetchErrorResponse(ResponseDefinitionBuilder responseDefinitionBuilder, int statusCode) {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(statusCode)
                                                                                              .withBody("oops")));
        stubCredentialsResponse(responseDefinitionBuilder);
    }

    private void stubCredentialsResponse(ResponseDefinitionBuilder responseDefinitionBuilder) {
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody(PROFILE_NAME)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME)).willReturn(responseDefinitionBuilder));
    }

    @Test
    public void resolveCredentials_instanceMetadataSourceAndCustomEndpoint_usesSourceEndpointAndMakesSecureCall() {
        String testFileContentsTemplate = "" +
                "[profile ec2Test]\n" +
                "role_arn=arn:aws:iam::123456789012:role/testRole3\n" +
                "credential_source = ec2instancemetadata\n" +
                "ec2_metadata_service_endpoint = http://localhost:%d\n";
        String profileFileContents = String.format(testFileContentsTemplate, wireMockServer.getPort());

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                .profileFile(configFile(profileFileContents))
                .profileName("ec2Test")
                .build();

        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));

        try {
            profileCredentialsProvider.resolveCredentials();
        } catch (StsException e) {
            // ignored
        }
        verifyImdsCallWithToken();
    }

    @Test
    public void resolveCredentials_instanceMetadataSource_fallbackToInsecureWhenTokenFails() {
        String testFileContentsTemplate = "" +
                                          "[profile ec2Test]\n" +
                                          "role_arn=arn:aws:iam::123456789012:role/testRole3\n" +
                                          "credential_source = ec2instancemetadata\n" +
                                          "ec2_metadata_service_endpoint = http://localhost:%d\n";
        String profileFileContents = String.format(testFileContentsTemplate, wireMockServer.getPort());

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                                                                                          .profileFile(configFile(profileFileContents))
                                                                                          .profileName("ec2Test")
                                                                                          .build();

        stubTokenFetchErrorResponse(aResponse().withBody(STUB_CREDENTIALS), 403);

        try {
            profileCredentialsProvider.resolveCredentials();
        } catch (StsException e) {
            // ignored
        }
        verifyImdsCallInsecure();
    }

    @Test
    public void resolveCredentials_instanceMetadataSourceAndFallbackToInsecureDisabled_throwsWhenTokenFails() {
        String testFileContentsTemplate = "" +
                                          "[profile ec2Test]\n" +
                                          "role_arn=arn:aws:iam::123456789012:role/testRole3\n" +
                                          "credential_source = ec2instancemetadata\n" +
                                          "ec2_metadata_v1_disabled = true\n" +
                                          "ec2_metadata_service_endpoint = http://localhost:%d\n";
        String profileFileContents = String.format(testFileContentsTemplate, wireMockServer.getPort());

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                                                                                          .profileFile(configFile(profileFileContents))
                                                                                          .profileName("ec2Test")
                                                                                          .build();

        stubTokenFetchErrorResponse(aResponse().withBody(STUB_CREDENTIALS), 403);

        try {
            profileCredentialsProvider.resolveCredentials();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SdkClientException.class);
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(SdkClientException.class);
            assertThat(cause).hasMessageContaining("fallback to IMDS v1 is disabled");
        }
    }

    private void verifyImdsCallWithToken() {
        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                            .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                            .withHeader(TOKEN_HEADER, equalTo(TOKEN_STUB))
                            .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile"))
                            .withHeader(TOKEN_HEADER, equalTo(TOKEN_STUB))
                            .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
    }

    private void verifyImdsCallInsecure() {
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                            .withoutHeader(TOKEN_HEADER)
                            .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile"))
                            .withoutHeader(TOKEN_HEADER)
                            .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
    }

    private ProfileFile configFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

}
