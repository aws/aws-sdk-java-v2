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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sso.internal.SsoAccessToken;
import software.amazon.awssdk.services.sso.internal.SsoAccessTokenProvider;
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
        assertThat(factory.create(profileFile.profile("foo").get(),
                                  profileFile,
                                  tokenProvider)).isInstanceOf(AwsCredentialsProvider.class);
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
        AwsCredentialsProvider credentialsProvider = factory.create(profileFile.profile("test").get(), profileFile, sdkTokenProvider);
        try {
            credentialsProvider.resolveCredentials();
        } catch (Exception e) {
            // sso client created internally which cannot be mocked.
        }
        Mockito.verify(sdkTokenProvider, times(1)).resolveToken();
    }
}