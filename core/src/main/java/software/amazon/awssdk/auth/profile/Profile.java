/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth.profile;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.auth.profile.internal.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.AwsSessionCredentials;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A named collection of configuration stored in a {@link ProfileFile}.
 *
 * Special access methods are provided for loading the {@link #region()} and {@link #credentialsProvider()} configured in this
 * profile. Raw property access can be made via {@link #property(String)} and {@link #properties()}.
 *
 * @see ProfileFile
 */
@SdkPublicApi
public final class Profile implements ToCopyableBuilder<Profile.Builder, Profile> {
    private static final String STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
            "software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory";

    /**
     * The name of this profile (minus any profile prefixes).
     */
    private final String name;

    /**
     * The raw properties in this profile.
     */
    private final Map<String, String> properties;

    /**
     * The profile from which this profile should derive its credentials if it doesn't have any of its own that are directly
     * configured. This may be null.
     *
     * This is used by assume-role credentials providers to find the credentials it should use for authentication when assuming
     * the role.
     */
    private final Profile credentialsParent;

    /**
     * @see ProfileFile
     * @see #builder()
     */
    private Profile(Builder builder) {
        this.name = Validate.paramNotNull(builder.name, "name");
        this.properties = Validate.paramNotNull(builder.properties, "properties");
        this.credentialsParent = builder.credentialsParent;
    }

    /**
     * Create a builder for defining a profile with specific attributes. For reading profiles from a file, see
     * {@link ProfileFile}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve the name of this profile.
     */
    public String name() {
        return name;
    }

    /**
     * Retrieve a specific raw property from this profile.
     *
     * @param propertyKey The name of the property to retrieve.
     * @return The value of the property, if configured.
     */
    public Optional<String> property(String propertyKey) {
        return Optional.ofNullable(properties.get(propertyKey));
    }

    /**
     * Retrieve an unmodifiable view of all of the properties currently in this profile.
     */
    public Map<String, String> properties() {
        return properties;
    }

    /**
     * Retrieve the region for which this profile has been configured, if available.
     */
    public Optional<Region> region() {
        return Optional.ofNullable(properties.get(ProfileProperties.REGION)).map(Region::of);
    }

    /**
     * Retrieve the credentials provider for which this profile has been configured, if available.
     *
     * If this profile is configured for role-based credential loading, the returned {@link AwsCredentialsProvider} implements
     * {@link SdkAutoCloseable} and should be cleaned up to prevent resource leaks in the event that multiple credentials
     * providers will be created.
     */
    public Optional<AwsCredentialsProvider> credentialsProvider() {
        if (properties.containsKey(ProfileProperties.ROLE_ARN)) {
            return Optional.of(roleBasedProfileCredentialsProvider());
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
     */
    private AwsCredentialsProvider roleBasedProfileCredentialsProvider() {
        requireProperties(ProfileProperties.SOURCE_PROFILE);

        Validate.validState(credentialsParent != null,
                            "The profile '%s' must be configured with a credentials parent in order to use assumed roles.", name);

        AwsCredentialsProvider parentCredentialsProvider = credentialsParent.credentialsProvider()
                                                                            .orElseThrow(this::noParentCredentialsException);

        return stsCredentialsProviderFactory().create(parentCredentialsProvider, this);
    }

    /**
     * Require that the provided properties are configured in this profile.
     */
    private void requireProperties(String... requiredProperties) {
        Arrays.stream(requiredProperties)
              .forEach(p -> Validate.isTrue(properties.containsKey(p),
                                            "Profile property '%s' was not configured for '%s'.", p, name));
    }

    private IllegalStateException noParentCredentialsException() {
        String error = String.format("The parent profile of '%s' was configured to be '%s', but that parent profile has no "
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
                                            + "be on the class path.");
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + "' profile credentials provider.", e);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().name(name)
                        .properties(properties)
                        .credentialsParent(credentialsParent);
    }

    @Override
    public String toString() {
        return "Profile(" + name + ", " + properties + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Profile profile = (Profile) o;
        return Objects.equals(name, profile.name) &&
               Objects.equals(properties, profile.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, properties);
    }

    /**
     * A builder for a {@link Profile}. See {@link #builder()}.
     */
    public static class Builder implements CopyableBuilder<Builder, Profile> {
        private String name;
        private Map<String, String> properties;
        private Profile credentialsParent;

        /**
         * @see #builder()
         */
        private Builder() {}

        /**
         * Define the name of this profile, without the legacy "profile" prefix.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Define the properties configured in this profile.
         */
        public Builder properties(Map<String, String> properties) {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            return this;
        }

        /**
         * Define 'credentials parent' of this profile. This parent is only required when using role-based credentials. When
         * credentials are loaded from a profile and role-based credentials are configured, the parent's credential provider is
         * used to authenticate the application with the Amazon service being used to assume the requested role.
         */
        public Builder credentialsParent(Profile credentialsParent) {
            this.credentialsParent = credentialsParent;
            return this;
        }

        /**
         * Create a profile using the current state of this builder.
         */
        public Profile build() {
            return new Profile(this);
        }
    }
}
