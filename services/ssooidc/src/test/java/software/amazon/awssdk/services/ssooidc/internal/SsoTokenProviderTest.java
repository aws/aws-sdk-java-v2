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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.core.exception.SdkClientException;

public class SsoTokenProviderTest {
    private TokenManager mockTokenManager;

    @BeforeEach
    public void setup() {
        mockTokenManager = mock(TokenManager.class);
    }

    @Test
    public void resolveToken_usesTokenManager() {
        SsoToken ssoToken = SsoToken.builder()
                                    .accessToken("accesstoken")
                                    .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                                    .build();
        Optional<SsoToken> returnVal = Optional.of(ssoToken);
        when(mockTokenManager.loadToken()).thenReturn(returnVal);

        SsoTokenProvider tokenProvider = new SsoTokenProvider("starturl", mockTokenManager, Clock.systemUTC());
        assertThat(tokenProvider.resolveToken()).isSameAs(ssoToken);
    }

    @Test
    public void resolveToken_cachedValueNotPresent_throws() {
        Optional<SsoToken> returnVal = Optional.empty();
        when(mockTokenManager.loadToken()).thenReturn(returnVal);

        SsoTokenProvider tokenProvider = new SsoTokenProvider("starturl", mockTokenManager, Clock.systemUTC());

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
        SsoToken token = SsoToken.builder()
            .startUrl("https://d-123.awsapps.com/start")
            .region("us-west-2")
            .accessToken("cachedtoken")
            .expiresAt(Instant.parse("2021-12-25T21:30:00Z"))
            .clientId("clientid")
            .clientSecret("YSBzZWNyZXQ=")
            .registrationExpiresAt(Instant.parse("2022-12-25T13:30:00Z"))
            .refreshToken("cachedrefreshtoken")
            .build();

        when(mockTokenManager.loadToken()).thenReturn(Optional.of(token));

        Clock clock = Clock.fixed(Instant.parse("2021-12-25T13:30:00Z"), ZoneId.of("UTC"));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("https://d-123.awsapps.com/start", mockTokenManager, clock);

        AwsToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo(token.token());
        assertThat(resolvedToken.expirationTime()).isEqualTo(token.expirationTime());
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

        SsoTokenProvider tokenProvider = new SsoTokenProvider("starturl", mockTokenManager, testClock);

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
        Instant expiresAt = Instant.parse("2021-12-25T13:00:00Z");
        SsoToken ssoToken = SsoToken.builder().accessToken("cachedtoken").expiresAt(expiresAt).build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(ssoToken));

        Clock testClock = Clock.fixed(Instant.parse("2021-12-25T13:30:00Z"), ZoneId.of("UTC"));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("starturl", mockTokenManager, testClock);

        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("expired");
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
        SsoToken ssoToken = SsoToken.builder().accessToken("cachedtoken").build();
        when(mockTokenManager.loadToken()).thenReturn(Optional.of(ssoToken));

        SsoTokenProvider tokenProvider = new SsoTokenProvider("starturl", mockTokenManager, Clock.systemUTC());

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

        SsoTokenProvider tokenProvider = new SsoTokenProvider("starturl", mockTokenManager, Clock.systemUTC());

        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("token cannot be null");
    }
}
