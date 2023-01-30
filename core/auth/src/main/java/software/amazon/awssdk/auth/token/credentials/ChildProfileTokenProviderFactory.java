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

package software.amazon.awssdk.auth.token.credentials;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;

/**
 * A factory for {@link SdkTokenProvider} that are derived from properties as defined in he given profile.
 *
 * Currently, this is used to allow a {@link Profile} configured with start_url and sso_region to configure a token
 * provider via the 'software.amazon.awssdk.services.ssooidc.internal.SsooidcTokenProviderFactory', assuming ssooidc is on the
 * classpath.
 */
@FunctionalInterface
@SdkProtectedApi
public interface ChildProfileTokenProviderFactory {

    /**
     * Create a token provider for the provided profile.
     *
     * @param profileFile The ProfileFile from which the Profile was derived.
     * @param profile The profile that should be used to load the configuration necessary to create the token provider.
     * @return The token provider with the properties derived from the source profile.
     */
    SdkTokenProvider create(ProfileFile profileFile, Profile profile);
}
