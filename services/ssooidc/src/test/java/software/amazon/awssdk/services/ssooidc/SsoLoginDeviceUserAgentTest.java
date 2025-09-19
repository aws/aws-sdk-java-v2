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

package software.amazon.awssdk.services.ssooidc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static software.amazon.awssdk.core.useragent.BusinessMetricCollection.METRIC_SEARCH_PATTERN;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import java.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssooidc.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcToken;

public class SsoLoginDeviceUserAgentTest {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    private SsoOidcClient ssoOidcClient;
    private static final String TEST_SESSION_NAME = "https://d-123.awsapps.com/start";

    @Before
    public void setup() {
        ssoOidcClient = SsoOidcClient.builder()
                                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                                    .region(Region.US_WEST_2)
                                    .endpointOverride(URI.create("http://localhost:" + mockServer.port()))
                                    .build();
    }

    @Test
    public void ssoOidcTokenProvider_shouldHaveSsoLoginDeviceUserAgent() {
        // Setup mock response for SSO OIDC service
        stubFor(any(urlEqualTo("/"))
                    .willReturn(aResponse()
                                    .withStatus(200)
                                    .withHeader("Content-Type", "application/x-amz-json-1.1")
                                    .withBody("{\"access_token\":\"new-access-token\",\"expires_in\":3600}")));

        // Create a test token that needs refreshing (expired)
        OnDiskTokenManager tokenManager = OnDiskTokenManager.create(TEST_SESSION_NAME);
        SsoOidcToken testToken = SsoOidcToken.builder()
                                            .accessToken("test-access-token")
                                            .clientId("test-client-id")
                                            .clientSecret("test-client-secret")
                                            .refreshToken("test-refresh-token")
                                            .startUrl(TEST_SESSION_NAME)
                                            .expiresAt(Instant.now().minusSeconds(10)) // Expired to force refresh
                                            .build();
        tokenManager.storeToken(testToken);

        // Create SsoOidcTokenProvider and trigger token refresh
        try (SsoOidcTokenProvider tokenProvider = SsoOidcTokenProvider.builder()
                                                                      .sessionName(TEST_SESSION_NAME)
                                                                      .ssoOidcClient(ssoOidcClient)
                                                                      .build()) {
            try {
                // This will trigger a token refresh, which should include the SSO_LOGIN_DEVICE metric
                tokenProvider.resolveToken();
            } catch (Exception e) {
                // Expected - we're using a mock server, so the actual token refresh may fail
                // But the important part is that the HTTP request was made with the correct User-Agent
            }
        }

        // Verify that the User-Agent header contains the SSO_LOGIN_DEVICE business metric
        verify(postRequestedFor(urlEqualTo("/")).withHeader("User-Agent",
                                                            matching(METRIC_SEARCH_PATTERN.apply(BusinessMetricFeatureId.SSO_LOGIN_DEVICE.value()))));
    }
}
