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
import software.amazon.awssdk.auth.token.SdkTokenProvider;
import software.amazon.awssdk.auth.token.SdkTokenProviderFactory;
import software.amazon.awssdk.auth.token.SdkTokenProviderFactoryProperties;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class to load SSO Token Providers.
 */
@SdkInternalApi
public final class ProfileTokenProviderLoader {
    private static final String SSO_OIDC_TOKEN_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.ssooidc.SsoOidcTokenProviderFactory";

    private final Profile profile;

    public ProfileTokenProviderLoader(Profile profile) {
        this.profile = Validate.paramNotNull(profile, "profile");
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
        requireProperties(ProfileProperty.SSO_REGION,
                          ProfileProperty.SSO_START_URL);

        SdkTokenProviderFactoryProperties factoryProperties =
            SdkTokenProviderFactoryProperties.builder()
                                             .startUrl(profile.properties().get(ProfileProperty.SSO_START_URL))
                                             .build();

        return ssoTokenProviderFactory().create(factoryProperties);
    }

    /**
     * Require that the provided properties are configured in this profile.
     */
    private void requireProperties(String... requiredProperties) {
        Arrays.stream(requiredProperties)
              .forEach(p -> Validate.isTrue(profile.properties().containsKey(p),
                                            "Property '%s' was not configured for profile '%s'.", p, profile.name()));
    }

    /**
     * Load the factory that can be used to create the SSO token provider, assuming it is on the classpath.
     */
    private SdkTokenProviderFactory ssoTokenProviderFactory() {
        try {
            Class<?> ssoOidcTokenProviderFactory = ClassLoaderHelper.loadClass(SSO_OIDC_TOKEN_PROVIDER_FACTORY,
                                                                               getClass());
            return (SdkTokenProviderFactory) ssoOidcTokenProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use SSO OIDC related properties in the '" + profile.name() + "' profile, "
                                            + "the 'ssooidc' service module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + profile.name() + "' token provider factory.", e);
        }
    }
}
