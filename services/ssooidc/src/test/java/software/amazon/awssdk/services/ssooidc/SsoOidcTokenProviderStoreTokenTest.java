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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcToken;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;

class SsoOidcTokenProviderStoreTokenTest {

    @Test
    @SuppressWarnings("unchecked")
    void resolveToken_refreshSucceedsButStoreTokenFails_returnsAndCachesRefreshedToken() {
        TokenManager<SsoOidcToken> tokenManager = mock(TokenManager.class);
        SsoOidcClient ssoOidcClient = mock(SsoOidcClient.class);
        SsoOidcToken diskToken = SsoOidcToken.builder()
                                             .startUrl("https://d-123.awsapps.com/start")
                                             .region("us-west-2")
                                             .accessToken("diskToken")
                                             .expiresAt(Instant.now().plus(Duration.ofMinutes(3)))
                                             .clientId("clientid")
                                             .clientSecret("clientsecret")
                                             .refreshToken("diskRefreshToken")
                                             .build();
        when(tokenManager.loadToken()).thenReturn(Optional.of(diskToken));
        doThrow(SdkClientException.create("Unable to write token to location"))
            .when(tokenManager).storeToken(any(SsoOidcToken.class));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(CreateTokenResponse.builder()
                                           .accessToken("refreshedToken")
                                           .expiresIn(3600)
                                           .refreshToken("refreshedRefreshToken")
                                           .build());

        SsoOidcTokenProvider tokenProvider = SsoOidcTokenProvider.builder(tokenManager)
                                                                 .sessionName("session")
                                                                 .ssoOidcClient(ssoOidcClient)
                                                                 .build();

        SdkToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo("refreshedToken");
        verify(tokenManager, times(1)).storeToken(any(SsoOidcToken.class));

        SdkToken secondResolvedToken = tokenProvider.resolveToken();
        assertThat(secondResolvedToken).isEqualTo(resolvedToken);
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
    }
}
