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
import java.security.Permission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
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
    void testClassName() {
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        assertThat(provider.toString()).contains("InstanceProfileCredentialsProvider");
    }

    @Test
    void resolveCredentials_usesTokenByDefault() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
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
        assertThat(credentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
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
        assertThat(credentialsIdentity.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
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
    void resolveCredentials_withExpiredCredentials_callsImdsOnceMoreThenBacksOff() {
        String credentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().minus(Duration.ofHours(1))) + '"'
            + "}";

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse));

        AwsCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.builder().build();

        // The initial fetch returns expired credentials. They are used, and no refresh backoff is applied.
        credentialsProvider.resolveCredentials();
        int requestCountAfterOneRefresh = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();

        // The next call refreshes, and IMDS returns expired credentials again: a failed refresh, which arms the backoff.
        credentialsProvider.resolveCredentials();
        int requestCountAfterTwoRefreshes = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();
        assertThat(requestCountAfterTwoRefreshes).isGreaterThan(requestCountAfterOneRefresh);

        // During the backoff, IMDS is not called.
        credentialsProvider.resolveCredentials();
        int requestCountAfterThreeRefreshes = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();
        assertThat(requestCountAfterThreeRefreshes).isEqualTo(requestCountAfterTwoRefreshes);
    }

    @Test
    void resolveCredentials_initialFetchExpired_nextCallAdoptsFreshCredentials() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
        Instant now = Instant.now();

        // The initial fetch returns expired credentials: they are cached and used rather than treated as a failure.
        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(expiredImdsCredentials(now, "SECRET_ACCESS_KEY")));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

        // No refresh backoff was applied, so the next call refreshes and adopts the fresh credentials.
        clock.time = now.plusSeconds(1);
        stubSecureCredentialsResponse(aResponse().withBody(freshImdsCredentials(now, "SECRET_ACCESS_KEY2")));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    @Test
    void resolveCredentials_initialFetchExpiredThenExpiredAgain_callsImdsAgainAfterBackoff() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
        Instant now = Instant.now();

        // The initial fetch returns expired credentials: they are cached and used rather than treated as a failure.
        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(expiredImdsCredentials(now, "SECRET_ACCESS_KEY")));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        int requestCountAfterInitialFetch = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();

        // The next call refreshes, and IMDS returns expired credentials again. That is a failed refresh: the cached
        // credentials are kept and the refresh backoff for expired values (60-90 seconds) is armed.
        Instant failureTime = now.plusSeconds(1);
        clock.time = failureTime;
        stubSecureCredentialsResponse(aResponse().withBody(expiredImdsCredentials(now, "SECRET_ACCESS_KEY_EXPIRED_AGAIN")));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        int requestCountAfterFailedRefresh = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();
        assertThat(requestCountAfterFailedRefresh).isGreaterThan(requestCountAfterInitialFetch);

        // IMDS is not called before the shortest possible backoff has elapsed.
        stubSecureCredentialsResponse(aResponse().withBody(freshImdsCredentials(now, "SECRET_ACCESS_KEY2")));
        clock.time = failureTime.plusSeconds(59);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount())
            .isEqualTo(requestCountAfterFailedRefresh);

        // Once the longest possible backoff has elapsed, IMDS is called again and the fresh credentials are adopted.
        clock.time = failureTime.plusSeconds(91);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    private static String expiredImdsCredentials(Instant now, String secretAccessKey) {
        return imdsCredentials(secretAccessKey, now.minus(Duration.ofHours(1)));
    }

    private static String freshImdsCredentials(Instant now, String secretAccessKey) {
        return imdsCredentials(secretAccessKey, now.plus(6, HOURS));
    }

    private static String imdsCredentials(String secretAccessKey, Instant expiration) {
        return "{"
               + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
               + "\"SecretAccessKey\":\"" + secretAccessKey + "\","
               + "\"Expiration\":\"" + DateUtils.formatIso8601Date(expiration) + '"'
               + "}";
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

        // Set the time to 2 minutes before expiration, and fail to call IMDS. The prefetch time (65 to 90 minutes after the
        // fetch) has certainly passed, so IMDS is called.
        Instant failureTime = now.minus(2, MINUTES);
        clock.time = failureTime;
        stubSecureCredentialsResponse(aResponse().withStatus(500));
        AwsCredentials credentials2MinutesAgo = credentialsProvider.resolveCredentials();

        // The failed refresh arms the refresh backoff (2 minutes left, so 60-90 seconds), so IMDS must not be called again
        // before the shortest possible backoff has elapsed.
        clock.time = failureTime.plusSeconds(59);
        stubSecureCredentialsResponse(aResponse().withBody(successfulCredentialsResponse2));
        AwsCredentials credentialsDuringBackoff = credentialsProvider.resolveCredentials();

        // Once the longest possible backoff has elapsed, 29 seconds before expiration, IMDS is called again to try to get
        // credentials in at the last moment before expiration, and the new credentials are adopted.
        clock.time = failureTime.plusSeconds(91);
        AwsCredentials credentialsAfterBackoff = credentialsProvider.resolveCredentials();

        assertThat(credentials24HoursAgo).isEqualTo(credentials2MinutesAgo);
        assertThat(credentials24HoursAgo).isEqualTo(credentialsDuringBackoff);
        assertThat(credentials24HoursAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentialsAfterBackoff.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    /**
     * Without a configured prefetch time, the provider refreshes at a random time between a lower and an upper bound after
     * the fetch. The bounds depend on the remaining lifetime L of the credentials and the advisory refresh window W for that
     * lifetime: hi = min(90 min, L - W) and lo = min(65 min, max(L / 2, 5 min), 3/4 * hi). If L - W is zero or negative, the
     * refresh is at the fetch.
     */
    @ParameterizedTest(name = "lifetime={0}s")
    @CsvSource({
        // lifetime (s), lo (ms), hi (ms)
        "21600, 3900000, 5400000", // 6 h: 65 to 90 min
        "9000,  3900000, 5400000", // 2 h 30 min: 65 to 90 min
        "7200,  2700000, 3600000", // 2 h: hi = L - W = 60 min, lo = 3/4 * hi = 45 min
        "5400,  1350000, 1800000", // 90 min (W = 60 min): hi = 30 min, lo = 22.5 min
        "5340,  2670000, 4440000", // 89 min (W = 15 min): hi = 74 min, lo = L / 2 = 44.5 min
        "3600,  1800000, 2700000", // 1 h: hi = 45 min, lo = 30 min
        "1200,   600000,  900000", // 20 min (W = 5 min): hi = 15 min, lo = 10 min
        "600,    225000,  300000", // 10 min: hi = 5 min, lo = 3/4 * hi = 3.75 min
        "370,     52500,   70000", // 6 min 10 s: hi = 70 s, lo = 3/4 * hi = 52.5 s
        "330,     22500,   30000", // 5 min 30 s: hi = 30 s, lo = 3/4 * hi = 22.5 s
        "300,         0,       0", // 5 min: L - W = 0, so at the fetch
        "180,         0,       0"  // 3 min: L - W < 0, so at the fetch
    })
    void defaultPrefetchTime_lowestAndHighestRandomFraction_returnsRangeBounds(long lifetimeSeconds, long loMillis,
                                                                               long hiMillis) {
        Instant now = Instant.now().truncatedTo(SECONDS);
        Instant expiration = now.plusSeconds(lifetimeSeconds);

        assertThat(InstanceProfileCredentialsProvider.defaultPrefetchTime(now, expiration, 0))
            .isEqualTo(now.plusMillis(loMillis));
        assertThat(InstanceProfileCredentialsProvider.defaultPrefetchTime(now, expiration, 1))
            .isEqualTo(now.plusMillis(hiMillis));

        Random random = new Random();
        for (int i = 0; i < 10_000; i++) {
            assertThat(InstanceProfileCredentialsProvider.defaultPrefetchTime(now, expiration, random.nextDouble()))
                .isBetween(now.plusMillis(loMillis), now.plusMillis(hiMillis));
        }
    }

    /**
     * For any lifetime, the default prefetch time is no sooner than the fetch. It is also no later than 90 minutes after the
     * fetch and no later than the start of the advisory refresh window, W before expiration, whenever that window has not
     * started yet. If the credentials are already inside their advisory refresh window, the prefetch time is the fetch.
     */
    @Test
    void defaultPrefetchTime_anyLifetime_isNoLaterThanAdvisoryWindowAndNoSoonerThanFetch() {
        Instant now = Instant.now().truncatedTo(SECONDS);
        Random random = new Random();
        for (int i = 0; i < 100_000; i++) {
            Duration lifetime = Duration.ofMillis(Duration.ofMinutes(1).toMillis()
                                                  + (long) (random.nextDouble() * Duration.ofHours(7).toMillis()));
            Instant expiration = now.plus(lifetime);

            Instant prefetch = InstanceProfileCredentialsProvider.defaultPrefetchTime(now, expiration, random.nextDouble());

            assertThat(prefetch).as("lifetime %s", lifetime).isAfterOrEqualTo(now);
            Duration window = advisoryRefreshWindow(lifetime);
            if (!lifetime.minus(window).isNegative()) {
                assertThat(prefetch).as("lifetime %s", lifetime)
                                    .isBeforeOrEqualTo(expiration.minus(window))
                                    .isBeforeOrEqualTo(now.plus(90, MINUTES));
            }
            if (lifetime.compareTo(window) <= 0) {
                assertThat(prefetch).as("lifetime %s", lifetime).isEqualTo(now);
            }
        }
    }

    /**
     * Credentials that are already inside their advisory refresh window when they are fetched (L <= W) have a default
     * prefetch time equal to the fetch, so the next call attempts an advisory refresh.
     */
    @ParameterizedTest(name = "lifetime={0}ms, randomFraction={1}")
    @CsvSource({
        "300000, 0",
        "300000, 1",
        "180000, 0",
        "180000, 1",
        "1,      1"
    })
    void defaultPrefetchTime_credentialsInsideAdvisoryWindow_returnsNow(long lifetimeMillis, double randomFraction) {
        Instant now = Instant.now().truncatedTo(SECONDS);

        assertThat(InstanceProfileCredentialsProvider.defaultPrefetchTime(now, now.plusMillis(lifetimeMillis), randomFraction))
            .isEqualTo(now);
    }

    /**
     * The advisory refresh window table of the credential cache specification.
     */
    private static Duration advisoryRefreshWindow(Duration lifetime) {
        if (lifetime.compareTo(Duration.ofMinutes(20)) <= 0) {
            return Duration.ofMinutes(5);
        }
        return lifetime.compareTo(Duration.ofMinutes(90)) < 0 ? Duration.ofMinutes(15) : Duration.ofMinutes(60);
    }

    /**
     * Credentials without an expiration are refreshed at a random time 60 to 65 minutes after the fetch.
     */
    @Test
    void noExpirationPrefetchTime_anyRandomFraction_isSixtyToSixtyFiveMinutesAfterFetch() {
        Instant now = Instant.now().truncatedTo(SECONDS);

        assertThat(InstanceProfileCredentialsProvider.noExpirationPrefetchTime(now, 0)).isEqualTo(now.plus(60, MINUTES));
        assertThat(InstanceProfileCredentialsProvider.noExpirationPrefetchTime(now, 1)).isEqualTo(now.plus(65, MINUTES));

        Random random = new Random();
        for (int i = 0; i < 10_000; i++) {
            assertThat(InstanceProfileCredentialsProvider.noExpirationPrefetchTime(now, random.nextDouble()))
                .isBetween(now.plus(60, MINUTES), now.plus(65, MINUTES));
        }
    }

    /**
     * Credentials without an expiration are returned, and IMDS is not called again until their prefetch time, 60 minutes after
     * the fetch with the random fraction pinned to 0.
     */
    @Test
    void resolveCredentials_credentialsWithoutExpiration_returnedAndRefreshedAtNoExpirationPrefetchTime() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithPrefetchJitter(clock, () -> 0);
        Instant now = Instant.now().truncatedTo(SECONDS);

        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponseWithoutExpiration("SECRET_ACCESS_KEY")));
        AwsCredentials credentials = credentialsProvider.resolveCredentials();
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.expirationTime()).isEmpty();
        int requestCountAfterFetch = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponseWithoutExpiration("SECRET_ACCESS_KEY2")));
        clock.time = now.plus(60, MINUTES).minusMillis(1);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount())
            .isEqualTo(requestCountAfterFetch);

        clock.time = now.plus(60, MINUTES);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    /**
     * When a refresh of credentials that have an expiration returns credentials without one, the new credentials are used.
     * They are kept past the expiration of the previous credentials, and refreshed 60 minutes after they were fetched.
     */
    @Test
    void resolveCredentials_refreshReturnsCredentialsWithoutExpiration_usesThem() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithPrefetchJitter(clock, () -> 0);
        Instant now = Instant.now().truncatedTo(SECONDS);
        Instant expiration = now.plus(70, MINUTES);
        Instant refreshTime = now.plus(35, MINUTES);

        // 70 minute credentials have a 15 minute advisory window. With the random fraction pinned to 0, they are refreshed
        // 35 minutes (half their lifetime) after the fetch.
        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY", expiration)));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponseWithoutExpiration("SECRET_ACCESS_KEY2")));
        clock.time = refreshTime;
        AwsCredentials refreshed = credentialsProvider.resolveCredentials();
        assertThat(refreshed.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
        assertThat(refreshed.expirationTime()).isEmpty();

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponseWithoutExpiration("SECRET_ACCESS_KEY3")));
        clock.time = expiration.plusSeconds(1);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");

        clock.time = refreshTime.plus(60, MINUTES);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY3");
    }

    /**
     * With the random fraction pinned to 0 or 1, the provider refreshes exactly at the lower or upper bound of the default
     * prefetch range, and not a millisecond earlier: no other jitter is applied.
     */
    @ParameterizedTest(name = "lifetime={0}s, randomFraction={1}")
    @CsvSource({
        "21600, 0, 3900000",
        "21600, 1, 5400000",
        "3600,  0, 1800000",
        "3600,  1, 2700000",
        "600,   0,  225000",
        "600,   1,  300000",
        "370,   0,   52500",
        "370,   1,   70000"
    })
    void resolveCredentials_prefetchTimeNotConfigured_refreshesExactlyAtDefaultPrefetchTime(long lifetimeSeconds,
                                                                                            double randomFraction,
                                                                                            long prefetchMillis) {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithPrefetchJitter(clock, () -> randomFraction);
        Instant now = Instant.now().truncatedTo(SECONDS);

        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(
            credentialsResponse("SECRET_ACCESS_KEY", now.plusSeconds(lifetimeSeconds))));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY2", now.plus(12, HOURS))));
        clock.time = now.plusMillis(prefetchMillis - 1);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

        clock.time = now.plusMillis(prefetchMillis);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    /**
     * With real randomness, the provider does not refresh before the lower bound of the default prefetch range, and has
     * refreshed by the upper bound. Each case runs several times to account for the jitter.
     */
    @ParameterizedTest(name = "lifetime={0}s")
    @CsvSource({
        "21600, 3900000, 5400000",
        "1200,   600000,  900000",
        "370,     52500,   70000"
    })
    void resolveCredentials_prefetchTimeNotConfigured_refreshesWithinDefaultPrefetchRange(long lifetimeSeconds,
                                                                                         long loMillis,
                                                                                         long hiMillis) {
        for (int i = 0; i < 10; i++) {
            AdjustableClock clock = new AdjustableClock();
            AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
            Instant now = Instant.now().truncatedTo(SECONDS);

            clock.time = now;
            stubSecureCredentialsResponse(aResponse().withBody(
                credentialsResponse("SECRET_ACCESS_KEY", now.plusSeconds(lifetimeSeconds))));
            assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

            stubSecureCredentialsResponse(aResponse().withBody(
                credentialsResponse("SECRET_ACCESS_KEY2", now.plus(12, HOURS))));
            clock.time = now.plusMillis(loMillis - 1);
            assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

            clock.time = now.plusMillis(hiMillis);
            assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
        }
    }

    /**
     * If IMDS returns the same credentials when the provider refreshes, the next refresh is again 65 to 90 minutes later,
     * measured from that refresh, rather than on every call.
     */
    @Test
    void resolveCredentials_prefetchTimeNotConfiguredAndImdsReturnsSameCredentials_nextRefreshIsMeasuredFromRefresh() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithPrefetchJitter(clock, () -> 0);
        Instant now = Instant.now().truncatedTo(SECONDS);
        Instant firstRefresh = now.plus(65, MINUTES);

        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY", now.plus(6, HOURS))));
        credentialsProvider.resolveCredentials();

        clock.time = firstRefresh;
        credentialsProvider.resolveCredentials();
        int requestCountAfterFirstRefresh = wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount();

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY2", now.plus(12, HOURS))));
        clock.time = firstRefresh.plus(65, MINUTES).minusMillis(1);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(wireMockServer.countRequestsMatching(RequestPattern.everything()).getCount())
            .isEqualTo(requestCountAfterFirstRefresh);

        clock.time = firstRefresh.plus(65, MINUTES);
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    /**
     * Without a configured prefetch time, credentials whose remaining lifetime is within their advisory refresh window (3
     * minutes left, 5 minute window) are refreshed on the next call. A refresh that returns credentials still inside their
     * window schedules another refresh on the next call.
     */
    @Test
    void resolveCredentials_prefetchTimeNotConfiguredAndCredentialsInsideAdvisoryWindow_refreshesOnNextCall() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithClock(clock);
        Instant now = Instant.now();

        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY", now.plus(3, MINUTES))));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY2", now.plus(3, MINUTES))));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY3", now.plus(6, HOURS))));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY3");
    }

    /**
     * A configured prefetch time replaces the default prefetch time and is honored exactly, rather than being jittered to some
     * later point. Here the 15 minute window for 30 minute credentials opens 15 minutes after they are retrieved. With jitter,
     * it would open at a random point between then and 2 minutes before expiration, so the refresh would almost never happen
     * exactly when the window opens. Each run uses a new provider so that jitter, if applied, would be detected.
     */
    @Test
    void resolveCredentials_prefetchTimeConfigured_advisoryWindowIsHonoredExactly() {
        for (int i = 0; i < 10; i++) {
            AdjustableClock clock = new AdjustableClock();
            AwsCredentialsProvider credentialsProvider = credentialsProviderWithPrefetchTime(clock, Duration.ofMinutes(15));
            Instant now = Instant.now().truncatedTo(SECONDS);
            Instant expiration = now.plus(30, MINUTES);
            Instant advisoryWindowOpens = expiration.minus(15, MINUTES);

            clock.time = now;
            stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY", expiration)));
            assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

            // One second before the advisory window opens, IMDS must not be called again.
            stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY2", now.plus(6, HOURS))));
            clock.time = advisoryWindowOpens.minusSeconds(1);
            assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

            // Exactly when the advisory window opens, IMDS must be called.
            clock.time = advisoryWindowOpens;
            assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
        }
    }

    /**
     * If the credentials' remaining lifetime is shorter than the configured prefetch time, they are inside their advisory
     * refresh window as soon as they arrive, so the provider refreshes on the next call.
     */
    @Test
    void resolveCredentials_prefetchTimeConfiguredAndLongerThanLifetime_refreshesOnNextCall() {
        AdjustableClock clock = new AdjustableClock();
        AwsCredentialsProvider credentialsProvider = credentialsProviderWithPrefetchTime(clock, Duration.ofMinutes(5));
        Instant now = Instant.now();

        clock.time = now;
        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY", now.plus(3, MINUTES))));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");

        stubSecureCredentialsResponse(aResponse().withBody(credentialsResponse("SECRET_ACCESS_KEY2", now.plus(6, HOURS))));
        assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    @Test
    void build_staleTimeGreaterThanPrefetchTime_throwsException() {
        assertThatThrownBy(() -> InstanceProfileCredentialsProvider.builder()
                                                                   .staleTime(Duration.ofMinutes(10))
                                                                   .prefetchTime(Duration.ofMinutes(5))
                                                                   .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("staleTime (PT10M) must be less than or equal to prefetchTime (PT5M)");
    }

    @Test
    void build_staleTimeEqualToPrefetchTime_succeeds() {
        assertThat(InstanceProfileCredentialsProvider.builder()
                                                     .staleTime(Duration.ofMinutes(5))
                                                     .prefetchTime(Duration.ofMinutes(5))
                                                     .build())
            .isNotNull();
    }

    @Test
    void build_staleTimeWithoutPrefetchTime_isNotValidatedAgainstDefaultPrefetchTime() {
        assertThat(InstanceProfileCredentialsProvider.builder()
                                                     .staleTime(Duration.ofHours(1))
                                                     .build())
            .isNotNull();
    }

    @Test
    void toBuilder_prefetchTimeConfigured_copiesPrefetchTime() {
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder()
                                                                                         .prefetchTime(Duration.ofMinutes(5))
                                                                                         .build();

        // The copied prefetch time is validated against the new stale time.
        assertThatThrownBy(() -> provider.toBuilder().staleTime(Duration.ofMinutes(10)).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be less than or equal to prefetchTime (PT5M)");
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

        // Source returns stale credentials (expiration in the past) — treated as a failed refresh.
        // The SDK retains the previously cached credentials and applies backoff.
        clock.time = expiration.minus(staleTime.minus(Duration.ofMinutes(2)));
        stubTokenFetchErrorResponse(aResponse().withFixedDelay(2000).withBody(STUB_CREDENTIALS), 500);
        stubSecureCredentialsResponse(aResponse().withBody(staleResponse));
        AwsCredentials refreshedWhileStale = provider.resolveCredentials();

        // Per spec: stale credentials from source are treated as a failure — original creds retained
        assertThat(refreshedWhileStale).isEqualTo(validCreds);
    }

    @Test
    void shouldNotRetry_whenSucceeds() {
        stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        AwsCredentials credentials = provider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
        verifyImdsCallWithToken();
        WireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_8, max = JRE.JAVA_16)
    void resolveCredentialsFromInstanceProfile_when_defaultProfileHasSecurityException() {
        SecurityManager originalSecurityManager = System.getSecurityManager();
        SecurityManager securityManager = new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof java.io.FilePermission) {
                    String path = perm.getName();
                    if (path.contains(".aws") && path.contains("credentials") &&
                        (perm.getActions().contains("read") || perm.getActions().contains("execute"))) {
                        throw new SecurityException("Access to AWS credentials denied");
                    }
                }
            }
        };

        System.setSecurityManager(securityManager);
        try {
            stubSecureCredentialsResponse(aResponse().withBody(STUB_CREDENTIALS));
            InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
            AwsCredentials credentials = provider.resolveCredentials();

            // Verify credentials are correctly resolved from instance profile
            assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
            assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
            assertThat(credentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_IMDS.value());

            // Verify IMDS was called
            verifyImdsCallWithToken();
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
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

    private AwsCredentialsProvider credentialsProviderWithPrefetchTime(Clock clock, Duration prefetchTime) {
        InstanceProfileCredentialsProvider.BuilderImpl builder =
            (InstanceProfileCredentialsProvider.BuilderImpl) InstanceProfileCredentialsProvider.builder();
        builder.clock(clock);
        builder.prefetchTime(prefetchTime);
        return builder.build();
    }

    private AwsCredentialsProvider credentialsProviderWithPrefetchJitter(Clock clock, DoubleSupplier prefetchJitterRandom) {
        InstanceProfileCredentialsProvider.BuilderImpl builder =
            (InstanceProfileCredentialsProvider.BuilderImpl) InstanceProfileCredentialsProvider.builder();
        builder.clock(clock);
        builder.prefetchJitterRandom(prefetchJitterRandom);
        return builder.build();
    }

    private static String credentialsResponse(String secretAccessKey, Instant expiration) {
        return "{"
               + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
               + "\"SecretAccessKey\":\"" + secretAccessKey + "\","
               + "\"Expiration\":\"" + DateUtils.formatIso8601Date(expiration) + '"'
               + "}";
    }

    private static String credentialsResponseWithoutExpiration(String secretAccessKey) {
        return "{"
               + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
               + "\"SecretAccessKey\":\"" + secretAccessKey + "\""
               + "}";
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

    @Test
    void invalidate_matchingAccessKeyId_invalidatesCache() {
        String accessKeyId = "ACCESS_KEY_ID";
        String expirationFarFuture = DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)));
        String credentialsJson = "{\"AccessKeyId\":\"" + accessKeyId + "\","
                                 + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                                 + "\"Expiration\":\"" + expirationFarFuture + "\"}";

        stubSecureCredentialsResponse(aResponse().withBody(credentialsJson));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        AwsCredentials firstCredentials = provider.resolveCredentials();
        assertThat(firstCredentials.accessKeyId()).isEqualTo(accessKeyId);

        String newAccessKeyId = "NEW_ACCESS_KEY_ID";
        String newCredentialsJson = "{\"AccessKeyId\":\"" + newAccessKeyId + "\","
                                    + "\"SecretAccessKey\":\"NEW_SECRET_ACCESS_KEY\","
                                    + "\"Expiration\":\"" + expirationFarFuture + "\"}";
        stubSecureCredentialsResponse(aResponse().withBody(newCredentialsJson));

        AwsCredentialsIdentity identity = AwsBasicCredentials.create(accessKeyId, "SECRET_ACCESS_KEY");
        provider.invalidate(identity).join();

        AwsCredentials refreshedCredentials = provider.resolveCredentials();
        assertThat(refreshedCredentials.accessKeyId()).isEqualTo(newAccessKeyId);
    }

    @Test
    void invalidate_nonMatchingAccessKeyId_doesNotInvalidateCache() {
        String accessKeyId = "ACCESS_KEY_ID";
        String expirationFarFuture = DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)));
        String credentialsJson = "{\"AccessKeyId\":\"" + accessKeyId + "\","
                                 + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
                                 + "\"Expiration\":\"" + expirationFarFuture + "\"}";

        stubSecureCredentialsResponse(aResponse().withBody(credentialsJson));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        AwsCredentials firstCredentials = provider.resolveCredentials();
        assertThat(firstCredentials.accessKeyId()).isEqualTo(accessKeyId);

        String newCredentialsJson = "{\"AccessKeyId\":\"NEW_ACCESS_KEY_ID\","
                                    + "\"SecretAccessKey\":\"NEW_SECRET_ACCESS_KEY\","
                                    + "\"Expiration\":\"" + expirationFarFuture + "\"}";
        stubSecureCredentialsResponse(aResponse().withBody(newCredentialsJson));

        AwsCredentialsIdentity differentIdentity = AwsBasicCredentials.create("DIFFERENT_KEY", "SECRET");
        provider.invalidate(differentIdentity).join();

        AwsCredentials secondCredentials = provider.resolveCredentials();
        assertThat(secondCredentials.accessKeyId()).isEqualTo(accessKeyId);
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
