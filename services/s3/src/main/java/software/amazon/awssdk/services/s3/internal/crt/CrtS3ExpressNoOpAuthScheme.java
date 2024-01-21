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

package software.amazon.awssdk.services.s3.internal.crt;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;

/**
 * An implementation of {@link S3ExpressAuthScheme} that returns a noop {@link IdentityProvider}.
 */
@SdkInternalApi
public final class CrtS3ExpressNoOpAuthScheme implements S3ExpressAuthScheme {
    @Override
    public String schemeId() {
        return S3ExpressAuthScheme.SCHEME_ID;
    }

    @Override
    public IdentityProvider<S3ExpressSessionCredentials> identityProvider(IdentityProviders providers) {
        return NoOpIdentityProvider.INSTANCE;
    }

    @Override
    public HttpSigner<S3ExpressSessionCredentials> signer() {
        return NoOpSigner.INSTANCE;
    }

    private static final class NoOpIdentityProvider implements IdentityProvider<S3ExpressSessionCredentials> {
        private static final NoOpIdentityProvider INSTANCE = new NoOpIdentityProvider();

        @Override
        public Class<S3ExpressSessionCredentials> identityType() {
            return S3ExpressSessionCredentials.class;
        }

        @Override
        public CompletableFuture<? extends S3ExpressSessionCredentials> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NoOpSigner implements HttpSigner<S3ExpressSessionCredentials>  {
        private static final NoOpSigner INSTANCE = new NoOpSigner();

        @Override
        public SignedRequest sign(SignRequest<? extends S3ExpressSessionCredentials> request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends S3ExpressSessionCredentials> request) {
            throw new UnsupportedOperationException();
        }
    }
}
