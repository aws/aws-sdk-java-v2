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

package software.amazon.awssdk.auth.token.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.ChildProfileTokenProviderFactory;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.internal.ProfileSection;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class to load SSO Token Providers.
 */
@SdkInternalApi
public final class ProfileTokenProviderLoader {
    private static final String SSO_OIDC_TOKEN_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.ssooidc.SsoOidcProfileTokenProviderFactory";

    private final Profile profile;
    private final ProfileFile profileFile;

    public ProfileTokenProviderLoader(ProfileFile profileFile, Profile profile) {
        this.profile = Validate.paramNotNull(profile, "profile");
        this.profileFile = Validate.paramNotNull(profileFile, "profileFile");
    }

    /**
     * Retrieve the token provider for which this profile has been configured, if available.
     */
    public Optional<SdkTokenProvider> tokenProvider() {
        return Optional.ofNullable(ssoProfileCredentialsProvider());
    }

    /**
     * Create the SSO credentials provider based on the related profile properties.
     */
    private SdkTokenProvider ssoProfileCredentialsProvider() {

        String profileSsoSectionName = profile.property(ProfileSection.SSO_SESSION.getPropertyKeyName())
                                              .orElseThrow(() -> new IllegalArgumentException(
                                                  "Profile " + profile.name() + " does not have sso_session property"));

        Profile ssoProfile = profileFile.getSection(ProfileSection.SSO_SESSION.getSectionTitle(), profileSsoSectionName)
                                        .orElseThrow(() -> new IllegalArgumentException(
                                            "Sso-session section not found with sso-session title " + profileSsoSectionName));

        validateRequiredProperties(ssoProfile,
                                   ProfileProperty.SSO_REGION,
                                   ProfileProperty.SSO_START_URL);
        return ssoTokenProviderFactory().create(profileFile, profile);
    }

    /**
     * Require that the provided properties are configured in this profile.
     */
    private void validateRequiredProperties(Profile ssoProfile, String... requiredProperties) {
        Arrays.stream(requiredProperties)
              .forEach(p -> Validate.isTrue(ssoProfile.properties().containsKey(p),
                                            "Property '%s' was not configured for profile '%s'.", p, this.profile.name()));
    }

    /**
     * Load the factory that can be used to create the SSO token provider, assuming it is on the classpath.
     */
    private ChildProfileTokenProviderFactory ssoTokenProviderFactory() {
        try {
            Class<?> ssoOidcTokenProviderFactory = ClassLoaderHelper.loadClass(SSO_OIDC_TOKEN_PROVIDER_FACTORY,
                                                                               getClass());
            return (ChildProfileTokenProviderFactory) ssoOidcTokenProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use SSO OIDC related properties in the '" + profile.name() + "' profile, "
                                            + "the 'ssooidc' service module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + profile.name() + "' token provider factory.", e);
        }
    }
}
