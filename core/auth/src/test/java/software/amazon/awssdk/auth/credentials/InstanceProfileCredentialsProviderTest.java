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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.DateUtils;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

public class InstanceProfileCredentialsProviderTest {
    private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
    private static final String CREDENTIALS_RESOURCE_PATH = "/latest/meta-data/iam/security-credentials/";
    private static final String STUB_CREDENTIALS = "{\"AccessKeyId\":\"ACCESS_KEY_ID\",\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofDays(1)))
            + "\"}";
    private static final String TOKEN_HEADER = "x-aws-ec2-metadata-token";
    private static final String EC2_METADATA_TOKEN_TTL_HEADER = "x-aws-ec2-metadata-token-ttl-seconds";


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public WireMockRule mockMetadataEndpoint = new WireMockRule();

    @Before
    public void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:" + mockMetadataEndpoint.port());
    }

    @AfterClass
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    @Test
    public void resolveCredentials_metadataLookupDisabled_throws() {
        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property(), "true");
        thrown.expect(SdkClientException.class);
        thrown.expectMessage("IMDS credentials have been disabled");
        try {
            InstanceProfileCredentialsProvider.builder().build().resolveCredentials();
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property());
        }
    }

    @Test
    public void resolveCredentials_requestsIncludeUserAgent() {
        String stubToken = "some-token";
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        String userAgentHeader = "User-Agent";
        String userAgent = SdkUserAgent.create().userAgent();
        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));
    }

    @Test
    public void resolveCredentials_queriesTokenResource() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(EC2_METADATA_TOKEN_TTL_HEADER, equalTo("21600")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_includedInCredentialsRequests() {
        String stubToken = "some-token";
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody(stubToken)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(TOKEN_HEADER, equalTo(stubToken)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(TOKEN_HEADER, equalTo(stubToken)));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_403Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(403).withBody("oops")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_404Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(404).withBody("oops")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_405Error_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(405).withBody("oops")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_queriesTokenResource_400Error_throws() {
        thrown.expect(SdkClientException.class);
        thrown.expectMessage("Failed to load credentials from IMDS");

        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withStatus(400).withBody("oops")));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();
    }

    @Test
    public void resolveCredentials_queriesTokenResource_socketTimeout_fallbackToInsecure() {
        stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH)).willReturn(aResponse().withBody("some-token").withFixedDelay(Integer.MAX_VALUE)));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).willReturn(aResponse().withBody("some-profile")));
        stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).willReturn(aResponse().withBody(STUB_CREDENTIALS)));

        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();

        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)));
        WireMock.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")));
    }

    @Test
    public void resolveCredentials_endpointSettingEmpty_throws() {
        thrown.expect(SdkClientException.class);

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "");
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();
    }

    @Test
    public void resolveCredentials_endpointSettingHostNotExists_throws() {
        thrown.expect(SdkClientException.class);

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "some-host-that-does-not-exist");
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();

        provider.resolveCredentials();
    }

    @Test
    public void resolveCredentials_customProfileFileAndName_usesCorrectEndpoint() {
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

            String userAgentHeader = "User-Agent";
            String userAgent = SdkUserAgent.create().userAgent();
            mockMetadataEndpoint_2.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));

            // all requests should have gone to the second server, and none to the other one
            mockMetadataEndpoint.verify(0, RequestPatternBuilder.allRequests());
        } finally {
            mockMetadataEndpoint_2.stop();
        }
    }

    @Test
    public void resolveCredentials_customProfileFileSupplierAndNameSettingEndpointOverride_usesCorrectEndpointFromSupplier() {
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

            String userAgentHeader = "User-Agent";
            String userAgent = SdkUserAgent.create().userAgent();
            mockMetadataEndpoint_2.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));

            // all requests should have gone to the second server, and none to the other one
            mockMetadataEndpoint.verify(0, RequestPatternBuilder.allRequests());
        } finally {
            mockMetadataEndpoint_2.stop();
        }
    }

    @Test
    public void resolveCredentials_customSupplierProfileFileAndNameSettingEndpointOverride_usesCorrectEndpointFromSupplier() {
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

            String userAgentHeader = "User-Agent";
            String userAgent = SdkUserAgent.create().userAgent();
            mockMetadataEndpoint_2.verify(putRequestedFor(urlPathEqualTo(TOKEN_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH)).withHeader(userAgentHeader, equalTo(userAgent)));
            mockMetadataEndpoint_2.verify(getRequestedFor(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile")).withHeader(userAgentHeader, equalTo(userAgent)));

            // all requests should have gone to the second server, and none to the other one
            mockMetadataEndpoint.verify(0, RequestPatternBuilder.allRequests());
        } finally {
            mockMetadataEndpoint_2.stop();
        }
    }

    @Test
    public void resolveCredentials_doesNotFailIfImdsReturnsExpiredCredentials() {
        String credentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().minus(Duration.ofHours(1))) + '"'
            + "}";

        stubCredentialsResponse(aResponse().withBody(credentialsResponse));

        AwsCredentials credentials = InstanceProfileCredentialsProvider.builder().build().resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("ACCESS_KEY_ID");
        assertThat(credentials.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
    }

    @Test
    public void resolveCredentials_onlyCallsImdsOnceEvenWithExpiredCredentials() {
        String credentialsResponse =
            "{"
            + "\"AccessKeyId\":\"ACCESS_KEY_ID\","
            + "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\","
            + "\"Expiration\":\"" + DateUtils.formatIso8601Date(Instant.now().minus(Duration.ofHours(1))) + '"'
            + "}";

        stubCredentialsResponse(aResponse().withBody(credentialsResponse));

        AwsCredentialsProvider credentialsProvider = InstanceProfileCredentialsProvider.builder().build();

        credentialsProvider.resolveCredentials();

        int requestCountAfterOneRefresh = mockMetadataEndpoint.countRequestsMatching(RequestPattern.everything()).getCount();

        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();

        int requestCountAfterThreeRefreshes = mockMetadataEndpoint.countRequestsMatching(RequestPattern.everything()).getCount();

        assertThat(requestCountAfterThreeRefreshes).isEqualTo(requestCountAfterOneRefresh);
    }

    @Test
    public void resolveCredentials_failsIfImdsReturns500OnFirstCall() {
        String errorMessage = "XXXXX";
        String credentialsResponse =
            "{"
            + "\"code\": \"InternalServiceException\","
            + "\"message\": \"" + errorMessage + "\""
            + "}";

        stubCredentialsResponse(aResponse().withStatus(500)
                                           .withBody(credentialsResponse));

        assertThatThrownBy(InstanceProfileCredentialsProvider.builder().build()::resolveCredentials)
            .isInstanceOf(SdkClientException.class)
            .hasRootCauseMessage(errorMessage);
    }

    @Test
    public void resolveCredentials_usesCacheIfImdsFailsOnSecondCall() {
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
        stubCredentialsResponse(aResponse().withBody(successfulCredentialsResponse));
        AwsCredentials credentialsBefore = credentialsProvider.resolveCredentials();

        // Travel to the present time take down IMDS, so we can see if we use the cached credentials
        clock.time = Instant.now();
        stubCredentialsResponse(aResponse().withStatus(500));
        AwsCredentials credentialsAfter = credentialsProvider.resolveCredentials();

        assertThat(credentialsBefore).isEqualTo(credentialsAfter);
    }

    @Test
    public void resolveCredentials_callsImdsIfCredentialsWithin5MinutesOfExpiration() {
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
        stubCredentialsResponse(aResponse().withBody(successfulCredentialsResponse1));
        AwsCredentials credentials24HoursAgo = credentialsProvider.resolveCredentials();

        // Set the time to 10 minutes before expiration, and fail to call IMDS
        clock.time = now.minus(10, MINUTES);
        stubCredentialsResponse(aResponse().withStatus(500));
        AwsCredentials credentials10MinutesAgo = credentialsProvider.resolveCredentials();

        // Set the time to 10 seconds before expiration, and verify that we still call IMDS to try to get credentials in at the
        // last moment before expiration
        clock.time = now.minus(10, SECONDS);
        stubCredentialsResponse(aResponse().withBody(successfulCredentialsResponse2));
        AwsCredentials credentials10SecondsAgo = credentialsProvider.resolveCredentials();

        assertThat(credentials24HoursAgo).isEqualTo(credentials10MinutesAgo);
        assertThat(credentials24HoursAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        assertThat(credentials10SecondsAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY2");
    }

    @Test
    public void imdsCallFrequencyIsLimited() {
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
            stubCredentialsResponse(aResponse().withBody(successfulCredentialsResponse1));
            AwsCredentials credentials5MinutesAgo = credentialsProvider.resolveCredentials();

            // Set the time to 2 seconds before expiration, and verify that do not call IMDS because it hasn't been 5 minutes yet
            clock.time = now.minus(2, SECONDS);
            stubCredentialsResponse(aResponse().withBody(successfulCredentialsResponse2));
            AwsCredentials credentials2SecondsAgo = credentialsProvider.resolveCredentials();

            assertThat(credentials2SecondsAgo).isEqualTo(credentials5MinutesAgo);
            assertThat(credentials5MinutesAgo.secretAccessKey()).isEqualTo("SECRET_ACCESS_KEY");
        }
    }

    private AwsCredentialsProvider credentialsProviderWithClock(Clock clock) {
        InstanceProfileCredentialsProvider.BuilderImpl builder =
            (InstanceProfileCredentialsProvider.BuilderImpl) InstanceProfileCredentialsProvider.builder();
        builder.clock(clock);
        return builder.build();
    }

    private void stubCredentialsResponse(ResponseDefinitionBuilder responseDefinitionBuilder) {
        mockMetadataEndpoint.stubFor(put(urlPathEqualTo(TOKEN_RESOURCE_PATH))
                                         .willReturn(aResponse().withBody("some-token")));
        mockMetadataEndpoint.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH))
                                         .willReturn(aResponse().withBody("some-profile")));
        mockMetadataEndpoint.stubFor(get(urlPathEqualTo(CREDENTIALS_RESOURCE_PATH + "some-profile"))
                                         .willReturn(responseDefinitionBuilder));
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
