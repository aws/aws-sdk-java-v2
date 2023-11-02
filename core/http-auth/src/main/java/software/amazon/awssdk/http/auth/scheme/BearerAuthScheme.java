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

package software.amazon.awssdk.http.auth.scheme;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.internal.scheme.DefaultBearerAuthScheme;
import software.amazon.awssdk.http.auth.signer.BearerHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * The <a href="https://smithy.io/2.0/spec/authentication-traits.html#httpbearerauth-trait">smithy.api#httpBearerAuth</a> auth
 * scheme, which uses a {@link TokenIdentity} and {@link BearerHttpSigner}.
 */
@SdkPublicApi
public interface BearerAuthScheme extends AuthScheme<TokenIdentity> {

    /**
     * The scheme ID for this interface.
     */
    String SCHEME_ID = "smithy.api#httpBearerAuth";

    /**
     * Get a default implementation of a {@link BearerAuthScheme}
     */
    static BearerAuthScheme create() {
        return DefaultBearerAuthScheme.create();
    }

    /**
     * Retrieve the {@link TokenIdentity} based {@link IdentityProvider} associated with this authentication scheme.
     */
    @Override
    IdentityProvider<TokenIdentity> identityProvider(IdentityProviders providers);

    /**
     * Retrieve the {@link BearerHttpSigner} associated with this authentication scheme.
     */
    @Override
    BearerHttpSigner signer();
}
