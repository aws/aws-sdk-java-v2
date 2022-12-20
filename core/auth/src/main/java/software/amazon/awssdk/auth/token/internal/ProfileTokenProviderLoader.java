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
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.token.credentials.ChildProfileTokenProviderFactory;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.internal.ProfileSection;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class to load SSO Token Providers.
 */
@SdkInternalApi
public final class ProfileTokenProviderLoader {
    private static final String SSO_OIDC_TOKEN_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.ssooidc.SsoOidcProfileTokenProviderFactory";

    private final Supplier<ProfileFile> profileFileSupplier;
    private final String profileName;
    private volatile ProfileFile currentProfileFile;
    private volatile SdkTokenProvider currentTokenProvider;

    private final Lazy<ChildProfileTokenProviderFactory> factory;

    public ProfileTokenProviderLoader(Supplier<ProfileFile> profileFile, String profileName) {
        this.profileFileSupplier = Validate.paramNotNull(profileFile, "profileFile");
        this.profileName = Validate.paramNotNull(profileName, "profileName");
        this.factory = new Lazy<>(this::ssoTokenProviderFactory);
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
        return () -> ssoProfileCredentialsProvider(profileFileSupplier, profileName).resolveToken();
    }

    private SdkTokenProvider ssoProfileCredentialsProvider(ProfileFile profileFile, Profile profile) {
        String profileSsoSectionName = profileSsoSectionName(profile);
        Profile ssoProfile = ssoProfile(profileFile, profileSsoSectionName);

        validateRequiredProperties(ssoProfile, ProfileProperty.SSO_REGION, ProfileProperty.SSO_START_URL);

        return factory.getValue().create(profileFile, profile);
    }

    private SdkTokenProvider ssoProfileCredentialsProvider(Supplier<ProfileFile> profileFile, String profileName) {
        ProfileFile profileFileInstance = profileFile.get();
        if (!Objects.equals(profileFileInstance, currentProfileFile)) {
            synchronized (this) {
                if (!Objects.equals(profileFileInstance, currentProfileFile)) {
                    Profile profileInstance = resolveProfile(profileFileInstance, profileName);
                    currentProfileFile = profileFileInstance;
                    currentTokenProvider = ssoProfileCredentialsProvider(profileFileInstance, profileInstance);
                }
            }
        }

        return currentTokenProvider;
    }

    private Profile resolveProfile(ProfileFile profileFile, String profileName) {
        return profileFile.profile(profileName)
                          .orElseThrow(() -> {
                              String errorMessage = String.format("Profile file contained no information for profile '%s': %s",
                                                                  profileName, profileFile);
                              return SdkClientException.builder().message(errorMessage).build();
                          });
    }

    private String profileSsoSectionName(Profile profile) {
        return Optional.ofNullable(profile)
                       .flatMap(p -> p.property(ProfileSection.SSO_SESSION.getPropertyKeyName()))
                       .orElseThrow(() -> new IllegalArgumentException(
                           "Profile " + profileName + " does not have sso_session property"));
    }

    private Profile ssoProfile(ProfileFile profileFile, String profileSsoSectionName) {
        return profileFile.getSection(ProfileSection.SSO_SESSION.getSectionTitle(), profileSsoSectionName)
                          .orElseThrow(() -> new IllegalArgumentException(
                              "Sso-session section not found with sso-session title " + profileSsoSectionName));
    }

    /**
     * Require that the provided properties are configured in this profile.
     */
    private void validateRequiredProperties(Profile ssoProfile, String... requiredProperties) {
        Arrays.stream(requiredProperties)
              .forEach(p -> Validate.isTrue(ssoProfile.properties().containsKey(p),
                                            "Property '%s' was not configured for profile '%s'.",
                                            p, profileName));
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
            throw new IllegalStateException("To use SSO OIDC related properties in the '" + profileName + "' profile, "
                                            + "the 'ssooidc' service module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '%s" + profileName + "' token provider factory.", e);
        }
    }
}
