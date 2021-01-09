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
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProviderFactory;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sso.SsoClient;
import software.amazon.awssdk.services.sso.internal.SsoAccessTokenProvider;
import software.amazon.awssdk.services.sso.model.GetRoleCredentialsRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * An implementation of {@link ProfileCredentialsProviderFactory} that allows users to get SSO role credentials using the startUrl
 * specified in either a {@link Profile} or environment variables.
 */
@SdkProtectedApi
public class SsoProfileCredentialsProviderFactory implements ProfileCredentialsProviderFactory {

    private static final String TOKEN_DIRECTORY = Paths.get(userHomeDirectory(), ".aws", "sso", "cache").toString();

    /**
     * Default method to create the {@link SsoProfileCredentialsProvider} with a {@link SsoAccessTokenProvider}
     * object created with the start url from {@link Profile} or environment variables and the default token file directory.
     */
    public AwsCredentialsProvider create(Profile profile) {
        return create(profile, new SsoAccessTokenProvider(
            generateCachedTokenPath(profile.properties().get(ProfileProperty.SSO_START_URL), TOKEN_DIRECTORY)));
    }

    /**
     * Alternative method to create the {@link SsoProfileCredentialsProvider} with a customized
     * {@link SsoAccessTokenProvider}. This method is only used for testing.
     */
    @SdkTestInternalApi
    public AwsCredentialsProvider create(Profile profile,
                                         SsoAccessTokenProvider tokenProvider) {
        return new SsoProfileCredentialsProvider(profile, tokenProvider);
    }

    /**
     * A wrapper for a {@link SsoCredentialsProvider} that is returned by this factory when {@link #create(Profile)} or
     * {@link #create(Profile, SsoAccessTokenProvider)} is invoked. This wrapper is important because it ensures the parent
     * credentials provider is closed when the sso credentials provider is no longer needed.
     */
    private static final class SsoProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
        private final SsoClient ssoClient;
        private final SsoCredentialsProvider credentialsProvider;

        private SsoProfileCredentialsProvider(Profile profile,
                                              SsoAccessTokenProvider tokenProvider) {
            String ssoAccountId = profile.properties().get(ProfileProperty.SSO_ACCOUNT_ID);
            String ssoRoleName = profile.properties().get(ProfileProperty.SSO_ROLE_NAME);
            String ssoRegion = profile.properties().get(ProfileProperty.SSO_REGION);

            this.ssoClient = SsoClient.builder()
                                      .credentialsProvider(AnonymousCredentialsProvider.create())
                                      .region(Region.of(ssoRegion))
                                      .build();

            GetRoleCredentialsRequest request = GetRoleCredentialsRequest.builder()
                                                                         .accountId(ssoAccountId)
                                                                         .roleName(ssoRoleName)
                                                                         .build();

            Supplier<GetRoleCredentialsRequest> supplier = () -> request.toBuilder()
                                                                        .accessToken(tokenProvider.resolveAccessToken()).build();


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
    }
}
