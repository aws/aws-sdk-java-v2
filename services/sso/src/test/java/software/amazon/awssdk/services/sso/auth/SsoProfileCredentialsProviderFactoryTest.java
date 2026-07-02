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

package software.amazon.awssdk.services.sso.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sso.SsoClient;
import software.amazon.awssdk.services.sso.auth.ExpiredTokenException;
import software.amazon.awssdk.services.sso.internal.SsoAccessToken;
import software.amazon.awssdk.services.sso.internal.SsoAccessTokenProvider;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsRequest;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsResponse;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.services.sso.model.RoleCredentials;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * Validate the code path of creating the {@link SsoCredentialsProvider} with {@link SsoProfileCredentialsProviderFactory}.
 */

@ExtendWith(MockitoExtension.class)
public class SsoProfileCredentialsProviderFactoryTest {

    @Mock SdkTokenProvider sdkTokenProvider;

    @Test
    public void createSsoCredentialsProviderWithFactorySucceed() throws IOException {
        String startUrl = "https//d-abc123.awsapps.com/start";
        String generatedTokenFileName = "6a888bdb653a4ba345dd68f21b896ec2e218c6f4.json";


        ProfileFile profileFile = configFile("[profile foo]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_region=region\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");


        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \""+ startUrl +"\"\n" +
                           "}";
        Path cachedTokenFilePath = prepareTestCachedTokenFile(tokenFile, generatedTokenFileName);
        SsoAccessTokenProvider tokenProvider = new SsoAccessTokenProvider(
            cachedTokenFilePath);

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThat(factory.create(ProfileProviderCredentialsContext.builder()
                                                                   .profile(profileFile.profile("foo").get())
                                                                   .profileFile(profileFile)
                                                                   .build(),
                                  tokenProvider))
            .isInstanceOf(AwsCredentialsProvider.class);
    }

    private Path prepareTestCachedTokenFile(String tokenFileContent, String generatedTokenFileName) throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path fileDirectory = fs.getPath("./foo");

        Files.createDirectory(fileDirectory);
        Path cachedTokenFilePath = fileDirectory.resolve(generatedTokenFileName);
        Files.write(cachedTokenFilePath, ImmutableList.of(tokenFileContent), StandardCharsets.UTF_8);

