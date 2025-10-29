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

package software.amazon.awssdk.services.signin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.services.signin.SigninClient;
import software.amazon.awssdk.services.signin.internal.AccessTokenManager;
import software.amazon.awssdk.services.signin.internal.LoginAccessToken;
import software.amazon.awssdk.services.signin.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.signin.model.CreateOAuth2TokenRequest;
import software.amazon.awssdk.services.signin.model.CreateOAuth2TokenResponse;
import software.amazon.awssdk.services.signin.model.SigninException;

public class LoginCredentialsProviderTest {
    private static final String LOGIN_SESSION_ID = "loginSessionId";

    private AccessTokenManager tokenManager;
    private SigninClient signinClient;
    private LoginCredentialsProvider loginCredentialsProvider;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        signinClient = mock(SigninClient.class);
        tokenManager = OnDiskTokenManager.create(tempDir, LOGIN_SESSION_ID);

        loginCredentialsProvider = LoginCredentialsProvider
            .builder()
            .loginSession(LOGIN_SESSION_ID)
            .signinClient(signinClient)
            .tokenCacheLocation(tempDir)
            .build();
    }

    @Test
    public void missingSigninClient_throwsException() {
        assertThrows(NullPointerException.class, ()-> {
            LoginCredentialsProvider.builder().loginSession(LOGIN_SESSION_ID).build();
        });
    }

    @Test
    public void missingLoginSession_throwsException() {
        assertThrows(IllegalArgumentException.class, ()-> {
            LoginCredentialsProvider.builder().loginSession("").signinClient(signinClient).build();
        });
    }

    @Test
    public void resolveCredentials_whenCredentialsFresh_usesFromDisk() {
        AwsSessionCredentials creds = buildCredentials( Instant.now().plusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);

        AwsCredentials resolveCredentials = loginCredentialsProvider.resolveCredentials();

        verify(signinClient, never()).createOAuth2Token(any(CreateOAuth2TokenRequest.class));

        assertEquals(creds.accessKeyId(), resolveCredentials.accessKeyId());
        assertEquals(creds.secretAccessKey(), resolveCredentials.secretAccessKey());
        assertEquals(creds.accountId(), resolveCredentials.accountId());
        assertEquals(creds.expirationTime(), resolveCredentials.expirationTime());
        assertEquals(BusinessMetricFeatureId.CREDENTIALS_LOGIN.value(), resolveCredentials.providerName().orElse(null));
    }

    @Test
    public void resolveCredentials_whenCredentialsNearExpiration_refreshesAndUpdatesCache() {
        // within the stale time
        AwsSessionCredentials creds = buildCredentials(Instant.now().plusSeconds(10));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);
        when(signinClient.createOAuth2Token(any(CreateOAuth2TokenRequest.class))).thenReturn(
            buildSuccessfulRefreshResponse()
        );
        AwsCredentials resolvedCredentials = loginCredentialsProvider.resolveCredentials();

        ArgumentCaptor<CreateOAuth2TokenRequest> captor =
            ArgumentCaptor.forClass(CreateOAuth2TokenRequest.class);

        // verify the service was called with correct arguments
        verify(signinClient, times(1)).createOAuth2Token(captor.capture());
        assertEquals(token.getClientId(), captor.getValue().tokenInput().clientId());
        assertEquals(token.getRefreshToken(), captor.getValue().tokenInput().refreshToken());
        assertEquals("refresh_token", captor.getValue().tokenInput().grantType());
        // TODO: Assert validity of DPoP header once implemented

        // verify that returned credentials are updated
        verifyResolvedCredentialsAreUpdated(resolvedCredentials);

        // verify that the token is correctly refreshed on disk
        verifyTokenCacheUpdated();
    }



    @Test
    public void resolveCredentials_whenCredentialsExpired_refreshesAndUpdatesCache() {
        // within the stale time
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(600));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);
        when(signinClient.createOAuth2Token(any(CreateOAuth2TokenRequest.class))).thenReturn(
            buildSuccessfulRefreshResponse()
        );
        AwsCredentials resolvedCredentials = loginCredentialsProvider.resolveCredentials();

        ArgumentCaptor<CreateOAuth2TokenRequest> captor =
            ArgumentCaptor.forClass(CreateOAuth2TokenRequest.class);

        // verify the service was called with correct arguments
        verify(signinClient, times(1)).createOAuth2Token(captor.capture());
        assertEquals(token.getClientId(), captor.getValue().tokenInput().clientId());
        assertEquals(token.getRefreshToken(), captor.getValue().tokenInput().refreshToken());
        assertEquals("refresh_token", captor.getValue().tokenInput().grantType());
        // TODO: Assert validity of DPoP header once implemented

        // verify that returned credentials are updated
        verifyResolvedCredentialsAreUpdated(resolvedCredentials);

        // verify that the token is correctly refreshed on disk
        verifyTokenCacheUpdated();
    }

    @Test
    public void resolveCredentials_whenCredentialsExpired_serviceCallFails_raisesException() {
        // expired
        AwsSessionCredentials creds = buildCredentials(Instant.now().minusSeconds(60));
        LoginAccessToken token = buildAccessToken(creds);
        tokenManager.storeToken(token);
        when(signinClient.createOAuth2Token(any(CreateOAuth2TokenRequest.class))).thenThrow(SigninException.class);
        assertThrows(SdkClientException.class, () -> loginCredentialsProvider.resolveCredentials());
    }


    private static void verifyResolvedCredentialsAreUpdated(AwsCredentials resolvedCredentials) {
        assertEquals("new-akid", resolvedCredentials.accessKeyId());
        assertEquals("new-skid", resolvedCredentials.secretAccessKey());
        assertInstanceOf(AwsSessionCredentials.class, resolvedCredentials);
        assertEquals("new-session-token", ((AwsSessionCredentials) resolvedCredentials).sessionToken());

        // assert that the expiration is close to expected, since its being added to the current time it won't be exact.
        Instant expectedExpirationTime = Instant.now().plusSeconds(600);
        Instant resolvedExpirationTime = resolvedCredentials.expirationTime().orElse(Instant.MIN);
        assertTrue(Math.abs(resolvedExpirationTime.toEpochMilli() - expectedExpirationTime.toEpochMilli()) < 1000);
    }

    private void verifyTokenCacheUpdated() {
        LoginAccessToken updatedToken = tokenManager.loadToken()
                                                    .orElseThrow(() -> new RuntimeException("Token not found after refresh"));
        assertEquals("new-akid", updatedToken.getAccessToken().accessKeyId());
        assertEquals("new-skid", updatedToken.getAccessToken().secretAccessKey());
        assertEquals("new-session-token", updatedToken.getAccessToken().sessionToken());
        assertEquals("new-refresh-token", updatedToken.getRefreshToken());
    }

    private static CreateOAuth2TokenResponse buildSuccessfulRefreshResponse() {
        return CreateOAuth2TokenResponse
            .builder()
            .tokenOutput(
                t ->
                    t
                        .expiresIn(600)
                        .refreshToken("new-refresh-token")
                        .accessToken(
                            c ->
                                c
                                    .accessKeyId("new-akid")
                                    .secretAccessKey("new-skid")
                                    .sessionToken("new-session-token"))
            )
            .build();
    }

    private AwsSessionCredentials buildCredentials(Instant expirationTime) {
        return AwsSessionCredentials.builder()
                             .accessKeyId("akid")
                             .secretAccessKey("skid")
                             .sessionToken("sessionToken")
                             .accountId("123456789012")
                             .expirationTime(expirationTime)
                             .build();
    }

    private LoginAccessToken buildAccessToken(AwsSessionCredentials credentials) {
        return LoginAccessToken.builder()
                               .accessToken(credentials)
                               .clientId("client-123")
                               .dpopKey("dpop-key")
                               .refreshToken("refresh-token")
                               .tokenType("aws_sigv4")
                               .identityToken("id-token")
                               .build();
    }


}
