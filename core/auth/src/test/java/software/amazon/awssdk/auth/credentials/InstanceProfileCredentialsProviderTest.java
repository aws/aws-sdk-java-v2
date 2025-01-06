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
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

@WireMockTest
public class InstanceProfileCredentialsProviderTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String TOKEN_STUB = "some-token";
    private static final String PROFILE_NAME = "some-profile";
    private static final String USER_AGENT = SdkUserAgent.create().userAgent();
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";
    private static final EnvironmentVariableHelper environmentVariableHelper = new EnvironmentVariableHelper();

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
                                                               .options(wireMockConfig().dynamicPort().dynamicPort())
                                                               .configureStaticDsl(true)
                                                               .build();

    @BeforeEach
    public void methodSetup() {
        environmentVariableHelper.reset();
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + wireMockServer.getPort());
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        environmentVariableHelper.reset();
    }

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

    private void verifyImdsCallWithToken() {
        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                            .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT))
                            .withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
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

    @Test
    void resolveCredentials_usesTokenByDefault() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.providerName()).isPresent().contains("InstanceProfileCredentialsProvider");
        verifyImdsCallWithToken();
    }

    @Test
    void resolveCredentials_WhenConnectionDelaySetToHighValue() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT, "10");
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS).withFixedDelay(3000));
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.providerName()).isPresent().contains("InstanceProfileCredentialsProvider");
        verifyImdsCallWithToken();
    }

    @Test
    void resolveCredentialsFails_WhenConnectionDelaySetToHighValue_ForDefaultConnectionTimeoutValue() {

        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS).withFixedDelay(1100));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        assertThatExceptionOfType(SdkClientException.class)
            .isThrownBy(provider::resolveCredentials)
            .withRootCauseExactlyInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void resolveIdentity_WhenConnectionDelaySetToHighValue() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT, "10");
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS).withFixedDelay(3000));
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentialsIdentity credentialsIdentity = provider.resolveIdentity().join();
        assertThat(credentialsIdentity.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentialsIdentity.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentialsIdentity.providerName()).isPresent().contains("InstanceProfileCredentialsProvider");
        verifyImdsCallWithToken();
    }

    @Test
    void resolveIdentityFails_WhenConnectionDelaySetToHighValue_ForDefaultConnectionTimeoutValue() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS).withFixedDelay(1100));
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> provider.resolveIdentity().join())
                                                           .withRootCauseExactlyInstanceOf(SocketTimeoutException.class);

    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 405})
    void resolveCredentials_queriesTokenResource_40xError_fallbackToInsecure(int statusCode) {
        stubTokenFetchErrorResponse(aResponse().withBody(STUB_CREDENTIALS), statusCode);
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        provider.resolveCredentials();
        verifyImdsCallInsecure();
    }

    @Test
    void resolveCredentials_queriesTokenResource_socketTimeout_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token").withFixedDelay(Integer.MAX_VALUE)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody(PROFILE_NAME)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME)).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        provider.resolveCredentials();
        verifyImdsCallInsecure();
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 405})
    void resolveCredentials_fallbackToInsecureDisabledThroughProperty_throwsWhenTokenFails(int statusCode) {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property(), "true");
        stubTokenFetchErrorResponse(aResponse().withBody(STUB_CREDENTIALS), statusCode);
        try {
            InstanceProfileCredentialsProvider.builder().build().resolveCredentials();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SdkClientException.class);
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(SdkClientException.class);
            assertThat(cause).hasMessageContaining("fallback to IMDS v1 is disabled");
        }
        finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property());
        }
    }

    @Test
    void resolveCredentials_fallbackToInsecureDisabledThroughProperty_returnsCredentialsWhenTokenReturned() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property(), "true");
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        try {
            InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
            provider.resolveCredentials();
            verifyImdsCallWithToken();
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.property());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 405})
    public void resolveCredentials_fallbackToInsecureDisabledThroughConfig_throwsWhenTokenFails(int statusCode) {
        stubTokenFetchErrorResponse(aResponse().withBody(STUB_CREDENTIALS), statusCode);
        try {
            InstanceProfileCredentialsProvider.builder()
                                              .profileFile(configFile("profile test", Pair.of(ProfileProperty.EC2_METADATA_V1_DISABLED, "true")))
                                              .profileName("test")
                                              .build()
                                              .resolveCredentials();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(SdkClientException.class);
            Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(SdkClientException.class);
            assertThat(cause).hasMessageContaining("fallback to IMDS v1 is disabled");
        }
    }

    @Test
    void resolveCredentials_fallbackToInsecureDisabledThroughConfig_returnsCredentialsWhenTokenReturned() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        InstanceProfileCredentialsProvider.builder()
                                          .profileFile(configFile("profile test", Pair.of(ProfileProperty.EC2_METADATA_V1_DISABLED, "true")))
                                          .profileName("test")
                                          .build()
                                          .resolveCredentials();
        verifyImdsCallWithToken();
    }

    @Test
    void resolveCredentials_fallbackToInsecureEnabledThroughConfig_returnsCredentialsWhenTokenReturned() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        InstanceProfileCredentialsProvider.builder()
                                          .profileFile(configFile("profile test",
                                                                  Pair.of(ProfileProperty.EC2_METADATA_V1_DISABLED, "false")))
                                          .profileName("test")
                                          .build()
                                          .resolveCredentials();
        verifyImdsCallWithToken();
    }

    @Test
    void resolveCredentials_queriesTokenResource_400Error_throws() {
        stubTokenFetchErrorResponse(aResponse().withBody(STUB_CREDENTIALS), 400);

        assertThatThrownBy(() ->  InstanceProfileCredentialsProvider.builder().build().resolveCredentials())
            .isInstanceOf(SdkClientException.class).hasMessage("Failed to load credentials from IMDS.");
    }

    @Test
    void resolveCredentials_endpointSettingEmpty_throws() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "");
        assertThatThrownBy(() ->  InstanceProfileCredentialsProvider.builder().build().resolveCredentials())
            .isInstanceOf(SdkClientException.class).hasMessage("Failed to load credentials from IMDS.");
    }

    @Test
    void resolveCredentials_endpointSettingHostNotExists_throws() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "some-host-that-does-not-exist");
        assertThatThrownBy(() ->  InstanceProfileCredentialsProvider.builder().build().resolveCredentials())
            .isInstanceOf(SdkClientException.class).hasMessage("Failed to load credentials from IMDS.");
    }

    @Test
    void resolveCredentials_metadataLookupDisabled_throws() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property(), "true");
        try {
            assertThatThrownBy(() ->  InstanceProfileCredentialsProvider.builder().build().resolveCredentials())
                .isInstanceOf(SdkClientException.class)
                .hasMessage("IMDS credentials have been disabled by environment variable or system property.");
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property());
        }
    }

    @Test
    void resolveCredentials_customProfileFileAndName_usesCorrectEndpoint() {
        WireMockServer mockMetadataEndpoint_2 = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockMetadataEndpoint_2.start();
        try {
            String stubToken = "some-token";
            mockMetadataEndpoint_2.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
            mockMetadataEndpoint_2.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
            mockMetadataEndpoint_2.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

            String mockServer2Endpoint = "http://localhost:" + mockMetadataEndpoint_2.port();

            InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder()
                    .endpoint(mockServer2Endpoint)
                    .build();

            provider.resolveCredentials();

            mockMetadataEndpoint_2.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile"))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));

            // all requests should have gone to the second server, and none to the other one
            wireMockServer.verify(0, RequestPatternBuilder.allRequests());
        } finally {
            mockMetadataEndpoint_2.stop();
        }
    }

    @Test
   void resolveCredentials_customProfileFileSupplierAndNameSettingEndpointOverride_usesCorrectEndpointFromSupplier() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        WireMockServer mockMetadataEndpoint_2 = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockMetadataEndpoint_2.start();
        try {
            String stubToken = "some-token";
            mockMetadataEndpoint_2.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
            mockMetadataEndpoint_2.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
            mockMetadataEndpoint_2.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

            String mockServer2Endpoint = "http://localhost:" + mockMetadataEndpoint_2.port();

            ProfileFile config = configFile("profile test",
                                            Pair.of(ProfileProperty.EC2_METADATA_SERVICE_ENDPOINT, mockServer2Endpoint));

            List<ProfileFile> profileFileList = Arrays.asList(credentialFile("test", "key1", "secret1"),
                                                              credentialFile("test", "key2", "secret2"),
                                                              credentialFile("test", "key3", "secret3"));

            InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider
                .builder()
                .profileFile(ProfileFileSupplier.aggregate(supply(profileFileList), () -> config))
                .profileName("test")
                .build();

            AwsCredentials awsCredentials1 = provider.resolveCredentials();

            assertThat(awsCredentials1).isNotNull();

            mockMetadataEndpoint_2.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile"))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));

            // all requests should have gone to the second server, and none to the other one
            wireMockServer.verify(0, RequestPatternBuilder.allRequests());
        } finally {
            mockMetadataEndpoint_2.stop();
        }
    }

    @Test
    void resolveCredentials_customSupplierProfileFileAndNameSettingEndpointOverride_usesCorrectEndpointFromSupplier() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        WireMockServer mockMetadataEndpoint_2 = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockMetadataEndpoint_2.start();
        try {
            String stubToken = "some-token";
            mockMetadataEndpoint_2.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
            mockMetadataEndpoint_2.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
            mockMetadataEndpoint_2.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

            String mockServer2Endpoint = "http://localhost:" + mockMetadataEndpoint_2.port();

            ProfileFile config = configFile("profile test",
                                            Pair.of(ProfileProperty.EC2_METADATA_SERVICE_ENDPOINT, mockServer2Endpoint));

            Supplier<ProfileFile> supplier = () -> config;

            InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider
                .builder()
                .profileFile(supplier)
                .profileName("test")
                .build();

            AwsCredentials awsCredentials1 = provider.resolveCredentials();

            assertThat(awsCredentials1).isNotNull();

            mockMetadataEndpoint_2.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile"))
                                              .withHeader(USER_AGENT_HEADER, equalTo(USER_AGENT)));

            // all requests should have gone to the second server, and none to the other one
            wireMockServer.verify(0, RequestPatternBuilder.allRequests());
        } finally {
            mockMetadataEndpoint_2.stop();
        }
    }

    @Test
    void resolveCredentials_doesNotFailIfImdsReturnsExpiredCredentials() {
        String credentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().minus(Duration.ofHours(1))) + '"'
            + "}";

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse));

        AwsCredentials credentials = InstanceProfileCredentialsProvider.builder().build().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
    }

    @Test
    void resolveCredentials_onlyCallsImdsOnceEvenWithExpiredCredentials() {
        String credentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().minus(Duration.ofHours(1))) + '"'
            + "}";

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse));

        AwsCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.builder().build();

        credentialsProvider.resolveCredentials();

        int requestCountAfterOneRefresh = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();

        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();

        int requestCountAfterThreeRefreshes = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();

        assertThat(requestCountAfterThreeRefreshes).isEqualTo(requestCountAfterOneRefresh);
    }

    @Test
    void resolveCredentials_failsIfImdsReturns500OnFirstCall() {
        String errorMessage = "XXXXX";
        String credentialsResponse =
            "{"
            + "\"code\": \"InternalServiceException\","
            + "\"message\": \"" + errorMessage + "\""
            + "}";

        stubSecureCredentialsResponse(aResponse().withStatus(500)
                                                 .withBody(credentialsResponse));

        assertThatThrownBy(InstanceProfileCredentialsProvider.builder().build()::resolveCredentials)
            .isInstanceOf(SdkClientException.class)
            .hasRootCauseMessage(errorMessage);
    }

    @Test
    void resolveCredentials_usesCacheIfImdsFailsOnSecondCall() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
        String successfulCredentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now()) + '"'
            + "}";

        // Set the time to the past, so that the cache expiration time is still is in the past, and then prime the cache
        clock.time = Instant.now().minus(24, HOURS);
        stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse));
        AwsCredentials credentialsBefore = credentialsProvider.resolveCredentials();

        // Travel to the present time take down IMDS, so we can see if we use the cached credentials
        clock.time = Instant.now();
        stubSecureCredentialsResponse(aResponse().withStatus(500));
        AwsCredentials credentialsAfter = credentialsProvider.resolveCredentials();

        assertThat(credentialsBefore).isEqualTo(credentialsAfter);
    }

    @Test
    void resolveCredentials_callsImdsIfCredentialsWithin5MinutesOfExpiration() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
        Instant now = Instant.now();
        String successfulCredentialsResponse1 =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(now) + '"'
            + "}";

        String successfulCredentialsResponse2 =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY2\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(now.plus(6, HOURS)) + '"'
            + "}";

        // Set the time to the past and call IMDS to prime the cache
        clock.time = now.minus(24, HOURS);
        stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse1));
        AwsCredentials credentials24HoursAgo = credentialsProvider.resolveCredentials();

        // Set the time to 10 minutes before expiration, and fail to call IMDS
        clock.time = now.minus(10, MINUTES);
        stubSecureCredentialsResponse(aResponse().withStatus(500));
        AwsCredentials credentials10MinutesAgo = credentialsProvider.resolveCredentials();

        // Set the time to 10 seconds before expiration, and verify that we still call IMDS to try to get credentials in at the
        // last moment before expiration
        clock.time = now.minus(10, SECONDS);
        stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse2));
        AwsCredentials credentials10SecondsAgo = credentialsProvider.resolveCredentials();

        assertThat(credentials24HoursAgo).isEqualTo(credentials10MinutesAgo);
        assertThat(credentials24HoursAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials10SecondsAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    @Test
    void imdsCallFrequencyIsLimited() {
        // Requires running the test multiple times to account for refresh jitter
        for (int i = 0; i < 10; i++) {
            AdjustableClock clock = new AdjustableClock();
            AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
            Instant now = Instant.now();
            String successfulCredentialsResponse1 =
                "{"
                + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
                + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                + "\"Expiration\":\"" + DateUtils.formatIso8601Date(now) + '"'
                + "}";

            String successfulCredentialsResponse2 =
                "{"
                + "\"AccessKeyId\":\"ACCESS_KEY_ID2\","
                + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY2\","
                + "\"Expiration\":\"" + DateUtils.formatIso8601Date(now.plus(6, HOURS)) + '"'
                + "}";

            // Set the time to 5 minutes before expiration and call IMDS
            clock.time = now.minus(5, MINUTES);
            stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse1));
            AwsCredentials credentials5MinutesAgo = credentialsProvider.resolveCredentials();

            // Set the time to 2 seconds before expiration, and verify that do not call IMDS because it hasn't been 5 minutes yet
            clock.time = now.minus(2, SECONDS);
            stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse2));
            AwsCredentials credentials2SecondsAgo = credentialsProvider.resolveCredentials();

            assertThat(credentials2SecondsAgo).isEqualTo(credentials5MinutesAgo);
            assertThat(credentials5MinutesAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        }
    }

    @Test
    void testErrorWhileCacheIsStale_shouldRecover() {
        AdjustableClock clock = new AdjustableClock();

        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofHours(6));

        String successfulCredentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(expiration) + '"'
            + "}";

        String staleResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID_2\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY_2\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now()) + '"'
            + "}";


        Duration staleTime = Duration.ofMinutes(5);
        AwsCredentialsProvider provider = credentialsProviderWithClock(clock, staleTime);

        // cache expiration with expiration = 6 hours
        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse));
        AwsCredentials validCreds = provider.resolveCredentials();

        // failure while cache is stale
        clock.time = expiration.minus(staleTime.minus(Duration.ofMinutes(2)));
        stubTokenFetchErrorResponse(aResponse().withFixedDelay(2000).withBody(STUB_CREDENTIALS), 500);
        stubSecureCredentialsResponse(aResponse().withBody(staleResponse));
        AwsCredentials refreshedWhileStale = provider.resolveCredentials();

        assertThat(refreshedWhileStale).isNotEqualTo(validCreds);
        assertThat(refreshedWhileStale.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY_2");
    }

    @Test
    void shouldNotRetry_whenSucceeds() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        InstanceProfileCredentialsProvider provider =
            InstanceProfileCredentialsProvider.builder()
                                              .retryPolicy((retriesAttempted, statusCode, exception) -> retriesAttempted < 3 && statusCode != 200)
                                              .build();
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.providerName()).isPresent().contains("InstanceProfileCredentialsProvider");
        verifyImdsCallWithToken();
        WireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void shouldRetryAndFail_whenFails_basedOnRetryPolicy(int retries) {
        InstanceProfileCredentialsProvider provider =
            InstanceProfileCredentialsProvider.builder()
                                              .retryPolicy((retriesAttempted, statusCode, exception) -> retriesAttempted < retries && statusCode != 200)
                                              .build();

        String errorMessage = "some error msg";
        String credentialsResponse =
            "{"
            + "\"code\": \"InternalServiceException\","
            + "\"message\": \"" + errorMessage + "\""
            + "}";

        stubSecureCredentialsResponse(aResponse().withStatus(500)
                                                 .withBody(credentialsResponse));
        assertThatThrownBy(provider::resolveCredentials).hasRootCauseMessage("some error msg");
        WireMock.verify(retries + 1, getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    void shouldRetryThenSucceed_basedOnRetryPolicy() {
        String errorMessage = "some error msg";
        String credentialsResponse =
            "{"
            + "\"code\": \"InternalServiceException\","
            + "\"message\": \"" + errorMessage + "\""
            + "}";

        wireMockServer.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                   .inScenario("Retry")
                                   .whenScenarioStateIs(STARTED)
                                   .willReturn(aResponse().withBody(TOKEN_STUB).withStatus(200)));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                   .inScenario("Retry")
                                   .willReturn(aResponse().withBody(PROFILE_NAME).withStatus(200))
                                   .whenScenarioStateIs(STARTED)
                                   .willSetStateTo("error"));
        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME))
                                   .inScenario("Retry")
                                   .whenScenarioStateIs("error")
                                   .willReturn(aResponse().withBody(credentialsResponse).withStatus(500))
                                   .willSetStateTo("success"));

        wireMockServer.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + PROFILE_NAME))
                                   .inScenario("Retry")
                                   .willReturn(aResponse().withBody(STUB_CREDENTIALS).withStatus(200))
                                   .whenScenarioStateIs("success"));

        InstanceProfileCredentialsProvider provider =
            InstanceProfileCredentialsProvider.builder()
                                              .retryPolicy((retriesAttempted, statusCode, exception) -> retriesAttempted < 3 && statusCode != 200)
                                              .build();

        provider.resolveCredentials();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(2, getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    private AwsCredentialsProvider credentialsProviderWithClock(Clock clock) {
        InstanceProfileCredentialsProvider.BuilderImpl builder =
            (InstanceProfileCredentialsProvider.BuilderImpl) InstanceProfileCredentialsProvider.builder();
        builder.clock(clock);
        return builder.build();
    }

    private AwsCredentialsProvider credentialsProviderWithClock(Clock clock, Duration staleTime) {
        InstanceProfileCredentialsProvider.BuilderImpl builder =
            (InstanceProfileCredentialsProvider.BuilderImpl) InstanceProfileCredentialsProvider.builder();
        builder.clock(clock);
        builder.staleTime(staleTime);
        return builder.build();
    }


    private static class AdjustableClock extends Clock {
        private Instant time;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return time;
        }
    }

    private static ProfileFileSupplier supply(Iterable<ProfileFile> iterable) {
        return iterable.iterator()::next;
    }

    private ProfileFile credentialFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CREDENTIALS)
                          .build();
    }

    private ProfileFile configFile(String credentialFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(credentialFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    private ProfileFile credentialFile(String name, String accessKeyId, String secretAccessKey) {
        String contents = String.format("[%s]\naws_access_key_id = %s\naws_secret_access_key = %s\n",
                                        name, accessKeyId, secretAccessKey);
        return credentialFile(contents);
    }

    private ProfileFile configFile(String name, Pair<?, ?>... pairs) {
        String values = Arrays.stream(pairs)
                              .map(pair -> String.format("%s=%s", pair.left(), pair.right()))
                              .collect(Collectors.joining(System.lineSeparator()));
        String contents = String.format("[%s]\n%s", name, values);

        return configFile(contents);
    }

}
