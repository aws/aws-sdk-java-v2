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
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.internal.ProfileSection;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * Utility class to load {@link #credentialsProvider()} configured in a profile.
 */
@SdkInternalApi
public final class ProfileCredentialsUtils {
    private static final String STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.sts.internal.StsProfileCredentialsProviderFactory";
    private static final String SSO_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.sso.auth.SsoProfileCredentialsProviderFactory";
    private static final String LOGIN_PROFILE_CREDENTIALS_PROVIDER_FACTORY =
        "software.amazon.awssdk.services.signin.auth.LoginProfileCredentialsProviderFactory";

    /**
     * The profile file containing {@code profile}.
     */
    private final ProfileFile profileFile;
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

    public ProfileCredentialsUtils(ProfileFile profileFile,
                                   Profile profile,
                                   Function<String, Optional<Profile>> credentialsSourceResolver) {
        this.profileFile = Validate.paramNotNull(profileFile, "profileFile");
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
        return credentialsProviderWithFeatureID(children).map(CredentialsWithFeatureId::provider);
    }

    /**
     * Internal method that returns both the credentials provider and its feature ID.
     */
    private Optional<CredentialsWithFeatureId> credentialsProviderWithFeatureID(Set<String> children) {
        if (properties.containsKey(ProfileProperty.ROLE_ARN) && properties.containsKey(ProfileProperty.WEB_IDENTITY_TOKEN_FILE)) {
            return Optional.of(roleAndWebIdentityTokenProfileCredentialsProvider());
        }

        if (properties.containsKey(ProfileProperty.SSO_ROLE_NAME)
            || properties.containsKey(ProfileProperty.SSO_ACCOUNT_ID)
            || properties.containsKey(ProfileProperty.SSO_REGION)
            || properties.containsKey(ProfileProperty.SSO_START_URL)
            || properties.containsKey(ProfileSection.SSO_SESSION.getPropertyKeyName())) {
            return Optional.of(ssoProfileCredentialsProvider());
        }

        if (properties.containsKey(ProfileProperty.ROLE_ARN)) {
            boolean hasSourceProfile = properties.containsKey(ProfileProperty.SOURCE_PROFILE);
            boolean hasCredentialSource = properties.containsKey(ProfileProperty.CREDENTIAL_SOURCE);
            Validate.validState(!(hasSourceProfile && hasCredentialSource),
                                "Invalid profile file: profile has both %s and %s.",
                                ProfileProperty.SOURCE_PROFILE, ProfileProperty.CREDENTIAL_SOURCE);

            if (hasSourceProfile) {
                return Optional.of(roleAndSourceProfileBasedProfileCredentialsProvider(children));
            }

            if (hasCredentialSource) {
                return Optional.of(roleAndCredentialSourceBasedProfileCredentialsProvider());
            }
        }

        if (properties.containsKey(ProfileProperty.LOGIN_SESSION)) {
            return Optional.of(loginProfileCredentialsProvider());
        }

        if (properties.containsKey(ProfileProperty.CREDENTIAL_PROCESS)) {
            return Optional.of(credentialProcessCredentialsProvider());
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
    private CredentialsWithFeatureId basicProfileCredentialsProvider() {
        requireProperties(ProfileProperty.AWS_ACCESS_KEY_ID,
                          ProfileProperty.AWS_SECRET_ACCESS_KEY);
        
        String featureId = BusinessMetricFeatureId.CREDENTIALS_PROFILE.value();
        AwsCredentials credentials = AwsBasicCredentials.builder()
                                                        .accessKeyId(properties.get(ProfileProperty.AWS_ACCESS_KEY_ID))
                                                        .secretAccessKey(properties.get(ProfileProperty.AWS_SECRET_ACCESS_KEY))
                                                        .accountId(properties.get(ProfileProperty.AWS_ACCOUNT_ID))
                                                        .providerName(featureId)
                                                        .build();
        
        return new CredentialsWithFeatureId(StaticCredentialsProvider.create(credentials), featureId);
    }

    /**
     * Load a set of session credentials that have been configured in this profile.
     */
    private CredentialsWithFeatureId sessionProfileCredentialsProvider() {
        requireProperties(ProfileProperty.AWS_ACCESS_KEY_ID,
                          ProfileProperty.AWS_SECRET_ACCESS_KEY,
                          ProfileProperty.AWS_SESSION_TOKEN);
        
        String featureId = BusinessMetricFeatureId.CREDENTIALS_PROFILE.value();
        AwsCredentials credentials = AwsSessionCredentials.builder()
                                                          .accessKeyId(properties.get(ProfileProperty.AWS_ACCESS_KEY_ID))
                                                          .secretAccessKey(properties.get(ProfileProperty.AWS_SECRET_ACCESS_KEY))
                                                          .sessionToken(properties.get(ProfileProperty.AWS_SESSION_TOKEN))
                                                          .accountId(properties.get(ProfileProperty.AWS_ACCOUNT_ID))
                                                          .providerName(featureId)
                                                          .build();
        
        return new CredentialsWithFeatureId(StaticCredentialsProvider.create(credentials), featureId);
    }

    private CredentialsWithFeatureId credentialProcessCredentialsProvider() {
        requireProperties(ProfileProperty.CREDENTIAL_PROCESS);

        String featureId = BusinessMetricFeatureId.CREDENTIALS_PROFILE_PROCESS.value();
        AwsCredentialsProvider provider = ProcessCredentialsProvider.builder()
                                         .command(properties.get(ProfileProperty.CREDENTIAL_PROCESS))
                                         .staticAccountId(properties.get(ProfileProperty.AWS_ACCOUNT_ID))
                                         .sourceChain(featureId)
                                         .build();
        
        return new CredentialsWithFeatureId(provider, featureId);
    }

    /**
     * Create the SSO credentials provider based on the related profile properties.
     */
    private CredentialsWithFeatureId ssoProfileCredentialsProvider() {
        validateRequiredPropertiesForSsoCredentialsProvider();
        boolean isLegacy = isLegacySsoConfiguration();
        String featureId = isLegacy ?
                        BusinessMetricFeatureId.CREDENTIALS_PROFILE_SSO_LEGACY.value() :
                        BusinessMetricFeatureId.CREDENTIALS_PROFILE_SSO.value();

        AwsCredentialsProvider provider = ssoCredentialsProviderFactory().create(
            ProfileProviderCredentialsContext.builder()
                                             .profile(profile)
                                             .profileFile(profileFile)
                                             .sourceChain(featureId)
                                             .build());
        
        return new CredentialsWithFeatureId(provider, featureId);
    }

    private void validateRequiredPropertiesForSsoCredentialsProvider() {
        requireProperties(ProfileProperty.SSO_ACCOUNT_ID,
                          ProfileProperty.SSO_ROLE_NAME);

        if (!properties.containsKey(ProfileSection.SSO_SESSION.getPropertyKeyName())) {
            requireProperties(ProfileProperty.SSO_REGION, ProfileProperty.SSO_START_URL);
        }
    }

    private boolean isLegacySsoConfiguration() {
        return !properties.containsKey(ProfileSection.SSO_SESSION.getPropertyKeyName());
    }

    /**
     * Create the SSO credentials provider based on the related profile properties.
     */
    private CredentialsWithFeatureId loginProfileCredentialsProvider() {
        AwsCredentialsProvider provider = loginCredentialsProviderFactory().create(
            ProfileProviderCredentialsContext.builder()
                                             .profile(profile)
                                             .profileFile(profileFile)
                                             .sourceChain(BusinessMetricFeatureId.CREDENTIALS_PROFILE_LOGIN.value())
                                             .build());

        return new CredentialsWithFeatureId(provider, BusinessMetricFeatureId.CREDENTIALS_PROFILE_LOGIN.value());
    }

    private CredentialsWithFeatureId roleAndWebIdentityTokenProfileCredentialsProvider() {
        requireProperties(ProfileProperty.ROLE_ARN, ProfileProperty.WEB_IDENTITY_TOKEN_FILE);

        String featureId = BusinessMetricFeatureId.CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN.value();
        String roleArn = properties.get(ProfileProperty.ROLE_ARN);
        String roleSessionName = properties.get(ProfileProperty.ROLE_SESSION_NAME);
        Path webIdentityTokenFile = Paths.get(properties.get(ProfileProperty.WEB_IDENTITY_TOKEN_FILE));

        WebIdentityTokenCredentialProperties credentialProperties =
            WebIdentityTokenCredentialProperties.builder()
                                                .roleArn(roleArn)
                                                .roleSessionName(roleSessionName)
                                                .webIdentityTokenFile(webIdentityTokenFile)
                                                .sourceChain(featureId)
                                                .build();

        return new CredentialsWithFeatureId(WebIdentityCredentialsUtils.factory().create(credentialProperties), featureId);
    }

    /**
     * Load an assumed-role credentials provider that has been configured in this profile. This will attempt to locate the STS
     * module in order to generate the credentials provider. If it's not available, an illegal state exception will be raised.
     *
     * @param children The child profiles that source credentials from this profile.
     */
    private CredentialsWithFeatureId roleAndSourceProfileBasedProfileCredentialsProvider(Set<String> children) {
        requireProperties(ProfileProperty.SOURCE_PROFILE);

        Validate.validState(!children.contains(name),
                            "Invalid profile file: Circular relationship detected with profiles %s.", children);
        Validate.validState(credentialsSourceResolver != null,
                            "The profile '%s' must be configured with a source profile in order to use assumed roles.",
                            name);

        children.add(name);
        Optional<CredentialsWithFeatureId> sourceResult = credentialsSourceResolver
            .apply(properties.get(ProfileProperty.SOURCE_PROFILE))
            .flatMap(p -> new ProfileCredentialsUtils(profileFile, p, credentialsSourceResolver)
                .credentialsProviderWithFeatureID(children));
        
        if (!sourceResult.isPresent()) {
            throw noSourceCredentialsException();
        }

        CredentialsWithFeatureId source = sourceResult.get();
        String profileMetric = BusinessMetricFeatureId.CREDENTIALS_PROFILE_SOURCE_PROFILE.value();
        String combinedMetrics = profileMetric + "," + source.featureId();

        AwsCredentialsProvider stsProvider = createStsCredentialsProviderWithFeatureID(source.provider(), combinedMetrics);
        return new CredentialsWithFeatureId(stsProvider, combinedMetrics);
    }

    /**
     * Load an assumed-role credentials provider that has been configured in this profile. This will attempt to locate the STS
     * module in order to generate the credentials provider. If it's not available, an illegal state exception will be raised.
     */
    private CredentialsWithFeatureId roleAndCredentialSourceBasedProfileCredentialsProvider() {
        requireProperties(ProfileProperty.CREDENTIAL_SOURCE);

        CredentialSourceType credentialSource = CredentialSourceType.parse(properties.get(ProfileProperty.CREDENTIAL_SOURCE));
        String profileMetric = BusinessMetricFeatureId.CREDENTIALS_PROFILE_NAMED_PROVIDER.value();
        CredentialsWithFeatureId sourceResult = credentialSourceCredentialProvider(credentialSource);
        
        String combinedMetrics = profileMetric + "," + sourceResult.featureId();
        AwsCredentialsProvider stsProvider = createStsCredentialsProviderWithFeatureID(sourceResult.provider(), combinedMetrics);
        return new CredentialsWithFeatureId(stsProvider, combinedMetrics);
    }

    /**
     * Helper method to create STS credentials provider with business metrics.
     */
    private AwsCredentialsProvider createStsCredentialsProviderWithFeatureID(AwsCredentialsProvider sourceProvider,
                                                                           String combinedMetrics) {
        ChildProfileCredentialsProviderFactory.ChildProfileCredentialsRequest request =
            ChildProfileCredentialsProviderFactory.ChildProfileCredentialsRequest.builder()
                .sourceCredentialsProvider(sourceProvider)
                .profile(profile)
                .sourceChain(combinedMetrics)
                .build();
        
        return stsCredentialsProviderFactory().create(request);
    }

    private CredentialsWithFeatureId credentialSourceCredentialProvider(CredentialSourceType credentialSource) {
        switch (credentialSource) {
            case ECS_CONTAINER:
                return new CredentialsWithFeatureId(
                    ContainerCredentialsProvider.builder().build(),
                    BusinessMetricFeatureId.CREDENTIALS_HTTP.value());
            case EC2_INSTANCE_METADATA:
                return new CredentialsWithFeatureId(
                    InstanceProfileCredentialsProvider.builder()
                                                     .profileFile(profileFile)
                                                     .profileName(name)
                                                     .build(),
                    BusinessMetricFeatureId.CREDENTIALS_IMDS.value());
            case ENVIRONMENT:
                return new CredentialsWithFeatureId(
                    AwsCredentialsProviderChain.builder()
                        .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
                        .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .build(),
                    BusinessMetricFeatureId.CREDENTIALS_ENV_VARS.value());
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
     * Simple data class to hold both a credentials provider and its feature ID.
     */
    private static final class CredentialsWithFeatureId {
        private final AwsCredentialsProvider provider;
        private final String featureId;

        CredentialsWithFeatureId(AwsCredentialsProvider provider, String featureId) {
            this.provider = provider;
            this.featureId = featureId;
        }

        AwsCredentialsProvider provider() {
            return provider;
        }

        String featureId() {
            return featureId;
        }
    }

    /**
     * Load the factory that can be used to create the STS credentials provider, assuming it is on the classpath.
     */
    private ChildProfileCredentialsProviderFactory stsCredentialsProviderFactory() {
        try {
            Class<?> stsCredentialsProviderFactory = ClassLoaderHelper.loadClass(STS_PROFILE_CREDENTIALS_PROVIDER_FACTORY,
                    getClass());
            return (ChildProfileCredentialsProviderFactory) stsCredentialsProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use assumed roles in the '" + name + "' profile, the 'sts' service module must "
                                            + "be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + "' profile credentials provider.", e);
        }
    }

    /**
     * Load the factory that can be used to create the SSO credentials provider, assuming it is on the classpath.
     */
    private ProfileCredentialsProviderFactory ssoCredentialsProviderFactory() {
        try {
            Class<?> ssoProfileCredentialsProviderFactory = ClassLoaderHelper.loadClass(SSO_PROFILE_CREDENTIALS_PROVIDER_FACTORY,
                                                                                 getClass());
            return (ProfileCredentialsProviderFactory) ssoProfileCredentialsProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use Sso related properties in the '" + name + "' profile, the 'sso' service "
                                            + "module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + "' profile credentials provider.", e);
        }
    }

    private ProfileCredentialsProviderFactory loginCredentialsProviderFactory() {
        try {
            Class<?> loginProfileCredentialsProviderFactory =
                ClassLoaderHelper.loadClass(LOGIN_PROFILE_CREDENTIALS_PROVIDER_FACTORY, getClass());
            return (ProfileCredentialsProviderFactory) loginProfileCredentialsProviderFactory.getConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("To use login_session property in the '" + name + "' profile, the 'signin' service "
                                            + "module must be on the class path.", e);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create the '" + name + "' profile credentials provider.", e);
        }
    }
}
