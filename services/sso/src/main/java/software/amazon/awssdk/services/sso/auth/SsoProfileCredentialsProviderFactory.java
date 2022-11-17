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

package software.amazon.awssdk.services.sso.auth;

import static software.amazon.awssdk.services.sso.internal.SsoTokenFileUtils.generateCachedTokenPath;
import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.auth.token.credentials.ProfileTokenProvider;
import software.amazon.awssdk.auth.token.credentials.SdkToken;
import software.amazon.awssdk.auth.token.credentials.SdkTokenProvider;
import software.amazon.awssdk.auth.token.internal.LazyTokenProvider;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.profiles.internal.ProfileSection;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sso.SsoClient;
import software.amazon.awssdk.services.sso.internal.SsoAccessTokenProvider;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link ProfileCredentialsProviderFactory} that allows users to get SSO role credentials using the startUrl
 * specified in either a {@link Profile} or environment variables.
 */
@SdkProtectedApi
public class SsoProfileCredentialsProviderFactory implements ProfileCredentialsProviderFactory {

    private static final String TOKEN_DIRECTORY = Paths.get(userHomeDirectory(), ".aws", "sso", "cache").toString();

    private static final String MISSING_PROPERTY_ERROR_FORMAT = "'%s' must be set to use role-based credential loading in the "
                                                                + "'%s' profile.";

    /**
     * Default method to create the {@link SsoProfileCredentialsProvider} with a {@link SsoAccessTokenProvider} object created
     * with the start url from {@link Profile}  in the {@link ProfileProviderCredentialsContext} or environment variables and the
     * default token file directory.
     */
    @Override
    public AwsCredentialsProvider create(ProfileProviderCredentialsContext credentialsContext) {
        return new SsoProfileCredentialsProvider(credentialsContext.profile(),
                                                 credentialsContext.profileFile(),
                                                 sdkTokenProvider(credentialsContext.profile(),
                                                                  credentialsContext.profileFile()));
    }

    /**
     * Alternative method to create the {@link SsoProfileCredentialsProvider} with a customized {@link SsoAccessTokenProvider}.
     * This method is only used for testing.
     */
    @SdkTestInternalApi
    public AwsCredentialsProvider create(Profile profile, ProfileFile profileFile,
                                         SdkTokenProvider tokenProvider) {
        return new SsoProfileCredentialsProvider(profile, profileFile, tokenProvider);
    }

    /**
     * A wrapper for a {@link SsoCredentialsProvider} that is returned by this factory when {@link
     * #create(ProfileProviderCredentialsContext)} * or {@link #create(Profile, ProfileFile, SdkTokenProvider)} is invoked. This
     * wrapper is important because it ensures * the parent credentials provider is closed when the sso credentials provider is no
     * longer needed.
     */
    private static final class SsoProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
        private final SsoClient ssoClient;
        private final SsoCredentialsProvider credentialsProvider;

        private SsoProfileCredentialsProvider(Profile profile, ProfileFile profileFile,
                                              SdkTokenProvider tokenProvider) {
            String ssoAccountId = profile.properties().get(ProfileProperty.SSO_ACCOUNT_ID);
            String ssoRoleName = profile.properties().get(ProfileProperty.SSO_ROLE_NAME);
            String ssoRegion = regionFromProfileOrSession(profile, profileFile);

            this.ssoClient = SsoClient.builder()
                                      .credentialsProvider(AnonymousCredentialsProvider.create())
                                      .region(Region.of(ssoRegion))
                                      .build();

            GetRoleCredentialsRequest request = GetRoleCredentialsRequest.builder()
                                                                         .accountId(ssoAccountId)
                                                                         .roleName(ssoRoleName)
                                                                         .build();
            SdkToken sdkToken = tokenProvider.resolveToken();
            Validate.paramNotNull(sdkToken, "Token provided by the TokenProvider is null");
            Supplier<GetRoleCredentialsRequest> supplier = () -> request.toBuilder()
                                                                        .accessToken(sdkToken.token())
                                                                        .build();


            this.credentialsProvider = SsoCredentialsProvider.builder()
                                                             .ssoClient(ssoClient)
                                                             .refreshRequest(supplier)
                                                             .build();
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return this.credentialsProvider.resolveCredentials();
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(credentialsProvider, null);
            IoUtils.closeQuietly(ssoClient, null);
        }

        private static String regionFromProfileOrSession(Profile profile, ProfileFile profileFile) {
            Optional<String> ssoSession = profile.property(ProfileSection.SSO_SESSION.getPropertyKeyName());
            String profileRegion = profile.properties().get(ProfileProperty.SSO_REGION);
            return ssoSession.isPresent() ?
                   propertyFromSsoSession(ssoSession.get(), profileFile, ProfileProperty.SSO_REGION) :
                   profileRegion;
        }

        private static String propertyFromSsoSession(String sessionName, ProfileFile profileFile, String propertyName) {
            Profile ssoProfile = ssoSessionInProfile(sessionName, profileFile);
            return requireProperty(ssoProfile, propertyName);
        }

        private static String requireProperty(Profile profile, String requiredProperty) {
            return profile.property(requiredProperty)
                          .orElseThrow(() -> new IllegalArgumentException(String.format(MISSING_PROPERTY_ERROR_FORMAT,
                                                                                        requiredProperty, profile.name())));
        }
    }

    private static Profile ssoSessionInProfile(String sessionName, ProfileFile profileFile) {
        Profile ssoProfile =
            profileFile.getSection(
                ProfileSection.SSO_SESSION.getSectionTitle(),
                sessionName).orElseThrow(() -> new IllegalArgumentException(
                "Sso-session section not found with sso-session title " + sessionName + "."));
        return ssoProfile;
    }

    private static SdkTokenProvider sdkTokenProvider(Profile profile, ProfileFile profileFile) {
        Optional<String> ssoSession = profile.property(ProfileSection.SSO_SESSION.getPropertyKeyName());


        if (ssoSession.isPresent()) {
            Profile ssoSessionProfileFile = ssoSessionInProfile(ssoSession.get(), profileFile);

            validateCommonProfileProperties(profile, ssoSessionProfileFile, ProfileProperty.SSO_REGION);
            validateCommonProfileProperties(profile, ssoSessionProfileFile, ProfileProperty.SSO_START_URL);

            return LazyTokenProvider.create(
                () -> ProfileTokenProvider.builder()
                                          .profileFile(() -> profileFile)
                                          .profileName(profile.name())
                                          .build());
        } else {
            return new SsoAccessTokenProvider(generateCachedTokenPath(
                profile.properties().get(ProfileProperty.SSO_START_URL), TOKEN_DIRECTORY));

        }
    }

    private static void validateCommonProfileProperties(Profile profile, Profile ssoSessionProfileFile, String propertyName) {
        profile.property(propertyName).ifPresent(
            property ->
                Validate.isTrue(property.equalsIgnoreCase(ssoSessionProfileFile.property(propertyName).get()),
                                "Profile " + profile.name() + " and Sso-session " + ssoSessionProfileFile.name() + " has "
                                + "different " + propertyName + "."));

    }

}
