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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.token.AwsToken;
import software.amazon.awssdk.auth.token.AwsTokenProvider;
import software.amazon.awssdk.auth.token.SsoOidcTokenProviderFactoryProperties;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.services.ssooidc.SsoOidcClient;
import software.amazon.awssdk.services.ssooidc.SsoOidcTokenProvider;
import software.amazon.awssdk.services.ssooidc.SsoOidcTokenProviderFactory;
import software.amazon.awssdk.services.ssooidc.internal.OnDiskTokenManager;
import software.amazon.awssdk.services.ssooidc.internal.SsoOidcToken;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenRequest;
import software.amazon.awssdk.services.ssooidc.model.CreateTokenResponse;
import software.amazon.awssdk.services.ssooidc.model.InvalidRequestException;
import software.amazon.awssdk.utils.StringInputStream;

public abstract class SsoOidcTokenRefreshTestBase {

    protected static final String REGION = "us-east-1";
    SsoOidcTokenProviderFactoryProperties factoryProperties;
    protected boolean shouldMockServiceClient;
    protected SsoOidcClient ssoOidcClient;
    protected String testStartUrl;
    protected String baseTokenResourceFile;

    @BeforeEach
    public void setUp() throws IOException {
        initializeClient();
        initializeProfileProperties();
    }

    protected abstract void initializeClient() ;

    protected void initializeProfileProperties(){

        String profileContent = "[profile sso-refresh]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url="+testStartUrl+"\n";

        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        Optional<Profile> profile = profiles.profile("sso-refresh");
        factoryProperties = SsoOidcTokenProviderFactoryProperties.builder()
                                                                 .startUrl(profile.get().properties().get(ProfileProperty.SSO_START_URL))
                                                                 .region(profile.get().properties().get(ProfileProperty.SSO_REGION))
                                                                 .build();

    }

    @Test
    public void tokenReadFromCacheLocation_when_tokenIsWithinExpiry() throws IOException {
        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testStartUrl);
        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile)
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        AwsTokenProvider awsTokenProvider = defaultTokenProvider();
        AwsToken resolvedToken = awsTokenProvider.resolveToken();
        assertThat(resolvedToken).isEqualTo(tokenFromJsonFile);
    }

    @Test
    public void tokenReadFromService_And_DiscCacheUpdate_whenTokenInCacheIsExpired() throws IOException {

        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testStartUrl);
        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        AwsTokenProvider awsTokenProvider = defaultTokenProvider();
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
    public void tokenProvider_with_customStaleTime_refreshes_token() throws IOException {

        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testStartUrl);
        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        AwsTokenProvider awsTokenProvider = tokenProviderBuilderWithClient().staleDuration(Duration.ofMinutes(70)).region(REGION).startUrl(testStartUrl).build();

        // Resolve first Time
        SsoOidcToken firstResolvedToken = (SsoOidcToken) awsTokenProvider.resolveToken();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(tokenFromJsonFile, firstResolvedToken);
        Optional<SsoOidcToken> loadedFromDiscAfterSecondRefresh = onDiskTokenManager.loadToken();
        assertThat(loadedFromDiscAfterSecondRefresh).hasValue(firstResolvedToken);

        // Resolve second time
        SsoOidcToken secondResolvedToken = (SsoOidcToken) awsTokenProvider.resolveToken();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(firstResolvedToken, secondResolvedToken);
        loadedFromDiscAfterSecondRefresh = onDiskTokenManager.loadToken();
        assertThat(loadedFromDiscAfterSecondRefresh).hasValue(secondResolvedToken);
    }

    @Test
    public void autoRefresh_for_regular_intervals() throws IOException, InterruptedException {
        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testStartUrl);
        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile).build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        SsoOidcTokenProvider awsTokenProvider = tokenProviderBuilderWithClient().staleDuration(Duration.ofMinutes(5)).region(REGION)
                                                                                .startUrl(testStartUrl).autoRefreshDuration(Duration.ofMillis(100)).build();

        // Make sure that tokens are not refreshed before the autoRefreshDuration time
        Thread.sleep(10);
        SsoOidcToken refreshedTokenInDiscBeforeRefresh = onDiskTokenManager.loadToken().get();
        assertThat(refreshedTokenInDiscBeforeRefresh).isEqualTo(tokenFromJsonFile);

        // Make sure that tokens are  refreshed from service after the autoRefreshDuration time
        Thread.sleep(1000);
        SsoOidcToken refreshedTokenInDisc = onDiskTokenManager.loadToken().get();
        assert_oldCachedToken_isNoSameAs_NewResolvedToken(tokenFromJsonFile, refreshedTokenInDisc);

        // Make sure that tokens are refreshed from disc for consecutive refresh
        Thread.sleep(500);
        awsTokenProvider.close();
        SsoOidcToken refreshedTokenAfterAutoRefresh = onDiskTokenManager.loadToken().get();
        assertThat(refreshedTokenAfterAutoRefresh).isEqualTo(refreshedTokenInDisc);
    }

    @Test
    public void tokenNotRefreshed_when_serviceRequestFailure() throws IOException {
        // Creating a token and saving to disc
        OnDiskTokenManager onDiskTokenManager = OnDiskTokenManager.create(testStartUrl);

        if (shouldMockServiceClient) {
            when(ssoOidcClient.createToken(any(CreateTokenRequest.class))).thenThrow(InvalidRequestException.builder().build());
        }

        SsoOidcToken tokenFromJsonFile = getTokenFromJsonFile(baseTokenResourceFile)
            .clientId("Incorrect Client Id").build();
        onDiskTokenManager.storeToken(tokenFromJsonFile);

        //Creating default token provider
        AwsTokenProvider awsTokenProvider = defaultTokenProvider();
        assertThatExceptionOfType(SdkClientException.class).isThrownBy(() -> awsTokenProvider.resolveToken()).withMessage(
            "Token is expired");

        // Cached token is same as before
        assertThat(onDiskTokenManager.loadToken().get()).isEqualTo(tokenFromJsonFile);
    }

    private AwsTokenProvider defaultTokenProvider() {

        if (shouldMockServiceClient) {
            return tokenProviderBuilderWithClient().startUrl(testStartUrl).region("us-region-1")
                                                   .ssoOidcClient(this.ssoOidcClient).build();
        }
        return new SsoOidcTokenProviderFactory().create(factoryProperties);
    }

    private SsoOidcTokenProvider.Builder tokenProviderBuilderWithClient() {
        return SsoOidcTokenProvider
            .builder().ssoOidcClient(shouldMockServiceClient ? this.ssoOidcClient : null);
    }

    private void assert_oldCachedToken_isNoSameAs_NewResolvedToken(SsoOidcToken firstResolvedToken,
                                                                   SsoOidcToken secondResolvedToken) {
        assertThat(secondResolvedToken).isNotEqualTo(firstResolvedToken);
        assertThat(secondResolvedToken.expirationTime()).isAfter(firstResolvedToken.expirationTime());
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
            throw new IllegalStateException("Could not load resource file " +fileName ,e);
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