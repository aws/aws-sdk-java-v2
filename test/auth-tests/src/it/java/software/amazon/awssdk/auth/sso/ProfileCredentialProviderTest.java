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

package software.amazon.awssdk.auth.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.sso.auth.SsoProfileCredentialsProviderFactory;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileCredentialProviderTest {


    private static Stream<Arguments> ssoTokenErrorValues() {

        // Session title is missing
        return Stream.of(Arguments.of(configFile("[profile test]\n" +
                                                 "sso_account_id=accountId\n" +
                                                 "sso_role_name=roleName\n" +
                                                 "sso_session=foo\n" +
                                                 "[sso-session foo]\n" +
                                                 "sso_start_url=https//d-abc123.awsapps.com/start\n" +
                                                 "sso_region=region")
                             , "Unable to load SSO token"),
                         Arguments.of(configFile("[profile test]\n" +
                                                 "sso_account_id=accountId\n" +
                                                 "sso_role_name=roleName\n" +
                                                 "sso_session=foo\n" +
                                                 "[sso-session foo]\n" +
                                                 "sso_region=region")
                             , "Property 'sso_start_url' was not configured for profile 'test'"),
                         Arguments.of(configFile("[profile test]\n" +
                                                 "sso_account_id=accountId\n" +
                                                 "sso_role_name=roleName\n" +
                                                 "sso_region=region\n" +
                                                 "sso_start_url=https//non-existing-Token/start")
                             , "java.nio.file.NoSuchFileException")


        );
    }

    private static ProfileFile configFile(String configFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(configFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

    @ParameterizedTest
    @MethodSource("ssoTokenErrorValues")
    void validateSsoFactoryErrorWithIncorrectProfiles(ProfileFile profiles, String expectedValue) {
        assertThat(profiles.profile("test")).hasValueSatisfying(profile -> {
            SsoProfileCredentialsProviderFactory factory = new SsoProfileCredentialsProviderFactory();
            assertThatThrownBy(() -> factory.create(ProfileProviderCredentialsContext.builder()
                                                                                     .profile(profile)
                                                                                     .profileFile(profiles)
                                                                                     .build())).hasMessageContaining(expectedValue);
        });
    }

}
