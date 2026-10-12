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

package software.amazon.awssdk.core;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Validate;


///
///  A container for the identity resolver, signer and auth option that we selected for use with this service call attempt.
/// ## The Hierarchy
/// ```
/// IDENTITY_PROVIDERS (IdentityProviders)
///     └── contains multiple IdentityProvider<T> instances
///             e.g., IdentityProvider<AwsCredentialsIdentity> for AWS credentials
///             e.g., IdentityProvider<TokenIdentity> for bearer tokens
///
/// AUTH_SCHEMES (Map<String, AuthScheme<?>>)
///     └── each AuthScheme knows:
///             - which IdentityProvider type it needs
///             - which HttpSigner to use
///
/// SELECTED_AUTH_SCHEME (SelectedAuthScheme<T>)
///     └── the chosen auth scheme, containing:
///             - identity: CompletableFuture<T>  ← the resolved identity!
///             - signer: HttpSigner<T>
///             - authSchemeOption: AuthSchemeOption
/// ```
@SdkProtectedApi
public final class SelectedAuthScheme<T extends Identity> {
    private final CompletableFuture<? extends T> identity;
    private final HttpSigner<T> signer;
    private final AuthSchemeOption authSchemeOption;

    public SelectedAuthScheme(CompletableFuture<? extends T> identity,
                              HttpSigner<T> signer,
                              AuthSchemeOption authSchemeOption) {
        this.identity = Validate.paramNotNull(identity, "identity");
        this.signer = Validate.paramNotNull(signer, "signer");
        this.authSchemeOption = Validate.paramNotNull(authSchemeOption, "authSchemeOption");
    }

    public CompletableFuture<? extends T> identity() {
        return identity;
    }

    public HttpSigner<T> signer() {
        return signer;
    }

    public AuthSchemeOption authSchemeOption() {
        return authSchemeOption;
    }
}
