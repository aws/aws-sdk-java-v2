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

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.token.credentials.ChildProfileTokenProviderFactory;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.internal.ProfileSection;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Factory for creating {@link SsoOidcTokenProvider}.
 */
@SdkProtectedApi
public final class SsoOidcProfileTokenProviderFactory implements ChildProfileTokenProviderFactory {

    private static final String MISSING_PROPERTY_ERROR_FORMAT = "'%s' must be set to use bearer tokens loading in the "
                                                                + "'%s' profile.";

    @Override
    public SdkTokenProvider create(ProfileFile profileFile, Profile profile) {
        return new SsooidcProfileTokenProvider(profileFile, profile);
    }


    /**
     * A wrapper for a {@link SdkTokenProvider} that is returned by this factory when {@link #create(ProfileFile,Profile)} is
     * invoked. This wrapper is important because it ensures the token provider is closed when it is no longer needed.
     */
    private static final class SsooidcProfileTokenProvider implements SdkTokenProvider, SdkAutoCloseable {
        private final SsoOidcTokenProvider sdkTokenProvider;

        private SsooidcProfileTokenProvider(ProfileFile profileFile, Profile profile) {
            String profileSsoSectionName = profile.property(
                ProfileSection.SSO_SESSION
                    .getPropertyKeyName()).orElseThrow(() -> new IllegalStateException(
                        "Profile " + profile.name() + " does not have sso_session property"));

            Profile ssoProfile =
                profileFile.getSection(
                    ProfileSection.SSO_SESSION.getSectionTitle(),
                    profileSsoSectionName).orElseThrow(() -> new IllegalArgumentException(
                    "Sso-session section not found with sso-session title " + profileSsoSectionName + "."));

            String startUrl = requireProperty(ssoProfile, ProfileProperty.SSO_START_URL);
            String region = requireProperty(ssoProfile, ProfileProperty.SSO_REGION);

            if (ssoProfile.property(ProfileProperty.SSO_ACCOUNT_ID).isPresent()
                || ssoProfile.property(ProfileProperty.SSO_ROLE_NAME).isPresent()) {
                throw new IllegalStateException("sso_account_id or sso_role_name properties must not be defined for"
                                                + "profiles that provide ssooidc providers");

            }

            this.sdkTokenProvider = SsoOidcTokenProvider.builder()
                                                        .sessionName(ssoProfile.name())
                                                        .ssoOidcClient(SsoOidcClient.builder()
                                                                                    .region(Region.of(region))
                                                                                    .credentialsProvider(
                                                                                        AnonymousCredentialsProvider.create())
                                                                                    .build())
                                                        .build();
        }

        private String requireProperty(Profile profile, String requiredProperty) {
            return profile.property(requiredProperty)
                          .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_PROPERTY_ERROR_FORMAT,
                                                                                        requiredProperty, profile.name())));
        }

        @Override
        public SdkToken resolveToken() {
            return this.sdkTokenProvider.resolveToken();
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(sdkTokenProvider, null);
        }
    }
}
