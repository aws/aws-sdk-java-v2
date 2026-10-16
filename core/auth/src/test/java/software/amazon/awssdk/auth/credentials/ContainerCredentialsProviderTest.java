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
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_CONTAINER_CREDENTIALS_RELATIVE_URI;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.DateUtils;

import java.util.Optional;

/**
 * Tests for the {@link ContainerCredentialsProviderTest}.
 */
public class ContainerCredentialsProviderTest {
    @ClassRule
    public static WireMockRule mockServer = new WireMockRule(0);

    /** Environment variable name for the AWS ECS Container credentials path. */
    private static final String CREDENTIALS_PATH = "/dummy/credentials/path";
    private static final String ACCESS_KEY_ID = "ACCESS_KEY_ID";
    private static final String SECRET_ACCESS_KEY = "SECRET_ACCESS_KEY";
    private static final String TOKEN = "TOKEN_TOKEN_TOKEN";
    private static final String EXPIRATION_TIME = "3000-05-03T04:55:54Z";
    private ContainerCredentialsProvider credentialsProvider;
    private static EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    @Before
    public void setup() {
        credentialsProvider = ContainerCredentialsProvider.builder()
                                                          .endpoint("http://localhost:" + mockServer.port())
                                                          .build();
        helper.set(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI, CREDENTIALS_PATH);
    }

    @AfterClass
    public static void tearDown() {
        helper.reset();
    }

    /**
     * Tests that when "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" is not set, throws exception.
     */
    @Test(expected = SdkClientException.class)
    public void testEnvVariableNotSet() {
        helper.remove(AWS_CONTAINER_CREDENTIALS_RELATIVE_URI);
        ContainerCredentialsProvider.builder()
                                    .build()
                                    .resolveCredentials();
    }

    @Test
    public void testClassName() {
        assertThat(credentialsProvider.toString()).contains("ContainerCredentialsProvider");
    }

