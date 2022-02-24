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

package software.amazon.awssdk.auth.ssooidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.internal.ProfileTokenProviderLoader;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileTokenProviderLoaderTest {

    @Test
    public void noProfile_throwsException() {
        assertThatThrownBy(() -> new ProfileTokenProviderLoader(null))
            .hasMessageContaining("profile must not be null");
    }

    @ParameterizedTest
    @MethodSource("ssoErrorValues")
    public void incorrectSsoProperties_throwsException(String ssoRegion, String ssoStartUrl, String msg) {
        ProfileFile profileFile = configFile(String.format("[profile sso]\n%s%s", ssoRegion, ssoStartUrl));

        assertThat(profileFile.profile("sso")).hasValueSatisfying(profile -> {
            ProfileTokenProviderLoader providerLoader = new ProfileTokenProviderLoader(profile);
            assertThatThrownBy(providerLoader::tokenProvider).hasMessageContaining(msg);
        });
    }

    @Test
    public void correctSsoProperties_createsTokenProvider() {
        String profileContent = "[profile sso]\n" +
                                "sso_region=us-east-1\n" +
                                "sso_start_url=https://d-abc123.awsapps.com/start\n";

        Optional<Profile> ssoProfile = configFile(profileContent).profile("sso");
        ProfileTokenProviderLoader providerLoader = new ProfileTokenProviderLoader(ssoProfile.get());
        Optional<SdkTokenProvider> tokenProvider = providerLoader.tokenProvider();
        assertThat(tokenProvider).isPresent();
        assertThatThrownBy(() -> tokenProvider.get().resolveToken())
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Unable to load SSO token");
    }

    private static Stream<Arguments> ssoErrorValues() {
        String ssoRegionErrorMsg = "Property 'sso_region' was not configured for profile";
        String ssoStartUrlErrorMsg = "Property 'sso_start_url' was not configured for profile";

        return Stream.of(Arguments.of("", "", ssoRegionErrorMsg),
                         Arguments.of("", "sso_start_url = https://domain.com/start", ssoRegionErrorMsg),
                         Arguments.of("sso_region = us-east-1\n", "", ssoStartUrlErrorMsg));
    }

    private ProfileFile configFile(String configFile) {
        return ProfileFile.builder()
                          .content(new StringInputStream(configFile))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }
}
