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

package software.amazon.awssdk.services.ssooidc.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;
import software.amazon.awssdk.services.ssooidc.model.UnauthorizedClientException;

public class SsoTokenProviderTest {
    private TokenManager<SsoToken> mockTokenManager;
    private SsoOidcClient ssoOidcClient;

    @BeforeEach
    public void setup() {
        mockTokenManager = mock(TokenManager.class);
        ssoOidcClient = mock(SsoOidcClient.class);
    }

    @Test
    public void resolveToken_usesTokenManager() {
        SsoToken ssoToken = SsoToken.builder()
                                    .accessToken("accesstoken")
                                    .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                                    .build();
        Optional<SsoToken> returnVal = Optional.of(ssoToken);
        when(mockTokenManager.loadToken()).thenReturn(returnVal);

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              null);
        assertThat(tokenProvider.resolveToken()).isSameAs(ssoToken);
    }

    @Test
    public void resolveToken_cachedValueNotPresent_throws() {
        Optional<SsoToken> returnVal = Optional.empty();
        when(mockTokenManager.loadToken()).thenReturn(returnVal);

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              null);

        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to load");
    }

    //        {
    //             "documentation": "Valid token with all fields",
    //             "currentTime": "2021-12-25T13:30:00Z",
    //             "cachedToken": {
    //                 "startUrl": "https://d-123.awsapps.com/start",
    //                 "region": "us-west-2",
    //                 "accessToken": "cachedtoken",
    //                 "expiresAt": "2021-12-25T21:30:00Z",
    //                 "clientId": "clientid",
    //                 "clientSecret": "YSBzZWNyZXQ=",
    //                 "registrationExpiresAt": "2022-12-25T13:30:00Z",
    //                 "refreshToken": "cachedrefreshtoken"
    //             },
    //             "expectedToken": {
    //                 "token": "cachedtoken",
    //                 "expiration": "2021-12-25T21:30:00Z"
    //             }
    //         },
    @Test
    public void standardTest_Valid_token_with_all_fields() {
        SsoToken token = getDefaultTokenBuilder()
            .expiresAt(Instant.parse("2021-12-25T21:30:00Z")).registrationExpiresAt(Instant.parse("2022-12-25T13:30:00Z"))
            .build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(token));

        Clock clock = Clock.fixed(Instant.parse("2021-12-25T13:30:00Z"), ZoneId.of("UTC"));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, clock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);

        AwsToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo(token.token());
        assertThat(resolvedToken.expirationTime()).isEqualTo(token.expirationTime());

    }

    @Test
    public void refresh_returns_cached_token_when_service_calls_fails() {
        SsoToken nearToExpiryToken = getDefaultTokenBuilder()
            .expiresAt(Instant.parse("2021-12-25T13:07:00Z")).registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
            .build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(nearToExpiryToken));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenThrow(UnauthorizedClientException.class);

        Clock clock = Clock.fixed(Instant.parse("2021-12-25T13:05:00Z"), ZoneId.of("UTC"));
        // https://d-123.awsapps.com/start


        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, clock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);

        AwsToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo(nearToExpiryToken.token());
        assertThat(resolvedToken.expirationTime()).isEqualTo(nearToExpiryToken.expirationTime());
    }

    @Test
    public void refresh_fails_when_supplier_fails_due_to_Non_service_issues() {
        SsoToken nearToExpiryToken = getDefaultTokenBuilder()
            .expiresAt(Instant.parse("2021-12-25T13:07:00Z")).registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
            .build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(nearToExpiryToken));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenThrow(SdkClientException.class);
        Clock clock = Clock.fixed(Instant.parse("2021-12-25T13:05:00Z"), ZoneId.of("UTC"));
        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, clock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);

        assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> tokenProvider.resolveToken());
    }

    //        {
    //             "documentation": "Minimal valid cached token",
    //             "currentTime": "2021-12-25T13:30:00Z",
    //             "cachedToken": {
    //                 "accessToken": "cachedtoken",
    //                 "expiresAt": "2021-12-25T21:30:00Z"
    //             },
    //             "expectedToken": {
    //                 "token": "cachedtoken",
    //                 "expiration": "2021-12-25T21:30:00Z"
    //             }
    //         },
    @Test
    public void standardTest_Minimal_valid_cached_token() {
        Instant expiresAt = Instant.parse("2021-12-25T21:30:00Z");
        SsoToken ssoToken = SsoToken.builder().accessToken("cachedtoken").expiresAt(expiresAt).build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(ssoToken));

        Clock testClock = Clock.fixed(Instant.parse("2021-12-25T13:30:00Z"), ZoneId.of("UTC"));


        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start", "region" ,
                                                              mockTokenManager, testClock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);



        assertThat(tokenProvider.resolveToken()).isEqualTo(ssoToken);
    }

    //         {
    //             "documentation": "Minimal expired cached token",
    //             "currentTime": "2021-12-25T13:30:00Z",
    //             "cachedToken": {
    //                 "accessToken": "cachedtoken",
    //                 "expiresAt": "2021-12-25T13:00:00Z"
    //             },
    //             "expectedException": "ExpiredToken"
    //         }
    @Test
    public void standardTest_Minimal_expired_cached_token() {
        String startUrl = "https://d-123.awsapps.com/start";
        Instant expiresAt = Instant.parse("2021-12-25T13:00:00Z");
        SsoToken ssoToken =
            SsoToken.builder().startUrl(startUrl).clientId("clientId").clientSecret("clientSecret")
                    .accessToken("cachedtoken").expiresAt(expiresAt).build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(ssoToken));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(CreateTokenResponse.builder().accessToken("cachedtoken")
                                           .expiresIn(1640437200)
                                           .build());

        Clock testClock = Clock.fixed(Instant.parse("2021-12-25T13:30:00Z"), ZoneId.of("UTC"));


        SsoTokenProvider tokenProvider = new SsoTokenProvider(startUrl, "region",
                                                              mockTokenManager, testClock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);


        assertThatThrownBy(tokenProvider::resolveToken).isInstanceOf(SdkClientException.class).hasMessageContaining("expired");
    }

    //         {
    //             "documentation": "Token missing the expiresAt field",
    //             "currentTime": "2021-12-25T13:30:00Z",
    //             "cachedToken": {
    //                 "accessToken": "cachedtoken"
    //             },
    //             "expectedException": "InvalidToken"
    //         },
    @Test
    public void standardTest_Token_missing_the_expiresAt_field() {
        SsoToken ssoToken = SsoToken.builder().startUrl("https://d-123.awsapps.com/start").accessToken("cachedtoken").build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(ssoToken));
        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              null);


        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expirationTime cannot be null");
    }

    //        {
    //             "documentation": "Token missing the accessToken field",
    //             "currentTime": "2021-12-25T13:30:00Z",
    //             "cachedToken": {
    //                 "expiresAt": "2021-12-25T13:00:00Z"
    //             },
    //             "expectedException": "InvalidToken"
    //         },
    @Test
    public void standardTest_Token_missing_the_accessToken_field() {
        SsoToken ssoToken = SsoToken.builder().expiresAt(Instant.now()).build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(ssoToken));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              null);

        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("token cannot be null");
    }

    @Test
    public void refresh_token_from_service_when_token_outside_expiry_window() {
        SsoToken nearToExpiryToken = getDefaultTokenBuilder()
            .expiresAt(Instant.parse("2021-12-25T13:07:00Z"))
            .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z")).build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(nearToExpiryToken));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().build());

        Clock clock = Clock.fixed(Instant.parse("2021-12-25T13:05:00Z"), ZoneId.of("UTC"));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, clock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);


        SsoToken resolvedToken = (SsoToken) tokenProvider.resolveToken();

        assertThat(resolvedToken.token()).isEqualTo("serviceToken");
        assertThat(resolvedToken.refreshToken()).isEqualTo("refreshedToken");
        assertThat(resolvedToken.expirationTime()).isEqualTo(Instant.parse("2021-12-25T13:27:00Z"));

        // Base properties of tokens are retained.
        assertThat(resolvedToken.clientId()).isEqualTo(nearToExpiryToken.clientId());
        assertThat(resolvedToken.clientSecret()).isEqualTo(nearToExpiryToken.clientSecret());
        assertThat(resolvedToken.region()).isEqualTo(nearToExpiryToken.region());
        assertThat(resolvedToken.startUrl()).isEqualTo(nearToExpiryToken.startUrl());
        assertThat(resolvedToken.registrationExpiresAt()).isEqualTo(nearToExpiryToken.registrationExpiresAt());
    }

    @Test
    public void refresh_token_does_not_fetch_from_service_when_token_inside_expiry_window() {
        SsoToken cachedDiskToken = getDefaultTokenBuilder()
            .expiresAt(Instant.parse("2021-12-25T13:11:00Z")).registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
            .build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(cachedDiskToken));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().build());

        Clock clock = Clock.fixed(Instant.parse("2021-12-25T13:05:00Z"), ZoneId.of("UTC"));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, clock,
                                                              null,
                                                              ssoOidcClient,
                                                              null);



        AwsToken resolvedToken = tokenProvider.resolveToken();

        assertThat(resolvedToken).isEqualTo(cachedDiskToken);
        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
    }

    @Test
    public void token_is_obtained_from_inmemory_when_token_is_within_inmemory_stale_time() {
        Instant futureExpiryDate = Instant.now().plus(Duration.ofDays(1));
        SsoToken cachedDiskToken = getDefaultTokenBuilder()
            .expiresAt(futureExpiryDate).registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z")).build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(cachedDiskToken));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().build());

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              null);

        AwsToken resolvedToken = tokenProvider.resolveToken();

        assertThat(resolvedToken).isEqualTo(cachedDiskToken);
        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
        AwsToken consecutiveToken = tokenProvider.resolveToken();
        assertThat(consecutiveToken).isEqualTo(resolvedToken);
        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, never()).storeToken(any(SsoToken.class));
        verify(mockTokenManager, times(1)).loadToken();
    }


    // Test to make sure cache fetches from Cached values.
    @Test
    public void token_is_obtained_from_refresher_and_then_refresher_cache_if_its_within_stale_time() {
        Instant closeToExpireTime = Instant.now().plus(Duration.ofMinutes(4));
        SsoToken cachedDiskToken = getDefaultTokenBuilder().accessToken("fourMinutesToExpire")
                                                           .expiresAt(closeToExpireTime)
                                                           .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z")).build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(cachedDiskToken));
        Instant eightMinutesFromNow = Instant.now().plus(Duration.ofMinutes(8));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().accessToken(
            "eightMinutesExpiry").expiresIn((int) eightMinutesFromNow.getEpochSecond()).build());

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              Duration.ofMinutes(7));


        AwsToken resolvedToken = tokenProvider.resolveToken();

        // Resolves to fresh token
        assertThat(resolvedToken.token()).isEqualTo("eightMinutesExpiry");
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
        AwsToken consecutiveToken = tokenProvider.resolveToken();
        assertThat(consecutiveToken).isEqualTo(resolvedToken);
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, times(1)).storeToken(any(SsoToken.class));
        verify(mockTokenManager, times(1)).loadToken();

        AwsToken thirdTokenAccess = tokenProvider.resolveToken();
        assertThat(thirdTokenAccess).isEqualTo(resolvedToken);
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, times(1)).storeToken(any(SsoToken.class));
        verify(mockTokenManager, times(1)).loadToken();
    }

    @Test
    public void token_is_retrieved_from_service_when_service_returns_tokens_with_short_expiration() {
        Instant closeToExpireTime = Instant.now().plus(Duration.ofMinutes(4));
        SsoToken cachedDiskToken = getDefaultTokenBuilder().accessToken("fourMinutesToExpire")
                                                           .expiresAt(closeToExpireTime)
                                                           .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
                                                           .build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(cachedDiskToken));
        Instant eightMinutesFromNow = Instant.now().plus(Duration.ofMinutes(8));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().accessToken(
            "eightMinutesExpiry").expiresIn((int) eightMinutesFromNow.getEpochSecond()).build());

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              Duration.ofMinutes(10));


        AwsToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo("eightMinutesExpiry");
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, times(1)).storeToken(any(SsoToken.class));

        AwsToken consecutiveToken = tokenProvider.resolveToken();
        assertThat(consecutiveToken).isEqualTo(resolvedToken);
        verify(ssoOidcClient, times(2)).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, times(2)).storeToken(any(SsoToken.class));
        // Loaded only ones , the second time it picks from cache
        verify(mockTokenManager, times(2)).loadToken();
    }

    @Test
    public void token_is_retrieved_automatically_when_auto_refresh_is_enabled() throws InterruptedException {
        Instant closeToExpireTime = Instant.now().plus(Duration.ofMillis(3));
        SsoToken cachedDiskToken = getDefaultTokenBuilder().accessToken("fourMinutesToExpire")
                                                           .expiresAt(closeToExpireTime)
                                                           .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
                                                           .build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(cachedDiskToken));
        Instant eightMinutesFromNow = Instant.now().plus(Duration.ofMillis(200));
        Instant twelveMinutesFromNow = Instant.now().plus(Duration.ofMillis(12000));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(getDefaultServiceResponse().accessToken("eightMinutesExpiry").expiresIn((int) eightMinutesFromNow.getEpochSecond()).build())
            .thenReturn(getDefaultServiceResponse().accessToken("twelveMinutesExpiry").expiresIn((int) twelveMinutesFromNow.getEpochSecond()).build());


        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start",
                                                              "region",
                                                              mockTokenManager, null,
                                                              null,
                                                              ssoOidcClient,
                                                              Duration.ofMillis(100));


        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, never()).storeToken(any(SsoToken.class));
        verify(mockTokenManager, never()).loadToken();

        // Giving some time for daemon thread to run some refreshes.
        Thread.sleep(1000);

        // 2 calls made , the first call gives 8 minutes expiry time , 2nd call gives 12 minutes expiry token
        verify(ssoOidcClient, atMost(2)).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, atMost(2)).storeToken(any(SsoToken.class));

        // 2 calls made , the first call gives 4 minutes expiry time , 2nd call gives 8 minutes expiry token then onwards cached
        verify(mockTokenManager, atMost(2)).loadToken();

        AwsToken awsToken = tokenProvider.resolveToken();
        assertThat(awsToken.token()).isEqualTo("twelveMinutesExpiry");

        // No disc access or service access since all values accessed from caches , this number is same as above.
        verify(ssoOidcClient, atMost(2)).createToken(any(CreateTokenRequest.class));
        verify(mockTokenManager, atMost(2)).storeToken(any(SsoToken.class));
        verify(mockTokenManager, atMost(2)).loadToken();
        tokenProvider.close();
    }

    @Test
    public void exception_when_client_and_supplier_passed_together(){
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> SsoTokenProvider.builder()
                            .tokenRetriever(
                                () -> getDefaultTokenBuilder().build()).ssoOidcClient(SsoOidcClient.create()).build()).withMessage(
                                    "Cannot provide both SsoOidcClient and a tokenRetriever.");
    }

    private CreateTokenResponse.Builder getDefaultServiceResponse() {
        return CreateTokenResponse.builder().accessToken(
            "serviceToken").expiresIn((int) Instant.parse("2021-12-25T13:27:00Z").getEpochSecond()).refreshToken(
            "refreshedToken");
    }

    private SsoToken.Builder getDefaultTokenBuilder() {
        return SsoToken.builder()
                       .startUrl("https://d-123.awsapps.com/start")
                       .region("us-west-2")
                       .accessToken("cachedtoken")
                       .expiresAt(Instant.parse("2022-12-25T13:30:00Z"))
                       .clientId("clientid")
                       .clientSecret("YSBzZWNyZXQ=")
                       .registrationExpiresAt(Instant.parse("2022-12-25T13:40:00Z"))
                       .refreshToken("cachedrefreshtoken");
    }
}
