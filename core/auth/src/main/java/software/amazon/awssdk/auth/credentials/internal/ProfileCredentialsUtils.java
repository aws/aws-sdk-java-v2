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

package software.amazon.awssdk.auth.credentials.internal;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.ChildProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
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
        if (properties.containsKey(ProfileProperty.ROLE_ARN)) {
            boolean hasSourceProfile = properties.containsKey(ProfileProperty.SOURCE_PROFILE);
            boolean hasCredentialSource = properties.containsKey(ProfileProperty.CREDENTIAL_SOURCE);
            boolean hasWebIdentityTokenFile = properties.containsKey(ProfileProperty.WEB_IDENTITY_TOKEN_FILE);
            boolean hasRoleArn = properties.containsKey(ProfileProperty.ROLE_ARN);
            Validate.validState(!(hasSourceProfile && hasCredentialSource),
                                "Invalid profile file: profile has both %s and %s.",
                                ProfileProperty.SOURCE_PROFILE, ProfileProperty.CREDENTIAL_SOURCE);

            if (hasWebIdentityTokenFile && hasRoleArn) {
                return Optional.ofNullable(roleAndWebIdentityTokenProfileCredentialsProvider());
            }

            if (hasSourceProfile) {
                return Optional.ofNullable(roleAndSourceProfileBasedProfileCredentialsProvider(children));
            }

            if (hasCredentialSource) {
                return Optional.ofNullable(roleAndCredentialSourceBasedProfileCredentialsProvider());
            }
        }

        if (properties.containsKey(ProfileProperty.CREDENTIAL_PROCESS)) {
            return Optional.ofNullable(credentialProcessCredentialsProvider());
        }

        if (properties.containsKey(ProfileProperty.AWS_SESSION_TOKEN)) {
            return Optional.of(sessionProfileCredentialsProvider());
        }

        if (properties.containsKey(ProfileProperty.AWS_ACCESS_KEY_ID)) {
            return Optional.of(basicProfileCredentialsProvider());
        }

        return Optional.empty();
    }

    /**
     * Load a basic set of credentials that have been configured in this profile.
     */
    private AwsCredentialsProvider basicProfileCredentialsProvider() {
        requireProperties(ProfileProperty.AWS_ACCESS_KEY_ID,
                          ProfileProperty.AWS_SECRET_ACCESS_KEY);
        AwsCredentials credentials = AwsBasicCredentials.create(properties.get(ProfileProperty.AWS_ACCESS_KEY_ID),
                                                                     properties.get(ProfileProperty.AWS_SECRET_ACCESS_KEY));
        return StaticCredentialsProvider.create(credentials);
    }

    /**
     * Load a set of session credentials that have been configured in this profile.
     */
    private AwsCredentialsProvider sessionProfileCredentialsProvider() {
        requireProperties(ProfileProperty.AWS_ACCESS_KEY_ID,
                          ProfileProperty.AWS_SECRET_ACCESS_KEY,
                          ProfileProperty.AWS_SESSION_TOKEN);
        AwsCredentials credentials = AwsSessionCredentials.create(properties.get(ProfileProperty.AWS_ACCESS_KEY_ID),
                                                                  properties.get(ProfileProperty.AWS_SECRET_ACCESS_KEY),
                                                                  properties.get(ProfileProperty.AWS_SESSION_TOKEN));
        return StaticCredentialsProvider.create(credentials);
    }

    private AwsCredentialsProvider credentialProcessCredentialsProvider() {
        requireProperties(ProfileProperty.CREDENTIAL_PROCESS);

        return ProcessCredentialsProvider.builder()
                                         .command(properties.get(ProfileProperty.CREDENTIAL_PROCESS))
                                         .build();
    }

    private AwsCredentialsProvider roleAndWebIdentityTokenProfileCredentialsProvider() {
        requireProperties(ProfileProperty.ROLE_ARN, ProfileProperty.WEB_IDENTITY_TOKEN_FILE);

        String roleArn = properties.get(ProfileProperty.ROLE_ARN);
        String roleSessionName = properties.get(ProfileProperty.ROLE_SESSION_NAME);
        Path webIdentityTokenFile = Paths.get(properties.get(ProfileProperty.WEB_IDENTITY_TOKEN_FILE));

        WebIdentityTokenCredentialProperties credentialProperties =
            WebIdentityTokenCredentialProperties.builder()
                                                .roleArn(roleArn)
                                                .roleSessionName(roleSessionName)
                                                .webIdentityTokenFile(webIdentityTokenFile)
                                                .build();

        return WebIdentityCredentialsUtils.factory().create(credentialProperties);
    }

    /**
     * Load an assumed-role credentials provider that has been configured in this profile. This will attempt to locate the STS
     * module in order to generate the credentials provider. If it's not available, an illegal state exception will be raised.
     *
     * @param children The child profiles that source credentials from this profile.
     */
    private AwsCredentialsProvider roleAndSourceProfileBasedProfileCredentialsProvider(Set<String> children) {
        requireProperties(ProfileProperty.SOURCE_PROFILE);

        Validate.validState(!children.contains(name),
                            "Invalid profile file: Circular relationship detected with profiles %s.", children);
        Validate.validState(credentialsSourceResolver != null,
                            "The profile '%s' must be configured with a source profile in order to use assumed roles.", name);

        children.add(name);
        AwsCredentialsProvider sourceCredentialsProvider =
            credentialsSourceResolver.apply(properties.get(ProfileProperty.SOURCE_PROFILE))
                                     .flatMap(p -> new ProfileCredentialsUtils(p, credentialsSourceResolver)
                                         .credentialsProvider(children))
                                     .orElseThrow(this::noSourceCredentialsException);

        return stsCredentialsProviderFactory().create(sourceCredentialsProvider, profile);
    }

    /**
     * Load an assumed-role credentials provider that has been configured in this profile. This will attempt to locate the STS
     * module in order to generate the credentials provider. If it's not available, an illegal state exception will be raised.
     */
    private AwsCredentialsProvider roleAndCredentialSourceBasedProfileCredentialsProvider() {
        requireProperties(ProfileProperty.CREDENTIAL_SOURCE);

        CredentialSourceType credentialSource = CredentialSourceType.parse(properties.get(ProfileProperty.CREDENTIAL_SOURCE));
        AwsCredentialsProvider credentialsProvider = credentialSourceCredentialProvider(credentialSource);
        return stsCredentialsProviderFactory().create(credentialsProvider, profile);
    }

    private AwsCredentialsProvider credentialSourceCredentialProvider(CredentialSourceType credentialSource) {
        switch (credentialSource) {
            case ECS_CONTAINER:
                return ContainerCredentialsProvider.builder().build();
            case EC2_INSTANCE_METADATA:
                return InstanceProfileCredentialsProvider.create();
            case ENVIRONMENT:
                return AwsCredentialsProviderChain.builder()
                    .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                    .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build();
            default:
                throw noSourceCredentialsException();
        }
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
                                     + "credentials configured.", name, properties.get(ProfileProperty.SOURCE_PROFILE));
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
