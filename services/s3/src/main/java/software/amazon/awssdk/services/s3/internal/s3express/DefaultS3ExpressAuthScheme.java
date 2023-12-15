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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;

@SdkInternalApi
public final class DefaultS3ExpressAuthScheme implements S3ExpressAuthScheme {
    private volatile S3ExpressIdentityCache cache;
    private final Object cacheLock = new Object();

    private DefaultS3ExpressAuthScheme() {
    }

    public static DefaultS3ExpressAuthScheme create() {
        return new DefaultS3ExpressAuthScheme();
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
        return new DefaultS3ExpressIdentityProvider(getOrCreateCache(), baseIdentityProvider);
    }

    @Override
    public HttpSigner<S3ExpressSessionCredentials> signer() {
        return DefaultS3ExpressHttpSigner.create();
    }

    private S3ExpressIdentityCache getOrCreateCache() {
        if (cache == null) {
            synchronized (cacheLock) {
                if (cache == null) {
                    cache = initCache();
                }
            }
        }
        return cache;
    }

    private S3ExpressIdentityCache initCache() {
        return S3ExpressIdentityCache.create();
    }
}
