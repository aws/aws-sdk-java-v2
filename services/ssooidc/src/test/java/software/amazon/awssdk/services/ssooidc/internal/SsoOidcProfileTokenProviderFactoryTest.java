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

import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProviderFactoryProperties;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.services.ssooidc.SsoOidcProfileTokenProviderFactory;
import software.amazon.awssdk.utils.StringInputStream;

public class SsoOidcProfileTokenProviderFactoryTest {

    private static Stream<Arguments> ssoSessionMissingProperties() {
        String ssoRegionErrorMsg = "'sso_region' must be set to use bearer tokens loading in the 'one' profile.";
        String ssoStartUrlErrorMsg = "'sso_start_url' must be set to use bearer tokens loading in the 'one' profile.";
        String ssoSectionNotFoundError = "Sso-session section not found with sso-session title one.";


        return Stream.of(Arguments.of("[profile sso]\n" +
                                      "sso_session=one\n" +
                                      "[sso-session one]\n" +
                                      "sso_regions=us-east-1\n" +
                                      "sso_start_url= https://start-url\n", ssoRegionErrorMsg),

                         Arguments.of("[profile sso]\n" +
                                      "sso_session=one\n" +
                                      "[sso-session one]\n" +
                                      "sso_region=us-east-1\n" +
                                      "sso_end_url= https://start-url\n", ssoStartUrlErrorMsg),
                         Arguments.of("[profile sso]\n" +
                                      "sso_session=one\n" +
                                      "[sso-session one]\n" , ssoStartUrlErrorMsg),
                         Arguments.of( "[profile sso]\n" +
                                       "sso_session=one\n" +
                                       "[sso-session two]\n" +
                                       "sso_region=us-east-1\n" +
                                       "sso_start_url= https://start-url\n", ssoSectionNotFoundError));
    }

    @Test
    void create_throwsExceptionIfRegionNotPassed() {
        String startUrl = "https://my-start-url.com";
        Assertions.assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () -> SdkTokenProviderFactoryProperties.builder()
                                                   .startUrl(startUrl)
                                                   .build()
        ).withMessage("region must not be null.");
    }

    @Test
    void create_throwsExceptionIfStartUrlNotPassed() {
        String region = "test-region";

        Assertions.assertThatExceptionOfType(NullPointerException.class).isThrownBy(
            () -> SdkTokenProviderFactoryProperties.builder()
                                                   .region(region)
                                                   .build()
        ).withMessage("startUrl must not be null.");
    }

    @Test
    void create_SsooidcTokenProvider_from_SsooidcSpecificProfile() {
        String profileContent = "[profile ssotoken]\n" +
                                "sso_session=admin\n" +
                                "[sso-session admin]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        SdkTokenProvider sdkTokenProvider = new SsoOidcProfileTokenProviderFactory().create(profiles,
                                                                                            profiles.profile("ssotoken").get());
        Assertions.assertThat(sdkTokenProvider).isNotNull();

    }

    @Test
    void create_SsoOidcTokenProvider_from_SsooidcSpecificProfileSupplier() {
        String profileContent = "[profile ssotoken]\n" +
                                "sso_session=admin\n" +
                                "[sso-session admin]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();
        SdkTokenProvider sdkTokenProvider = new SsoOidcProfileTokenProviderFactory().create(() -> profiles, "ssotoken");
        Assertions.assertThat(sdkTokenProvider).isNotNull();

    }

    @Test
    void create_SsoOidcTokenProvider_with_ssoAccountIdInProfile() {
        String profileContent = "[profile sso]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_account_id=1234567\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

        Assertions.assertThatExceptionOfType(IllegalStateException.class)
                  .isThrownBy(() -> new SsoOidcProfileTokenProviderFactory().create(profiles, profiles.profile("sso").get()));

    }

    @Test
    void create_SsoOidcTokenProvider_with_ssoRoleNameInProfile() {
        String profileContent = "[profile sso]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_role_name=ssoSpecificRole\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

        Assertions.assertThatExceptionOfType(IllegalStateException.class)
                  .isThrownBy(() -> new SsoOidcProfileTokenProviderFactory().create(profiles, profiles.profile("sso").get()));

    }

    @Test
    void create_SsoOidcTokenProvider_with_ssoRoleNameInProfileSupplier() {
        String profileContent = "[profile sso]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_role_name=ssoSpecificRole\n" +
                                "sso_start_url= https://start-url\n";
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(profileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();

        Assertions.assertThatNoException()
                  .isThrownBy(() -> new SsoOidcProfileTokenProviderFactory().create(() -> profiles, "sso"));

    }

    @ParameterizedTest
    @MethodSource("ssoSessionMissingProperties")
    void incorrectSsoProperties_throwsException(String ssoProfileContent, String msg) {
        ProfileFile profiles = ProfileFile.builder()
                                          .content(new StringInputStream(ssoProfileContent))
                                          .type(ProfileFile.Type.CONFIGURATION)
                                          .build();


        Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                      () -> new SsoOidcProfileTokenProviderFactory().create(profiles, profiles.profile("sso").get()))
                  .withMessageContaining(msg);
    }

}