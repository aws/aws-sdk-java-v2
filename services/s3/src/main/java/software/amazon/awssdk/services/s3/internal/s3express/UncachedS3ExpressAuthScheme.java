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

package software.amazon.awssdk.services.s3.internal.s3express;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.identity.SdkIdentityProperty;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;

/**
 * An {@link S3ExpressAuthScheme} that calls CreateSession on every identity resolution, with no cache and no background
 * refresh. Used by the presigner so each presigned URL gets a session with its full lifetime.
 */
@SdkInternalApi
public final class UncachedS3ExpressAuthScheme implements S3ExpressAuthScheme {
    private static final UncachedS3ExpressAuthScheme INSTANCE = new UncachedS3ExpressAuthScheme();

    private UncachedS3ExpressAuthScheme() {
    }

    public static UncachedS3ExpressAuthScheme create() {
        return INSTANCE;
    }

    @Override
    public String schemeId() {
        return SCHEME_ID;
    }

    @Override
    public IdentityProvider<S3ExpressSessionCredentials> identityProvider(IdentityProviders providers) {
        IdentityProvider<AwsCredentialsIdentity> baseIdentityProvider = providers.identityProvider(AwsCredentialsIdentity.class);
        if (baseIdentityProvider == null) {
            throw new IllegalStateException("Could not find a provider for AwsCredentialsIdentity");
        }
        return new UncachedIdentityProvider(baseIdentityProvider);
    }

    @Override
    public HttpSigner<S3ExpressSessionCredentials> signer() {
        return DefaultS3ExpressHttpSigner.create();
    }

    private static final class UncachedIdentityProvider implements IdentityProvider<S3ExpressSessionCredentials> {
        private final IdentityProvider<AwsCredentialsIdentity> baseIdentityProvider;

        private UncachedIdentityProvider(IdentityProvider<AwsCredentialsIdentity> baseIdentityProvider) {
            this.baseIdentityProvider = baseIdentityProvider;
        }

        @Override
        public Class<S3ExpressSessionCredentials> identityType() {
            return S3ExpressSessionCredentials.class;
        }

        @Override
        public CompletableFuture<S3ExpressSessionCredentials> resolveIdentity(ResolveIdentityRequest request) {
            String bucket = request.property(S3ExpressAuthSchemeProvider.BUCKET);
            SdkClient client = request.property(SdkIdentityProperty.SDK_CLIENT);

            return baseIdentityProvider.resolveIdentity(request).thenApply(identity -> {
                S3ExpressIdentityKey key = S3ExpressIdentityKey.builder()
                                                               .bucket(bucket)
                                                               .client(client)
                                                               .identity(identity)
                                                               .build();
                return S3ExpressSessionCredentials.fromSessionResponse(S3ExpressIdentityCache.createSession(key));
            });
        }
    }
}
