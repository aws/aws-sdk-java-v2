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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * A default implementation of {@link NoAuthAuthScheme}. This implementation always:
 *
 * <ul>
 *     <li>Returns an {@link IdentityProvider} that always returns the same static instance that implements the
 *     {@link AnonymousIdentity} interface</li>
 *     <li>Returns an {@link HttpSigner} that returns the same request given in the signing request.</li>
 * </ul>
 */
@SdkInternalApi
public final class DefaultNoAuthAuthScheme implements NoAuthAuthScheme {
    private static final DefaultNoAuthAuthScheme DEFAULT = new DefaultNoAuthAuthScheme();
    private static final IdentityProvider<AnonymousIdentity> DEFAULT_IDENTITY_PROVIDER = noAuthIdentityProvider();
    private static final HttpSigner<AnonymousIdentity> DEFAULT_SIGNER = noAuthSigner();
    private static final AnonymousIdentity ANONYMOUS_IDENTITY = anonymousIdentity();

    /**
     * Returns an instance of the {@link NoAuthAuthScheme}.
     */
    public static NoAuthAuthScheme create() {
        return DEFAULT;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public IdentityProvider<AnonymousIdentity> identityProvider(IdentityProviders providers) {
        return DEFAULT_IDENTITY_PROVIDER;
    }

    @Override
    public HttpSigner<AnonymousIdentity> signer() {
        return DEFAULT_SIGNER;
    }

    private static IdentityProvider<AnonymousIdentity> noAuthIdentityProvider() {
        return new IdentityProvider<AnonymousIdentity>() {

            @Override
            public Class identityType() {
                return AnonymousIdentity.class;
            }

            @Override
            public CompletableFuture<AnonymousIdentity> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.completedFuture(ANONYMOUS_IDENTITY);
            }
        };
    }

    private static HttpSigner<AnonymousIdentity> noAuthSigner() {
        return new HttpSigner<AnonymousIdentity>() {
            @Override
            public SignedRequest sign(SignRequest<? extends AnonymousIdentity> request) {
                return SignedRequest.builder()
                                    .request(request.request())
                                    .payload(request.payload().orElse(null))
                                    .build();
            }

            @Override
            public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<?
                extends AnonymousIdentity> request) {
                return CompletableFuture.completedFuture(
                    AsyncSignedRequest.builder()
                                      .request(request.request())
                                      .payload(request.payload().orElse(null))
                                      .build()
                );
            }
        };
    }

    private static AnonymousIdentity anonymousIdentity() {
        return new AnonymousIdentity() {
        };
    }
}
