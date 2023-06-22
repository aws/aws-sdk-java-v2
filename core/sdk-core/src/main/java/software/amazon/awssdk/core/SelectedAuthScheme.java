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

// TODO: Figure out the right module/package for this. auth? http-auth?

package software.amazon.awssdk.core;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * A container for the identity resolver, signer and auth option that we selected for use with this service call attempt.
 */
@SdkProtectedApi
public final class SelectedAuthScheme<T extends Identity> {

    private IdentityProvider<T> identityProvider;
    private HttpSigner<T> signer;
    private AuthSchemeOption authSchemeOption;

    public SelectedAuthScheme(IdentityProvider<T> identityProvider,
                              HttpSigner<T> signer,
                              AuthSchemeOption authSchemeOption) {
        this.identityProvider = Validate.paramNotNull(identityProvider, "identityProvider");
        this.signer = Validate.paramNotNull(signer, "signer");
        this.authSchemeOption = Validate.paramNotNull(authSchemeOption, "authSchemeOption");
    }

    public IdentityProvider<T> identityProvider() {
        return identityProvider;
    }

    public HttpSigner<T> signer() {
        return signer;
    }

    public AuthSchemeOption authSchemeOption() {
        return authSchemeOption;
    }
}
