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

package software.amazon.awssdk.services.signin.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.utils.Validate;

/**
 * An AuthScheme representing authentication withOAuth 2.0 Demonstrating Proof of Possession (DPoP) header.
 */
@SdkInternalApi
public class DpopAuthScheme implements AuthScheme<DpopIdentity> {
    public static final String SCHEME_NAME = "DPOP";

    private final IdentityProvider<DpopIdentity> identityProvider;

    private DpopAuthScheme(IdentityProvider<DpopIdentity> identityProvider) {
        this.identityProvider = Validate.paramNotNull(identityProvider, "identityProvider");
    }

    public static DpopAuthScheme create(IdentityProvider<DpopIdentity> identityProvider) {
        return new DpopAuthScheme(identityProvider);
    }

    @Override
    public String schemeId() {
        return SCHEME_NAME;
    }

    @Override
    public IdentityProvider<DpopIdentity> identityProvider(IdentityProviders providers) {
        // we don't currently support adding an arbitrary identityProvider as a request level override
        // return the identity provider configured up front instead
        return identityProvider;
    }

    @Override
    public HttpSigner<DpopIdentity> signer() {
        return new DpopSigner();
    }
}