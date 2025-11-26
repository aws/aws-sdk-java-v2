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

package software.amazon.awssdk.services.signin.auth;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProviderFactory;
import software.amazon.awssdk.auth.credentials.ProfileProviderCredentialsContext;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.services.signin.SigninClient;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * An implementation of {@link ProfileCredentialsProviderFactory} that allows users to get login credentials using the
 * login_session specified in a {@link Profile}.
 */
@SdkProtectedApi
public class LoginProfileCredentialsProviderFactory implements ProfileCredentialsProviderFactory {

    /**
     * Default method to create the {@link LoginProfileCredentialsProviderFactory} object created
     * with the login_session from {@link Profile}  in the {@link ProfileProviderCredentialsContext}.
     */
    @Override
    public AwsCredentialsProvider create(ProfileProviderCredentialsContext profileProviderCredentialsContext) {
        return new LoginProfileCredentialsProvider(profileProviderCredentialsContext);
    }

    private static class LoginProfileCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {
        private final LoginCredentialsProvider credentialsProvider;
        private final SigninClient signinClient;

        private LoginProfileCredentialsProvider(ProfileProviderCredentialsContext credentialsContext) {
            Profile profile = credentialsContext.profile();
            String loginSession = profile.property(ProfileProperty.LOGIN_SESSION)
                                         .orElseThrow(() -> new IllegalArgumentException("login_session property is required"));

            this.signinClient = SigninClient.create();
            this.credentialsProvider = LoginCredentialsProvider
                .builder()
                .loginSession(loginSession)
                .signinClient(signinClient)
                .sourceChain(credentialsContext.sourceChain())
                .build();

        }

        @Override
        public AwsCredentials resolveCredentials() {
            return this.credentialsProvider.resolveCredentials();
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(credentialsProvider, null);
            IoUtils.closeQuietly(signinClient, null);
        }

    }
}
