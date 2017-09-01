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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.AwsSessionCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.auth.profile.internal.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.profile.internal.ProfileProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

public final class Profile {
    private static final String STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
            "software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory";

    private final String name;
    private final Map<String, String> properties;
    private final ProfilesFile profilesFile;

    public Profile(Builder builder) {
        this.name = Validate.paramNotNull(builder.name, "name");
        this.properties = Validate.paramNotNull(builder.properties, "properties");
        this.profilesFile = Validate.paramNotNull(builder.profilesFile, "profilesFile");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public Optional<String> property(String propertyKey) {
        return Optional.ofNullable(properties.get(propertyKey));
    }

    public Optional<Region> region() {
        return Optional.ofNullable(properties.get(ProfileProperties.REGION)).map(Region::of);
    }

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

    private void requireProperties(String... requiredProperties) {
        Arrays.stream(requiredProperties)
              .forEach(p -> Validate.isTrue(properties.containsKey(p),
                                            "Profile property '%s' was not configured for '%s'.", p, name));
    }

    private AwsCredentialsProvider basicProfileCredentialsProvider() {
        requireProperties(ProfileProperties.AWS_ACCESS_KEY_ID,
                          ProfileProperties.AWS_SECRET_ACCESS_KEY);
        AwsCredentials credentials = new AwsCredentials(properties.get(ProfileProperties.AWS_ACCESS_KEY_ID),
                                                        properties.get(ProfileProperties.AWS_SECRET_ACCESS_KEY));
        return new StaticCredentialsProvider(credentials);
    }

    private AwsCredentialsProvider sessionProfileCredentialsProvider() {
        requireProperties(ProfileProperties.AWS_ACCESS_KEY_ID,
                          ProfileProperties.AWS_SECRET_ACCESS_KEY,
                          ProfileProperties.AWS_SESSION_TOKEN);
        AwsCredentials credentials = new AwsSessionCredentials(properties.get(ProfileProperties.AWS_ACCESS_KEY_ID),
                                                               properties.get(ProfileProperties.AWS_SECRET_ACCESS_KEY),
                                                               properties.get(ProfileProperties.AWS_SESSION_TOKEN));
        return new StaticCredentialsProvider(credentials);
    }

    private AwsCredentialsProvider roleBasedProfileCredentialsProvider() {
        requireProperties(ProfileProperties.SOURCE_PROFILE);

        Profile parentProfile = profilesFile.profile(properties.get(ProfileProperties.SOURCE_PROFILE))
                                            .orElseThrow(this::parentProfileDoesNotExistException);

        AwsCredentialsProvider parentCredentialsProvider = parentProfile.credentialsProvider()
                                                                        .orElseThrow(this::noParentProfileCredentialsException);

        return stsCredentialsProviderFactory().create(parentCredentialsProvider, this);
    }

    private IllegalStateException parentProfileDoesNotExistException() {
        String error = String.format("The parent profile of '%s' was configured to be '%s', but that parent profile "
                                     + "does not exist.", name, properties.get(ProfileProperties.SOURCE_PROFILE));
        return new IllegalStateException(error);
    }

    private IllegalStateException noParentProfileCredentialsException() {
        String error = String.format("The parent profile of '%s' was configured to be '%s', but that parent profile has no "
                                     + "credentials configured.", name, properties.get(ProfileProperties.SOURCE_PROFILE));
        return new IllegalStateException(error);
    }

    private ChildProfileCredentialsProviderFactory stsCredentialsProviderFactory() {
        try {
            Class<?> stsCredentialsProviderFactory = Class.forName(STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY, true,
                                                                   Thread.currentThread().getContextClassLoader());
            return (ChildProfileCredentialsProviderFactory) stsCredentialsProviderFactory.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use assumed roles in the '" + name + "' profile, the 'sts' service module must "
                                            + "be on the class path.");
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + " ' profile credentials provider.", e);
        }
    }

    @Override
    public String toString() {
        return "Profile(" + name + ")";
    }

    public static class Builder {
        private String name;
        private Map<String, String> properties;
        private ProfilesFile profilesFile;

        private Builder() {}


        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            return this;
        }

        public Builder profilesFile(ProfilesFile profilesFile) {
            this.profilesFile = profilesFile;
            return this;
        }

        public Profile build() {
            return new Profile(this);
        }
    }
}
