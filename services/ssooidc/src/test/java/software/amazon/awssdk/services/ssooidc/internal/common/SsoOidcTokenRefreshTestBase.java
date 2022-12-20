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

package software.amazon.awssdk.services.ssooidc.internal.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.ssooidc.internal.SsoOidcTokenProviderTest.deriveCacheKey;
import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.internal.ProfileTokenProviderLoader;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;
import software.amazon.awssdk.services.ssooidc.SsoOidcTokenProvider;
import software.amazon.awssdk.services.ssooidc.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcToken;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;
import software.amazon.awssdk.services.ssooidc.model.InvalidRequestException;
import software.amazon.awssdk.utils.StringInputStream;

public abstract class SsoOidcTokenRefreshTestBase {

    protected static final String REGION = "us-east-1";
    protected boolean shouldMockServiceClient;
    protected SsoOidcClient ssoOidcClient;
    protected String testStartUrl;
    protected String testSessionName = "sso-prod";
    protected String baseTokenResourceFile;
    protected ProfileTokenProviderLoader profileTokenProviderLoader;

    public static final Path DEFAULT_TOKEN_LOCATION = Paths.get(userHomeDirectory(), ".aws", "sso", "cache");

    @BeforeEach
    public void setUp() throws IOException {
        initializeClient();
        initializeProfileProperties();

        File cacheDirectory = DEFAULT_TOKEN_LOCATION.toFile();
        if(! cacheDirectory.exists()){
            cacheDirectory.mkdirs();
        }
        Path file = DEFAULT_TOKEN_LOCATION.resolve(deriveCacheKey(this.testStartUrl)).getFileName();
        try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }catch (Exception e){

        }
    }

    @AfterAll
    public static void cleanUp(){
        File cacheDirectory = DEFAULT_TOKEN_LOCATION.toFile();
        if(cacheDirectory.exists()){
            cacheDirectory.delete();
        }
    }

    protected abstract void initializeClient();

    protected void initializeProfileProperties() {
        String profileContent = "[profile sso-refresh]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url=" + testStartUrl + "\n";

        ProfileFile profile = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        profileTokenProviderLoader = new ProfileTokenProviderLoader(() -> profile, "sso-refresh");
    }

    @Test
    public void tokenReadFromCacheLocation_when_tokenIsWithinExpiry() throws IOException {
        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testSessionName);
        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile)
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        SdkTokenProvider awsTokenProvider = defaultTokenProvider();
        SdkToken resolvedToken = awsTokenProvider.resolveToken();
        assertThat(resolvedToken).isEqualTo(tokenFromJsonFile);
    }

    @Test
    public void tokenReadFromService_And_DiscCacheUpdate_whenTokenInCacheIsExpired() {
        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testSessionName);
        SsoOidcToken tokenFromJsonFile =
            getTokenFromJsonFile(baseTokenResourceFile).expiresAt(Instant.now().minusSeconds(10)).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        SdkTokenProvider awsTokenProvider = defaultTokenProvider();

        SsoOidcToken firstResolvedToken = (SsoOidcToken) awsTokenProvider.resolveToken();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(tokenFromJsonFile, firstResolvedToken);
        Optional<SsoOidcToken> loadedFromDiscAfterSecondRefresh = onDiskTokenManager.loadToken();
        assertThat(loadedFromDiscAfterSecondRefresh).hasValue(firstResolvedToken);
        // Token taken from Cache
        SsoOidcToken secondResolvedToken = (SsoOidcToken) awsTokenProvider.resolveToken();
        assertThat(firstResolvedToken).isEqualTo(secondResolvedToken);
        assertThat(secondResolvedToken).isNotEqualTo(tokenFromJsonFile);
        assertThat(onDiskTokenManager.loadToken()).hasValue(secondResolvedToken);
    }

    @Test
    public void tokenProvider_with_customStaleTime_refreshes_token() throws IOException, InterruptedException {

        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testSessionName);
        SsoOidcToken tokenFromJsonFile =
            getTokenFromJsonFile(baseTokenResourceFile).expiresAt(Instant.now().minusSeconds(10)).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        SdkTokenProvider awsTokenProvider =
            tokenProviderBuilderWithClient().staleTime(Duration.ofMinutes(70)).sessionName(testSessionName).build();

        // Resolve first Time
        SsoOidcToken firstResolvedToken = (SsoOidcToken) awsTokenProvider.resolveToken();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(tokenFromJsonFile, firstResolvedToken);
        Optional<SsoOidcToken> loadedFromDiscAfterSecondRefresh = onDiskTokenManager.loadToken();
        assertThat(loadedFromDiscAfterSecondRefresh).hasValue(firstResolvedToken);

        // Resolve second time
        Thread.sleep(1000);
        SsoOidcToken secondResolvedToken = (SsoOidcToken) awsTokenProvider.resolveToken();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(firstResolvedToken, secondResolvedToken);
        loadedFromDiscAfterSecondRefresh = onDiskTokenManager.loadToken();
        assertThat(loadedFromDiscAfterSecondRefresh).hasValue(secondResolvedToken);
    }

    @Test
    public void asyncCredentialUpdateEnabled_prefetch_when_prefetch_time_expired() throws InterruptedException, IOException {

        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testSessionName);
        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        SsoOidcTokenProvider awsTokenProvider =
            tokenProviderBuilderWithClient().staleTime(Duration.ofMinutes(5)).asyncTokenUpdateEnabled(true)
                .prefetchTime(Duration.ofMinutes(10))
                                            .sessionName(testSessionName).prefetchTime(Duration.ofMillis(100)).build();
        // Make sure that tokens are  refreshed from service after the autoRefreshDuration time
        awsTokenProvider.resolveToken();
        SsoOidcToken refreshedTokenInDisc = onDiskTokenManager.loadToken().get();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(tokenFromJsonFile, refreshedTokenInDisc);

        // Make sure that tokens are refreshed from disc for consecutive refresh
        awsTokenProvider.resolveToken();
        Thread.sleep(100);
        awsTokenProvider.close();
        SsoOidcToken refreshedTokenAfterPrefetch = onDiskTokenManager.loadToken().get();
        assertThat(refreshedTokenAfterPrefetch).isEqualTo(refreshedTokenInDisc);
    }

    @Test
    public void tokenNotRefreshed_when_serviceRequestFailure() throws IOException {
        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testSessionName);

        if (shouldMockServiceClient) {
            when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenThrow(InvalidRequestException.builder().build());
        }

        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile)
            .clientId("Incorrect Client Id").expiresAt(Instant.now().minusSeconds(10)).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        SdkTokenProvider awsTokenProvider = defaultTokenProvider();
        assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> awsTokenProvider.resolveToken()).withMessage(
            "Token is expired");

        // Cached token is same as before
        assertThat(onDiskTokenManager.loadToken().get()).isEqualTo(tokenFromJsonFile);
    }

    private SdkTokenProvider defaultTokenProvider() {

        if (shouldMockServiceClient) {
            return tokenProviderBuilderWithClient().sessionName(testSessionName)
                                                   .ssoOidcClient(this.ssoOidcClient).build();
        }
        //     return profileTokenProviderLoader.tokenProvider().get();
        // TODO : remove the custom client before GA.
        return tokenProviderBuilderWithClient().sessionName(testSessionName)
                                               .ssoOidcClient(this.ssoOidcClient).build();
    }

    private SsoOidcTokenProvider.Builder tokenProviderBuilderWithClient() {
        return SsoOidcTokenProvider.builder().ssoOidcClient(ssoOidcClient);
    }

    private void assert_oldCachedToken_isNoSameAs_NewResolvedToken(SsoOidcToken firstResolvedToken,
                                                                   SsoOidcToken secondResolvedToken) {
        assertThat(secondResolvedToken).isNotEqualTo(firstResolvedToken);
        assertThat(secondResolvedToken.expirationTime().get()).isAfter(firstResolvedToken.expirationTime().get());
        assertThat(secondResolvedToken.token()).isNotEqualTo(firstResolvedToken.token());
        assertThat(secondResolvedToken.refreshToken()).isEqualTo(firstResolvedToken.refreshToken());
        assertThat(secondResolvedToken.startUrl()).isEqualTo(firstResolvedToken.startUrl());
        assertThat(secondResolvedToken.clientId()).isEqualTo(firstResolvedToken.clientId());
        assertThat(secondResolvedToken.clientSecret()).isEqualTo(firstResolvedToken.clientSecret());
    }

    protected SsoOidcClient mockSsoOidcClient() {
        ssoOidcClient = mock(SsoOidcClient.class);

        SsoOidcToken baseToken = getTokenFromJsonFile(baseTokenResourceFile).build();

        Supplier<CreateTokenResponse> responseSupplier = () -> CreateTokenResponse.builder().expiresIn(3600)
                                                                                  .accessToken(RandomStringUtils.randomAscii(20))
                                                                                  .refreshToken(baseToken.refreshToken())
                                                                                  .build();

        when(ssoOidcClient.createToken(any(CreateTokenRequest.class)))
            .thenReturn(responseSupplier.get()).thenReturn(responseSupplier.get()).thenReturn(responseSupplier.get());

        return ssoOidcClient;
    }

    private SsoOidcToken.Builder getTokenFromJsonFile(String fileName) {

        String content = null;
        try {
            content = new String(Files.readAllBytes(Paths.get(fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not load resource file " + fileName, e);
        }
        JsonNode jsonNode = JsonNode.parser().parse(content);
        return SsoOidcToken.builder()
                           .accessToken(jsonNode.field("accessToken").get().asString())
                           .refreshToken(jsonNode.field("refreshToken").get().asString())
                           .clientId(jsonNode.field("clientId").get().asString())
                           .clientSecret(jsonNode.field("clientSecret").get().asString())
                           .region(jsonNode.field("region").get().asString())
                           .startUrl(jsonNode.field("startUrl").get().asString())
                           .expiresAt(Instant.parse(jsonNode.field("expiresAt").get().asString()));
    }
}