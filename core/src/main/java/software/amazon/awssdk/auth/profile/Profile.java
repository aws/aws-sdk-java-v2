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

public class Profile {
    private static final String STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
            "software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory";
    private static final String PROFILE_PREFIX = "profile ";

    private final String name;
    private final boolean isProfilePrefixed;
    private final Map<String, String> properties;

    public Profile(Builder builder) {
        Validate.paramNotNull(builder.name, "name");
        Validate.paramNotNull(builder.properties, "properties");

        this.name = parseName(builder.name);
        this.isProfilePrefixed = isProfilePrefixed(builder.name);
        this.properties = builder.properties;
    }

    private String parseName(String profileName) {
        return isProfilePrefixed(profileName) ? profileName.substring(PROFILE_PREFIX.length()) : profileName;
    }

    private boolean isProfilePrefixed(String profileName) {
        return profileName.startsWith(PROFILE_PREFIX);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return name;
    }

    public boolean isProfilePrefixed() {
        return isProfilePrefixed;
    }

    public Optional<Region> region() {
        return Optional.ofNullable(properties.get(ProfileProperties.REGION)).map(Region::of);
    }

    public Optional<AwsCredentialsProvider> credentialsProvider() {
        if (properties.containsKey(ProfileProperties.ROLE_ARN)) {
            return Optional.of(roleBasedProfileCredentialsProvider());
        }

        return nonRoleCredentialsProvider();
    }

    private Optional<AwsCredentialsProvider> nonRoleCredentialsProvider() {
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
        requireProperties(ProfileProperties.AWS_ACCESS_KEY_ID,
                          ProfileProperties.AWS_SECRET_ACCESS_KEY);

        AwsCredentialsProvider parentCredentialsProvider = nonRoleCredentialsProvider()
                .orElseThrow(() -> new IllegalStateException("Unexpected: credentials provider could not be created, but "
                                                             + "access and secret keys were configured."));

        ChildProfileCredentialsProviderFactory credentialsProviderFactory = loadStsCredentialsProviderFactory();
        return credentialsProviderFactory.create(parentCredentialsProvider, properties);
    }

    private ChildProfileCredentialsProviderFactory loadStsCredentialsProviderFactory() {
        try {
            Class<?> stsCredentialsProviderFactory = Class.forName(STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY, true,
                                                                   Thread.currentThread().getContextClassLoader());
            return (ChildProfileCredentialsProviderFactory) stsCredentialsProviderFactory.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use assume role profiles, the 'sts' service module must be on the class path.");
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + " ' profile credentials provider.", e);
        }
    }

    public static class Builder {
        private String name;
        private Map<String, String> properties;

        private Builder() {}


        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            return this;
        }

        public Profile build() {
            return new Profile(this);
        }
    }
}
