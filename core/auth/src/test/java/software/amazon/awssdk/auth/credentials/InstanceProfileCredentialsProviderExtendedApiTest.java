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

package software.amazon.awssdk.auth.credentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.DateUtils;

import java.time.Duration;
import java.time.Instant;

/**
 * Tests verifying IMDS credential resolution with account ID support.
 */
@WireMockTest
public class InstanceProfileCredentialsProviderExtendedApiTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String CREDENTIALS_EXTENDED_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials-extended/";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String TOKEN_STUB = "some-token";
    private static final String PROFILE_NAME = "some-profile";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    private static final String ACCOUNT_ID = "123456789012";
    private static final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                              .options(wireMockConfig().dynamicPort())
                                                              .configureStaticDsl(true)
                                                              .build();

    @BeforeEach
    public void methodSetup() {
        environmentVariableHelper.reset();
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                          "http://localhost:" + wireMockServer.getPort());
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        environmentVariableHelper.reset();
    }

    @Test
    void resolveCredentials_usesExtendedEndpoint_withAccountId() {
        String credentialsWithAccountId = String.format(
            "{\"AccessKeyId\":\"ACCESS_KEY_ID\"," +
            "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\"," +
            "\"Token\":\"SESSION_TOKEN\"," +
            "\"Expiration\":\"%s\"," +
            "\"AccountId\":\"%s\"}",
            DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1))),
            ACCOUNT_ID
        );

        stubSecureCredentialsResponse(aResponse().withBody(credentialsWithAccountId), true);
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(((AwsSessionCredentials)credentials).sessionToken()).isEqualTo("SESSION_TOKEN");
        assertThat(credentials.accountId()).hasValue(ACCOUNT_ID);
        verifyImdsCallWithToken(true);
    }

    @Test
    void resolveCredentials_fallsBackToLegacy_noAccountId() {
        String credentialsWithoutAccountId = String.format(
            "{\"AccessKeyId\":\"ACCESS_KEY_ID\"," +
            "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\"," +
            "\"Token\":\"SESSION_TOKEN\"," +
            "\"Expiration\":\"%s\"," +
            "\"Code\":\"Success\"}",  // No AccountId field at all
            DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
        );

        stubSecureCredentialsResponse(aResponse().withBody(credentialsWithoutAccountId), false);
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(((AwsSessionCredentials)credentials).sessionToken()).isEqualTo("SESSION_TOKEN");
        verifyImdsCallWithToken(false);
    }

    @Test
    void resolveCredentials_withImdsDisabled_returnsNoCredentials() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.environmentVariable(), "true");
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        assertThatThrownBy(() -> provider.resolveCredentials())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("IMDS credentials have been disabled");

        verify(0, putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)));
        verify(0, getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        verify(0, getRequestedFor(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH)));
    }


    @Test
    void resolveCredentials_withInvalidProfile_throwsException() {
        String invalidProfile = "my-profile-0004";

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH))
            .willReturn(aResponse().withBody(invalidProfile)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
            .willReturn(aResponse().withBody(invalidProfile)));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + invalidProfile))
            .willReturn(aResponse().withStatus(404)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + invalidProfile))
            .willReturn(aResponse().withStatus(404)));

        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .willReturn(aResponse().withBody(TOKEN_STUB)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        assertThatThrownBy(() -> provider.resolveCredentials())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Failed to load credentials from IMDS.");
    }

    @Test
    void resolveCredentials_withUnstableProfile_noAccountId_refreshesCredentials() {
        String firstCredentials = String.format(
            "{\"AccessKeyId\":\"ASIAIOSFODNN7EXAMPLE\"," +
            "\"SecretAccessKey\":\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\"," +
            "\"Token\":\"AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKw\"," +
            "\"Expiration\":\"%s\"," +
            "\"Code\":\"Success\"," +
            "\"Type\":\"AWS-HMAC\"," +
            "\"LastUpdated\":\"2025-03-18T20:53:17.832308Z\"," +
            "\"UnexpectedElement7\":{\"Name\":\"ignore-me-7\"}}",
            DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
        );

        String secondCredentials = String.format(
            "{\"AccessKeyId\":\"ASIAIOSFODNN7EXAMPLE\"," +
            "\"SecretAccessKey\":\"wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY\"," +
            "\"Token\":\"AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk5TthT+FvwqnKw\"," +
            "\"Expiration\":\"%s\"," +
            "\"Code\":\"Success\"," +
            "\"Type\":\"AWS-HMAC\"," +
            "\"LastUpdated\":\"2025-03-18T20:53:17.832308Z\"," +
            "\"UnexpectedElement7\":{\"Name\":\"ignore-me-7\"}}",
            DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
        );

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH))
            .inScenario("Profile Change No AccountId")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse().withBody("my-profile-0007"))
            .willSetStateTo("First Profile"));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + "my-profile-0007"))
            .inScenario("Profile Change No AccountId")
            .whenScenarioStateIs("First Profile")
            .willReturn(aResponse().withBody(firstCredentials))
            .willSetStateTo("First Profile Done"));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + "my-profile-0007"))
            .inScenario("Profile Change No AccountId")
            .whenScenarioStateIs("First Profile Done")
            .willReturn(aResponse().withStatus(404))
            .willSetStateTo("Profile Changed"));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH))
            .inScenario("Profile Change No AccountId")
            .whenScenarioStateIs("Profile Changed")
            .willReturn(aResponse().withBody("my-profile-0007-b")));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + "my-profile-0007-b"))
            .willReturn(aResponse().withBody(secondCredentials)));

        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .willReturn(aResponse().withBody(TOKEN_STUB)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        AwsCredentials creds1 = provider.resolveCredentials();
        assertThat(creds1.accountId()).isEmpty();
        assertThat(creds1.accessKeyId()).isEqualTo("ASIAIOSFODNN7EXAMPLE");

        AwsCredentials creds2 = provider.resolveCredentials();
        assertThat(creds2.accountId()).isEmpty();
        assertThat(creds2.accessKeyId()).isEqualTo("ASIAIOSFODNN7EXAMPLE");
    }

    @Test
    void resolveCredentials_withDiscoveredInvalidProfile_noAccountId_throwsException() {
        String invalidProfile = "my-profile-0008";

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH))
            .willReturn(aResponse().withBody(invalidProfile)));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + invalidProfile))
            .willReturn(aResponse().withStatus(404)));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + invalidProfile))
            .willReturn(aResponse().withStatus(404)));

        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .willReturn(aResponse().withBody(TOKEN_STUB)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        assertThatThrownBy(() -> provider.resolveCredentials())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Failed to load credentials from IMDS");
    }

    @Test
    void resolveCredentials_cachesProfile_maintainsAccountId() {
        String credentialsWithAccountId = String.format(
            "{\"AccessKeyId\":\"ACCESS_KEY_ID\"," +
            "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\"," +
            "\"Token\":\"SESSION_TOKEN\"," +
            "\"Expiration\":\"%s\"," +
            "\"AccountId\":\"%s\"}",
            DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1))),
            ACCOUNT_ID
        );

        stubSecureCredentialsResponse(aResponse().withBody(credentialsWithAccountId), true);
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        // First call
        AwsCredentials creds1 = provider.resolveCredentials();
        assertThat(creds1.accountId()).hasValue(ACCOUNT_ID);

        // Second call - should use cached profile
        AwsCredentials creds2 = provider.resolveCredentials();
        assertThat(creds2.accountId()).hasValue(ACCOUNT_ID);

        // Verify profile discovery only called once
        verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH)));
    }

    private void stubSecureCredentialsResponse(com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder responseDefinitionBuilder, boolean useExtended) {
        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(TOKEN_STUB)));
        String path = useExtended ? CREDENTIALS_EXTENDED_RESOURCE_PATH : CREDENTIALS_RESOURCE_PATH;
        
        if (useExtended) {
            wireMockServer.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withBody(PROFILE_NAME)));
            wireMockServer.stubFor(get(urlPathEqualTo(path + PROFILE_NAME)).willReturn(responseDefinitionBuilder));
        } else {
            // Extended endpoint fails, fallback to legacy
            wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH))
                .willReturn(aResponse().withStatus(404)));
            wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + PROFILE_NAME))
                .willReturn(aResponse().withStatus(404)));
            wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody(PROFILE_NAME)));
            wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME)).willReturn(responseDefinitionBuilder));
        }
    }

    private void verifyImdsCallWithToken(boolean useExtended) {
        verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));

        String path = useExtended ? CREDENTIALS_EXTENDED_RESOURCE_PATH : CREDENTIALS_RESOURCE_PATH;
        verify(getRequestedFor(urlPathEqualTo(path))
            .withHeader(TOKEN_HEADER, equalTo(TOKEN_STUB)));
        verify(getRequestedFor(urlPathEqualTo(path + PROFILE_NAME))
            .withHeader(TOKEN_HEADER, equalTo(TOKEN_STUB)));

        if (useExtended) {
            // Verify extended endpoint was tried first
            verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH)));
            verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_EXTENDED_RESOURCE_PATH + PROFILE_NAME)));
        }
    }
}