        return cachedTokenFilePath;
    }

    private static ProfileFile configFile(String configFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(configFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    @ParameterizedTest
    @MethodSource("ssoErrorValues")
    void validateSsoFactoryErrorWithIncorrectProfiles(ProfileFile profiles, String expectedValue) {

        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
            assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                     .profileFile(profiles)
                                                                                     .profile(profile)
                                                                                     .build())).hasMessageContaining(expectedValue);
        });
    }

    private static Stream<Arguments> ssoErrorValues() {
        // Session title is missing
        return Stream.of(
            Arguments.of(configFile("[profile test]\n" +
                                    "sso_account_id=accountId\n" +
                                    "sso_role_name=roleName\n" +
                                    "sso_session=foo\n" +
                                    "[sso-session bar]\n" +
                                    "sso_start_url=https//d-abc123.awsapps.com/start\n" +
                                    "sso_region=region")
                , "Sso-session section not found with sso-session title foo."),
            // No sso_region in sso_session
            Arguments.of(configFile("[profile test]\n" +
                                    "sso_account_id=accountId\n" +
                                    "sso_role_name=roleName\n" +
                                    "sso_session=foo\n" +
                                    "[sso-session foo]\n" +
                                    "sso_start_url=https//d-abc123.awsapps.com/start")
                , "'sso_region' must be set to use role-based credential loading in the 'foo' profile."),
            // sso_start_url mismatch in sso-session and profile
            Arguments.of(configFile("[profile test]\n" +
                                    "sso_account_id=accountId\n" +
                                    "sso_role_name=roleName\n" +
                                    "sso_start_url=https//d-abc123.awsapps.com/startTwo\n" +
                                    "sso_session=foo\n" +
                                    "[sso-session foo]\n" +
                                    "sso_region=regionTwo\n" +
                                    "sso_start_url=https//d-abc123.awsapps.com/startOne")
                , "Profile test and Sso-session foo has different sso_start_url."),
            // sso_region mismatch in sso-session and profile
            Arguments.of(configFile("[profile test]\n" +
                                    "sso_account_id=accountId\n" +
                                    "sso_role_name=roleName\n" +
                                    "sso_region=regionOne\n" +
                                    "sso_session=foo\n" +
                                    "[sso-session foo]\n" +
                                    "sso_region=regionTwo\n" +
                                    "sso_start_url=https//d-abc123.awsapps.com/start")
                , "Profile test and Sso-session foo has different sso_region.")
        );
    }

    @Test
    public void tokenResolvedFromTokenProvider(@Mock SdkTokenProvider sdkTokenProvider){
        SsoClient mockSsoClient = mock(SsoClient.class);

        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");
        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        when(sdkTokenProvider.resolveToken()).thenReturn(SsoAccessToken.builder().accessToken("sample").expiresAt(Instant.now()).build());

        RoleCredentials roleCredentials = RoleCredentials.builder()
                                                         .accessKeyId("AKID")
                                                         .secretAccessKey("secret")
                                                         .sessionToken("session")
                                                         .expiration(Instant.now().minusSeconds(1).toEpochMilli())
                                                         .build();
        when(mockSsoClient.getRoleCredentials(Mockito.any(GetRoleCredentialsRequest.class)))
            .thenReturn(GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build());

        AwsCredentialsProvider credentialsProvider = factory.create(ProfileProviderCredentialsContext.builder()
                                                                                                     .profile(profileFile.profile("test").get())
                                                                                                     .profileFile(profileFile)
                                                                                                     .build(),
                                                                    sdkTokenProvider,
                                                                    mockSsoClient);
        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();

        Mockito.verify(sdkTokenProvider, times(2)).resolveToken();
    }

    @Test
    public void missingSsoSessionSection_throwsSsoSessionNotFound() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_session=nonexistent\n" +
                                             "[sso-session bar]\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start\n" +
                                             "sso_region=region");

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                 .profileFile(profileFile)
                                                                                 .profile(profileFile.profile("test").get())
                                                                                 .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sso-session section not found with sso-session title nonexistent.");
    }


    @Test
    public void missingSsoRegionInSsoSession_throwsValidationError() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                 .profileFile(profileFile)
                                                                                 .profile(profileFile.profile("test").get())
                                                                                 .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'sso_region' must be set to use role-based credential loading in the 'foo' profile.");
    }

    @Test
    public void ssoStartUrlMismatch_throwsValidationError() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/startProfile\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/startSession");

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                 .profileFile(profileFile)
                                                                                 .profile(profileFile.profile("test").get())
                                                                                 .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Profile test and Sso-session foo has different sso_start_url.");
    }

    @Test
    public void ssoRegionMismatch_throwsValidationError() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=us-west-2\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                 .profileFile(profileFile)
                                                                                 .profile(profileFile.profile("test").get())
                                                                                 .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Profile test and Sso-session foo has different sso_region.");
    }

    @Test
    public void validProfileWithTokenProvider_createsProviderSuccessfully() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        lenient().when(sdkTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("valid-token").expiresAt(Instant.now().plusSeconds(3600)).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider);

        assertThat(credentialsProvider).isNotNull();
        assertThat(credentialsProvider).isInstanceOf(AwsCredentialsProvider.class);
    }

    @Test
    public void tokenIsReResolvedOnEachCredentialRefresh() {
        int numberOfRefreshCalls = 3;
        SsoClient mockSsoClient = mock(SsoClient.class);

        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        when(sdkTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("token-1").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-2").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-3").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-4").expiresAt(Instant.now().plusSeconds(3600)).build()
        );

        RoleCredentials roleCredentials = RoleCredentials.builder()
                                                         .accessKeyId("AKID")
                                                         .secretAccessKey("secret")
                                                         .sessionToken("session")
                                                         .expiration(Instant.now().minusSeconds(1).toEpochMilli())
                                                         .build();
        when(mockSsoClient.getRoleCredentials(Mockito.any(GetRoleCredentialsRequest.class)))
            .thenReturn(GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider,
            mockSsoClient);

        for (int i = 0; i < numberOfRefreshCalls; i++) {
            credentialsProvider.resolveCredentials();
        }

        Mockito.verify(sdkTokenProvider, Mockito.atLeast(numberOfRefreshCalls)).resolveToken();
    }

    @Test
    public void ssoSessionPath_eachRefreshUsesLatestToken() {
        SsoClient mockSsoClient = mock(SsoClient.class);
        SdkTokenProvider mockTokenProvider = mock(SdkTokenProvider.class);

        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=123456789\n" +
                                             "sso_role_name=TestRole\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        when(mockTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("token-A").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-B").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-C").expiresAt(Instant.now().plusSeconds(3600)).build()
        );

        RoleCredentials roleCredentials = RoleCredentials.builder()
                                                         .accessKeyId("AKID")
                                                         .secretAccessKey("secret")
                                                         .sessionToken("session")
                                                         .expiration(Instant.now().minusSeconds(1).toEpochMilli())
                                                         .build();
        when(mockSsoClient.getRoleCredentials(Mockito.any(GetRoleCredentialsRequest.class)))
            .thenReturn(GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            mockTokenProvider,
            mockSsoClient);

        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();

        ArgumentCaptor<GetRoleCredentialsRequest> requestCaptor =
            ArgumentCaptor.forClass(GetRoleCredentialsRequest.class);
        Mockito.verify(mockSsoClient, Mockito.atLeast(3)).getRoleCredentials(requestCaptor.capture());

        assertThat(requestCaptor.getAllValues().get(0).accessToken()).isEqualTo("token-A");
        assertThat(requestCaptor.getAllValues().get(1).accessToken()).isEqualTo("token-B");
        assertThat(requestCaptor.getAllValues().get(2).accessToken()).isEqualTo("token-C");
    }

    @Test
    public void legacyPath_tokenReReadFromDiskOnEachRefresh() {
        SsoClient mockSsoClient = mock(SsoClient.class);
        SdkTokenProvider mockTokenProvider = mock(SdkTokenProvider.class);

        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=987654321\n" +
                                             "sso_role_name=LegacyRole\n" +
                                             "sso_region=us-west-2\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        when(mockTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("disk-token-1").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("disk-token-2").expiresAt(Instant.now().plusSeconds(3600)).build()
        );

        RoleCredentials roleCredentials = RoleCredentials.builder()
                                                         .accessKeyId("AKID")
                                                         .secretAccessKey("secret")
                                                         .sessionToken("session")
                                                         .expiration(Instant.now().minusSeconds(1).toEpochMilli())
                                                         .build();
        when(mockSsoClient.getRoleCredentials(Mockito.any(GetRoleCredentialsRequest.class)))
            .thenReturn(GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            mockTokenProvider,
            mockSsoClient);

        credentialsProvider.resolveCredentials();
        credentialsProvider.resolveCredentials();

        ArgumentCaptor<GetRoleCredentialsRequest> requestCaptor =
            ArgumentCaptor.forClass(GetRoleCredentialsRequest.class);
        Mockito.verify(mockSsoClient, times(2)).getRoleCredentials(requestCaptor.capture());

        assertThat(requestCaptor.getAllValues().get(0).accessToken()).isEqualTo("disk-token-1");
        assertThat(requestCaptor.getAllValues().get(1).accessToken()).isEqualTo("disk-token-2");
        Mockito.verify(mockTokenProvider, times(2)).resolveToken();
    }

    @Test
    public void errorPropagation_resolveTokenThrowsUncheckedIOException_propagatesToCaller() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        UncheckedIOException ioException = new UncheckedIOException(new IOException("Token file not found"));
        when(sdkTokenProvider.resolveToken()).thenThrow(ioException);

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider);

        assertThatThrownBy(credentialsProvider::resolveCredentials)
            .isInstanceOf(ExpiredTokenException.class)
            .hasMessageContaining("expired or is otherwise invalid")
            .hasCauseInstanceOf(UncheckedIOException.class);
    }

    @Test
    public void errorPropagation_resolveTokenThrowsRuntimeException_propagatesToCaller() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        RuntimeException tokenExpired = new RuntimeException("Token is expired");
        when(sdkTokenProvider.resolveToken()).thenThrow(tokenExpired);

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider);

        assertThatThrownBy(credentialsProvider::resolveCredentials)
            .isInstanceOf(ExpiredTokenException.class)
            .hasMessageContaining("expired or is otherwise invalid")
            .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    public void errorPropagation_resolveTokenReturnsNullTokenValue_errorPropagates() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        // Mock SdkToken to return null from token()
        SdkToken nullToken = mock(SdkToken.class);
        when(nullToken.token()).thenReturn(null);
        when(sdkTokenProvider.resolveToken()).thenReturn(nullToken);

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider);

        assertThatThrownBy(credentialsProvider::resolveCredentials)
            .isInstanceOf(ExpiredTokenException.class)
            .hasMessageContaining("expired or is otherwise invalid");
    }

    @Test
    public void tokenProviderThrowsOnSecondCall_staleCachedCredentialsReturned() {
        SsoClient mockSsoClient = mock(SsoClient.class);

        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        RoleCredentials roleCredentials = RoleCredentials.builder()
                                                         .accessKeyId("AKID")
                                                         .secretAccessKey("secret")
                                                         .sessionToken("session")
                                                         .expiration(Instant.now().minusSeconds(1).toEpochMilli())
                                                         .build();
        when(mockSsoClient.getRoleCredentials(Mockito.any(GetRoleCredentialsRequest.class)))
            .thenReturn(GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build());

        RuntimeException secondCallError = new RuntimeException("Token refresh failed on second attempt");
        when(sdkTokenProvider.resolveToken())
            .thenReturn(SsoAccessToken.builder().accessToken("valid-token").expiresAt(Instant.now().plusSeconds(3600)).build())
            .thenThrow(secondCallError);

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider,
            mockSsoClient);

        // First call succeeds and caches credentials
        credentialsProvider.resolveCredentials();

        // Second call: token provider throws InvalidTokenException, but CachedSupplier with
        // StaleValueBehavior.ALLOW returns stale cached credentials (static stability)
        assertThat(credentialsProvider.resolveCredentials()).isNotNull();
    }

    @Test
    public void fiveSequentialRefreshes_eachUsesCorrectToken() {
        SsoClient mockSsoClient = mock(SsoClient.class);
        SdkTokenProvider mockTokenProvider = mock(SdkTokenProvider.class);

        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=111222333\n" +
                                             "sso_role_name=MultiRefreshRole\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=us-east-1\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        when(mockTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("token-1").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-2").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-3").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-4").expiresAt(Instant.now().plusSeconds(3600)).build(),
            SsoAccessToken.builder().accessToken("token-5").expiresAt(Instant.now().plusSeconds(3600)).build()
        );

        RoleCredentials roleCredentials = RoleCredentials.builder()
                                                         .accessKeyId("AKID")
                                                         .secretAccessKey("secret")
                                                         .sessionToken("session")
                                                         .expiration(Instant.now().minusSeconds(1).toEpochMilli())
                                                         .build();
        when(mockSsoClient.getRoleCredentials(Mockito.any(GetRoleCredentialsRequest.class)))
            .thenReturn(GetRoleCredentialsResponse.builder().roleCredentials(roleCredentials).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            mockTokenProvider,
            mockSsoClient);

        for (int i = 0; i < 5; i++) {
            credentialsProvider.resolveCredentials();
        }

        ArgumentCaptor<GetRoleCredentialsRequest> requestCaptor =
            ArgumentCaptor.forClass(GetRoleCredentialsRequest.class);
        Mockito.verify(mockSsoClient, times(5)).getRoleCredentials(requestCaptor.capture());

        assertThat(requestCaptor.getAllValues().get(0).accessToken()).isEqualTo("token-1");
        assertThat(requestCaptor.getAllValues().get(1).accessToken()).isEqualTo("token-2");
        assertThat(requestCaptor.getAllValues().get(2).accessToken()).isEqualTo("token-3");
        assertThat(requestCaptor.getAllValues().get(3).accessToken()).isEqualTo("token-4");
        assertThat(requestCaptor.getAllValues().get(4).accessToken()).isEqualTo("token-5");
        Mockito.verify(mockTokenProvider, times(5)).resolveToken();
    }

    @Test
    public void profileWithSsoSessionPointingToMissingSection_throwsWithSessionName() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_session=my-missing-session");

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                 .profileFile(profileFile)
                                                                                 .profile(profileFile.profile("test").get())
                                                                                 .build()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sso-session section not found with sso-session title my-missing-session.");
    }


    @Test
    public void close_cleansUpResourcesWithoutException() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        lenient().when(sdkTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("close-test-token").expiresAt(Instant.now().plusSeconds(3600)).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider);

        // Verify the returned provider implements SdkAutoCloseable
        assertThat(credentialsProvider).isInstanceOf(SdkAutoCloseable.class);

        // Calling close() should not throw any exceptions
        ((SdkAutoCloseable) credentialsProvider).close();
    }

    @Test
    public void close_calledMultipleTimes_doesNotThrow() {
        ProfileFile profileFile = configFile("[profile test]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_region=region\n" +
                                             "sso_session=foo\n" +
                                             "[sso-session foo]\n" +
                                             "sso_region=region\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        lenient().when(sdkTokenProvider.resolveToken()).thenReturn(
            SsoAccessToken.builder().accessToken("close-test-token").expiresAt(Instant.now().plusSeconds(3600)).build());

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("test").get())
                                            .profileFile(profileFile)
                                            .build(),
            sdkTokenProvider);

        SdkAutoCloseable closeable = (SdkAutoCloseable) credentialsProvider;

        // First close
        closeable.close();
        // Second close - should not throw
        closeable.close();
    }

    @Test
    public void factoryCreateWithLegacyProfile_constructsProviderSuccessfully() throws IOException {
        String startUrl = "https//d-abc123.awsapps.com/start";
        String generatedTokenFileName = "6a888bdb653a4ba345dd68f21b896ec2e218c6f4.json";

        ProfileFile profileFile = configFile("[profile foo]\n" +
                                             "sso_account_id=accountId\n" +
                                             "sso_region=region\n" +
                                             "sso_role_name=roleName\n" +
                                             "sso_start_url=https//d-abc123.awsapps.com/start");

        String tokenFile = "{\n" +
                           "\"accessToken\": \"base64string\",\n" +
                           "\"expiresAt\": \"2090-01-01T00:00:00Z\",\n" +
                           "\"region\": \"us-west-2\", \n" +
                           "\"startUrl\": \"" + startUrl + "\"\n" +
                           "}";
        Path cachedTokenFilePath = prepareTestCachedTokenFile(tokenFile, generatedTokenFileName);
        SsoAccessTokenProvider tokenProvider = new SsoAccessTokenProvider(cachedTokenFilePath);

        SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
        AwsCredentialsProvider credentialsProvider = factory.create(
            ProfileProviderCredentialsContext.builder()
                                            .profile(profileFile.profile("foo").get())
                                            .profileFile(profileFile)
                                            .build(),
            tokenProvider);

        assertThat(credentialsProvider).isNotNull();
        assertThat(credentialsProvider).isInstanceOf(AwsCredentialsProvider.class);
        assertThat(credentialsProvider).isInstanceOf(SdkAutoCloseable.class);

        // Verify close works properly on the fully-constructed provider
        ((SdkAutoCloseable) credentialsProvider).close();
    }
}