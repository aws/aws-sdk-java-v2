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

package software.amazon.awssdk.http.auth.internal.scheme;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.signer.BearerHttpSigner;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.TokenIdentity;

/**
 * A default implementation of {@link BearerAuthScheme}.
 */
@SdkInternalApi
public final class DefaultBearerAuthScheme implements BearerAuthScheme {
    private static final DefaultBearerAuthScheme DEFAULT = new DefaultBearerAuthScheme();
    private static final BearerHttpSigner DEFAULT_SIGNER = BearerHttpSigner.create();

    /**
     * Returns an instance of the {@link DefaultBearerAuthScheme}.
     */
    public static DefaultBearerAuthScheme create() {
        return DEFAULT;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public IdentityProvider<TokenIdentity> identityProvider(IdentityProviders providers) {
        return providers.identityProvider(TokenIdentity.class);
    }

    @Override
    public BearerHttpSigner signer() {
        return DEFAULT_SIGNER;
    }
}
