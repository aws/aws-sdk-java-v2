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
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.awssdk.utils.DateUtils;

public class ProfileCredentialsProviderIntegrationTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";

    @Test
    public void profileWithCredentialSourceUsingEc2InstanceMetadataAndCustomEndpoint_usesEndpointInSourceProfile() {
        String testFileContentsTemplate = "" +
                "[profile a]\n" +
                "role_arn=arn:aws:iam::123456789012:role/testRole3\n" +
                "credential_source = ec2instancemetadata\n" +
                "ec2_metadata_service_endpoint = http://localhost:%d\n";

        WireMockServer mockMetadataEndpoint = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockMetadataEndpoint.start();

        String profileFileContents = String.format(testFileContentsTemplate, mockMetadataEndpoint.port());

        ProfileFile profileFile = ProfileFile.builder()
                .type(ProfileFile.Type.CONFIGURATION)
                .content(new ByteArrayInputStream(profileFileContents.getBytes(StandardCharsets.UTF_8)))
                .build();

        ProfileCredentialsProvider profileCredentialsProvider = ProfileCredentialsProvider.builder()
                .profileFile(profileFile)
                .profileName("a")
                .build();

        String stubToken = "some-token";
        mockMetadataEndpoint.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
        mockMetadataEndpoint.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        mockMetadataEndpoint.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        try {
            profileCredentialsProvider.resolveCredentials();

        } catch (StsException e) {
            // ignored
        } finally {
            mockMetadataEndpoint.stop();
        }

        String userAgentHeader = "User-Agent";
        String userAgent = SdkUserAgent.create().userAgent();
        mockMetadataEndpoint.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        mockMetadataEndpoint.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        mockMetadataEndpoint.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));
    }
}
