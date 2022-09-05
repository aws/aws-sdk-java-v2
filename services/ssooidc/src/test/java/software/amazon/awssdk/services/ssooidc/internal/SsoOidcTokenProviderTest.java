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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.awscore.internal.token.TokenManager;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;
import software.amazon.awssdk.services.ssooidc.SsoOidcTokenProvider;
import software.amazon.awssdk.services.ssooidc.internal.common.SsoOidcTokenRefreshTestBase;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;
import software.amazon.awssdk.services.ssooidc.model.UnauthorizedClientException;
import software.amazon.awssdk.utils.BinaryUtils;

public class SsoOidcTokenProviderTest {
    public static final String START_URL = "https://d-123.awsapps.com/start";
    public static final String REGION = "region";
    private TokenManager<SsoOidcToken> mockTokenManager;
    private SsoOidcClient ssoOidcClient;

    private FileSystem testFs;
    private Path cache;

    public static String deriveCacheKey(String startUrl) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("sha1");
            sha1.update(startUrl.getBytes(StandardCharsets.UTF_8));
            return BinaryUtils.toHex(sha1.digest()).toLowerCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException e) {
            throw SdkClientException.create("Unable to derive cache key", e);
        }
    }

    @BeforeEach
    public void setup() throws IOException {

        File cacheDirectory = SsoOidcTokenRefreshTestBase.DEFAULT_TOKEN_LOCATION.toFile();
        if(! cacheDirectory.exists()){
            cacheDirectory.mkdirs();
        }
        Path file = Paths.get(cacheDirectory.getPath()+ deriveCacheKey(START_URL) + ".json");
        if(file.toFile().exists()){
            file.toFile().delete();
        }
        Files.createDirectories(file.getParent());
        Files.createFile(file);
        mockTokenManager = OnDiskTokenManager.create(START_URL);
        ssoOidcClient = mock(SsoOidcClient.class);
    }

    @AfterEach
    public void teardown() throws IOException {
        Path path = Paths.get(userHomeDirectory(), ".aws", "sso", "cache");
        Path resolve = path.resolve(deriveCacheKey(START_URL) + ".json");
        Files.deleteIfExists(resolve);
    }

    @Test
    public void resolveToken_usesTokenManager() {
        SsoOidcToken ssoOidcToken = SsoOidcToken.builder()
                                                .accessToken("accesstoken")
                                                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                                                .build();
        mockTokenManager.storeToken(ssoOidcToken);

        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();
        assertThat(tokenProvider.resolveToken()).isEqualTo(ssoOidcToken);
    }

    private SsoOidcTokenProvider.Builder getDefaultSsoOidcTokenProviderBuilder() {
        return SsoOidcTokenProvider.builder().sessionName(START_URL).ssoOidcClient(ssoOidcClient);
    }

    @Test
    public void resolveToken_cachedValueNotPresent_throws() {

        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();

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
        SsoOidcToken token = getDefaultTokenBuilder()
            .expiresAt(Instant.now().plusSeconds(10000)).registrationExpiresAt(Instant.now().plusSeconds(90000))
            .build();

        mockTokenManager.storeToken(token);

        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();
        SdkToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo(token.token());
        assertThat(resolvedToken.expirationTime()).isEqualTo(token.expirationTime());

    }

    @Test
    public void refresh_returns_cached_token_when_service_calls_fails() {
        SsoOidcToken nearToExpiryToken = getDefaultTokenBuilder()
            .expiresAt(Instant.now().plusSeconds(5000)).registrationExpiresAt(Instant.now().plusSeconds(10000))
            .build();

        mockTokenManager.storeToken(nearToExpiryToken);
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenThrow(UnauthorizedClientException.class);


        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();

        SdkToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).isEqualTo(nearToExpiryToken.token());
        assertThat(resolvedToken.expirationTime()).isEqualTo(nearToExpiryToken.expirationTime());
    }

    @Test
    public void refresh_fails_when_supplier_fails_due_to_Non_service_issues() {
        SsoOidcToken nearToExpiryToken = getDefaultTokenBuilder()
            .expiresAt(Instant.now().minusSeconds(2)).registrationExpiresAt(Instant.now().minusSeconds(2))
            .build();
        mockTokenManager.storeToken(nearToExpiryToken);
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenThrow(SdkClientException.class);
        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();

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
        Instant expiresAt = Instant.now().plusSeconds(3600);
        SsoOidcToken ssoOidcToken = SsoOidcToken.builder().accessToken("cachedtoken").expiresAt(expiresAt).build();
        mockTokenManager.storeToken(ssoOidcToken);
        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();
        assertThat(tokenProvider.resolveToken()).isEqualTo(ssoOidcToken);
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
        String startUrl = START_URL;
        Instant expiresAt = Instant.parse("2021-12-25T13:00:00Z");
        SsoOidcToken ssoOidcToken =
            SsoOidcToken.builder().startUrl(startUrl).clientId("clientId").clientSecret("clientSecret")
                        .accessToken("cachedtoken").expiresAt(expiresAt).build();
        mockTokenManager.storeToken(ssoOidcToken);
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(CreateTokenResponse.builder().accessToken("cachedtoken")
                                           .expiresIn(-1)
                                           .build());


        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();


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
        SsoOidcToken ssoOidcToken = SsoOidcToken.builder()
                                                .startUrl(START_URL)
                                                .accessToken("cachedtoken").clientId("client").clientSecret("secret")
                                                .expiresAt(Instant.parse("2021-12-25T13:00:00Z"))
                                                .build();
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(CreateTokenResponse.builder().accessToken("cachedtoken")
                                           .build());
        mockTokenManager.storeToken(ssoOidcToken);
        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();
        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expiresIn must not be null.");
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
        SsoOidcToken ssoOidcToken = SsoOidcToken.builder()
                                                .startUrl(START_URL)
                                                .accessToken("cachedtoken").clientId("client").clientSecret("secret")
                                                .expiresAt(Instant.parse("2021-12-25T13:00:00Z"))
                                                .build();

        mockTokenManager.storeToken(ssoOidcToken);
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(CreateTokenResponse.builder().expiresIn(3600).build());
        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();

        assertThatThrownBy(tokenProvider::resolveToken)
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("accessToken must not be null.");
    }

    @Test
    public void refresh_token_from_service_when_token_outside_expiry_window() {
        SsoOidcToken nearToExpiryToken = getDefaultTokenBuilder()
            .expiresAt(Instant.now().plusSeconds(59))
            .registrationExpiresAt(Instant.now().plusSeconds(59)).build();

        mockTokenManager.storeToken(nearToExpiryToken);

        int extendedExpiryTimeInSeconds = 120;
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().expiresIn(extendedExpiryTimeInSeconds).build());


        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();


        SsoOidcToken resolvedToken = (SsoOidcToken) tokenProvider.resolveToken();

        assertThat(resolvedToken.token()).isEqualTo("serviceToken");
        assertThat(resolvedToken.refreshToken()).isEqualTo("refreshedToken");
        Duration extendedExpiryDate = Duration.between(resolvedToken.expirationTime().get(), Instant.now());
        // Base properties of tokens are retained.
        assertThat(Duration.ofSeconds(extendedExpiryTimeInSeconds)).isGreaterThanOrEqualTo(extendedExpiryDate);
        assertThat(resolvedToken.clientId()).isEqualTo(nearToExpiryToken.clientId());
        assertThat(resolvedToken.clientSecret()).isEqualTo(nearToExpiryToken.clientSecret());
        assertThat(resolvedToken.region()).isEqualTo(nearToExpiryToken.region());
        assertThat(resolvedToken.startUrl()).isEqualTo(nearToExpiryToken.startUrl());
        assertThat(resolvedToken.registrationExpiresAt()).isEqualTo(nearToExpiryToken.registrationExpiresAt());
    }

    @Test
    public void refresh_token_does_not_fetch_from_service_when_token_inside_expiry_window() {
        SsoOidcToken cachedDiskToken = getDefaultTokenBuilder()
            .expiresAt(Instant.now().plusSeconds(120)).registrationExpiresAt(Instant.now().plusSeconds(120))
            .build();
        mockTokenManager.storeToken(cachedDiskToken);
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().build());
        SsoOidcTokenProvider tokenProvider =
            getDefaultSsoOidcTokenProviderBuilder().prefetchTime(Duration.ofSeconds(90)).build();
        SdkToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken).isEqualTo(cachedDiskToken);
        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
    }

    @Test
    public void token_is_obtained_from_inmemory_when_token_is_within_inmemory_stale_time() {
        Instant futureExpiryDate = Instant.now().plus(Duration.ofDays(1));
        SsoOidcToken cachedDiskToken = getDefaultTokenBuilder()
            .expiresAt(futureExpiryDate).registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z")).build();

        mockTokenManager.storeToken(cachedDiskToken);
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().build());

        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().build();

        SdkToken resolvedToken = tokenProvider.resolveToken();

        assertThat(resolvedToken).isEqualTo(cachedDiskToken);
        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
        SdkToken consecutiveToken = tokenProvider.resolveToken();
        assertThat(consecutiveToken).isEqualTo(resolvedToken);
        verify(ssoOidcClient, never()).createToken(any(CreateTokenRequest.class));
    }

    // Test to make sure cache fetches from Cached values.
    @Test
    public void token_is_obtained_from_refresher_and_then_refresher_cache_if_its_within_stale_time() {
        Instant closeToExpireTime = Instant.now().plus(Duration.ofMinutes(4));
        SsoOidcToken cachedDiskToken = getDefaultTokenBuilder().accessToken("fourMinutesToExpire")
                                                               .expiresAt(closeToExpireTime)
                                                               .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z")).build();

        mockTokenManager.storeToken(cachedDiskToken);
        Instant eightMinutesFromNow = Instant.now().plus(Duration.ofMinutes(8));
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(getDefaultServiceResponse().accessToken(
            "eightMinutesExpiry").expiresIn((int) eightMinutesFromNow.getEpochSecond()).build());

        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().staleTime(Duration.ofMinutes(7)).build();


        SdkToken resolvedToken = tokenProvider.resolveToken();

        // Resolves to fresh token
        assertThat(resolvedToken.token()).isEqualTo("eightMinutesExpiry");
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
        SdkToken consecutiveToken = tokenProvider.resolveToken();
        assertThat(consecutiveToken).isEqualTo(resolvedToken);
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));

        SdkToken thirdTokenAccess = tokenProvider.resolveToken();
        assertThat(thirdTokenAccess).isEqualTo(resolvedToken);
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
    }

    @Test
    public void token_is_retrieved_from_service_when_service_returns_tokens_with_short_expiration() {
        Instant closeToExpireTime = Instant.now().plus(Duration.ofSeconds(4));
        SsoOidcToken cachedDiskToken = getDefaultTokenBuilder().accessToken("fourMinutesToExpire")
                                                               .expiresAt(closeToExpireTime)
                                                               .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
                                                               .build();
        mockTokenManager.storeToken(cachedDiskToken);
        CreateTokenResponse eightSecondExpiryToken = getDefaultServiceResponse().accessToken(
            "eightSecondExpiryToken").expiresIn(8).build();
        CreateTokenResponse eightySecondExpiryToken = getDefaultServiceResponse().accessToken(
            "eightySecondExpiryToken").expiresIn(80).build();
        when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenReturn(eightSecondExpiryToken).thenReturn(eightySecondExpiryToken);

        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder().staleTime(Duration.ofSeconds(10)).build();


        SdkToken resolvedToken = tokenProvider.resolveToken();
        assertThat(resolvedToken.token()).contains("eightSecondExpiryToken");
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));

        SdkToken consecutiveToken = tokenProvider.resolveToken();
        assertThat(consecutiveToken.token()).contains("eightySecondExpiryToken");
        verify(ssoOidcClient, times(2)).createToken(any(CreateTokenRequest.class));

    }

    @Test
    public void token_is_retrieved_automatically_when_prefetch_time_is_set() throws InterruptedException {
        Instant closeToExpireTime = Instant.now().plus(Duration.ofMillis(3));

        SsoOidcToken cachedDiskToken = getDefaultTokenBuilder().accessToken("closeToExpire")
                                                               .expiresAt(closeToExpireTime)
                                                               .registrationExpiresAt(Instant.parse("2022-12-25T11:30:00Z"))
                                                               .build();

        mockTokenManager.storeToken(cachedDiskToken);

        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(getDefaultServiceResponse().accessToken("tokenGreaterThanStaleButLessThanPrefetch").expiresIn(1)
                                                   .build())
            .thenReturn(getDefaultServiceResponse().accessToken("tokenVeryHighExpiry").expiresIn(20000).build());


        SsoOidcTokenProvider tokenProvider = getDefaultSsoOidcTokenProviderBuilder()
            .asyncTokenUpdateEnabled(true)
            .staleTime(Duration.ofSeconds(50))
            .prefetchTime(Duration.ofSeconds(300))
            .build();

        Thread.sleep(100);
        SdkToken sdkToken = tokenProvider.resolveToken();
        assertThat(sdkToken.token()).isEqualTo("tokenGreaterThanStaleButLessThanPrefetch");

        Thread.sleep(1000);
        // Sleep to make sure Async prefetch thread gets picked up and it calls createToken to get new token.
        verify(ssoOidcClient, times(1)).createToken(any(CreateTokenRequest.class));
        SdkToken highExpiryDateToken = tokenProvider.resolveToken();
        Thread.sleep(1000);
        assertThat(highExpiryDateToken.token()).isEqualTo("tokenVeryHighExpiry");
    }

    private CreateTokenResponse someOne(CreateTokenResponse.Builder builder, String tokenGreaterThanStaleButLessThanPrefetch,
                                        int i) {
        return builder.accessToken(tokenGreaterThanStaleButLessThanPrefetch).expiresIn(i).build();
    }

    @Test
    public void tokenProvider_throws_exception_if_client_is_null() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () -> SsoOidcTokenProvider.builder().sessionName(START_URL).build()).withMessage("ssoOidcClient must not be null.");
    }

    @Test
    public void tokenProvider_throws_exception_if_start_url_is_null() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () -> SsoOidcTokenProvider.builder().ssoOidcClient(ssoOidcClient).build()).withMessage("sessionName must not be "
                                                                                                   + "null.");
    }

    private CreateTokenResponse.Builder getDefaultServiceResponse() {
        return CreateTokenResponse.builder().accessToken(
            "serviceToken").expiresIn(3600).refreshToken(
            "refreshedToken");
    }

    private SsoOidcToken.Builder getDefaultTokenBuilder() {
        return SsoOidcToken.builder()
                           .startUrl(START_URL)
                           .region("us-west-2")
                           .accessToken("cachedtoken")
                           .expiresAt(Instant.parse("2022-12-25T13:30:00Z"))
                           .clientId("clientid")
                           .clientSecret("YSBzZWNyZXQ=")
                           .registrationExpiresAt(Instant.parse("2022-12-25T13:40:00Z"))
                           .refreshToken("cachedrefreshtoken");
    }
}