    /**
     * Tests that the getCredentials returns a valid response from endpoint.
     */
    @Test
    public void testGetCredentialsReturnsValidResponseFromEcsEndpoint() {
        stubForSuccessResponse();

        AwsSessionCredentials credentials = (AwsSessionCredentials) credentialsProvider.resolveCredentials();

        assertThat(credentials).isNotNull();
        assertThat(credentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(credentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
        assertThat(credentials.sessionToken()).isEqualTo(TOKEN);
        assertThat(credentials.expirationTime().isPresent());
        assertThat(credentials.expirationTime().get()).isEqualTo(EXPIRATION_TIME);
        assertThat(credentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_HTTP.value());
    }

    /**
     * Tests that the getCredentials won't leak credentials data if the response from ECS is corrupted.
     */
    @Test
    public void getCredentialsWithCorruptResponseDoesNotIncludeCredentialsInExceptionMessage() {
        stubForCorruptedSuccessResponse();

        assertThatThrownBy(credentialsProvider::resolveCredentials).satisfies(t -> {
            String stackTrace = ExceptionUtils.getStackTrace(t);
            assertThat(stackTrace).doesNotContain("ACCESS_KEY_ID");
            assertThat(stackTrace).doesNotContain("SECRET_ACCESS_KEY");
            assertThat(stackTrace).doesNotContain("TOKEN_TOKEN_TOKEN");
        });
    }

    private void stubForSuccessResponse() {
        stubFor200Response(getSuccessfulBody());
    }

    private void stubForCorruptedSuccessResponse() {
        String body = getSuccessfulBody();
        stubFor200Response(body.substring(0, body.length() - 2));
    }

    private void stubFor200Response(String body) {
        stubFor(get(urlPathEqualTo(CREDENTIALS_PATH))
                        .withHeader("User-Agent", equalTo(SdkUserAgent.create().userAgent()))
                        .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withHeader("charset", "utf-8")
                                            .withBody(body)));
    }

    private String getSuccessfulBody() {
        return "{\"AccessKeyId\":\"ACCESS_KEY_ID\"," +
               "\"SecretAccessKey\":\"SECRET_ACCESS_KEY\"," +
               "\"Token\":\"TOKEN_TOKEN_TOKEN\"," +
               "\"Expiration\":\"3000-05-03T04:55:54Z\"}";
    }

    /**
     * Tests that when the cache is stale and refresh fails, the provider returns cached credentials
     * instead of throwing an exception (ALLOW behavior / static stability).
     */
    @Test
    public void testRefreshFailureReturnsCachedCredentials_whenCacheIsStale() {
        // First call succeeds with valid credentials
        stubForSuccessResponse();
        AwsCredentials firstCredentials = credentialsProvider.resolveCredentials();
        assertThat(firstCredentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);

        // Invalidate the cached credentials so the next call must refresh them (mandatory refresh)
        credentialsProvider.invalidate(AwsBasicCredentials.create(ACCESS_KEY_ID, SECRET_ACCESS_KEY)).join();

        // Now stub the endpoint to return a 500 error (simulating container metadata endpoint failure)
        stubFor(get(urlPathEqualTo(CREDENTIALS_PATH))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("Internal Server Error")));

        // Second call: cache is stale (invalidated), refresh fails with 500,
        // but ALLOW behavior should return the cached credentials
        AwsCredentials secondCredentials = credentialsProvider.resolveCredentials();
        assertThat(secondCredentials.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(secondCredentials.secretAccessKey()).isEqualTo(SECRET_ACCESS_KEY);
    }

    /**
     * When the very first call to the endpoint returns credentials that are already expired, they are cached and returned
     * instead of being treated as a failure, without a refresh backoff. The next call refreshes; if the endpoint returns
     * expired credentials again, that is a failed refresh: the cached credentials are kept and the refresh backoff
     * (60-90 seconds) is armed, so the call after that does not contact the endpoint.
     */
    @Test
    public void resolveCredentials_initialFetchReturnsExpiredCredentials_returnsThemAndBacksOffAfterNextFailedRefresh() {
        stubFor200Response(credentialsBody(ACCESS_KEY_ID, "2020-01-01T00:00:00Z"));
        mockServer.resetRequests();

        AwsSessionCredentials first = (AwsSessionCredentials) credentialsProvider.resolveCredentials();
        assertThat(first.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        assertThat(first.expirationTime()).contains(Instant.parse("2020-01-01T00:00:00Z"));
        mockServer.verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));

        AwsCredentials second = credentialsProvider.resolveCredentials();
        assertThat(second.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        mockServer.verify(2, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));

        AwsCredentials third = credentialsProvider.resolveCredentials();
        assertThat(third.accessKeyId()).isEqualTo(ACCESS_KEY_ID);
        mockServer.verify(2, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));
    }

    /**
     * When the very first call to the endpoint returns credentials that are already expired, the next call refreshes them and
     * adopts the valid credentials the endpoint returns.
     */
    @Test
    public void resolveCredentials_initialFetchReturnsExpiredCredentials_nextCallAdoptsFreshCredentials() {
        stubFor200Response(credentialsBody("expired", "2020-01-01T00:00:00Z"));
        assertThat(credentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("expired");

        stubFor200Response(credentialsBody(ACCESS_KEY_ID, EXPIRATION_TIME));
        assertThat(credentialsProvider.resolveCredentials().accessKeyId()).isEqualTo(ACCESS_KEY_ID);
    }

    /**
     * The prefetch time is 15 minutes before expiration, capped at 1 hour from now, and jittered to a random point no later
     * than 1 minute before the stale time. For credentials that expire in 90 seconds, the prefetch time and its whole jitter
     * range are already in the past, while the stale time (1 minute before expiration) is not, so the next call prefetches.
     */
    @Test
    public void resolveCredentials_credentialsExpiringWithinFifteenMinutes_refreshedOnNextCall() {
        stubFor200Response(credentialsBody(ACCESS_KEY_ID, DateUtils.formatIso8601Date(Instant.now().plusSeconds(90))));
        mockServer.resetRequests();

        credentialsProvider.resolveCredentials();
        mockServer.verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));

        credentialsProvider.resolveCredentials();
        mockServer.verify(2, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));
    }

    /**
     * For credentials that expire in 2 hours, the prefetch time is capped at 1 hour from now, so the next call is served from
     * the cache.
     */
    @Test
    public void resolveCredentials_longLivedCredentials_notRefreshedOnNextCall() {
        stubFor200Response(credentialsBody(ACCESS_KEY_ID,
                                           DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofHours(2)))));
        mockServer.resetRequests();

        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();
        mockServer.verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));
    }

    /**
     * Without a configured prefetch time, the default prefetch time is jittered. For credentials that expire in 10 minutes,
     * the default prefetch time (15 minutes before expiration) is in the past, and is jittered to a random point up to 2
     * minutes before expiration, so a provider only refreshes on its next call if the jittered point is also in the past. At
     * least one of several providers must serve its next call from the cache. Without jitter, every provider would refresh.
     */
    @Test
    public void resolveCredentials_prefetchTimeNotConfigured_defaultPrefetchTimeIsJittered() {
        stubFor200Response(credentialsBody(ACCESS_KEY_ID,
                                           DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofMinutes(10)))));
        int providersStillCached = 0;
        for (int i = 0; i < 20; i++) {
            ContainerCredentialsProvider provider =
                ContainerCredentialsProvider.builder()
                                            .endpoint("http://localhost:" + mockServer.port())
                                            .build();
            mockServer.resetRequests();

            provider.resolveCredentials();
            provider.resolveCredentials();
            if (mockServer.findAll(getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH))).size() == 1) {
                providersStillCached++;
            }
        }

        assertThat(providersStillCached).isPositive();
    }

    /**
     * A configured prefetch time is honored exactly, rather than being jittered to some later point. Here the configured
     * window covers the credentials' entire lifetime, so the advisory window opens the moment the credentials are issued and
     * the very next call must contact the endpoint. A jittered window would instead open at a random point up to 2 minutes
     * before expiration, and the next call would usually be served from the cache. Each run uses a new provider so that
     * jitter, if applied, would be detected.
     */
    @Test
    public void resolveCredentials_prefetchTimeConfigured_advisoryWindowIsNotJittered() {
        Duration lifetime = Duration.ofMinutes(10);
        stubFor200Response(credentialsBody(ACCESS_KEY_ID, DateUtils.formatIso8601Date(Instant.now().plus(lifetime))));
        for (int i = 0; i < 10; i++) {
            ContainerCredentialsProvider provider =
                ContainerCredentialsProvider.builder()
                                            .endpoint("http://localhost:" + mockServer.port())
                                            .prefetchTime(lifetime)
                                            .build();
            mockServer.resetRequests();

            provider.resolveCredentials();
            mockServer.verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));

            provider.resolveCredentials();
            mockServer.verify(2, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));
        }
    }

    /**
     * A configured prefetch time replaces the default prefetch time. For credentials that expire in 10 minutes, the default
     * (15 minutes before expiration) is already in the past, but a configured 1 minute window does not open for 9 minutes, so
     * the next call is served from the cache.
     */
    @Test
    public void resolveCredentials_prefetchTimeConfigured_replacesDefaultPrefetchTime() {
        stubFor200Response(credentialsBody(ACCESS_KEY_ID,
                                           DateUtils.formatIso8601Date(Instant.now().plus(Duration.ofMinutes(10)))));
        mockServer.resetRequests();
        ContainerCredentialsProvider provider =
            ContainerCredentialsProvider.builder()
                                        .endpoint("http://localhost:" + mockServer.port())
                                        .prefetchTime(Duration.ofMinutes(1))
                                        .build();

        provider.resolveCredentials();
        provider.resolveCredentials();
        mockServer.verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));
    }

    @Test
    public void build_staleTimeGreaterThanPrefetchTime_throwsException() {
        assertThatThrownBy(() -> ContainerCredentialsProvider.builder()
                                                             .staleTime(Duration.ofMinutes(10))
                                                             .prefetchTime(Duration.ofMinutes(5))
                                                             .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("staleTime (PT10M) must be less than or equal to prefetchTime (PT5M)");
    }

    @Test
    public void build_staleTimeWithoutPrefetchTime_isNotValidatedAgainstDefaultPrefetchTime() {
        assertThat(ContainerCredentialsProvider.builder()
                                               .staleTime(Duration.ofMinutes(30))
                                               .build())
            .isNotNull();
    }

    @Test
    public void toBuilder_prefetchTimeConfigured_copiesPrefetchTime() {
        ContainerCredentialsProvider provider = ContainerCredentialsProvider.builder()
                                                                            .prefetchTime(Duration.ofMinutes(5))
                                                                            .build();

        // The copied prefetch time is validated against the new stale time.
        assertThatThrownBy(() -> provider.toBuilder().staleTime(Duration.ofMinutes(10)).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be less than or equal to prefetchTime (PT5M)");
    }

    /**
     * Tests that when no credentials are cached (initial fetch) and the endpoint fails,
     * an exception is thrown.
     */
    @Test
    public void testInitialFetchFailure_throwsException() {
        stubFor(get(urlPathEqualTo(CREDENTIALS_PATH))
                    .willReturn(aResponse()
                                    .withStatus(500)
                                    .withBody("Internal Server Error")));

        assertThatThrownBy(credentialsProvider::resolveCredentials)
            .isInstanceOf(SdkClientException.class);
    }

    /**
     * A refresh that returns new credentials that are inside the mandatory refresh window (expiring in less than a minute) but
     * have not expired must use them, rather than treating them as a failed refresh. Only credentials whose expiration is at or
     * before now count as a failed refresh. Because they are inside the mandatory refresh window, the next call refreshes again.
     */
    @Test
    public void resolveCredentials_refreshReturnsUnexpiredCredentialsInsideMandatoryWindow_usesThem() {
        ContainerCredentialsProvider provider =
            ContainerCredentialsProvider.builder()
                                        .endpoint("http://localhost:" + mockServer.port())
                                        .build();

        stubFor200Response(credentialsBody("first", EXPIRATION_TIME));
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("first");

        provider.invalidate(AwsBasicCredentials.create("first", "x")).join();

        stubFor200Response(credentialsBody("second", DateUtils.formatIso8601Date(Instant.now().plusSeconds(30))));
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("second");

        // No refresh backoff was applied, so the next call contacts the endpoint again.
        mockServer.resetRequests();
        assertThat(provider.resolveCredentials().accessKeyId()).isEqualTo("second");
        mockServer.verify(1, getRequestedFor(urlPathEqualTo(CREDENTIALS_PATH)));
    }

    private static String credentialsBody(String accessKeyId, String expiration) {
        return "{\"AccessKeyId\":\"" + accessKeyId + "\"," +
               "\"SecretAccessKey\":\"" + SECRET_ACCESS_KEY + "\"," +
               "\"Token\":\"" + TOKEN + "\"," +
               "\"Expiration\":\"" + expiration + "\"}";
    }
}
