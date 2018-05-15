/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth.credentials.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperties;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class to load {@link #credentialsProvider()} configured in a profile.
 */
@SdkInternalApi
public final class ProfileCredentialsUtils {
    private static final String STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory";

    private final Profile profile;

    /**
     * The name of this profile (minus any profile prefixes).
     */
    private final String name;

    /**
     * The raw properties in this profile.
     */
    private final Map<String, String> properties;

    /**
     * A function to resolve the profile from which this profile should derive its credentials.
     *
     * This is used by assume-role credentials providers to find the credentials it should use for authentication when assuming
     * the role.
     */
    private final Function<String, Optional<Profile>> credentialsSourceResolver;

    public ProfileCredentialsUtils(Profile profile, Function<String, Optional<Profile>> credentialsSourceResolver) {
        this.profile = Validate.paramNotNull(profile, "profile");
        this.name = profile.name();
        this.properties = profile.properties();
        this.credentialsSourceResolver = credentialsSourceResolver;
    }

    /**
     * Retrieve the credentials provider for which this profile has been configured, if available.
     *
     * If this profile is configured for role-based credential loading, the returned {@link AwsCredentialsProvider} implements
     * {@link SdkAutoCloseable} and should be cleaned up to prevent resource leaks in the event that multiple credentials
     * providers will be created.
     */
    public Optional<AwsCredentialsProvider> credentialsProvider() {
        return credentialsProvider(new HashSet<>());
    }

    /**
     * Retrieve the credentials provider for which this profile has been configured, if available.
     *
     * @param children The child profiles that source credentials from this profile.
     */
    private Optional<AwsCredentialsProvider> credentialsProvider(Set<String> children) {
        if (properties.containsKey(ProfileProperties.ROLE_ARN)) {
            return Optional.ofNullable(roleBasedProfileCredentialsProvider(children));
        }

        if (properties.containsKey(ProfileProperties.AWS_SESSION_TOKEN)) {
            return Optional.of(sessionProfileCredentialsProvider());
        }

        if (properties.containsKey(ProfileProperties.AWS_ACCESS_KEY_ID)) {
            return Optional.of(basicProfileCredentialsProvider());
        }

        return Optional.empty();
    }

    /**
     * Load a basic set of credentials that have been configured in this profile.
     */
    private AwsCredentialsProvider basicProfileCredentialsProvider() {
        requireProperties(ProfileProperties.AWS_ACCESS_KEY_ID,
                          ProfileProperties.AWS_SECRET_ACCESS_KEY);
        AwsCredentials credentials = AwsCredentials.create(properties.get(ProfileProperties.AWS_ACCESS_KEY_ID),
                                                           properties.get(ProfileProperties.AWS_SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * Load a set of session credentials that have been configured in this profile.
     */
    private AwsCredentialsProvider sessionProfileCredentialsProvider() {
        requireProperties(ProfileProperties.AWS_ACCESS_KEY_ID,
                          ProfileProperties.AWS_SECRET_ACCESS_KEY,
                          ProfileProperties.AWS_SESSION_TOKEN);
        AwsCredentials credentials = AwsSessionCredentials.create(properties.get(ProfileProperties.AWS_ACCESS_KEY_ID),
                                                                  properties.get(ProfileProperties.AWS_SECRET_ACCESS_KEY),
                                                                  properties.get(ProfileProperties.AWS_SESSION_TOKEN));
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * Load an assumed-role credentials provider that has been configured in this profile. This will attempt to locate the STS
     * module in order to generate the credentials provider. If it's not available, an illegal state exception will be raised.
     *
     * @param children The child profiles that source credentials from this profile.
     */
    private AwsCredentialsProvider roleBasedProfileCredentialsProvider(Set<String> children) {
        requireProperties(ProfileProperties.SOURCE_PROFILE);

        Validate.validState(!children.contains(name),
                            "Invalid profile file: Circular relationship detected with profiles %s.", children);
        Validate.validState(credentialsSourceResolver != null,
                            "The profile '%s' must be configured with a source profile in order to use assumed roles.", name);

        children.add(name);
        AwsCredentialsProvider sourceCredentialsProvider =
            credentialsSourceResolver.apply(properties.get(ProfileProperties.SOURCE_PROFILE))
                                     .flatMap(p -> new ProfileCredentialsUtils(p, credentialsSourceResolver)
                                         .credentialsProvider(children))
                                     .orElseThrow(this::noSourceCredentialsException);

        return stsCredentialsProviderFactory().create(sourceCredentialsProvider, profile);
    }

    /**
     * Require that the provided properties are configured in this profile.
     */
    private void requireProperties(String... requiredProperties) {
        Arrays.stream(requiredProperties)
              .forEach(p -> Validate.isTrue(properties.containsKey(p),
                                            "Profile property '%s' was not configured for '%s'.", p, name));
    }

    private IllegalStateException noSourceCredentialsException() {
        String error = String.format("The source profile of '%s' was configured to be '%s', but that source profile has no "
                                     + "credentials configured.", name, properties.get(ProfileProperties.SOURCE_PROFILE));
        return new IllegalStateException(error);
    }

    /**
     * Load the factory that can be used to create the STS credentials provider, assuming it is on the classpath.
     */
    private ChildProfileCredentialsProviderFactory stsCredentialsProviderFactory() {
        try {
            Class<?> stsCredentialsProviderFactory = Class.forName(STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY, true,
                                                                   Thread.currentThread().getContextClassLoader());
            return (ChildProfileCredentialsProviderFactory) stsCredentialsProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use assumed roles in the '" + name + "' profile, the 'sts' service module must "
                                            + "be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + "' profile credentials provider.", e);
        }
    }
}
